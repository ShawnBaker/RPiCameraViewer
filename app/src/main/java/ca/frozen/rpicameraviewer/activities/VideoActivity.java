// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class VideoActivity extends AppCompatActivity implements VideoFragment.OnFadeListener
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "VideoActivity";

	// instance variables
	private Camera camera;
	private FrameLayout frameLayout;
	private VideoFragment videoFragment;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video);
		//Log.d(TAG, "onCreate");

		// load the settings and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = data.getParcelable(CAMERA);

		// get the frame layout, handle system visibility changes
		frameLayout = (FrameLayout) findViewById(R.id.video);
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
		videoFragment = videoFragment.newInstance(camera, true);
		FragmentTransaction fragTran = getSupportFragmentManager().beginTransaction();
		fragTran.add(R.id.video, videoFragment);
		fragTran.commit();
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
	// onRequestPermissionsResult
	//******************************************************************************
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		videoFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
}
