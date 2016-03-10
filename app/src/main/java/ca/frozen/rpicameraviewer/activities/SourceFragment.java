// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class SourceFragment extends Fragment
{
	// public constants
	public final static int MIN_PORT = 1;
	public final static int MAX_PORT = 65535;

	// instance variables
	private Source source;
	private boolean forCamera;
	private List<Source.ConnectionType> connectionTypes;

	//******************************************************************************
	// onCreateView
	//******************************************************************************
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_source, container, false);
	}

	//******************************************************************************
	// configure
	//******************************************************************************
	public void configure(Source source, boolean forCamera)
	{
		// save the parameters
		this.source = source;
		this.forCamera = forCamera;

		final View view = getView();
		if (forCamera)
		{
			Spinner spinner = (Spinner) view.findViewById(R.id.source_connection_type);
			spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
				{
					configureConnectionType(view, connectionTypes.get(position));
				}

				@Override
				public void onNothingSelected(AdapterView<?> parentView)
				{
				}
			});
			connectionTypes = new ArrayList<>();
			connectionTypes.add(Source.ConnectionType.RawTcpIp);
			connectionTypes.add(Source.ConnectionType.RawHttp);
			connectionTypes.add(Source.ConnectionType.RawMulticast);
			configureConnectionType(view, getConnectionType(view));
		}
		else
		{
			LinearLayout layout = (LinearLayout) view.findViewById(R.id.source_connection_type_row);
			layout.setVisibility(View.GONE);
			if (source.connectionType != Source.ConnectionType.RawMulticast)
			{
				layout = (LinearLayout) view.findViewById(R.id.source_address_row);
				layout.setVisibility(View.GONE);
			}
			EditText edit = (EditText) view.findViewById(R.id.source_port);
			edit.setHint("");
			edit = (EditText) view.findViewById(R.id.source_width);
			edit.setHint(R.string.use_stream_width);
			edit = (EditText) view.findViewById(R.id.source_height);
			edit.setHint(R.string.use_stream_height);
			edit = (EditText) view.findViewById(R.id.source_fps);
			edit.setHint(R.string.dont_set);
			edit = (EditText) view.findViewById(R.id.source_bps);
			edit.setHint(R.string.dont_set);
		}

		// set the view values
		setViews(source);
	}

	//******************************************************************************
	// configureConnectionType
	//******************************************************************************
	private void configureConnectionType(View view, Source.ConnectionType connectionType)
	{
		// get the current values and settings
		Source newSource = getSource();
		Settings settings = Utils.getSettings();

		// adjust the values for the new connection type
		switch (connectionType)
		{
			case RawTcpIp:
				if (newSource.address.isEmpty() || checkMulticastAddress(newSource.address) >= 0)
				{
					newSource.address = Utils.getBaseIpAddress();
				}
				adjustValues(newSource, settings.rawHttpSource, settings.rawMulticastSource, settings.rawTcpIpSource);
				break;
			case RawHttp:
				if (newSource.address.isEmpty() || checkMulticastAddress(newSource.address) >= 0)
				{
					newSource.address = Utils.getBaseIpAddress();
				}
				adjustValues(newSource, settings.rawTcpIpSource, settings.rawMulticastSource, settings.rawHttpSource);
				break;
			case RawMulticast:
				if (newSource.address.isEmpty() || checkMulticastAddress(newSource.address) < 0)
				{
					newSource.address = settings.rawMulticastSource.address;
				}
				adjustValues(newSource, settings.rawTcpIpSource, settings.rawHttpSource, settings.rawMulticastSource);
				break;
		}

		// update the view values
		setViews(newSource);
	}

	//******************************************************************************
	// adjustValues
	//******************************************************************************
	private void adjustValues(Source newSource, Source fromSource1, Source fromSource2, Source toSource)
	{
		if (newSource.port < 0 || newSource.port == fromSource1.port || newSource.port == fromSource2.port)
		{
			newSource.port = toSource.port;
		}
		if (newSource.width < 0 || newSource.width == fromSource1.width || newSource.width == fromSource2.width)
		{
			newSource.width = toSource.width;
		}
		if (newSource.height < 0 || newSource.height == fromSource1.height || newSource.height == fromSource2.height)
		{
			newSource.height = toSource.height;
		}
		if (newSource.fps < 0 || newSource.fps == fromSource1.fps || newSource.fps == fromSource2.fps)
		{
			newSource.fps = toSource.fps;
		}
		if (newSource.bps < 0 || newSource.bps == fromSource1.bps || newSource.bps == fromSource2.bps)
		{
			newSource.bps = toSource.bps;
		}
	}

	//******************************************************************************
	// setViews
	//******************************************************************************
	private void setViews(Source source)
	{
		// get the view
		View view = getView();

		// set the connection type
		if (forCamera)
		{
			Spinner spinner = (Spinner) view.findViewById(R.id.source_connection_type);
			if (spinner.getVisibility() == View.VISIBLE)
			{
				for (int i = 0; i < connectionTypes.size(); i++)
				{
					Source.ConnectionType connectionType = connectionTypes.get(i);
					if (connectionType == source.connectionType)
					{
						spinner.setSelection(i);
						break;
					}
				}
			}
		}

		// set the address
		if (forCamera || source.connectionType == Source.ConnectionType.RawMulticast)
		{
			EditText edit = (EditText) view.findViewById(R.id.source_address);
			edit.setText(source.address);
		}

		// set the numeric values
		setNumber(view, R.id.source_port, source.port);
		setNumber(view, R.id.source_width, source.width);
		setNumber(view, R.id.source_height, source.height);
		setNumber(view, R.id.source_fps, source.fps);
		setNumber(view, R.id.source_bps, source.bps);
	}

	//******************************************************************************
	// getSource
	//******************************************************************************
	private Source getSource()
	{
		// get the view and create a new source
		View view = getView();
		Source editedSource = new Source(source);

		// get the connection type
		if (forCamera)
		{
			editedSource.connectionType = getConnectionType(view);
		}

		// get the address
		if (forCamera || source.connectionType == Source.ConnectionType.RawMulticast)
		{
			EditText edit = (EditText) view.findViewById(R.id.source_address);
			editedSource.address = edit.getText().toString().trim();
		}
		else
		{
			editedSource.address = "";
		}

		// get the numeric values
		editedSource.port = getNumber(view, R.id.source_port);
		editedSource.width = getNumber(view, R.id.source_width);
		editedSource.height = getNumber(view, R.id.source_height);
		editedSource.fps = getNumber(view, R.id.source_fps);
		editedSource.bps = getNumber(view, R.id.source_bps);

		return editedSource;
	}

	//******************************************************************************
	// getAndCheckEditedSource
	//******************************************************************************
	public Source getAndCheckEditedSource()
	{
		// get the updated source
		Source editedSource = getSource();

		// check the address
		if (forCamera || editedSource.connectionType == Source.ConnectionType.RawMulticast)
		{
			// make sure there's an address
			if (editedSource.address.isEmpty())
			{
				App.error(getActivity(), R.string.error_no_address);
				return null;
			}

			try
			{
				// check the address
				Uri uri = Uri.parse(editedSource.address);

				// check IP addresses
				String address = uri.getPath();
				char c = address.charAt(0);
				if (c >= '0' && c <= '9')
				{
					if (!checkIpAddress(address))
					{
						App.error(getActivity(), R.string.error_bad_address);
						return null;
					}
				}

				// use the port if it's there
				int port = uri.getPort();
				if (port != -1)
				{
					editedSource.address = editedSource.address.replace(":" + port, "");
					if (editedSource.port <= 0)
					{
						editedSource.port = port;
					}
				}

				// for HTTP, remove the scheme if it's there
				if (editedSource.connectionType == Source.ConnectionType.RawHttp)
				{
					String scheme = uri.getScheme();
					if (scheme != null && !scheme.isEmpty())
					{
						editedSource.address = editedSource.address.substring(scheme.length() + 3);
					}
				}
			}
			catch (Exception ex)
			{
				App.error(getActivity(), R.string.error_bad_address);
				return null;
			}

			// make sure it's a valid multicast address
			if (editedSource.connectionType == Source.ConnectionType.RawMulticast)
			{
				int check = checkMulticastAddress(editedSource.address);
				if (check < 0)
				{
					App.error(getActivity(), R.string.error_bad_multicast_address);
					return null;
				}
				else if (check == 0)
				{
					Toast.makeText(getActivity(), R.string.warning_multicast_address,
							Toast.LENGTH_LONG).show();
				}
			}
		}

		// for settings, make sure there's a port
		if (!forCamera)
		{
			if (editedSource.port == 0)
			{
				App.error(getActivity(), R.string.error_no_port);
				return null;
			}
		}

		// make sure the port is within range
		if (editedSource.port != 0 && (editedSource.port < MIN_PORT || editedSource.port > MAX_PORT))
		{
			App.error(getActivity(), String.format(getString(R.string.error_bad_port), MIN_PORT, MAX_PORT));
			return null;
		}

		// check the width
		if (editedSource.width < 0)
		{
			App.error(getActivity(), R.string.error_bad_width);
			return null;
		}

		// check the height
		if (editedSource.height < 0)
		{
			App.error(getActivity(), R.string.error_bad_height);
			return null;
		}

		// check the FPS
		if (editedSource.fps < 0)
		{
			App.error(getActivity(), R.string.error_bad_fps);
			return null;
		}

		// check the BPS
		if (editedSource.bps < 0)
		{
			App.error(getActivity(), R.string.error_bad_bps);
			return null;
		}

		// return the successfully edited source
		return editedSource;
	}

	//******************************************************************************
	// getConnectionType
	//******************************************************************************
	private Source.ConnectionType getConnectionType(View view)
	{
		Spinner spinner = (Spinner) view.findViewById(R.id.source_connection_type);
		return connectionTypes.get(spinner.getSelectedItemPosition());
	}

	//******************************************************************************
	// setNumber
	//******************************************************************************
	private void setNumber(View view, int id, int value)
	{
		EditText edit = (EditText) view.findViewById(id);
		edit.setText((value != 0) ? Integer.toString(value) : "");
	}

	//******************************************************************************
	// getNumber
	//******************************************************************************
	private int getNumber(View view, int id)
	{
		int value = 0;
		EditText edit = (EditText) view.findViewById(id);
		String text = edit.getText().toString().trim();
		if (text.length() > 0)
		{
			try
			{
				value = Integer.parseInt(text);
			}
			catch (Exception ex)
			{
				value = -1;
			}
		}
		return value;
	}

	//******************************************************************************
	// checkIpAddress
	//******************************************************************************
	private boolean checkIpAddress(String address)
	{
		String octets[] = address.split("\\.");
		boolean result = false;
		if (octets.length == 4)
		{
			int octet1, octet2, octet3, octet4;
			try
			{
				octet1 = getOctet(octets[0]);
				octet2 = getOctet(octets[1]);
				octet3 = getOctet(octets[2]);
				octet4 = getOctet(octets[3]);
				if (octet1 >= 0 && octet2 >= 0 && octet3 >= 0 && octet4 >= 0)
				{
					result = true;
				}
			}
			catch (Exception ec)
			{
			}
		}
		return result;
	}

	//******************************************************************************
	// checkMulticastAddress
	//******************************************************************************
	private int checkMulticastAddress(String address)
	{
		String octets[] = address.split("\\.");
		int result = -1;
		if (octets.length == 4)
		{
			int octet1, octet2, octet3, octet4;
			try
			{
				octet1 = getOctet(octets[0]);
				octet2 = getOctet(octets[1]);
				octet3 = getOctet(octets[2]);
				octet4 = getOctet(octets[3]);
				if (octet1 >= 0 && octet2 >= 0 && octet3 >= 0 && octet4 >= 0)
				{
					if (octet1 == 239)
					{
						result = 1;
					}
					else if (octet1 >= 224 && octet1 <= 239)
					{
						result = 0;
					}
				}
			}
			catch (Exception ec)
			{
			}
		}
		return result;
	}

	//******************************************************************************
	// getOctet
	//******************************************************************************
	private int getOctet(String octetStr)
	{
		int value = -1;
		try
		{
			value = Integer.parseInt(octetStr);
			if (value < 0 || value > 255)
			{
				value = -1;
			}
		}
		catch (Exception ec)
		{
		}
		return value;
	}
}
