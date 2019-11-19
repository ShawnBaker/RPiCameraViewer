// Copyright © 2016-2019 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ca.frozen.rpicameraviewer.R;

public class CameraAdapter extends BaseAdapter
{
	// local constants
	private final static int VIEW_CAMERA = 0;
	private final static int VIEW_MESSAGE = 1;
	private final static int NUM_VIEWS = 2;

	// instance variables
	private List<Camera> cameras = new ArrayList<>();
	private View.OnClickListener scanButtonOnClickListener;
	private View.OnClickListener editButtonOnClickListener;
	private boolean showNetwork = false;

	//******************************************************************************
	// refresh
	//******************************************************************************
	public CameraAdapter(View.OnClickListener scanClickListener, View.OnClickListener editClickListener)
	{
		super();
		scanButtonOnClickListener = scanClickListener;
		editButtonOnClickListener = editClickListener;
	}

	//******************************************************************************
	// refresh
	//******************************************************************************
	public void refresh()
	{
		boolean showAllCameras = !Utils.connectedToNetwork() || Utils.getSettings().showAllCameras;

		if (showAllCameras)
		{
			cameras = Utils.getCameras();
		}
		else
		{
			String network = Utils.getNetworkName();
			showAllCameras = network == null || network.isEmpty();
			cameras = showAllCameras ? Utils.getCameras() : Utils.getNetworkCameras(network, true);
		}

		showNetwork = showAllCameras;
		notifyDataSetChanged();
	}

	//******************************************************************************
	// getCount
	//******************************************************************************
	@Override
	public int getCount() { return (cameras.size() > 0) ? cameras.size() : 1; }

	//******************************************************************************
	// getItem
	//******************************************************************************
	@Override
	public Camera getItem(int position)
	{
		return cameras.get(position);
	}

	//******************************************************************************
	// getItemId
	//******************************************************************************
	@Override
	public long getItemId(int position) { return 0; }

	/******************************************************
	 * getViewTypeCount
	 ******************************************************/
	@Override
	public int getViewTypeCount() { return NUM_VIEWS; }

	/******************************************************
	 * getItemViewType
	 ******************************************************/
	public int getItemViewType(int position)
	{
		return (cameras.size() > 0) ? VIEW_CAMERA : VIEW_MESSAGE;
	}

	//******************************************************************************
	// getView
	//******************************************************************************
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// get the view type
		int type = getItemViewType(position);

		// inflate the view if necessary
		final Context context = parent.getContext();

		if (convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate((type == VIEW_CAMERA) ? R.layout.row_camera : R.layout.row_message, null);
		}

		if (type == VIEW_CAMERA)
		{
			// get the camera for this row
			Camera camera = getItem(position);

			if (camera != null)
			{
				// get the views
				convertView.setTag(camera);
				TextView name = convertView.findViewById(R.id.camera_name);
				TextView address = convertView.findViewById(R.id.camera_address);
				ImageButton edit = convertView.findViewById(R.id.camera_edit);
				edit.setTag(camera);
				edit.setOnClickListener(editButtonOnClickListener);

				// set the views
				name.setText(camera.name);
				String addr = camera.address + ":" + camera.port;

				if (showNetwork && Utils.isIpAddress(camera.address))
				{
					addr = camera.network + ":" + addr;
				}

				address.setText(addr);
			}
		}
		else
		{
			TextView msg = convertView.findViewById(R.id.message_text);
			msg.setText(R.string.no_cameras);
			Button scan = convertView.findViewById(R.id.message_scan);
			scan.setOnClickListener(scanButtonOnClickListener);
		}

		// return the view
		return convertView;
	}

	//******************************************************************************
	// getCameras
	//******************************************************************************
	public List<Camera> getCameras() { return cameras; }
}
