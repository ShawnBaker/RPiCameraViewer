// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class SettingsActivity extends AppCompatActivity
{
	// public constants
	public final static int FILTERED_CAMERAS = 0;
	public final static int ALL_CAMERAS = 1;

	// local constants
	private final static String TAG = "SettingsActivity";

	// instance variables
	private EditText cameraName;
	private Spinner showCameras;
	private SourceFragment sourceFragment;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// get the settings
		Settings settings = Utils.getSettings();

		// set the views
		cameraName = (EditText) findViewById(R.id.settings_camera_name);
		cameraName.setText(settings.cameraName);

		showCameras = (Spinner) findViewById(R.id.settings_show_cameras);
		showCameras.setSelection(settings.showAllCameras ? ALL_CAMERAS : FILTERED_CAMERAS);

		sourceFragment = (SourceFragment) getSupportFragmentManager().findFragmentById(R.id.settings_source);
		sourceFragment.configureForSettings(settings.source);
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
			Settings editedSettings = getAndCheckEditedSettings();
			if (editedSettings != null)
			{
				Utils.setSettings(editedSettings);
				Utils.saveData();
				finish();
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// getAndCheckEditedSettings
	//******************************************************************************
	private Settings getAndCheckEditedSettings()
	{
		// create a new settings and get the source
		Settings editedSettings = new Settings(sourceFragment.getSource());

		// get and check the camera name
		editedSettings.cameraName = cameraName.getText().toString().trim();
		if (editedSettings.cameraName.isEmpty())
		{
			App.error(this, R.string.error_no_camera_name);
			return null;
		}

		// get the show all cameras flag
		editedSettings.showAllCameras = showCameras.getSelectedItemPosition() == ALL_CAMERAS;

		// check the source values
		if (!sourceFragment.checkForSettings(editedSettings.source))
		{
			return null;
		}

		// return the new settings
		return editedSettings;
	}
}