// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class CameraActivity extends AppCompatActivity
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String ValidIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
	private final static String ValidHostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

	// instance variables
	private Camera camera;
	private EditText nameEdit;
	private EditText addressEdit;
	private EditText portEdit;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// load the settings and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = data.getParcelable(CAMERA);
		Log.info("camera: " + (camera.name.isEmpty() ? "new" : camera.name));

		// get the controls
		TextView network = findViewById(R.id.camera_network);
		nameEdit = findViewById(R.id.camera_name);
		addressEdit = findViewById(R.id.camera_address);
		portEdit = findViewById(R.id.camera_port);

		// initialize the control values
		network.setText(camera.network);
		nameEdit.setText(camera.name);
		addressEdit.setText(camera.address);
		portEdit.setText(Integer.toString(camera.port));
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_save, menu);
		return true;
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		// save the camera
		if (id == R.id.action_save)
		{
			Camera editedCamera = getAndCheckEditedCamera();
			if (editedCamera != null)
			{
				List<Camera> cameras = Utils.getCameras();
				if (!camera.name.isEmpty())
				{
					cameras.remove(camera);
				}
				cameras.add(editedCamera);
				Log.info("menu: save " + editedCamera.toString());
				Utils.saveData();
				finish();
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// getAndCheckEditedCamera
	//******************************************************************************
	private Camera getAndCheckEditedCamera()
	{
		// create a new camera
		Camera editedCamera = new Camera(camera);

		// get and check the camera name
		editedCamera.name = nameEdit.getText().toString().trim();
		if (editedCamera.name.isEmpty())
		{
			App.error(this, R.string.error_no_name);
			return null;
		}

		// make sure the name doesn't already exist
		String name = camera.name;
		if (name.isEmpty() || !name.equals(editedCamera.name))
		{
			Camera existingCamera = Utils.findCamera(editedCamera.name);
			if (existingCamera != null)
			{
				App.error(this, R.string.error_name_already_exists);
				return null;
			}
		}

		// make sure there's an address
		editedCamera.address = addressEdit.getText().toString().trim();
		if (editedCamera.address.isEmpty())
		{
			App.error(this, R.string.error_no_address);
			return null;
		}

		// check the address
		if (!editedCamera.address.matches(ValidIpAddressRegex) && !editedCamera.address.matches(ValidHostnameRegex))
		{
			App.error(this, R.string.error_bad_address);
			return null;
		}

		// get and check the port number
		editedCamera.port = Utils.getNumber(portEdit);
		if (editedCamera.port < Settings.MIN_PORT || editedCamera.port > Settings.MAX_PORT)
		{
			App.error(this, String.format(getString(R.string.error_bad_port), Settings.MIN_PORT, Settings.MAX_PORT));
			return null;
		}

		// return the successfully edited camera
		return editedCamera;
	}
}