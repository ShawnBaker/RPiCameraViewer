package ca.frozen.rpicameraviewer.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import ca.frozen.rpicameraviewer.R;

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

		// set the views
		TextView text = (TextView)findViewById(R.id.help_info);
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
