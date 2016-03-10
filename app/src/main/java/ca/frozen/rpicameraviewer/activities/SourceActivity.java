// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.Utils;

public class SourceActivity extends AppCompatActivity
{
	// public constants
	public final static String SOURCE = "source";

	// local constants
	private final static String TAG = "SourceActivity";

	// instance variables
	private Source source;
	private SourceFragment sourceFragment;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_source);

		// load the settings and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		source = data.getParcelable(SOURCE);

		// set the title in the action bar
		ActionBar ab = getSupportActionBar();
		switch (source.connectionType)
		{
			case RawTcpIp:
				ab.setTitle(R.string.tcp_ip_defaults);
				break;
			case RawHttp:
				ab.setTitle(R.string.http_defaults);
				break;
			case RawMulticast:
				ab.setTitle(R.string.multicast_defaults);
				break;
		}

		// create the source fragment
		sourceFragment = (SourceFragment) getSupportFragmentManager().findFragmentById(R.id.source_source);
		sourceFragment.configure(source, false);
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
			Source editedSource = sourceFragment.getAndCheckEditedSource();
			if (editedSource != null)
			{
				Intent intent = new Intent();
				intent.putExtra(SOURCE, editedSource);
				setResult(RESULT_OK, intent);
				finish();
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
