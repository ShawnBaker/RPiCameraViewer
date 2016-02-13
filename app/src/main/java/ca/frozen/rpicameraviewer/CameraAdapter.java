package ca.frozen.rpicameraviewer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CameraAdapter extends BaseAdapter
{
    // instance variables
    private List<Camera> cameras = new ArrayList<>();

	//******************************************************************************
	// setCameras
	//******************************************************************************
    public void setCameras(List<Camera> cameras)
    {
        this.cameras = cameras;
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
		name.setText(camera.getName());
		address.setText(camera.getNetwork() + ":" + camera.getAddress() + ":" + camera.getPort());

		// return the view
        return convertView;
    }
}
