package ca.frozen.rpicameraviewer.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Network;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class CameraActivity extends AppCompatActivity
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "CameraActivity";

	// instance variables
	private EditText nameEdit;
	private Spinner networkSpinner;
	private SourceFragment sourceFragment;
	private Camera camera;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		// load the settings, networks and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = (Camera)data.getParcelable(CAMERA);

		// handled clicks on the edit network image
		ImageView editNetwork = (ImageView) findViewById(R.id.camera_edit_network);
		editNetwork.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				int pos = networkSpinner.getSelectedItemPosition();
				Network network = Utils.getNetworks().get(pos);
				Intent intent = new Intent(App.getContext(), NetworkActivity.class);
				intent.putExtra(NetworkActivity.NETWORK, network);
				startActivity(intent);
			}
		});

		// set the name
		nameEdit = (EditText) findViewById(R.id.camera_name);
		nameEdit.setText(camera.name);

		// set the networks
		List<Network> networks = Utils.getNetworks();
		networkSpinner = (Spinner) findViewById(R.id.camera_network);
		List<String> networkNames = new ArrayList<String>();
		for (Network network : networks)
		{
			networkNames.add(network.name);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, networkNames);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		networkSpinner.setAdapter(dataAdapter);
		networkSpinner.setSelection(networks.indexOf(camera.network));

		// set the source fragment
		sourceFragment = (SourceFragment)getSupportFragmentManager().findFragmentById(R.id.camera_source);
		sourceFragment.setSource(camera.source);
		sourceFragment.configureForCamera();
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_camera, menu);
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
		Camera editedCamera = new Camera();
		editedCamera.source = sourceFragment.getSource();

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

		// get the network
		int pos = networkSpinner.getSelectedItemPosition();
		editedCamera.network = Utils.getNetworks().get(pos);

		// check the source values
		if (!sourceFragment.checkForCamera(editedCamera))
		{
			return null;
		}

		// return the new settings
		return editedCamera;
	}
}