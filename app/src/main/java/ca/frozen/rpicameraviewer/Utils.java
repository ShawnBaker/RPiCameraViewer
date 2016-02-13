package ca.frozen.rpicameraviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class Utils
{
	//******************************************************************************
	// loadCameras
	//******************************************************************************
	public static List<Camera> loadCameras()
	{
		List<Camera> cameras = new ArrayList<Camera>();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String camerasString = preferences.getString(App.getStr(R.string.settings_cameras), "");
		if (!camerasString.isEmpty())
		{
			try
			{
				JSONArray arr = new JSONArray(camerasString);
				for (int i=0; i < arr.length(); i++)
				{
					JSONObject obj = arr.getJSONObject(i);
					Camera camera = new Camera(obj);
					cameras.add(camera);
				}
			}
			catch (JSONException e)
			{
			}
			Collections.sort(cameras);
		}
		return cameras;
	}

	//******************************************************************************
	// saveCameras
	//******************************************************************************
	public static void saveCameras(List<Camera> cameras)
	{
		JSONArray arr = new JSONArray();
		for (Camera camera : cameras)
		{
			arr.put(camera.toJson());
		}
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(App.getStr(R.string.settings_cameras), arr.toString());
		editor.commit();
	}

	//******************************************************************************
	// findCamera
	//******************************************************************************
	public static Camera findCamera(List<Camera> cameras, String name)
	{
		for (Camera camera : cameras)
		{
			if (camera.getName().equals(name))
			{
				return camera;
			}
		}
		return null;
	}

	//******************************************************************************
	// getDefaultCameraName
	//******************************************************************************
	public static String getDefaultCameraName()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String defaultName = App.getStr(R.string.camera);
		return preferences.getString(App.getStr(R.string.settings_default_camera_name), defaultName);
	}

	//******************************************************************************
	// getDefaultPort
	//******************************************************************************
	public static int getDefaultPort()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		int defaultPort = App.getInt(R.integer.default_port);
		return Integer.parseInt(preferences.getString(App.getStr(R.string.settings_default_port), Integer.toString(defaultPort)));
	}

	//******************************************************************************
	// getLocalIpAddress
	//******************************************************************************
	public static String getLocalIpAddress()
	{
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
					{
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex)
		{
			ex.printStackTrace();
		}
		return null;
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
	// getWifiName
	//******************************************************************************
	public static String getWifiName()
	{
		String ssid = "";
		WifiManager manager = (WifiManager)App.getContext().getSystemService(Context.WIFI_SERVICE);
		if (manager.isWifiEnabled())
		{
			WifiInfo wifiInfo = manager.getConnectionInfo();
			if (wifiInfo != null) {
				NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
				if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR)
				{
					ssid = wifiInfo.getSSID();
					if (ssid == null) ssid = "";
					ssid = ssid.replaceAll("^\"|\"$", "");
				}
			}
		}
		return ssid;
	}

	//******************************************************************************
	// getNetworkName
	//******************************************************************************
	public static String getNetworkName()
	{
		String name = getWifiName();
		if (name == null || name.isEmpty())
		{

		}
		return name;
	}

	//******************************************************************************
	// getNetworkName
	//******************************************************************************
	public static String getNextCameraName(List<Camera> cameras)
	{
		String name = getDefaultCameraName();
		int number = 0;
		for (Camera camera : cameras)
		{
			String cameraName = camera.getName();
			if (cameraName.startsWith(name + " "))
			{
				String suffix = cameraName.substring(name.length() + 1);
				int n = 0;
				try
				{
					n = Integer.parseInt(suffix);
				}
				catch (NumberFormatException ex)
				{
					n = number;
				}
				if (n > number)
				{
					number = n;
				}
			}
		}
		name += " " + (number + 1);
		return name;
	}
}
