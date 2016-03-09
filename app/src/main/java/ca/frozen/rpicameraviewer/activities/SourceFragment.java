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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
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
		// save the source
		this.source = source;
		this.forCamera = forCamera;

		setConnectionTypes();
		if (!forCamera)
		{
			View view = getView();
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
		}
		setViews();
	}

	//******************************************************************************
	// setConnectionTypes
	//******************************************************************************
	private void setConnectionTypes()
	{
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
		}
	}

	//******************************************************************************
	// configureConnectionType
	//******************************************************************************
	private void configureConnectionType(View view, Source.ConnectionType connectionType)
	{
		Settings settings = Utils.getSettings();
		EditText addressEdit = (EditText) view.findViewById(R.id.source_address);
		String address = addressEdit.getText().toString().trim();
		int port = getNumber(view, R.id.source_port);
		switch (connectionType)
		{
			case RawTcpIp:
				addressEdit.setText((address.isEmpty() || checkMulticastAddress(address) >= 0) ? Utils.getBaseIpAddress() : address);
				if (port == settings.rawHttpSource.port || port == settings.rawMulticastSource.port || port < 0)
				{
					port = settings.rawTcpIpSource.port;
				}
				break;
			case RawHttp:
				addressEdit.setText((address.isEmpty() || checkMulticastAddress(address) >= 0) ? Utils.getBaseIpAddress() : address);
				if (port == settings.rawTcpIpSource.port || port == settings.rawMulticastSource.port || port < 0)
				{
					port = settings.rawHttpSource.port;
				}
				break;
			case RawMulticast:
				if (address.isEmpty() || checkMulticastAddress(address) < 0)
				{
					addressEdit.setText(settings.rawMulticastSource.address);
				}
				if (port == settings.rawTcpIpSource.port || port == settings.rawHttpSource.port || port < 0)
				{
					port = settings.rawMulticastSource.port;
				}
				break;
		}
		setNumber(view, R.id.source_port, port);
	}

	//******************************************************************************
	// setViews
	//******************************************************************************
	private void setViews()
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
	public Source getSource()
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
		if (camera.source.connectionType == Source.ConnectionType.RawTcpIp &&
			!Patterns.WEB_URL.matcher(camera.source.address).matches())
		{
			App.error(getActivity(), R.string.error_bad_address);
			return false;
		}

		if (camera.source.connectionType == Source.ConnectionType.RawHttp)
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
