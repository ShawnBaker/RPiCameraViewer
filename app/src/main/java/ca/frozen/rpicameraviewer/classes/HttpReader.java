// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HttpReader extends RawH264Reader
{
	// local constants
	private final static String TAG = "HttpReader";
	private final static int CONNECT_TIMEOUT = 5000;
	private final static int READ_TIMEOUT = 5000;
	private final static int TEST_CONNECT_TIMEOUT = 200;
	private final static int TEST_READ_TIMEOUT = 200;
	private final static int BLOCK_SIZE = 2048;
	private final static int NUM_START_BLOCKS = 10;
	private final static int MAX_BLOCKS = 100;

	// instance variables
	private HttpURLConnection http = null;
	private InputStream inputStream = null;
	private Queue<Block> emptyBlocks, fullBlocks;
	private ReaderThread reader;
	private int numBlocks = 0;

	//******************************************************************************
	// HttpReader
	//******************************************************************************
	public HttpReader(Source source)
	{
		super(source);
		try
		{
			// create the initial set of packets
			fullBlocks = new ConcurrentLinkedQueue<Block>();
			emptyBlocks = new ConcurrentLinkedQueue<Block>();
			for (int i = 0; i < NUM_START_BLOCKS; i++)
			{
				emptyBlocks.add(new Block());
			}
			numBlocks = NUM_START_BLOCKS;

			// get the connection
			http = getConnection(source.address, source.port, false);

			// get the input stream
			inputStream = http.getInputStream();

			// create and start the reader thread
			reader = new ReaderThread();
			reader.setPriority(reader.getPriority() + 1);
			reader.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
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
				Block block = fullBlocks.peek();
				if (block != null)
				{
					if (len + block.length > buffer.length)
					{
						break;
					}
					block = fullBlocks.poll();
					System.arraycopy(block.buffer, 0, buffer, len, block.length);
					len += block.length;
					emptyBlocks.add(block);
					//Log.d(TAG, String.format("read:  %d  %d", block.length, len));
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
		return reader != null;
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
		if (inputStream != null)
		{
			try
			{
				inputStream.close();
			}
			catch (Exception ex) {}
			inputStream = null;
		}
		if (http != null)
		{
			try
			{
				http.disconnect();
			}
			catch (Exception ex) {}
			http = null;
		}
	}


	//******************************************************************************
	// getConnection
	//******************************************************************************
	public static HttpURLConnection getConnection(String baseAddress, int port, boolean test)
	{
		HttpURLConnection http = null;
		try
		{
			// get the URL
			String address = Utils.getHttpAddress(Utils.getFullAddress(baseAddress, port));
			URL url = new URL(address);

			// get the connection
			http = (HttpURLConnection) url.openConnection();
			http.setConnectTimeout(test ? TEST_CONNECT_TIMEOUT : CONNECT_TIMEOUT);
			http.setReadTimeout(test ? TEST_READ_TIMEOUT : READ_TIMEOUT);
			http.setRequestProperty("Connection", "close");
			http.connect();
		}
		catch (Exception ex)
		{
			http = null;
		}
		return http;
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
					Block block = emptyBlocks.poll();
					if (block == null && numBlocks < MAX_BLOCKS)
					{
						block = new Block();
						numBlocks++;
						//Log.d(TAG, String.format("read:  numBlocks  =  %d", numBlocks));
					}
					if (block != null)
					{
						block.length = inputStream.read(block.buffer);
						fullBlocks.add(block);
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

	////////////////////////////////////////////////////////////////////////////////
	// Block
	////////////////////////////////////////////////////////////////////////////////
	private class Block
	{
		public byte[] buffer;
		public int length;
		
		public Block()
		{
			buffer = new byte[BLOCK_SIZE];
			length = 0;
		}
	}
}
