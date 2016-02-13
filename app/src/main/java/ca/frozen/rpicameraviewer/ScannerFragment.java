package ca.frozen.rpicameraviewer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ScannerFragment extends DialogFragment
{
	// local constants
	private final static String TAG = "DeviceScanner";

	// instance variables
	private WeakReference<DeviceScanner> scannerWeakRef;
	private TextView message, status;
	private ProgressBar progress;
	private Button cancelButton = null;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		DeviceScanner scanner = new DeviceScanner(this);
		scannerWeakRef = new WeakReference<DeviceScanner>(scanner);
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
			dialog.setTitle(R.string.scanning_for_cameras);
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

		// Watch for button clicks.
		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				cancel();
				dismiss();
			}
		});

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
		DeviceScanner scanner = (scannerWeakRef != null) ? scannerWeakRef.get() : null;
		if (scanner != null)
		{
			scanner.cancel(true);
		}
	}

	private class DeviceScanner extends AsyncTask<Void, Void, Void>
	{
		// local constants
		private final static String TAG = "DeviceScanner";
		private final static int NO_DEVICE = -1;
		private final static int NUM_THREADS = 42;
		private final static int TIMEOUT = 500;

		// instance variables
		private WeakReference<ScannerFragment> fragmentWeakRef;
		private String ipAddress;
		private int port, device;
		private List<Camera> cameras, newCameras;

		//******************************************************************************
		// DeviceScanner
		//******************************************************************************
		private DeviceScanner(ScannerFragment fragment)
		{
			fragmentWeakRef = new WeakReference<ScannerFragment>(fragment);
		}

		//******************************************************************************
		// onPreExecute
		//******************************************************************************
		@Override
		protected void onPreExecute()
		{
			// get our IP address and the default port
			ipAddress = Utils.getLocalIpAddress();
			port = Utils.getDefaultPort();
			device = 0;
			newCameras = new ArrayList<Camera>();
			cameras = Utils.loadCameras();
		}

		//******************************************************************************
		// doInBackground
		//******************************************************************************
		@Override
		protected Void doInBackground(Void... params)
		{
			if (ipAddress != null && !ipAddress.isEmpty())
			{
				final int timeout = TIMEOUT;
				int i = ipAddress.lastIndexOf('.');
				final int myDevice = Integer.parseInt(ipAddress.substring(i + 1));
				final String baseAddress = ipAddress.substring(0, i + 1);
				device = 0;
				Runnable runner = new Runnable()
				{
					@Override
					public void run()
					{
						for (int dev = getNextDevice(); !isCancelled() && dev != NO_DEVICE; dev = getNextDevice())
						{
							if (dev == myDevice)
							{
								continue;
							}
							try
							{
								// try to connect to the device
								String address = baseAddress + Integer.toString(dev);
								Socket socket = new Socket();
								InetSocketAddress socketAddress = new InetSocketAddress(address, port);
								socket.connect(socketAddress, timeout);

								// add a camera
								Log.d(TAG, address);
								String name = socketAddress.getHostName();
								if (name == null || name.equals(address))
								{
									name = "";
								}
								Camera camera = new Camera(name, address, port);
								addCamera(camera);

								// close the connection
								socket.close();
							}
							catch (Exception ex)
							{
								//String msg = ex.getMessage();
								//Log.d(TAG, msg);
							}
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
				while (!isCancelled() && device < 255)
				{
					SystemClock.sleep(TIMEOUT);
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
							activity.addScannedCameras(newCameras);
						}
					}
				});
			}
		}

		//******************************************************************************
		// addCamera
		//******************************************************************************
		private synchronized void addCamera(Camera newCamera)
		{
			boolean found = false;
			for (Camera camera : cameras)
			{
				if (newCamera.getNetwork().equals(camera.getNetwork()) &&
					newCamera.getAddress().equals(camera.getAddress()) &&
					newCamera.getPort() == camera.getPort())
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				if (newCamera.getName().isEmpty())
				{
					newCamera.setName(Utils.getNextCameraName(cameras));
				}
				cameras.add(newCamera);
				newCameras.add(newCamera);
			}
		}

		//******************************************************************************
		// getNextDevice
		//******************************************************************************
		private synchronized int getNextDevice()
		{
			if (device < 255)
			{
				device++;
				setStatus(false);
				return device;
			}
			return NO_DEVICE;
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
						message.setText(String.format(App.getStr(R.string.scanning_on_port), port));
						progress.setProgress(device);
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