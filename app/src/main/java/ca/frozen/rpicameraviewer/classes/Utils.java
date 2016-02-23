// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

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

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;

public class Utils
{
	private static Settings settings = null;
	private static List<Network> networks = null;
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
			if (preferences == null)
			{
				preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
			}
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

		// load the networks
		if (networks == null)
		{
			if (preferences == null)
			{
				preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
			}
			networks = new ArrayList<Network>();
			try
			{
				String networksString = preferences.getString(App.getStr(R.string.settings_networks), "");
				if (!networksString.isEmpty())
				{
					JSONArray arr = new JSONArray(networksString);
					for (int i = 0; i < arr.length(); i++)
					{
						JSONObject obj = arr.getJSONObject(i);
						Network network = new Network(obj);
						networks.add(network);
					}
				}
			}
			catch (JSONException e)
			{
				save = true;
			}
			Collections.sort(networks);
		}

		// load the cameras
		if (cameras == null)
		{
			if (preferences == null)
			{
				preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
			}
			cameras = new ArrayList<Camera>();
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
		settings = null;
		networks = null;
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
		if (settings != null)
		{
			if (preferences == null)
			{
				preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
				editor = preferences.edit();
			}
			JSONObject obj = settings.toJson();
			editor.putString(App.getStr(R.string.settings_settings), obj.toString());
		}

		// save the networks
		if (networks != null)
		{
			if (preferences == null)
			{
				preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
				editor = preferences.edit();
			}
			JSONArray arr = new JSONArray();
			for (Network network : networks)
			{
				arr.put(network.toJson());
			}
			editor.putString(App.getStr(R.string.settings_networks), arr.toString());
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
			editor.commit();
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
	// getNetworks
	//******************************************************************************
	public static List<Network> getNetworks()
	{
		loadData();
		return networks;
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
		return settings.source.port;
	}

	//******************************************************************************
	// getNetwork
	//******************************************************************************
	public static Network getNetwork(String name)
	{
		loadData();

		// search for an existing network with the same name
		for (Network network : networks)
		{
			if (network.name.equals(name))
			{
				return network;
			}
		}

		// create a new network and add it to the list
		Network network = new Network(name);
		networks.add(network);
		return network;
	}

	//******************************************************************************
	// getNetworkCameras
	//******************************************************************************
	public static List<Camera> getNetworkCameras(Network network)
	{
		loadData();

		// find the cameras for the network
		List<Camera> networkCameras = new ArrayList<Camera>();
		for (Camera camera : cameras)
		{
			if (camera.network == network)
			{
				networkCameras.add(camera);
			}
		}

		return networkCameras;
	}

	//******************************************************************************
	// getNetworkCameras
	//******************************************************************************
	public static List<Camera> getNetworkCameras(String networkName)
	{
		loadData();

		Network network = findNetwork(networkName);
		return (network != null) ? getNetworkCameras(network) : null;
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
	// findNetwork
	//******************************************************************************
	public static Network findNetwork(String name)
	{
		loadData();

		for (Network network : networks)
		{
			if (network.name.equals(name))
			{
				return network;
			}
		}
		return null;
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
	// getHttpAddress
	//******************************************************************************
	public static String getHttpAddress(String baseAddress)
	{
		String address = baseAddress;
		if (!address.startsWith("http://"))
		{
			address = "http://" + address;
		}
		return address;
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
			if (wifiInfo != null)
			{
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
	// connectedToWifi
	//******************************************************************************
	public static boolean connectedToWifi()
	{
		String name = getWifiName();
		return name != null && !name.isEmpty();
	}

	//******************************************************************************
	// connectedToNetwork
	//******************************************************************************
	public static boolean connectedToNetwork()
	{
		boolean connected = connectedToWifi();
		if (!connected)
		{
		}
		return connected;
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
}
