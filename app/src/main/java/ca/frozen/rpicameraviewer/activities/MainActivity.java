// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.BuildConfig;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.CameraAdapter;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class MainActivity extends AppCompatActivity
{
	// local constants
	private final static int REQUEST_ACCESS_FINE_LOCATION = 74;

	// instance variables
	private CameraAdapter adapter;
	private Menu mainMenu = null;
	private ConnectivityChangeReceiver receiver = null;
	private int value = 43;
	private boolean set_network = false;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// set the view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());
		Log.info(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);

		// load the settings and cameras
		Utils.loadData();

		// create the toolbar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// set the list adapter
		adapter = new CameraAdapter(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startScannerWithPermission();
			}
		}, new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.info("camera: edit");
				Camera camera = (Camera)v.getTag();
				startCameraActivity(camera);
			}
		});
		adapter.refresh();
		ListView listView = findViewById(R.id.cameras);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adaptr, View view, int position, long id)
			{
				startVideoActivity(adapter.getCameras().get(position));
			}
		});

		listView.setLongClickable(true);
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> adaptr, View view, int position, long id)
			{
				Log.info("long press: delete");
				final Camera camera = (Camera)view.getTag();
				AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
				alert.setMessage(R.string.ok_to_delete_camera);
				alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Log.info("long press: deleting camera " + camera.name);
						Utils.getCameras().remove(camera);
						updateCameras();
						dialog.dismiss();
					}
				});
				alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});

				alert.show();
				return true;
			}
		});

		// create the add button
		FloatingActionButton addCameraButton = findViewById(R.id.add_camera);
		addCameraButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Camera camera = new Camera(Utils.getNextCameraName(adapter.getCameras()), Utils.getDefaultPort());
				startCameraActivity(camera);
			}
		});

		// do a scan if there are no cameras
		if (savedInstanceState == null && adapter.getCameras().size() == 0 && Utils.connectedToNetwork())
		{
			Log.info("starting auto scan");
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					startScannerWithPermission();
				}
			}, 500);
		}
	}

	//******************************************************************************
	// onStart
	//******************************************************************************
	@Override
	public void onStart()
	{
		super.onStart();
		if (receiver == null)
		{
			receiver = new ConnectivityChangeReceiver();
			registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}
	}

	//******************************************************************************
	// onStop
	//******************************************************************************
	@Override
	public void onStop()
	{
		super.onStop();
		if (receiver != null)
		{
			unregisterReceiver(receiver);
			receiver = null;
		}
	}

	//******************************************************************************
	// onResume
	//******************************************************************************
	@Override
	public void onResume()
	{
		Log.setTag(getClass().getSimpleName());
		super.onResume();
		Utils.reloadData();
		adapter.refresh();
	}

	//******************************************************************************
	// onSaveInstanceState
	//******************************************************************************
	@Override
	protected void onSaveInstanceState(Bundle state)
	{
		state.putInt("value", value);
		super.onSaveInstanceState(state);
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_main, menu);
		mainMenu = menu;
		return true;
	}

	//******************************************************************************
	// onPrepareOptionsMenu
	//******************************************************************************
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// disable Delete All if there are no cameras
		MenuItem item = menu.findItem(R.id.action_delete_all);
		item.setEnabled(adapter.getCameras().size() != 0);

		// set the network name
		if (!set_network)
		{
			setNetworkName();
		}

		return true;
	}

	//******************************************************************************
	// setNetworkName
	//******************************************************************************
	private void setNetworkName()
	{
		if (mainMenu != null)
		{
			MenuItem item = mainMenu.findItem(R.id.action_network);
			if (Utils.getSettings().showAllCameras)
			{
				item.setTitle(R.string.all_networks);
			}
			else
			{
				String name = Utils.getNetworkName();
				if (name == null || name.isEmpty())
				{
					item.setTitle(R.string.no_network);
				}
				else
				{
					item.setTitle(name);
				}
			}
			Log.info("setNetworkName: " + item.getTitle().toString());
			set_network = true;
		}
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        int id = item.getItemId();

		// network name
        if (id == R.id.action_network)
        {
			// nothing to do right now
			Log.info("menu: network");
            return true;
        }

		// scan for cameras
		else if (id == R.id.action_scan)
		{
			Log.info("menu: scan");
			startScannerWithPermission();
			return true;
		}

		// delete all the cameras
		else if (id == R.id.action_delete_all)
		{
			Log.info("menu: delete all");
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage(R.string.ok_to_delete_all_cameras);
			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if (Utils.getSettings().showAllCameras)
					{
						Log.info("menu: deleting all cameras");
						Utils.getCameras().clear();
					}
					else
					{
						Log.info("menu: deleting network cameras");
						List<Camera> allCameras = Utils.getCameras();
						for (Camera camera : adapter.getCameras())
						{
							allCameras.remove(camera);
						}
					}
					updateCameras();
					dialog.dismiss();
				}
			});
			alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			});

			alert.show();
		}

		// edit the settings
		else if (id == R.id.action_settings)
		{
			Log.info("menu: settings");
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}

		// display the help information
		else if (id == R.id.action_help)
		{
			Log.info("menu: help");
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		}

		// display the log files
		else if (id == R.id.action_log_files)
		{
			Log.info("menu: log files");
			Intent intent = new Intent(this, LogFilesActivity.class);
			startActivity(intent);
			return true;
		}

		// display the about information
        else if (id == R.id.action_about)
        {
			Log.info("menu: about");
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// startScannerWithPermission
	//******************************************************************************
	private void startScannerWithPermission()
	{
		int check = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
		if (check != PackageManager.PERMISSION_GRANTED)
		{
			Log.info("ask for fine location permission");
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
												REQUEST_ACCESS_FINE_LOCATION);
		}
		else
		{
			startScanner();
		}
	}

	//******************************************************************************
	// onRequestPermissionsResult
	//******************************************************************************
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0 &&
			grantResults[0] == PackageManager.PERMISSION_GRANTED)
		{
			Log.info("fine location permission granted");
			setNetworkName();
		}
		startScanner();
	}

	//******************************************************************************
	// startScanner
	//******************************************************************************
	private void startScanner()
	{
		Log.info("startScanner");
		FragmentManager fm = getFragmentManager();
		ScannerFragment scannerFragment = new ScannerFragment();
		scannerFragment.show(fm, "Scanner");
	}

	//******************************************************************************
	// startCameraActivity
	//******************************************************************************
	private void startCameraActivity(Camera camera)
	{
		Log.info("startCameraActivity: " + camera.name);
		Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
		intent.putExtra(CameraActivity.CAMERA, camera);
		startActivity(intent);
	}

	//******************************************************************************
	// startVideoActivity
	//******************************************************************************
	private void startVideoActivity(Camera camera)
	{
		Log.info("startVideoActivity: " + camera.name);
		Intent intent = new Intent(getApplicationContext(), VideoActivity.class);
		intent.putExtra(VideoActivity.CAMERA, camera);
		startActivity(intent);

		notifyCameraChange(camera);
	}

	//******************************************************************************
	// notifyCameraChange
	//******************************************************************************
	public boolean notifyCameraChange(Camera camera) {
		final String packageName = "it.robint.tux.vipergcs";

		final PackageManager manager = getPackageManager();

		final Intent intent = manager.getLaunchIntentForPackage(packageName);

		if (intent == null)
			return false;

		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.putExtra("BACKWARD_FLAG",camera.backward);

		startActivity(intent);

		return true;
	}

	//******************************************************************************
	// updateCameras
	//******************************************************************************
	public void updateCameras()
	{
		Log.info("updateCameras");
		Collections.sort(Utils.getCameras());
		Utils.saveData();
		adapter.refresh();
	}

	////////////////////////////////////////////////////////////////////////////////
	// ConnectivityChangeReceiver
	////////////////////////////////////////////////////////////////////////////////
	private class ConnectivityChangeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context c, Intent intent)
		{
			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
			{
				Log.info("network change");
				setNetworkName();
				if (adapter != null)
				{
					adapter.refresh();
				}
				value++;
			}
		}
	}
}
