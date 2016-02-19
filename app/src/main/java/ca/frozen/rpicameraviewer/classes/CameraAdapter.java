// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Camera;

public class CameraAdapter extends BaseAdapter
{
	// local constants
	private final static int VIEW_CAMERA = 0;
	private final static int VIEW_MESSAGE = 1;
	private final static int NUM_VIEWS = 2;

	// instance variables
	private List<Camera> cameras = new ArrayList<>();

	//******************************************************************************
	// refresh
	//******************************************************************************
	public void refresh()
	{
		boolean showAllCameras = Utils.getSettings().showAllCameras;
		if (showAllCameras)
		{
			cameras = Utils.getCameras();
		}
		else
		{
			String network = Utils.getNetworkName();
			List<Camera> allCameras = Utils.getCameras();
			cameras = new ArrayList<Camera>();
			for (Camera camera : Utils.getCameras())
			{
				if (camera.network.name.equals(network))
				{
					cameras.add(camera);
				}
			}
		}
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
	public Object getItem(int position)
	{
		return (cameras.size() > 0) ? cameras.get(position) : new Camera();
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
			Camera camera = cameras.get(position);

			// get the views
			TextView name = (TextView) convertView.findViewById(R.id.camera_name);
			TextView address = (TextView) convertView.findViewById(R.id.camera_address);

			// set the views
			Source source = camera.getSource();
			name.setText(camera.name);
			address.setText(camera.network.name + ":" + source.address + ":" + source.port);
		}
		else
		{
			TextView msg = (TextView) convertView.findViewById(R.id.message);
			msg.setText(R.string.no_cameras);
		}

		// return the view
		return convertView;
	}

	//******************************************************************************
	// getCameras
	//******************************************************************************
	public List<Camera> getCameras() { return cameras; }
}