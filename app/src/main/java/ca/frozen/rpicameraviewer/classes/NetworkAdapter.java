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
    // local constants
    private final static int VIEW_NETWORK = 0;
    private final static int VIEW_MESSAGE = 1;
    private final static int NUM_VIEWS = 2;

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
	public int getCount() { return (networks.size() > 0) ? networks.size() : 1; }

	//******************************************************************************
	// getItem
	//******************************************************************************
    @Override
    public Object getItem(int position)
    {
		return (networks.size() > 0) ? networks.get(position) : new Network();
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
        return (networks.size() > 0) ? VIEW_NETWORK : VIEW_MESSAGE;
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
            convertView = inflater.inflate((type == VIEW_NETWORK) ? R.layout.row_network : R.layout.row_message, null);
        }

        if (type == VIEW_NETWORK)
        {
            // get the network for this row
            Network network = networks.get(position);

            // set the name
            TextView name = (TextView) convertView.findViewById(R.id.network_name);
            name.setText(network.name);
        }
        else
        {
			TextView msg = (TextView) convertView.findViewById(R.id.message);
			msg.setText(R.string.no_networks);
        }

		// return the view
        return convertView;
    }
}
