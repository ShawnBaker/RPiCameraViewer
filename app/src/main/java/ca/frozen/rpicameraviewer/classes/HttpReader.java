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

			URL url = new URL("http://" + source.address + ":" + source.port + "/stream/video.h264");
			//inputStream = new BufferedInputStream(url.openStream());

			// get the connection
			http = (HttpURLConnection) url.openConnection();
			http.setRequestProperty("Connection", "close");
			http.connect();

			// get the input stream
			inputStream = http.getInputStream();

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
		return http != null;
	}

	//******************************************************************************
	// close
	//******************************************************************************
	public void close()
	{
		if (reader != null)
		{
			reader.interrupt();
			reader = null;
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
