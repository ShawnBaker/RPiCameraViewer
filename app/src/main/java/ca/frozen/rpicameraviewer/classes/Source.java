// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Source implements Comparable, Parcelable
{
	// local constants
	private final static String TAG = "Source";

	// instance variables
	public ConnectionType connectionType;
	public String address;
	public int port;
	public int width;
	public int height;
	public int fps;
	public int bps;

	//******************************************************************************
	// Source
	//******************************************************************************
	public Source(ConnectionType connectionType, String address, int port)
	{
		initialize();
		this.connectionType = connectionType;
		this.address = address;
		this.port = port;
		//Log.d(TAG, "init: " + toString());
	}

	//******************************************************************************
	// Source
	//******************************************************************************
	public Source(Parcel in)
	{
		readFromParcel(in);
		//Log.d(TAG, "parcel: " + toString());
	}

	//******************************************************************************
	// Source
	//******************************************************************************
	public Source(Source source)
	{
		connectionType = source.connectionType;
		address = source.address;
		port = source.port;
		width = source.width;
		height = source.height;
		fps = source.fps;
		bps = source.bps;
		//Log.d(TAG, "source: " + toString());
	}

	//******************************************************************************
	// Source
	//******************************************************************************
	public Source(JSONObject obj)
	{
		try
		{
			connectionType = intToConType(obj.getInt("connection_type"));
			address = obj.getString("address");
			port = obj.getInt("port");
			width = obj.getInt("width");
			height = obj.getInt("height");
			fps = obj.getInt("fps");
			bps = obj.getInt("bps");
		}
		catch (JSONException ex)
		{
			initialize();
		}
		//Log.d(TAG, "json: " + toString());
	}

	//******************************************************************************
	// initialize
	//******************************************************************************
	private void initialize()
	{
		connectionType = ConnectionType.RawTcpIp;
		address = "";
		port = 0;
		width = 0;
		height = 0;
		fps = 0;
		bps = 0;
	}

	//******************************************************************************
	// combine
	//******************************************************************************
	public Source combine(Source source)
	{
		Source newSource = new Source(source.connectionType, source.address.isEmpty() ? address : source.address,
										(source.port != 0) ? source.port : port);
		newSource.width = (source.width != 0) ? source.width : width;
		newSource.height = (source.height != 0) ? source.height : height;
		newSource.fps = (source.fps != 0) ? source.fps : fps;
		newSource.bps = (source.bps != 0) ? source.bps : bps;
		return newSource;
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(conTypeToInt(connectionType));
		dest.writeString(address);
		dest.writeInt(port);
		dest.writeInt(width);
		dest.writeInt(height);
		dest.writeInt(fps);
		dest.writeInt(bps);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		connectionType = intToConType(in.readInt());
		address = in.readString();
		port = in.readInt();
		width = in.readInt();
		height = in.readInt();
		fps = in.readInt();
		bps = in.readInt();
	}

	//******************************************************************************
	// describeContents
	//******************************************************************************
	public int describeContents()
	{
		return 0;
	}

	//******************************************************************************
	// Parcelable.Creator
	//******************************************************************************
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
	{
		public Source createFromParcel(Parcel in)
		{
			return new Source(in);
		}
		public Source[] newArray(int size)
		{
			return new Source[size];
		}
	};

	//******************************************************************************
	// equals
	//******************************************************************************
	@Override
	public boolean equals(Object otherSource)
	{
		return compareTo(otherSource) == 0;
	}

	//******************************************************************************
	// compareTo
	//******************************************************************************
	@Override
	public int compareTo(Object otherSource)
	{
		int result = 1;
		if (otherSource instanceof Source)
		{
			Source source = (Source) otherSource;
			result = conTypeToInt(connectionType) - conTypeToInt(source.connectionType);
			if (result == 0)
			{
				result = address.compareTo(source.address);
				if (result == 0)
				{
					result = port - source.port;
					if (result == 0)
					{
						result = width - source.width;
						if (result == 0)
						{
							result = height - source.height;
							if (result == 0)
							{
								result = fps - source.fps;
								if (result == 0)
								{
									result = bps - source.bps;
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	//******************************************************************************
	// toString
	//******************************************************************************
	@Override
	public String toString()
	{
		return connectionType + "," + address + "," + port + "," + width + "x" + height + "," + fps + "," + bps;
	}

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("connection_type", conTypeToInt(connectionType));
			obj.put("address", address);
			obj.put("port", port);
			obj.put("width", width);
			obj.put("height", height);
			obj.put("fps", fps);
			obj.put("bps", bps);
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	//******************************************************************************
	// intToConType
	//******************************************************************************
	private ConnectionType intToConType(int n)
	{
		if (n == 3) return ConnectionType.RawHttp;
		if (n == 2) return ConnectionType.RawMulticast;
		return ConnectionType.RawTcpIp;
	}

	//******************************************************************************
	// conTypeToInt
	//******************************************************************************
	private int conTypeToInt(ConnectionType m)
	{
		if (m == ConnectionType.RawHttp) return 3;
		if (m == ConnectionType.RawMulticast) return 2;
		return 1;
	}

	//******************************************************************************
	// ConnectionType
	//******************************************************************************
	public enum ConnectionType
	{
		RawTcpIp,
		RawHttp,
		RawMulticast
	}
}
