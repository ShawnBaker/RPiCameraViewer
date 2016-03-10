// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;

public class Settings implements Parcelable
{
	// local constants
	private final static String TAG = "Settings";

	// instance variables
	public String cameraName;
	public boolean showAllCameras;
	public Source rawTcpIpSource;
	public Source rawHttpSource;
	public Source rawMulticastSource;

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings()
	{
		initialize();
		//Log.d(TAG, "init: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings(Parcel in)
	{
		readFromParcel(in);
		//Log.d(TAG, "parcel: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings(Settings settings)
	{
		cameraName = settings.cameraName;
		showAllCameras = settings.showAllCameras;
		rawTcpIpSource = new Source(settings.rawTcpIpSource);
		rawHttpSource = new Source(settings.rawHttpSource);
		rawMulticastSource = new Source(settings.rawMulticastSource);
		//Log.d(TAG, "settings: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings(JSONObject obj)
	{
		try
		{
			cameraName = obj.getString("cameraName");
			showAllCameras = obj.getBoolean("showAllCameras");
			rawTcpIpSource = new Source(obj.getJSONObject("rawTcpIpSource"));
			rawHttpSource = new Source(obj.getJSONObject("rawHttpSource"));
			rawMulticastSource = new Source(obj.getJSONObject("rawMulticastSource"));
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
		cameraName = App.getStr(R.string.camera);
		showAllCameras = false;

		rawTcpIpSource = new Source(Source.ConnectionType.RawTcpIp, "", App.getInt(R.integer.default_tcpip_port));
		rawTcpIpSource.fps = App.getInt(R.integer.default_fps);
		rawTcpIpSource.bps = App.getInt(R.integer.default_bps);

		rawHttpSource = new Source(Source.ConnectionType.RawHttp, "", App.getInt(R.integer.default_http_port));
		rawHttpSource.fps = App.getInt(R.integer.default_fps);
		rawHttpSource.bps = App.getInt(R.integer.default_bps);

		rawMulticastSource = new Source(Source.ConnectionType.RawMulticast, App.getStr(R.string.default_multicast_address),
										App.getInt(R.integer.default_multicast_port));
		rawMulticastSource.fps = App.getInt(R.integer.default_fps);
		rawMulticastSource.bps = App.getInt(R.integer.default_bps);
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(cameraName);
		dest.writeInt(showAllCameras ? 1 : 0);
		dest.writeParcelable(rawTcpIpSource, flags);
		dest.writeParcelable(rawHttpSource, flags);
		dest.writeParcelable(rawMulticastSource, flags);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		cameraName = in.readString();
		showAllCameras = in.readInt() != 0;
		rawTcpIpSource = in.readParcelable(Source.class.getClassLoader());
		rawHttpSource = in.readParcelable(Source.class.getClassLoader());
		rawMulticastSource = in.readParcelable(Source.class.getClassLoader());
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
		public Settings createFromParcel(Parcel in)
		{
			return new Settings(in);
		}
		public Settings[] newArray(int size)
		{
			return new Settings[size];
		}
	};

	//******************************************************************************
	// getSource
	//******************************************************************************
	public Source getSource(Source.ConnectionType type)
	{
		switch (type)
		{
			case RawTcpIp:
				return rawTcpIpSource;
			case RawHttp:
				return rawHttpSource;
			case RawMulticast:
				return rawMulticastSource;
		}
		return null;
	}

	//******************************************************************************
	// toString
	//******************************************************************************
	@Override
	public String toString()
	{
		return cameraName + "," + showAllCameras + "," + rawTcpIpSource.toString() +
				"," + rawHttpSource.toString() + "," + rawMulticastSource.toString();
	}

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("cameraName", cameraName);
			obj.put("showAllCameras", showAllCameras);
			obj.put("rawTcpIpSource", rawTcpIpSource.toJson());
			obj.put("rawHttpSource", rawHttpSource.toJson());
			obj.put("rawMulticastSource", rawMulticastSource.toJson());
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
