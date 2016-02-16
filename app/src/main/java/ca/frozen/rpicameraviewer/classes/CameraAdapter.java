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
	public int getCount() { return cameras.size(); }

	//******************************************************************************
	// getItem
	//******************************************************************************
	@Override
	public Object getItem(int position)
	{
		return cameras.get(position);
	}

	//******************************************************************************
	// getItemId
	//******************************************************************************
	@Override
	public long getItemId(int position) { return 0; }

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
			convertView = inflater.inflate(R.layout.row_camera, null);
		}

		// get the camera for this row
		final Camera camera = cameras.get(position);

		// get the views
		TextView name = (TextView)convertView.findViewById(R.id.camera_name);
		TextView address = (TextView)convertView.findViewById(R.id.camera_address);

		// set the views
		Source source = camera.getSource();
		name.setText(camera.name);
		address.setText(camera.network.name + ":" + source.address + ":" + source.port);

		// return the view
		return convertView;
	}

	//******************************************************************************
	// getCameras
	//******************************************************************************
	public List<Camera> getCameras() { return cameras; }
}
