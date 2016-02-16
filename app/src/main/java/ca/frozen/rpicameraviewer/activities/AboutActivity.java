package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.BuildConfig;
import ca.frozen.rpicameraviewer.R;

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

		// set the views
		ImageView icon = (ImageView)findViewById(R.id.about_icon);
		icon.setImageResource(R.drawable.logo);

		TextView text = (TextView)findViewById(R.id.about_title);
		text.setText(App.getStr(R.string.app_name));

		text = (TextView)findViewById(R.id.about_version);
		text.setText(App.getStr(R.string.version) + " " + BuildConfig.VERSION_NAME);

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
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
    }
}
