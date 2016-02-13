package ca.frozen.rpicameraviewer;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
	// public constants
	public final static int CAMERA_REQUEST_CODE = 1;

	// local constants
	private final static String TAG = "MainActivity";

	// instance variables
	private CameraAdapter adapter;
	private List<Camera> cameras;
	private ScannerFragment scannerFragment;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// set the view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// create the toolbar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// set the default preferences
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		// set the list adapter
		ListView listView = (ListView)findViewById(R.id.cameras);
		adapter = new CameraAdapter();
		cameras = Utils.loadCameras();
		adapter.setCameras(cameras);
		listView.setAdapter(adapter);
		registerForContextMenu(listView);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
			{
				startVideoActivity(cameras.get(position));
			}
		});

		// create the add button
		FloatingActionButton addCameraButton = (FloatingActionButton) findViewById(R.id.add_camera);
		addCameraButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Camera camera = new Camera();
				camera.setName(Utils.getNextCameraName(cameras));
				startCameraActivity(camera);
			}
		});

		// do a scan if there are no cameras
		if (savedInstanceState == null && cameras.size() == 0)
		{
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					startScanner();
				}
			}, 500);
		}
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        int id = item.getItemId();

		// scan for cameras
        if (id == R.id.action_scan)
        {
			startScanner();
            return true;
        }

		// delete all the cameras
		else if (id == R.id.action_delete_all)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage(R.string.ok_to_delete_all_cameras);
			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					cameras.clear();
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
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}

		// display the help information
		else if (id == R.id.action_help)
		{
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		}

		// display the about information
        else if (id == R.id.action_about)
        {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// onCreateContextMenu
	//******************************************************************************
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		if (v.getId() == R.id.cameras)
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu_cameras, menu);
		}
	}

	//******************************************************************************
	// onContextItemSelected
	//******************************************************************************
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterView.AdapterContextMenuInfo info;

		switch (item.getItemId())
		{
			// edit the selected camera
			case R.id.action_edit:
				info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				Camera camera = cameras.get(info.position);
				startCameraActivity(camera);
				return true;

			// prompt the user to delete the selected camera
			case R.id.action_delete:
				info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				final int index = info.position;
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setMessage(R.string.ok_to_delete_camera);
				alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						cameras.remove(index);
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

			// call the super
			default:
				return super.onContextItemSelected(item);
		}
	}

	//******************************************************************************
	// onActivityResult
	//******************************************************************************
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null)
		{
			Camera camera = (Camera)data.getParcelableExtra(CameraActivity.CAMERA);
			Camera editedCamera = (Camera)data.getParcelableExtra(CameraActivity.EDITED_CAMERA);
			if (!camera.getName().isEmpty())
			{
				cameras.remove(camera);
			}
			cameras.add(editedCamera);
			updateCameras();
		}
	}

	//******************************************************************************
	// addScannedCameras
	//******************************************************************************
	public void addScannedCameras(List<Camera> newCameras)
	{
		if (newCameras.size() > 0)
		{
			cameras.addAll(newCameras);
			updateCameras();
		}
	}

	//******************************************************************************
	// startScanner
	//******************************************************************************
	private void startScanner()
	{
		FragmentManager fm = getFragmentManager();
		scannerFragment = new ScannerFragment();
		scannerFragment.show(fm, "Scanner");
	}

	//******************************************************************************
	// startCameraActivity
	//******************************************************************************
	private void startCameraActivity(Camera camera)
	{
		Intent cameraActivity = new Intent(App.getContext(), CameraActivity.class);
		cameraActivity.putExtra(CameraActivity.CAMERA, camera);
		startActivityForResult(cameraActivity, CAMERA_REQUEST_CODE);
	}

	//******************************************************************************
	// startVideoActivity
	//******************************************************************************
	private void startVideoActivity(Camera camera)
	{
		Intent videoActivity = new Intent(App.getContext(), VideoActivity.class);
		videoActivity.putExtra(VideoActivity.CAMERA, camera);
		startActivity(videoActivity);
	}

	//******************************************************************************
	// updateCameras
	//******************************************************************************
	private void updateCameras()
	{
		Collections.sort(cameras);
		Utils.saveCameras(cameras);
		adapter.setCameras(cameras);
	}
}
