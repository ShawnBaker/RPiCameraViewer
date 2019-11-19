// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.TelemetryService;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.PacketParser;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class VideoActivity extends AppCompatActivity implements VideoFragment.OnFadeListener
{
	// public constants
	public final static String CAMERA = "camera";

	private final TelemetryReceiver telemetryReceiver;
	private final PacketParser telemetryParser;

	// instance variables
	private FrameLayout frameLayout;
	private VideoFragment videoFragment;
	private int localUDPPort;

	VideoActivity()
	{
		telemetryParser = new PacketParser(
				PacketParser.DEFAULT_SoM,
				PacketParser.DEFAULT_EoM,
				new PacketParser.PacketDescriptor<Integer>('S', 4, true, 1, 2),
				new PacketParser.PacketDescriptor<Integer>('A', 4, true, 1, 2),
				new PacketParser.PacketDescriptor<Integer>('V', 4, true, 1, 2),
				new PacketParser.PacketDescriptor<Integer>('T', 4, true, 1, 2),
				new PacketParser.PacketDescriptor<Integer>('L', 4, true, 1, 2),
				new PacketParser.PacketDescriptor<Integer>('F', 4, true, 1, 2),
				new PacketParser.PacketDescriptor<Integer>('B', 4, true, 1, 2),
				new PacketParser.PacketDescriptor<Integer>('P', 8, true, 1, 4),
				new PacketParser.PacketDescriptor<Integer>('O', 4)
		);

		telemetryReceiver = new TelemetryReceiver();
	}

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// load the settings and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		Camera camera = data.getParcelable(CAMERA);
		Log.info("camera: " + camera.toString());

		localUDPPort = Utils.getSettings().telemetryPort;

		// get the frame layout, handle system visibility changes
		frameLayout = findViewById(R.id.video);
		frameLayout.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
		{
			@Override
			public void onSystemUiVisibilityChange(int visibility)
			{
				if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
				{
					videoFragment.startFadeIn();
				}
			}
		});

		// set full screen layout
		int visibility = frameLayout.getSystemUiVisibility();
		visibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
		frameLayout.setSystemUiVisibility(visibility);

		// create the video fragment
		videoFragment = VideoFragment.newInstance(camera, true);
		FragmentTransaction fragTran = getSupportFragmentManager().beginTransaction();
		fragTran.add(R.id.video, videoFragment);
		fragTran.commit();

		registerTelemetryReceiver();

		startTelemetryService();
	}

	@Override
	protected void onStop() {
		super.onStop();

		stopTelemetryService();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(telemetryReceiver);
	}

	//******************************************************************************
	// onStartFadeIn
	//******************************************************************************
	@Override
	public void onStartFadeIn()
	{
	}

	//******************************************************************************
	// onStartFadeOut
	//******************************************************************************
	@Override
	public void onStartFadeOut()
	{
		// hide the status and navigation bars
		int visibility = frameLayout.getSystemUiVisibility();
		visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		frameLayout.setSystemUiVisibility(visibility);
	}

	//******************************************************************************
	// onBackPressed
	//******************************************************************************
	@Override
	public void onBackPressed()
	{
		videoFragment.stop();
		super.onBackPressed();
	}

	private void startTelemetryService() {
		final Intent intent = new Intent(this, TelemetryService.class);

		intent.putExtra(TelemetryService.LOCAL_PORT_KEY, localUDPPort);

		this.startService(intent);
	}

	private void stopTelemetryService() {
		final Intent intent = new Intent(this, TelemetryService.class);

		stopService(intent);
	}

	private void registerTelemetryReceiver() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(TelemetryService.TELEMETRY_BROADCAST_KEY);
		registerReceiver(telemetryReceiver, intentFilter);
	}

	private void telemetryTask(final String message) {
		if (message == null) return;

		for (char c : message.toCharArray()) {
			final PacketParser.Result result = telemetryParser.parse(c);

			if (result == PacketParser.Result.completed) {
				final String id = telemetryParser.getId();
				final int index = telemetryParser.getIndex() - 1;

				switch (id) {
					case "S":
						break;
					case "T":
						break;
					case "A":
						break;
					case "V":
						break;
					case "L":
						break;
					case "F":
						break;
					case "B":
						break;
					case "P":
						break;
					case "O":
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								final int value =  telemetryParser.getValue();

								if(value==0)
									videoFragment.rotateCamera(0, false);
								else
									videoFragment.rotateCamera(180, false);
							}
						});
						break;
				}

			} else if (result == PacketParser.Result.error) {
				PacketParser.Error parseError = telemetryParser.getError();

				Log.error(String.format("Parser error: %s",parseError.toString()));
			}
		}
	}

	class TelemetryReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			try {
				Log.debug("onReceive()");

				final String message = intent.getStringExtra(TelemetryService.TELEMETRY_MESSAGE_KEY);

				telemetryTask(message);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
