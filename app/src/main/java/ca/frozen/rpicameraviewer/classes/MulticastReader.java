// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MulticastReader extends RawH264Reader
{
	// local constants
	private final static String TAG = "MulticastReader";
	private final static int PACKET_SIZE = 2048;
	private final static int NUM_START_PACKETS = 10;
	private final static int MAX_PACKETS = 100;

	// instance variables
	private MulticastSocket socket = null;
	private Queue<DatagramPacket> emptyPackets, fullPackets;
	private ReaderThread reader;
	private int numPackets = 0;

	//******************************************************************************
	// MulticastReader
	//******************************************************************************
	public MulticastReader(Source source)
	{
		super(source);
		try
		{
			// create the initial set of packets
			fullPackets = new ConcurrentLinkedQueue<DatagramPacket>();
			emptyPackets = new ConcurrentLinkedQueue<DatagramPacket>();
			for (int i = 0; i < NUM_START_PACKETS; i++)
			{
				byte[] buffer = new byte[PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer, PACKET_SIZE);
				emptyPackets.add(packet);
			}
			numPackets = NUM_START_PACKETS;

			// join the multicast
			InetAddress addr = InetAddress.getByName(source.address);
			socket = new MulticastSocket(source.port);
			socket.joinGroup(addr);

			// create and start the reader thread
			reader = new ReaderThread();
			reader.setPriority(reader.getPriority() + 1);
			reader.start();
		}
		catch (Exception ex) {}
	}

	//******************************************************************************
	// read
	//******************************************************************************
	public int read(byte[] buffer)
	{
		int len = 0;
		while (true)
		{
			try
			{
				DatagramPacket packet = fullPackets.peek();
				if (packet != null)
				{
					int packetLen = packet.getLength();
					if (len + packetLen > buffer.length)
					{
						break;
					}
					packet = fullPackets.poll();
					System.arraycopy(packet.getData(), packet.getOffset(), buffer, len, packetLen);
					len += packetLen;
					emptyPackets.add(packet);
					//Log.d(TAG, String.format("read:  %d  %d", packetLen, len));
				}
				else
				{
					Thread.sleep(1);
				}
			}
			catch (Exception ex) {}
		}
		return len;
	}

	//******************************************************************************
	// isConnected
	//******************************************************************************
	public boolean isConnected()
	{
		return (socket != null) ? socket.isConnected() : false;
	}

	//******************************************************************************
	// close
	//******************************************************************************
	public void close()
	{
		if (reader != null)
		{
			try
			{
				reader.interrupt();
			}
			catch (Exception ex) {}
			reader = null;
		}
		if (socket != null)
		{
			try
			{
				socket.close();
			}
			catch (Exception ex) {}
			socket = null;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// ReaderThread
	////////////////////////////////////////////////////////////////////////////////
	private class ReaderThread extends Thread
	{
		//******************************************************************************
		// run
		//******************************************************************************
		@Override
		public void run()
		{
			while (!Thread.interrupted())
			{
				try
				{
					DatagramPacket packet = emptyPackets.poll();
					if (packet == null && numPackets < MAX_PACKETS)
					{
						byte[] buffer = new byte[PACKET_SIZE];
						packet = new DatagramPacket(buffer, PACKET_SIZE);
						numPackets++;
						//Log.d(TAG, String.format("read:  numPackets  =  %d", numPackets));
					}
					if (packet != null)
					{
						socket.receive(packet);
						fullPackets.add(packet);
					}
					else
					{
						//Log.d(TAG, "reader:  sleeping");
						Thread.sleep(1);
					}
				}
				catch (Exception ex)
				{
				}
			}
		}
	}
}
