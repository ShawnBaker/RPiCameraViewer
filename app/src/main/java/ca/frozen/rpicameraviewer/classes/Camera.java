// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.MutableBoolean;

import org.json.JSONException;
import org.json.JSONObject;

public class Camera implements Comparable, Parcelable
{
	// local constants
	private final static String TAG = "Camera";

	// instance variables
	public Network network;
	public String name;
	public Source source;

	//******************************************************************************
	// Camera
	//******************************************************************************
    public Camera(String network, String name, Source source)
    {
		this.network = Utils.getNetwork(network);
        this.name = name;
        this.source = source;
		//Log.d(TAG, "values/source: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(String network, String name, String address)
	{
		this.network = Utils.getNetwork(network);
		this.name = name;
		source = new Source();
		source.address = address;
		//Log.d(TAG, "values/address: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(Parcel in)
	{
		readFromParcel(in);
		//Log.d(TAG, "parcel: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
    public Camera()
    {
		initialize();
		//Log.d(TAG, "init: " + toString());
    }

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(JSONObject obj)
	{
		try
		{
			network = Utils.getNetwork(obj.getString("network"));
			name = obj.getString("name");
			source = new Source(obj.getJSONObject("source"));
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
		network = Utils.getNetwork(Utils.getWifiName());
		name = Utils.getDefaultCameraName();
		source = new Source();
		source.address = Utils.getBaseIpAddress();
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(network.name);
		dest.writeString(name);
		dest.writeParcelable(source, flags);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		network = Utils.getNetwork(in.readString());
		name = in.readString();
		source = (Source) in.readParcelable(Source.class.getClassLoader());
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
		public Camera createFromParcel(Parcel in)
		{
			return new Camera(in);
		}
		public Camera[] newArray(int size)
		{
			return new Camera[size];
		}
	};

	//******************************************************************************
	// equals
	//******************************************************************************
    @Override
    public boolean equals(Object otherCamera)
    {
		return compareTo(otherCamera) == 0;
    }

	//******************************************************************************
	// compareTo
	//******************************************************************************
    @Override
    public int compareTo(Object otherCamera)
    {
		int result = 1;
		if (otherCamera instanceof Camera)
		{
			Camera camera = (Camera) otherCamera;
			result = name.compareTo(camera.name);
			if (result == 0)
			{
				result = source.compareTo(camera.source);
				if (result == 0)
				{
					result = network.compareTo(camera.network);
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
        return name + "," + network.name + "," + source.toString();
    }

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("network", network.name);
			obj.put("name", name);
			obj.put("source", source.toJson());
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	//******************************************************************************
	// getSource
	//******************************************************************************
	public Source getSource()
	{
		return network.getSource().compound(source);
	}

	//******************************************************************************
	// getMulticast
	//******************************************************************************
	public Source.Multicast getMulticast()
	{
		return getSource().multicast;
	}
}
