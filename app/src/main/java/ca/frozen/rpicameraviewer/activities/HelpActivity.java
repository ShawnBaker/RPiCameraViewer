// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Utils;

public class HelpActivity extends AppCompatActivity
{
	//******************************************************************************
	// onCreate
	//******************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// set the views
		TextView text = findViewById(R.id.help_info);
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
