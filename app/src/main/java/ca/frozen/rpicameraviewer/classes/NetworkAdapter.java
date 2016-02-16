// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ca.frozen.rpicameraviewer.R;

public class NetworkAdapter extends BaseAdapter
{
    // instance variables
    private List<Network> networks = new ArrayList<>();

	//******************************************************************************
	// refresh
	//******************************************************************************
    public void refresh()
    {
        networks = Utils.getNetworks();
        notifyDataSetChanged();
    }

	//******************************************************************************
	// getCount
	//******************************************************************************
    @Override
    public int getCount() { return networks.size(); }

	//******************************************************************************
	// getItem
	//******************************************************************************
    @Override
    public Object getItem(int position)
    {
        return networks.get(position);
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
            convertView = inflater.inflate(R.layout.row_network, null);
        }

		// get the network for this row
		Network network = networks.get(position);

        // set the views
		TextView name = (TextView)convertView.findViewById(R.id.network_name);
		name.setText(network.name);

		// return the view
        return convertView;
    }
}
