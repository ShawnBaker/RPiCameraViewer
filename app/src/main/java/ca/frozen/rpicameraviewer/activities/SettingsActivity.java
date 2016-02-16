// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.graphics.Color;
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
	public final static int HIDDEN = 0;
	public final static int TOP_LEFT = 1;
	public final static int TOP_RIGHT = 2;
	public final static int BOTTOM_LEFT = 3;
	public final static int BOTTOM_RIGHT = 4;
	public final static int FILTERED_CAMERAS = 0;
	public final static int ALL_CAMERAS = 1;

	// local constants
	private final static String TAG = "SettingsActivity";

	// instance variables
	private EditText cameraName, cameraNameColor;
	private Spinner cameraNamePosition, showCameras;
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

		// load the settings, networks and cameras
		Utils.loadData();

		// get the settings
		Settings settings = Utils.getSettings();

		// set the views
		cameraName = (EditText) findViewById(R.id.settings_camera_name);
		cameraName.setText(settings.cameraName);

		cameraNamePosition = (Spinner) findViewById(R.id.settings_camera_name_position);
		int position = HIDDEN;
		switch (settings.cameraNamePosition)
		{
			case TopLeft:
				position = TOP_LEFT;
				break;
			case TopRight:
				position = TOP_RIGHT;
				break;
			case BottomLeft:
				position = BOTTOM_LEFT;
				break;
			case BottomRight:
				position = BOTTOM_RIGHT;
				break;
		}
		cameraNamePosition.setSelection(position);

		cameraNameColor = (EditText) findViewById(R.id.settings_camera_name_color);
		cameraNameColor.setText(String.format("%08X", settings.cameraNameColor));

		showCameras = (Spinner) findViewById(R.id.settings_show_cameras);
		showCameras.setSelection(settings.showAllCameras ? ALL_CAMERAS : FILTERED_CAMERAS);

		sourceFragment = (SourceFragment) getSupportFragmentManager().findFragmentById(R.id.settings_source);
		sourceFragment.setSource(settings.source);
		sourceFragment.configureForSettings();
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

		// get the camera name position
		int position = cameraNamePosition.getSelectedItemPosition();
		switch (position)
		{
			case TOP_LEFT:
				editedSettings.cameraNamePosition = Settings.NamePosition.TopLeft;
				break;
			case TOP_RIGHT:
				editedSettings.cameraNamePosition = Settings.NamePosition.TopRight;
				break;
			case BOTTOM_LEFT:
				editedSettings.cameraNamePosition = Settings.NamePosition.BottomLeft;
				break;
			case BOTTOM_RIGHT:
				editedSettings.cameraNamePosition = Settings.NamePosition.BottomRight;
				break;
			default:
				editedSettings.cameraNamePosition = Settings.NamePosition.Hidden;
				break;
		}

		// get and check the camera name color
		try
		{
			String color = cameraNameColor.getText().toString().trim();
			if (color.length() == 6)
			{
				color = "FF" + color;
			}
			else if (color.length() == 4)
			{
				String alpha = color.substring(0, 1);
				String red = color.substring(1, 2);
				String green = color.substring(2, 3);
				String blue = color.substring(3, 4);
				color = alpha + alpha + red + red + green + green + blue + blue;
			}
			else if (color.length() == 3)
			{
				String red = color.substring(0, 1);
				String green = color.substring(1, 2);
				String blue = color.substring(2, 3);
				color = "FF" + red + red + green + green + blue + blue;
			}
			else if (editedSettings.cameraNamePosition != Settings.NamePosition.Hidden && color.length() != 8)
			{
				App.error(this, R.string.error_bad_color);
				return null;
			}
			editedSettings.cameraNameColor = (int)Long.parseLong(color, 16);
		}
		catch (Exception ex)
		{
			if (editedSettings.cameraNamePosition != Settings.NamePosition.Hidden)
			{
				App.error(this, R.string.error_bad_color);
				return null;
			}
		}
		if (editedSettings.cameraNamePosition != Settings.NamePosition.Hidden)
		{
			if (Color.alpha(editedSettings.cameraNameColor) == 0)
			{
				App.error(this, R.string.error_color_not_visible);
				return null;
			}
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