// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class CameraActivity extends AppCompatActivity
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "CameraActivity";

	// instance variables
	private Camera camera;
	private EditText nameEdit;
	private SourceFragment sourceFragment;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		// load the settings and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = data.getParcelable(CAMERA);

		// set the name
		nameEdit = (EditText) findViewById(R.id.camera_name);
		nameEdit.setText(camera.name);

		// set the network
		TextView network = (TextView) findViewById(R.id.camera_network);
		network.setText(camera.network);

		// set the source fragment
		sourceFragment = (SourceFragment)getSupportFragmentManager().findFragmentById(R.id.camera_source);
		sourceFragment.configure(camera.source, true);
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
		// create a new network and get the source
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

		// check the source values
		editedCamera.source = sourceFragment.getAndCheckEditedSource();
		if (editedCamera.source == null)
		{
			return null;
		}

		// return the successfully edited camera
		return editedCamera;
	}
}