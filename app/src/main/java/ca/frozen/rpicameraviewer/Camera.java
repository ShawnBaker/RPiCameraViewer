package ca.frozen.rpicameraviewer;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Camera implements Comparable, Parcelable
{
	// local constants
	private final static String TAG = "Camera";

	// instance variables
    private String name;
    private String address;
    private int port;
	private String network;

	//******************************************************************************
	// Camera
	//******************************************************************************
    public Camera(String name, String address, int port)
    {
        this.name = name;
        this.address = address;
        this.port = port;
		network = Utils.getNetworkName();
		Log.d(TAG, "values: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(Parcel in)
	{
		readFromParcel(in);
		Log.d(TAG, "parcel: " + toString());
	}

	//******************************************************************************
	// Camera
	//******************************************************************************
    public Camera()
    {
		initialize();
		Log.d(TAG, "init: " + toString());
    }

	//******************************************************************************
	// Camera
	//******************************************************************************
	public Camera(JSONObject obj)
	{
		try
		{
			name = obj.getString("name");
			address = obj.getString("address");
			port = obj.getInt("port");
			network = obj.getString("network");
		}
		catch (JSONException ex)
		{
			initialize();
		}
		Log.d(TAG, "json: " + toString());
	}

	//******************************************************************************
	// getName, setName
	//******************************************************************************
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

	//******************************************************************************
	// getAddress, setAddress
	//******************************************************************************
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

	//******************************************************************************
	// getPort, setPort
	//******************************************************************************
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

	//******************************************************************************
	// getNetwork, setNetwork
	//******************************************************************************
	public String getNetwork() { return network; }
	public void setNetwork(String network) { this.network = network; }

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(name);
        dest.writeString(address);
        dest.writeInt(port);
		dest.writeString(network);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		name = in.readString();
		address = in.readString();
		port = in.readInt();
		network = in.readString();
	}

	//******************************************************************************
	// initialize
	//******************************************************************************
	private void initialize()
	{
		name = Utils.getDefaultCameraName();
		address = Utils.getBaseIpAddress();
		port = Utils.getDefaultPort();
		network = Utils.getNetworkName();
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
        if (otherCamera instanceof Camera)
        {
            Camera camera = (Camera)otherCamera;
            if (getName().equals(camera.getName()) && getAddress().equals(camera.getAddress()) &&
					getPort() == camera.getPort() && getNetwork().equals(camera.getNetwork()))
            {
                return true;
            }
        }
        return false;
    }

	//******************************************************************************
	// compareTo
	//******************************************************************************
    @Override
    public int compareTo(Object otherCamera)
    {
        Camera camera = (Camera)otherCamera;
        int result = getName().compareTo(camera.getName());
        if (result == 0)
        {
            result = getAddress().compareTo(camera.getAddress());
            if (result == 0)
            {
                result = getPort() - camera.getPort();
				if (result == 0)
				{
					result = getNetwork().compareTo(camera.getNetwork());
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
        return getName() + ": " + getAddress() + ":" + getPort() + "," + getNetwork();
    }

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("name", getName());
			obj.put("address", getAddress());
			obj.put("port", getPort());
			obj.put("network", getNetwork());
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
