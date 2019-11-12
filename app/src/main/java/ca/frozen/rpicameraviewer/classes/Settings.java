// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.os.Parcel;
import android.os.Parcelable;
//import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;

public class Settings implements Parcelable
{
	// public constants
	public final static int MIN_TIMEOUT = 100;
	public final static int MAX_TIMEOUT = 5000;
	public final static int DEFAULT_TIMEOUT = 500;
	public final static int MIN_PORT = 1024;
	public final static int MAX_PORT = 65535;
	public final static int DEFAULT_PORT = 5000;
	public final static int DEFAULT_FRAME_ROTATION = 0;

	// local constants
	//private final static String TAG = "Settings";

	// instance variables
	public String cameraName;
	public boolean showAllCameras;
	public int scanTimeout;
	public int port;
	public int frameRotation;

	//******************************************************************************
	// Settings
	//******************************************************************************
	Settings()
	{
		initialize();
		//Log.d(TAG, "init: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	private Settings(Parcel in)
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
		scanTimeout = settings.scanTimeout;
		port = settings.port;
		frameRotation = settings.frameRotation;
		//Log.d(TAG, "settings: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	Settings(JSONObject obj)
	{
		// get the common values
		try
		{
			cameraName = obj.getString("cameraName");
			showAllCameras = obj.getBoolean("showAllCameras");
			scanTimeout = obj.getInt("scanTimeout");
			frameRotation = obj.getInt("frameRotation");
		}
		catch (JSONException ex)
		{
			initialize();
			return;
		}

		// get the new values
		try
		{
			port = obj.getInt("port");
		}
		catch (JSONException ex)
		{
			// get the old values
			try
			{
				JSONObject source = obj.getJSONObject("rawTcpIpSource");
				port = source.getInt("port");
			}
			catch (JSONException ex2)
			{
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
		cameraName = App.getStr(R.string.camera);
		showAllCameras = false;
		scanTimeout = DEFAULT_TIMEOUT;
		port = DEFAULT_PORT;
		frameRotation = DEFAULT_FRAME_ROTATION;
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(cameraName);
		dest.writeInt(showAllCameras ? 1 : 0);
		dest.writeInt(scanTimeout);
		dest.writeInt(port);
		dest.writeInt(frameRotation);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		cameraName = in.readString();
		showAllCameras = in.readInt() != 0;
		scanTimeout = in.readInt();
		port = in.readInt();
		frameRotation = in.readInt();
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
	// toString
	//******************************************************************************
	@Override
	public String toString()
	{
		return cameraName + "," + showAllCameras + "," + scanTimeout + "," + port + "," + frameRotation;
	}

	//******************************************************************************
	// toJson
	//******************************************************************************
	JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();

			obj.put("cameraName", cameraName);
			obj.put("showAllCameras", showAllCameras);
			obj.put("scanTimeout", scanTimeout);
			obj.put("port", port);
			obj.put("frameRotation", frameRotation);

			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}

		return null;
	}
}
