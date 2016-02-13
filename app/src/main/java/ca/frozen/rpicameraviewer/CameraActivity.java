package ca.frozen.rpicameraviewer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity
{
	// public constants
	public final static String CAMERA = "camera";
	public final static String EDITED_CAMERA = "edited_camera";
	public final static int NO_PORT = Integer.MIN_VALUE;
	public final static int MIN_PORT = 0;
	public final static int MAX_PORT = 65535;

	// local constants
	private final static String TAG = "CameraActivity";

	// instance variables
	private EditText nameEdit, addressEdit, portEdit;
	private Button saveButton;
	private Camera camera;
	private List<Camera> cameras = null;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = (Camera)data.getParcelable(CAMERA);

		// create the change watcher
		TextWatcher watcher = new TextWatcher()
		{
			public void afterTextChanged(Editable s)
			{
				Camera editedCamera = getEditedCamera();
				saveButton.setEnabled(!camera.equals(editedCamera));
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		};

		// set the views
		nameEdit = (EditText) findViewById(R.id.camera_name);
		nameEdit.setText(camera.getName());
		nameEdit.addTextChangedListener(watcher);

		addressEdit = (EditText) findViewById(R.id.camera_address);
		addressEdit.setText(camera.getAddress());
		addressEdit.addTextChangedListener(watcher);

		portEdit = (EditText) findViewById(R.id.camera_port);
		portEdit.setText(Integer.toString(camera.getPort()));
		portEdit.addTextChangedListener(watcher);

		saveButton = (Button) findViewById(R.id.camera_save);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Camera editedCamera = getEditedCamera();
				if (checkEditedCamera(editedCamera))
				{
					Intent intent = getIntent();
					intent.putExtra(EDITED_CAMERA, editedCamera);
					setResult(RESULT_OK, intent);
					finish();
				}
			}
		});
	}

	//******************************************************************************
	// getEditedCamera
	//******************************************************************************
	private Camera getEditedCamera()
	{
		String portStr = portEdit.getText().toString();
		int port = portStr.isEmpty() ? NO_PORT : Integer.parseInt(portStr);
		Camera editedCamera = new Camera(nameEdit.getText().toString().trim(),
											addressEdit.getText().toString().trim(),
											port);
		return editedCamera;
	}

	//******************************************************************************
	// checkEditedCamera
	//******************************************************************************
	private boolean checkEditedCamera(Camera editedCamera)
	{
		// make sure there's a name
		if (editedCamera.getName().isEmpty())
		{
			App.error(this, R.string.error_no_name);
			return false;
		}

		// make sure the name doesn't already exist
		String name = camera.getName();
		if (name.isEmpty() || !name.equals(editedCamera.getName()))
		{
			if (cameras == null)
			{
				cameras = Utils.loadCameras();
			}
			Camera existingCamera = Utils.findCamera(cameras, editedCamera.getName());
			if (existingCamera != null)
			{
				App.error(this, R.string.error_name_already_exists);
				return false;
			}
		}

		// make sure there's an address
		if (editedCamera.getAddress().isEmpty())
		{
			App.error(this, R.string.error_no_address);
			return false;
		}

		// make sure the address is a valid URL
		if (!Patterns.WEB_URL.matcher(editedCamera.getAddress()).matches())
		{
			App.error(this, R.string.error_bad_address);
			return false;
		}

		// make sure there's a port
		int port = editedCamera.getPort();
		if (port == NO_PORT)
		{
			App.error(this, R.string.error_no_port);
			return false;
		}

		// make sure the port is within range
		if (port < MIN_PORT || port > MAX_PORT)
		{
			App.error(this, String.format(getString(R.string.error_bad_port), MIN_PORT, MAX_PORT));
			return false;
		}

		// indicate success
		return true;
	}
}