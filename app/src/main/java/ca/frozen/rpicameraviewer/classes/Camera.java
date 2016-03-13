// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Camera implements Comparable, Parcelable
{
	// local constants
	private final static String TAG = "Camera";

	// instance variables
	public String network;
	public String name;
	public Source source;

	//******************************************************************************
	// Camera
	//******************************************************************************
    public Camera(Source.ConnectionType connectionType, String network, String address, int port)
    {
		this.network = network;
        this.name = "";
		this.source = new Source(connectionType, address, port);
		//Log.d(TAG, "address/source: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(String name, Source source)
	{
		network = Utils.getNetworkName();
		this.name = name;
		this.source = new Source(source.connectionType, "", source.port);
		//Log.d(TAG, "name/source: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(Camera camera)
	{
		network = camera.network;
		name = camera.name;
		source = new Source(camera.source);
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
		try
		{
			network = obj.getString("network");
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
		network = Utils.getNetworkName();
		name = Utils.getDefaultCameraName();
		source = new Source(Utils.getSettings().rawTcpIpSource);
		source.address = Utils.getBaseIpAddress();
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(network);
		dest.writeString(name);
		dest.writeParcelable(source, flags);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		network = in.readString();
		name = in.readString();
		source = in.readParcelable(Source.class.getClassLoader());
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
        return name + "," + network + "," + source.toString();
    }

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("network", network);
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
	// getCombinedSource
	//******************************************************************************
	public Source getCombinedSource()
	{
		return Utils.getSettings().getSource(source.connectionType).combine(source);
	}
}
