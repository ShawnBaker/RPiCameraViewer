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
	public Multicast multicast;
	public String address;
	public int port;
	public int width;
	public int height;
	public int fps;
	public int bps;

	//******************************************************************************
	// Source
	//******************************************************************************
	public Source()
	{
		initialize();
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
	public Source(JSONObject obj)
	{
		try
		{
			multicast = intToMulti(obj.getInt("multicast"));
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
		multicast = Multicast.UseDefault;
		address = "";
		port = 0;
		width = 0;
		height = 0;
		fps = 0;
		bps = 0;
	}

	//******************************************************************************
	// compound
	//******************************************************************************
	public Source compound(Source source)
	{
		Source newSource = new Source();
		newSource.multicast = (source.multicast != Source.Multicast.UseDefault) ? source.multicast : multicast;
		newSource.address = source.address.isEmpty() ? address : source.address;
		newSource.port = (source.port != 0) ? source.port : port;
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
		dest.writeInt(multiToInt(multicast));
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
		multicast = intToMulti(in.readInt());
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
			result = multiToInt(multicast) - multiToInt(source.multicast);
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
		return multicast + "," + address + "," + port + "," + width + "x" + height + "," + fps + "," + bps;
	}

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("multicast", multiToInt(multicast));
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
	// intToMulti
	//******************************************************************************
	private Multicast intToMulti(int n)
	{
		if (n == 2) return Multicast.Off;
		if (n == 1) return Multicast.On;
		return Multicast.UseDefault;
	}

	//******************************************************************************
	// multiToInt
	//******************************************************************************
	private int multiToInt(Multicast m)
	{
		if (m == Multicast.Off) return 2;
		if (m == Multicast.On) return 1;
		return 0;
	}

	//******************************************************************************
	// Multicast
	//******************************************************************************
	public enum Multicast
	{
		UseDefault,
		On,
		Off
	}
}
