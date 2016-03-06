// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;

public class Settings
{
	// local constants
	private final static String TAG = "Settings";

	// instance variables
	public String cameraName;
	public boolean showAllCameras;
	public Source source;

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
	public Settings(Source source)
	{
		initialize();
		this.source = source;
		//Log.d(TAG, "init/source: " + toString());
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
		cameraName = App.getStr(R.string.camera);
		showAllCameras = false;
		source = new Source();
		source.connectionType = Source.ConnectionType.RawTcpIp;
		source.port = App.getInt(R.integer.default_port);
		source.fps = App.getInt(R.integer.default_fps);
		source.bps = App.getInt(R.integer.default_bps);
	}

	//******************************************************************************
	// toString
	//******************************************************************************
	@Override
	public String toString()
	{
		return cameraName + "," + showAllCameras + "," + source.toString();
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
			obj.put("source", source.toJson());
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
