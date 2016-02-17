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
	public NamePosition cameraNamePosition;
	public int cameraNameColor;
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
			cameraNamePosition = intToPos(obj.getInt("cameraNamePosition"));
			cameraNameColor = obj.getInt("cameraNameColor");
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
		cameraNamePosition = NamePosition.BottomLeft;
		cameraNameColor = App.getClr(R.color.accent);
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
		return cameraName + "," + cameraNamePosition + "," + cameraNameColor + "," +
				showAllCameras + "," + source.toString();
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
			obj.put("cameraNamePosition", posToInt(cameraNamePosition));
			obj.put("cameraNameColor", cameraNameColor);
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

	//******************************************************************************
	// intToPos
	//******************************************************************************
	private NamePosition intToPos(int n)
	{
		if (n == 4) return NamePosition.BottomRight;
		if (n == 3) return NamePosition.BottomLeft;
		if (n == 2) return NamePosition.TopRight;
		if (n == 1) return NamePosition.TopLeft;
		return NamePosition.Hidden;
	}

	//******************************************************************************
	// posToInt
	//******************************************************************************
	private int posToInt(NamePosition p)
	{
		if (p == NamePosition.BottomRight) return 4;
		if (p == NamePosition.BottomLeft) return 3;
		if (p == NamePosition.TopRight) return 2;
		if (p == NamePosition.TopLeft) return 1;
		return 0;
	}

	//******************************************************************************
	// NamePosition
	//******************************************************************************
	public enum NamePosition
	{
		Hidden,
		TopLeft,
		TopRight,
		BottomLeft,
		BottomRight
	}
}
