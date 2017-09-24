// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.BuildConfig;
import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Utils;

public class AboutActivity extends AppCompatActivity
{
	//******************************************************************************
	// onCreate
	//******************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// set the views
		ImageView icon = (ImageView)findViewById(R.id.about_icon);
		icon.setImageResource(R.drawable.logo);

		TextView text = (TextView)findViewById(R.id.about_title);
		text.setText(getString(R.string.app_name));

		text = (TextView)findViewById(R.id.about_version);
		text.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);

		text = (TextView)findViewById(R.id.about_license);
		text.setMovementMethod(LinkMovementMethod.getInstance());
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
	{
        if (item.getItemId() == android.R.id.home)
		{
			Log.info("finish");
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
    }
}
