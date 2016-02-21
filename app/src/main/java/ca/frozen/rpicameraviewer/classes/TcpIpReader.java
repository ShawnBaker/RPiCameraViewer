// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpIpReader extends RawH264Reader
{
	// local constants
	private final static String TAG = "TcpIpReader";
	private final static int SOCKET_TIMEOUT = 200;

	// instance variables
	private Socket socket = null;
	private InputStream inputStream = null;

	//******************************************************************************
	// TcpIpReader
	//******************************************************************************
	public TcpIpReader(Source source)
	{
		super(source);
		try
		{
			socket = new Socket();
			InetSocketAddress socketAddress = new InetSocketAddress(source.address, source.port);
			socket.connect(socketAddress, SOCKET_TIMEOUT);
			inputStream = socket.getInputStream();
		}
		catch (Exception ex) {}
	}

	//******************************************************************************
	// read
	//******************************************************************************
	public int read(byte[] buffer)
	{
		try
		{
			return (inputStream != null) ? inputStream.read(buffer) : 0;
		}
		catch (IOException ex)
		{
			return 0;
		}
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
		try
		{
			if (inputStream != null)
			{
				inputStream.close();
				inputStream = null;
			}
			if (socket != null)
			{
				socket.close();
				socket = null;
			}
		}
		catch (IOException ex) {}
	}
}
