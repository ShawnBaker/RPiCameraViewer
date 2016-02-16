// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Network;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class NetworkActivity extends AppCompatActivity
{
	// public constants
	public final static String NETWORK = "network";

	// local constants
	private final static String TAG = "NetworkActivity";

	// instance variables
	private EditText nameEdit;
	private SourceFragment sourceFragment;
	private Network network;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_network);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// load the settings, networks and cameras
		Utils.loadData();

		// get the network object
		Bundle data = getIntent().getExtras();
		network = (Network)data.getParcelable(NETWORK);

		// set the name
		nameEdit = (EditText) findViewById(R.id.network_name);
		nameEdit.setText(network.name);

		// set the source fragment
		sourceFragment = (SourceFragment)getSupportFragmentManager().findFragmentById(R.id.network_source);
		sourceFragment.setSource(network.source);
		sourceFragment.configureForNetwork();
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_network, menu);
		return true;
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		// navigate to the parent
		if (id == android.R.id.home)
		{
			finish();
			return true;
		}

		// save the network
		else if (id == R.id.action_save)
		{
			Network editedNetwork = getAndCheckEditedNetwork();
			if (editedNetwork != null)
			{
				List<Network> networks = Utils.getNetworks();
				if (!network.name.isEmpty())
				{
					networks.remove(network);
				}
				networks.add(editedNetwork);
				Utils.saveData();
				finish();
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// getAndCheckEditedNetwork
	//******************************************************************************
	private Network getAndCheckEditedNetwork()
	{
		// create a new network and get the source
		Network editedNetwork = new Network();
		editedNetwork.source = sourceFragment.getSource();

		// get and check the camera name
		editedNetwork.name = nameEdit.getText().toString().trim();
		if (editedNetwork.name.isEmpty())
		{
			App.error(this, R.string.error_no_name);
			return null;
		}

		// check the source values
		if (!sourceFragment.checkForNetwork(editedNetwork))
		{
			return null;
		}

		// return the new settings
		return editedNetwork;
	}
}