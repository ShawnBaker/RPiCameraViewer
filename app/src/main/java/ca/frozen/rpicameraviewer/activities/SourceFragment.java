package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

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
	public void configureForSettings()
	{
		final View view = getView();
		RadioGroup group = (RadioGroup) view.findViewById(R.id.source_multicast);
		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				configureMulticast(view, checkedId == R.id.source_multicast_on, false);
			}
		});
		RadioButton button = (RadioButton) view.findViewById(R.id.source_multicast_default);
		button.setVisibility(View.GONE);
		configureMulticast(view, getMulticast(view) == Source.Multicast.On, false);
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

	//******************************************************************************
	// configureForNetwork
	//******************************************************************************
	public void configureForNetwork()
	{
		final View view = getView();
		RadioGroup group = (RadioGroup) view.findViewById(R.id.source_multicast);
		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				configureMulticast(view, checkedId == R.id.source_multicast_on, false);
			}
		});
		configureMulticast(view, getMulticast(view) == Source.Multicast.On, false);
	}

	//******************************************************************************
	// configureForCamera
	//******************************************************************************
	public void configureForCamera()
	{
		final View view = getView();
		RadioGroup group = (RadioGroup) view.findViewById(R.id.source_multicast);
		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				configureMulticast(view, checkedId == R.id.source_multicast_on, true);
			}
		});
		configureMulticast(view, getMulticast(view) == Source.Multicast.On, true);
	}

	//******************************************************************************
	// configureMulticast
	//******************************************************************************
	private void configureMulticast(View view, boolean multicast, boolean forCamera)
	{
		TextView tv = (TextView) view.findViewById(R.id.source_address_prompt);
		EditText edit = (EditText) view.findViewById(R.id.source_address);
		if (!multicast)
		{
			edit.setText(forCamera ? (source.address.isEmpty() ? Utils.getBaseIpAddress() : source.address) : "");
		}
		else
		{
			String address = edit.getText().toString().trim();
			if (address.length() == 0 || !isMulticastAddress(address))
			{
				edit.setText("224.0.0.1");
			}
		}
		edit.setEnabled(multicast || forCamera);
		tv.setEnabled(multicast || forCamera);
	}

	//******************************************************************************
	// getSource
	//******************************************************************************
	public Source getSource()
	{
		// get the view and create a new source
		View view = getView();
		Source source = new Source();

		// get the multicast
		source.multicast = getMulticast(view);

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
	// setSource
	//******************************************************************************
	public void setSource(Source source)
	{
		// save the source
		this.source = source;

		// get the view
		View view = getView();

		// set the multicast
		RadioGroup multicast = (RadioGroup) view.findViewById(R.id.source_multicast);
		int id = R.id.source_multicast_default;
		if (source.multicast == Source.Multicast.Off)
		{
			id = R.id.source_multicast_off;
		}
		else if (source.multicast == Source.Multicast.On)
		{
			id = R.id.source_multicast_on;
		}
		multicast.check(id);

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
		if (editedSource.multicast == Source.Multicast.On)
		{
			// make sure there's an address
			if (editedSource.address.isEmpty())
			{
				App.error(getActivity(), R.string.error_no_address);
				return false;
			}

			// make sure it's a valid multicast address
			if (!isMulticastAddress(editedSource.address))
			{
				App.error(getActivity(), R.string.error_bad_multicast_address);
				return false;
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
		Source.Multicast multicast = camera.getMulticast();
		if (multicast == Source.Multicast.Off &&
			!Patterns.WEB_URL.matcher(camera.source.address).matches())
		{
			App.error(getActivity(), R.string.error_bad_address);
			return false;
		}

		// indicate success
		return true;
	}

	//******************************************************************************
	// getMulticast
	//******************************************************************************
	private Source.Multicast getMulticast(View view)
	{
		RadioGroup multicast = (RadioGroup) view.findViewById(R.id.source_multicast);
		int id = multicast.getCheckedRadioButtonId();
		if (id == R.id.source_multicast_off)
		{
			return Source.Multicast.Off;
		}
		else if (id == R.id.source_multicast_on)
		{
			return Source.Multicast.On;
		}
		return Source.Multicast.UseDefault;
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
	// isMulticastAddress
	//******************************************************************************
	private boolean isMulticastAddress(String address)
	{
		String octets[] = address.split("\\.");
		boolean ok = false;
		if (octets.length == 4)
		{
			int octet1, octet2, octet3, octet4;
			try
			{
				octet1 = Integer.parseInt(octets[0]);
				octet2 = Integer.parseInt(octets[1]);
				octet3 = Integer.parseInt(octets[2]);
				octet4 = Integer.parseInt(octets[3]);
				if (octet1 == 224 && octet2 == 0 && octet3 == 0)
				{
					ok = true;
				}
			}
			catch (Exception ec)
			{
			}
		}
		return ok;
	}
}
