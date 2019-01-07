// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;

public class Utils
{
	// public constants
	public final static String IpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
	public final static String HostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

	// local variables
	private static Settings settings = null;
	private static List<Camera> cameras = null;

	//******************************************************************************
	// loadData
	//******************************************************************************
	public static void loadData()
	{
		boolean save = false;
		SharedPreferences preferences = null;

		// load the settings
		if (settings == null)
		{
			Log.info("load settings");
			preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
			settings = null;
			try
			{
				String settingsString = preferences.getString(App.getStr(R.string.settings_settings), "");
				if (!settingsString.isEmpty())
				{
					settings = new Settings(new JSONObject(settingsString));
				}
			}
			catch (JSONException e)
			{
			}
			if (settings == null)
			{
				settings = new Settings();
				save = true;
			}
		}

		// load the cameras
		if (cameras == null)
		{
			Log.info("load cameras");
			if (preferences == null)
			{
				preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
			}
			cameras = new ArrayList<>();
			try
			{
				String camerasString = preferences.getString(App.getStr(R.string.settings_cameras), "");
				if (!camerasString.isEmpty())
				{
					JSONArray arr = new JSONArray(camerasString);
					for (int i = 0; i < arr.length(); i++)
					{
						JSONObject obj = arr.getJSONObject(i);
						Camera camera = new Camera(obj);
						cameras.add(camera);
					}
				}
			}
			catch (JSONException e)
			{
				save = true;
			}
			Collections.sort(cameras);
		}

		// save the data if we changed something
		if (save)
		{
			saveData();
		}
	}

	//******************************************************************************
	// reloadData
	//******************************************************************************
	public static void reloadData()
	{
		Log.info("reloadData");
		settings = null;
		cameras = null;
		loadData();
	}

	//******************************************************************************
	// saveData
	//******************************************************************************
	public static void saveData()
	{
		SharedPreferences preferences = null;
		SharedPreferences.Editor editor = null;

		// save the settings
		Log.info("saveData");
		if (settings != null)
		{
			preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
			editor = preferences.edit();
			JSONObject obj = settings.toJson();
			editor.putString(App.getStr(R.string.settings_settings), obj.toString());
		}

		// save the cameras
		if (cameras != null)
		{
			if (preferences == null)
			{
				preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
				editor = preferences.edit();
			}
			JSONArray arr = new JSONArray();
			for (Camera camera : cameras)
			{
				arr.put(camera.toJson());
			}
			editor.putString(App.getStr(R.string.settings_cameras), arr.toString());
		}

		// commit the data
		if (editor != null)
		{
			editor.apply();
		}
	}

	//******************************************************************************
	// getSettings
	//******************************************************************************
	public static Settings getSettings()
	{
		loadData();
		return settings;
	}

	//******************************************************************************
	// getSettings
	//******************************************************************************
	public static void setSettings(Settings newSettings)
	{
		loadData();
		settings = newSettings;
	}

	//******************************************************************************
	// getDefaultCameraName
	//******************************************************************************
	public static String getDefaultCameraName()
	{
		loadData();
		return settings.cameraName;
	}

	//******************************************************************************
	// getDefaultPort
	//******************************************************************************
	public static int getDefaultPort()
	{
		loadData();
		return settings.port;
	}

	//******************************************************************************
	// getNetworkCameras
	//******************************************************************************
	public static List<Camera> getNetworkCameras(String network, boolean includeHostnames)
	{
		loadData();

		// find the cameras for the network
		List<Camera> networkCameras = new ArrayList<>();
		for (Camera camera : cameras)
		{
			if ((isIpAddress(camera.address) && camera.network.equals(network)) ||
				(includeHostnames && isHostname(camera.address)))
			{
				networkCameras.add(camera);
			}
		}

		return networkCameras;
	}

	//******************************************************************************
	// getCameras
	//******************************************************************************
	public static List<Camera> getCameras()
	{
		loadData();
		return cameras;
	}

	//******************************************************************************
	// findCamera
	//******************************************************************************
	public static Camera findCamera(String name)
	{
		loadData();

		for (Camera camera : cameras)
		{
			if (camera.name.equals(name))
			{
				return camera;
			}
		}
		return null;
	}

	//******************************************************************************
	// getLocalIpAddress
	//******************************************************************************
	public static String getLocalIpAddress()
	{
		String address = "";
		WifiManager manager = (WifiManager)App.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (manager.isWifiEnabled())
		{
			WifiInfo wifiInfo = manager.getConnectionInfo();
			if (wifiInfo != null)
			{
				NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
				if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR)
				{
					int ip = wifiInfo.getIpAddress();
					address = Formatter.formatIpAddress(ip);
				}
			}
		}
		return address;
	}

	//******************************************************************************
	// getBaseIpAddress
	//******************************************************************************
	public static String getBaseIpAddress()
	{
		String ipAddress = getLocalIpAddress();
		int i = ipAddress.lastIndexOf('.');
		return ipAddress.substring(0, i + 1);
	}

	//******************************************************************************
	// getFullAddress
	//******************************************************************************
	public static String getFullAddress(String baseAddress, int port)
	{
		String address = baseAddress;
		int i = address.indexOf("://");
		i = address.indexOf("/", (i != -1) ? (i + 3) : 0);
		if (i != -1)
		{
			address = address.substring(0, i) + ":" + port + address.substring(i);
		}
		else
		{
			address += ":" + port;
		}
		return address;
	}

	//******************************************************************************
	// isIpAddress
	//******************************************************************************
	public static boolean isIpAddress(String address)
	{
		return address.matches(IpAddressRegex);
	}

	//******************************************************************************
	// isHostname
	//******************************************************************************
	public static boolean isHostname(String address)
	{
		return address.matches(HostnameRegex);
	}

	//******************************************************************************
	// getNumber
	//******************************************************************************
	public static int getNumber(EditText edit)
	{
		int value = 0;
		String text = edit.getText().toString().trim();
		if (text.length() > 0)
		{
			try
			{
				value = Integer.parseInt(text);
			}
			catch (Exception ex)
			{
				value = -1;
			}
		}
		return value;
	}

	//******************************************************************************
	// getNetworkName
	//******************************************************************************
	public static String getNetworkName()
	{
		String ssid = "";
		WifiManager manager = (WifiManager)App.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (manager.isWifiEnabled())
		{
			WifiInfo wifiInfo = manager.getConnectionInfo();
			if (wifiInfo != null)
			{
				NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
				if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR)
				{
					ssid = wifiInfo.getSSID();
					if (ssid == null) ssid = "";
					ssid = ssid.replaceAll("^\"|\"$", "");
					if (ssid.equals("<unknown ssid>"))
					{
						ssid = App.getStr(R.string.unknown_network);
					}
				}
			}
		}
		return ssid;
	}

	//******************************************************************************
	// connectedToNetwork
	//******************************************************************************
	public static boolean connectedToNetwork()
	{
		String name = getNetworkName();
		return name != null && !name.isEmpty();
	}

	//******************************************************************************
	// getMaxCameraNumber
	//******************************************************************************
	public static int getMaxCameraNumber(List<Camera> cameras)
	{
		// get the maximum number from the existing camera names
		int max = 0;
		String defaultName = Utils.getDefaultCameraName() + " ";
		for (int i = 0; i < cameras.size(); i++)
		{
			Camera camera = cameras.get(i);
			if (camera.name.startsWith(defaultName))
			{
				int num = -1;
				try
				{
					num = Integer.parseInt(camera.name.substring(defaultName.length()));
				}
				catch (Exception ex) {}
				if (num != -1)
				{
					if (num > max) max = num;
				}
			}
		}
		return max;
	}

	//******************************************************************************
	// getNextCameraName
	//******************************************************************************
	public static String getNextCameraName(List<Camera> cameras)
	{
		return getDefaultCameraName() + " " + (getMaxCameraNumber(cameras) + 1);
	}

	//******************************************************************************
	// saveImage
	//******************************************************************************
	public static String saveImage(ContentResolver contentResolver, Bitmap source,
								   String title, String description)
	{
		File snapshot;
		Uri url;
		try
		{
			// get/create the snapshots folder
			File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			File rpi = new File(pictures, App.getStr(R.string.app_name));
			if (!rpi.exists())
			{
				rpi.mkdirs();
			}

			// save the file within the snapshots folder
			snapshot = new File(rpi, title);
			OutputStream stream = new FileOutputStream(snapshot);
			source.compress(Bitmap.CompressFormat.JPEG, 90, stream);
			stream.flush();
			stream.close();

			// create the content values
			ContentValues values = new ContentValues();
			values.put(MediaStore.Images.Media.TITLE, title);
			values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
			if (description != null)
			{
				values.put(MediaStore.Images.Media.DESCRIPTION, description);
			}
			values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
			values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
			values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
			values.put(MediaStore.Images.ImageColumns.BUCKET_ID, snapshot.toString().toLowerCase(Locale.US).hashCode());
			values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, snapshot.getName().toLowerCase(Locale.US));
			values.put("_data", snapshot.getAbsolutePath());

			// insert the image into the database
			url = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		}
		catch (Exception ex)
		{
			return null;
		}

		// return the URL
		return (url != null) ? url.toString() : null;
	}

	//******************************************************************************
	// getNavigationBarWidth
	//******************************************************************************
	public static int getNavigationBarWidth(Context context)
	{
		Resources resources = context.getResources();
		int id = resources.getIdentifier("navigation_bar_height_landscape", "dimen", "android");
		return (id > 0) ?resources.getDimensionPixelSize(id) : 0;
	}

	//******************************************************************************
	// getStatusBarHeight
	//******************************************************************************
	public static int getStatusBarHeight(Context context)
	{
		Resources resources = context.getResources();
		int id = resources.getIdentifier("status_bar_height", "dimen", "android");
		return (id > 0) ?resources.getDimensionPixelSize(id) : 0;
	}

	//******************************************************************************
	// initLogFile
	//******************************************************************************
	public static void initLogFile(String tag)
	{
		String baseFileName = App.getStr(R.string.app_name).replaceAll("\\s+", "");
		Log.init(App.getContext(), tag, baseFileName);
		Log.info("onCreate");
	}
}
