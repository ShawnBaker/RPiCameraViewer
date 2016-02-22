// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class VideoActivity extends AppCompatActivity
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "VideoActivity";

	// instance variables
	private Camera camera;
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
		Log.d(TAG, "onCreate");

		// load the settings, networks and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = data.getParcelable(CAMERA);

		// create the video fragment
		videoFragment = videoFragment.newInstance(camera);
		FragmentTransaction fragTran = getSupportFragmentManager().beginTransaction();
		fragTran.add(R.id.video, videoFragment);
		fragTran.commit();
	}
}
