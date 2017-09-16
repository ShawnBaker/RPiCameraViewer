// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.HttpReader;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.TcpIpReader;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class ScannerFragment extends DialogFragment
{
	// instance variables
	private WeakReference<DeviceScanner> scannerWeakRef;
	private TextView message, status;
	private ProgressBar progress;
	private Button cancelButton = null;
	private Runnable dismissRunner;
	private Handler dismissHandler = new Handler();

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// load the settings and cameras
		Utils.loadData();

		// create and run the scanner asynchronously
		DeviceScanner scanner = new DeviceScanner(this);
		scannerWeakRef = new WeakReference<>(scanner);
		scanner.execute();
	}

	//******************************************************************************
	// onCreateView
	//******************************************************************************
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// get the controls
		View view = inflater.inflate(R.layout.fragment_scanner, container, false);
		message = (TextView)view.findViewById(R.id.scanner_message);
		status = (TextView)view.findViewById(R.id.scanner_status);
		progress = (ProgressBar)view.findViewById(R.id.scanner_progress);
		cancelButton = (Button)view.findViewById(R.id.scanner_cancel);

		// configure the dialog
		Dialog dialog = getDialog();
		if (dialog != null)
		{
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnKeyListener(new DialogInterface.OnKeyListener()
			{
				@Override
				public boolean onKey(android.content.DialogInterface dialog, int keyCode, android.view.KeyEvent event)
				{
					if ((keyCode == android.view.KeyEvent.KEYCODE_BACK))
					{
						cancel();
					}
					return false;
				}
			});
		}

		// handle the cancel button
		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				cancel();
				dismiss();
			}
		});

		// create the dismiss handler and runnable
		dismissHandler = new Handler();
		dismissRunner = new Runnable()
		{
			@Override
			public void run()
			{
				dismiss();
			}
		};

		// configure the view if we've already done the scan
		if (savedInstanceState != null)
		{
			DeviceScanner scanner = (scannerWeakRef != null) ? scannerWeakRef.get() : null;
			if (scanner != null)
			{
				boolean complete = scanner.isComplete();
				scanner.setStatus(complete);
				if (complete)
				{
					cancelButton.setText(App.getStr(R.string.done));
				}
			}
		}

		return view;
	}

	//******************************************************************************
	// onDestroyView
	//******************************************************************************
	@Override
	public void onDestroyView()
	{
		dismissHandler.removeCallbacks(dismissRunner);
		if (getDialog() != null && getRetainInstance())
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	//******************************************************************************
	// cancel
	//******************************************************************************
	private void cancel()
	{
		Log.info("cancel");
		DeviceScanner scanner = (scannerWeakRef != null) ? scannerWeakRef.get() : null;
		if (scanner != null)
		{
			scanner.cancel(true);
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// DeviceScanner
	////////////////////////////////////////////////////////////////////////////////
	private class DeviceScanner extends AsyncTask<Void, Void, Void>
	{
		// local constants
		private final static int NO_DEVICE = -1;
		private final static int NUM_THREADS = 42;
		private final static int SLEEP_TIMEOUT = 10;
		private final static int DISMISS_TIMEOUT = 1500;

		// instance variables
		private WeakReference<ScannerFragment> fragmentWeakRef;
		private String ipAddress, network;
		private int device, numDone;
		private List<Camera> cameras, newCameras;
		private Settings settings;

		//******************************************************************************
		// DeviceScanner
		//******************************************************************************
		private DeviceScanner(ScannerFragment fragment)
		{
			fragmentWeakRef = new WeakReference<>(fragment);
		}

		//******************************************************************************
		// onPreExecute
		//******************************************************************************
		@Override
		protected void onPreExecute()
		{
			// get our IP address and the default port
			network = Utils.getNetworkName();
			ipAddress = Utils.getLocalIpAddress();
			settings = Utils.getSettings();
			device = 0;
			numDone = 0;
			cameras = Utils.getNetworkCameras(network);
			newCameras = new ArrayList<>();
			Log.info("onPreExecute: " + network + "," + ipAddress + "," + settings.toString());
		}

		//******************************************************************************
		// doInBackground
		//******************************************************************************
		@Override
		protected Void doInBackground(Void... params)
		{
			Log.info("onPreExecute");
			if (ipAddress != null && !ipAddress.isEmpty())
			{
				int i = ipAddress.lastIndexOf('.');
				final int myDevice = Integer.parseInt(ipAddress.substring(i + 1));
				final String baseAddress = ipAddress.substring(0, i + 1);
				Runnable runner = new Runnable()
				{
					@Override
					public void run()
					{
						for (int dev = getNextDevice(); !isCancelled() && dev != NO_DEVICE; dev = getNextDevice())
						{
							if (dev == myDevice)
							{
								doneDevice(dev);
								continue;
							}
							boolean found = false;
							String address = baseAddress + Integer.toString(dev);

							// look for a TCP/IP connection
							try
							{
								// try to connect to the device
								Socket socket = TcpIpReader.getConnection(address, settings.rawTcpIpSource.port);
								if (socket != null)
								{
									Camera camera = new Camera(Source.ConnectionType.RawTcpIp, network, address, settings.rawTcpIpSource.port);
									addCamera(camera);
									socket.close();
									found = true;
								}
							}
							catch (Exception ex) {}

							// look for an HTTP connection
							if (!found)
							{
								try
								{
									HttpURLConnection http = HttpReader.getConnection(address, settings.rawHttpSource.port, true);
									if (http != null)
									{
										InputStream stream = http.getInputStream();
										byte[] buffer = new byte[1024];
										int len = stream.read(buffer);
										String page = new String(buffer, 0, len);
										if (page.contains("UV4L Streaming Server"))
										{
											address += "/stream/video.h264";
											Camera camera = new Camera(Source.ConnectionType.RawHttp, network, address, settings.rawHttpSource.port);
											addCamera(camera);
											http.disconnect();
											found = true;
										}
									}
								}
								catch (Exception ex) {}
							}
							doneDevice(dev);
						}
					}
				};

				// create and start the threads
				for (int t = 0; t < NUM_THREADS; t++)
				{
					Thread thread = new Thread(runner);
					thread.start();
				}

				// wait for the threads to finish
				while (!isCancelled() && numDone < 254)
				{
					SystemClock.sleep(SLEEP_TIMEOUT);
				}

				// add the new cameras
				if (!isCancelled() && newCameras.size() > 0)
				{
					addCameras();
				}
				setStatus(true);
			}
			return null;
		}

		//******************************************************************************
		// onPostExecute
		//******************************************************************************
		@Override
		protected void onPostExecute(Void unused)
		{
			Log.info("onPostExecute");
			final MainActivity activity = getActivity(cancelButton);
			if (activity != null)
			{
				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
						cancelButton.setText(App.getStr(R.string.done));
						if (newCameras.size() > 0)
						{
							activity.updateCameras();
							dismissHandler.postDelayed(dismissRunner, DISMISS_TIMEOUT);
						}
					}
				});
			}
		}

		//******************************************************************************
		// addCameras
		//******************************************************************************
		private void addCameras()
		{
			// sort the new cameras by IP address
			Log.info("addCameras");
			Collections.sort(newCameras, new Comparator<Camera>()
			{
				@Override
				public int compare(Camera camera1, Camera camera2)
				{
					int octet1 = getLastOctet(camera1.source.address);
					int octet2 = getLastOctet(camera2.source.address);
					return octet1 - octet2;
				}
			});

			// get the maximum number from the existing camera names
			int max = Utils.getMaxCameraNumber(cameras);

			// set the camera names and add the new cameras to the list of all cameras
			String defaultName = Utils.getDefaultCameraName() + " ";
			List<Camera> allCameras = Utils.getCameras();
			for (Camera camera : newCameras)
			{
				camera.name = defaultName + ++max;
				allCameras.add(camera);
				Log.info("camera: " + camera.toString());
			}
		}

		//******************************************************************************
		// getLastOctet
		//******************************************************************************
		private int getLastOctet(String address)
		{
			String ip = address;
			int i = ip.indexOf("://");
			if (i != -1)
			{
				ip = ip.substring(i + 3);
			}
			i = ip.indexOf("?");
			if (i != -1)
			{
				ip = ip.substring(0, i);
			}
			i = ip.indexOf("/");
			if (i != -1)
			{
				ip = ip.substring(0, i);
			}
			String[] octets = ip.split("\\.");
			int octet = -1;
			try
			{
				octet = Integer.parseInt(octets[3]);
			}
			catch (Exception ex) {}
			return octet;
		}

		//******************************************************************************
		// addCamera
		//******************************************************************************
		private synchronized void addCamera(Camera newCamera)
		{
			boolean found = false;
			for (Camera camera : cameras)
			{
				if (newCamera.source.address.equals(camera.source.address) &&
					newCamera.source.port == camera.source.port)
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				Log.info("addCamera: " + newCamera.source.toString());
				newCameras.add(newCamera);
			}
		}

		//******************************************************************************
		// getNextDevice
		//******************************************************************************
		private synchronized int getNextDevice()
		{
			if (device < 254)
			{
				device++;
				return device;
			}
			return NO_DEVICE;
		}

		//******************************************************************************
		// doneDevice
		//******************************************************************************
		private synchronized void doneDevice(int device)
		{
			numDone++;
			setStatus(false);
		}

		//******************************************************************************
		// setStatus
		//******************************************************************************
		private synchronized void setStatus(final boolean last)
		{
			MainActivity activity = getActivity(status);
			if (activity != null)
			{
				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
						message.setText(String.format(App.getStr(R.string.scanning_on_ports),
										settings.rawTcpIpSource.port, settings.rawHttpSource.port));
						progress.setProgress(numDone);
						status.setText(String.format(App.getStr(R.string.num_new_cameras_found), newCameras.size()));
						if (newCameras.size() > 0)
						{
							status.setTextColor(ContextCompat.getColor(App.getContext(), R.color.good_text));
						}
						else if (last)
						{
							status.setTextColor(ContextCompat.getColor(App.getContext(), R.color.bad_text));
						}
					}
				});
			}
		}

		//******************************************************************************
		// getActivity
		//******************************************************************************
		private MainActivity getActivity(View view)
		{
			MainActivity activity = null;
			if (view != null)
			{
				ScannerFragment fragment = (fragmentWeakRef != null) ? fragmentWeakRef.get() : null;
				if (fragment != null)
				{
					activity = (MainActivity)fragment.getActivity();
				}
			}
			return activity;
		}

		//******************************************************************************
		// isComplete
		//******************************************************************************
		public boolean isComplete()
		{
			return device == 255;
		}
	}
}