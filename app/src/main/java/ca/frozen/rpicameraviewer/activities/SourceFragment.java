// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Network;
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
	// configureForSettings
	//******************************************************************************
	public void configureForSettings(Source source)
	{
		// save the source
		this.source = source;

		final View view = getView();
		setConnectionTypes(false, false);
		EditText edit = (EditText) view.findViewById(R.id.source_port);
		edit.setHint("");
		edit = (EditText) view.findViewById(R.id.source_width);
		edit.setHint(R.string.use_stream_width);
		edit = (EditText) view.findViewById(R.id.source_height);
		edit.setHint(R.string.use_stream_height);
		edit = (EditText) view.findViewById(R.id.source_fps);
		edit.setHint("");
		edit = (EditText) view.findViewById(R.id.source_bps);
		edit.setHint("");
		setViews();
	}

	//******************************************************************************
	// configureForNetwork
	//******************************************************************************
	public void configureForNetwork(Source source)
	{
		// save the source
		this.source = source;

		setConnectionTypes(true, false);
		setViews();
	}

	//******************************************************************************
	// configureForCamera
	//******************************************************************************
	public void configureForCamera(Source source)
	{
		// save the source
		this.source = source;

		setConnectionTypes(true, true);
		setViews();
	}

	//******************************************************************************
	// getSource
	//******************************************************************************
	public Source getSource()
	{
		// get the view and create a new source
		View view = getView();
		Source source = new Source();

		// get the connection type
		source.connectionType = getConnectionType(view);

		// get the address
		EditText edit = (EditText) view.findViewById(R.id.source_address);
		source.address = edit.getText().toString().trim();

		// get the numeric values
		source.port = getNumber(view, R.id.source_port);
		source.width = getNumber(view, R.id.source_width);
		source.height = getNumber(view, R.id.source_height);
		source.fps = getNumber(view, R.id.source_fps);
		source.bps = getNumber(view, R.id.source_bps);

		return source;
	}

	//******************************************************************************
	// setConnectionTypes
	//******************************************************************************
	private void setConnectionTypes(boolean includeDefault, final boolean forCamera)
	{
		final View view = getView();
		Spinner spinner = (Spinner) view.findViewById(R.id.source_connection_type);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
			{
				configureConnectionType(view, connectionTypes.get(position), forCamera);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView)
			{
			}
		});
		List<String> names = new ArrayList<String>();
		connectionTypes = new ArrayList<Source.ConnectionType>();
		if (includeDefault)
		{
			names.add(App.getStr(R.string.default1));
			connectionTypes.add(Source.ConnectionType.Default);
		}
		names.add(App.getStr(R.string.raw_tcp_ip));
		connectionTypes.add(Source.ConnectionType.RawTcpIp);
		names.add(App.getStr(R.string.raw_multicast));
		connectionTypes.add(Source.ConnectionType.RawMulticast);
		names.add(App.getStr(R.string.raw_http));
		connectionTypes.add(Source.ConnectionType.RawHttp);
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, names);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);
		configureConnectionType(view, getConnectionType(view), false);
	}

	//******************************************************************************
	// configureConnectionType
	//******************************************************************************
	private void configureConnectionType(View view, Source.ConnectionType connectionType, boolean forCamera)
	{
		EditText addressEdit = (EditText) view.findViewById(R.id.source_address);
		String address = addressEdit.getText().toString().trim();
		int port = getNumber(view, R.id.source_port);
		int defaultPort = App.getInt(R.integer.default_port);
		int defaultHttpPort = App.getInt(R.integer.default_http_port);
		if (connectionType == Source.ConnectionType.RawMulticast)
		{
			if (address.length() == 0 || checkMulticastAddress(address) < 0)
			{
				addressEdit.setText("239.0.0.0");
			}
			if (port == defaultHttpPort || port < 0)
			{
				port = defaultPort;
			}
		}
		else if (connectionType == Source.ConnectionType.RawHttp)
		{
			addressEdit.setText(forCamera ? (address.isEmpty() ? Utils.getBaseIpAddress() : address) : "");
			if (port == defaultPort || port < 0)
			{
				port = defaultHttpPort;
			}
		}
		else
		{
			addressEdit.setText(forCamera ? (address.isEmpty() ? Utils.getBaseIpAddress() : address) : "");
			if (port == defaultHttpPort || port < 0)
			{
				port = defaultPort;
			}
		}
		setNumber(view, R.id.source_port, port);

		// enable/disable the address and its prompt
		boolean enableAddress = connectionType == Source.ConnectionType.RawMulticast || forCamera;
		addressEdit.setEnabled(enableAddress);
		TextView prompt = (TextView) view.findViewById(R.id.source_address_prompt);
		prompt.setEnabled(enableAddress);
	}

	//******************************************************************************
	// setViews
	//******************************************************************************
	private void setViews()
	{
		// get the view
		View view = getView();

		// set the connection type
		Spinner spinner = (Spinner) view.findViewById(R.id.source_connection_type);
		for (int i = 0; i < connectionTypes.size(); i++)
		{
			Source.ConnectionType connectionType = connectionTypes.get(i);
			if (connectionType == source.connectionType)
			{
				spinner.setSelection(i);
				break;
			}
		}

		// set the address
		EditText edit = (EditText) view.findViewById(R.id.source_address);
		edit.setText(source.address);

		// set the numeric values
		setNumber(view, R.id.source_port, source.port);
		setNumber(view, R.id.source_width, source.width);
		setNumber(view, R.id.source_height, source.height);
		setNumber(view, R.id.source_fps, source.fps);
		setNumber(view, R.id.source_bps, source.bps);
	}

	//******************************************************************************
	// checkCommon
	//******************************************************************************
	private boolean checkCommon(Source editedSource)
	{
		// check the address, remove the port if necessary
		if (!editedSource.address.isEmpty())
		{
			try
			{
				Uri uri = Uri.parse(editedSource.address);
				int port = uri.getPort();
				if (port != -1)
				{
					editedSource.address = editedSource.address.replace(":" + port, "");
					if (editedSource.port <= 0)
					{
						editedSource.port = port;
					}
				}
			}
			catch (Exception ex)
			{
				App.error(getActivity(), R.string.error_bad_address);
				return false;
			}
		}

		if (editedSource.connectionType == Source.ConnectionType.RawMulticast)
		{
			// make sure there's an address
			if (editedSource.address.isEmpty())
			{
				App.error(getActivity(), R.string.error_no_address);
				return false;
			}

			// make sure it's a valid multicast address
			int check = checkMulticastAddress(editedSource.address);
			if (check < 0)
			{
				App.error(getActivity(), R.string.error_bad_multicast_address);
				return false;
			}
			else if (check == 0)
			{
				Toast.makeText(getActivity(), R.string.warning_multicast_address,
								Toast.LENGTH_LONG).show();
			}
		}

		// make sure the port is within range
		if (editedSource.port != 0 && (editedSource.port < MIN_PORT || editedSource.port > MAX_PORT))
		{
			App.error(getActivity(), String.format(getString(R.string.error_bad_port), MIN_PORT, MAX_PORT));
			return false;
		}

		// check the width
		if (editedSource.width < 0)
		{
			App.error(getActivity(), R.string.error_bad_width);
			return false;
		}

		// check the height
		if (editedSource.height < 0)
		{
			App.error(getActivity(), R.string.error_bad_height);
			return false;
		}

		// check the FPS
		if (editedSource.fps < 0)
		{
			App.error(getActivity(), R.string.error_bad_fps);
			return false;
		}

		// check the BPS
		if (editedSource.bps < 0)
		{
			App.error(getActivity(), R.string.error_bad_bps);
			return false;
		}

		// indicate success
		return true;
	}

	//******************************************************************************
	// checkForSettings
	//******************************************************************************
	public boolean checkForSettings(Source editedSource)
	{
		// check the common errors first
		if (!checkCommon(editedSource))
		{
			return false;
		}

		// make sure there's a port
		if (editedSource.port == 0)
		{
			App.error(getActivity(), R.string.error_no_port);
			return false;
		}

		// indicate success
		return true;
	}

	//******************************************************************************
	// checkForNetwork
	//******************************************************************************
	public boolean checkForNetwork(Network network)
	{
		// check the common errors first
		if (!checkCommon(network.source))
		{
			return false;
		}

		// indicate success
		return true;
	}

	//******************************************************************************
	// checkForCamera
	//******************************************************************************
	public boolean checkForCamera(Camera camera)
	{
		// check the common errors first
		if (!checkCommon(camera.source))
		{
			return false;
		}

		// make sure the address is a valid URL
		Source.ConnectionType connectionType = camera.getConnectionType();
		if (connectionType == Source.ConnectionType.RawTcpIp &&
			!Patterns.WEB_URL.matcher(camera.source.address).matches())
		{
			App.error(getActivity(), R.string.error_bad_address);
			return false;
		}

		if (connectionType == Source.ConnectionType.RawHttp)
		{
			// check the address
			if (!camera.source.address.isEmpty())
			{
				try
				{
					Uri uri = Uri.parse(camera.source.address);
					String scheme = uri.getScheme();
					if (scheme != null && !scheme.isEmpty())
					{
						camera.source.address = camera.source.address.substring(scheme.length() + 3);
					}
				}
				catch (Exception ex)
				{
					App.error(getActivity(), R.string.error_bad_address);
					return false;
				}
			}
		}

		// indicate success
		return true;
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
