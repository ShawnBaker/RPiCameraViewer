// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.os.Parcel;
import android.os.Parcelable;
//import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Camera implements Comparable, Parcelable
{
	// local constants
	//private final static String TAG = "Camera";

	// instance variables
	public String network;
	public String name;
	public String address;
	public int port;

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(String network, String address, int port)
	{
		this.network = network;
		this.name = "";
		this.address = address;
		this.port = port;
		//Log.d(TAG, "net/addr/port: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(String name, int port)
	{
		network = Utils.getNetworkName();
		this.name = name;
		address = "";
		this.port = port;
		//Log.d(TAG, "name/port: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(Camera camera)
	{
		network = camera.network;
		name = camera.name;
		address = camera.name;
		port = camera.port;
		//Log.d(TAG, "camera: " + toString());
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
	public Camera(JSONObject obj)
	{
		// get the common values
		try
		{
			network = obj.getString("network");
			name = obj.getString("name");
		}
		catch (JSONException ex)
		{
			initialize();
			return;
		}

		// get the new values
		try
		{
			address = obj.getString("address");
			port = obj.getInt("port");
		}
		catch (JSONException ex)
		{
			// get the old values
			try
			{
				JSONObject source = obj.getJSONObject("source");
				address = source.getString("address");
				port = source.getInt("port");
			}
			catch (JSONException ex2)
			{
				address = "";
				port = Settings.DEFAULT_PORT;
			}
		}
		//Log.d(TAG, "json: " + toString());
	}

	//******************************************************************************
	// initialize
	//******************************************************************************
	private void initialize()
	{
		network = Utils.getNetworkName();
		name = Utils.getDefaultCameraName();
		address = Utils.getBaseIpAddress();
		port = Utils.getDefaultPort();
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(network);
		dest.writeString(name);
		dest.writeString(address);
		dest.writeInt(port);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		network = in.readString();
		name = in.readString();
		address = in.readString();
		port = in.readInt();
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
				result = address.compareTo(camera.address);
				if (result == 0)
				{
					result = port - camera.port;
					if (result == 0)
					{
						result = network.compareTo(camera.network);
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
        return name + "," + network + "," + address + "," + port;
    }

	//******************************************************************************
	// toJson
	//******************************************************************************
	JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("network", network);
			obj.put("name", name);
			obj.put("address", address);
			obj.put("port", port);
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
