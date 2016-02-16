package ca.frozen.rpicameraviewer.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Network;
import ca.frozen.rpicameraviewer.classes.NetworkAdapter;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class NetworksActivity extends AppCompatActivity
{
	// local constants
	private final static String TAG = "NetworksActivity";

	// instance variables
	private NetworkAdapter adapter;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// set the view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_networks);

		// load the settings, networks and cameras
		Utils.loadData();

		// set the list adapter
		ListView listView = (ListView)findViewById(R.id.networks);
		adapter = new NetworkAdapter();
		adapter.refresh();
		listView.setAdapter(adapter);
		registerForContextMenu(listView);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
			{
				startNetworkActivity(Utils.getNetworks().get(position));
			}
		});

		// create the add button
		FloatingActionButton addNetworkButton = (FloatingActionButton) findViewById(R.id.add_network);
		addNetworkButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Network network = new Network();
				startNetworkActivity(network);
			}
		});
	}

	//******************************************************************************
	// onResume
	//******************************************************************************
	@Override
	public void onResume()
	{
		super.onResume();
		Utils.reloadData();
		adapter.refresh();
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_networks, menu);
		return true;
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        int id = item.getItemId();

		// delete all the networks
		if (id == R.id.action_delete_all)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage(R.string.ok_to_delete_all_networks);
			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Utils.getNetworks().clear();
					Utils.getCameras().clear();
					updateNetworks();
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

        return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// onCreateContextMenu
	//******************************************************************************
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		if (v.getId() == R.id.networks)
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu_networks_list, menu);
		}
	}

	//******************************************************************************
	// onContextItemSelected
	//******************************************************************************
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		final Network network;
		AdapterView.AdapterContextMenuInfo info;

		switch (item.getItemId())
		{
			// edit the selected network
			case R.id.action_edit:
				info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				network = Utils.getNetworks().get(info.position);
				startNetworkActivity(network);
				return true;

			// prompt the user to delete the selected network
			case R.id.action_delete:
				info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				network = Utils.getNetworks().get(info.position);
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setMessage(R.string.ok_to_delete_network);
				alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						List<Camera> cameras = Utils.getCameras();
						for (int i = cameras.size() - 1; i >= 0; i--)
						{
							Camera camera = cameras.get(i);
							if (camera.network == network)
							{
								cameras.remove(i);
							}
						}
						Utils.getNetworks().remove(network);
						updateNetworks();
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
	// startNetworkActivity
	//******************************************************************************
	private void startNetworkActivity(Network network)
	{
		Intent intent = new Intent(App.getContext(), NetworkActivity.class);
		intent.putExtra(NetworkActivity.NETWORK, network);
		startActivity(intent);
	}

	//******************************************************************************
	// updateNetworks
	//******************************************************************************
	private void updateNetworks()
	{
		Collections.sort(Utils.getNetworks());
		Utils.saveData();
		adapter.refresh();
	}
}
