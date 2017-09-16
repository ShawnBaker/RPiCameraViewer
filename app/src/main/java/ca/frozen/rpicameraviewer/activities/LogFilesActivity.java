// Copyright © 2017 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Utils;

public class LogFilesActivity extends AppCompatActivity
{
	// local constants
	private static final int BUFFER_SIZE = 16384;

	// instance variables
	private Button file1Button, file2Button;
	private TextView textView;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_files);

		// initialize the log file
		Utils.initLogFile(getClass().getSimpleName());

		// get the text view
		textView = (TextView)findViewById(R.id.log_file);
		textView.setMovementMethod(LinkMovementMethod.getInstance());

		// get the file buttons
		file1Button = (Button)findViewById(R.id.log_file_1);
		file2Button = (Button)findViewById(R.id.log_file_2);

		// handle the file 1 button
		file1Button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				loadLogFile(Log.getFile1(), R.string.no_file_1, file1Button, file2Button);
			}
		});

		// handle the file 2 button
		file2Button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				loadLogFile(Log.getFile2(), R.string.no_file_2, file2Button, file1Button);
			}
		});

		// handle the clear button
		Button clearButton = (Button)findViewById(R.id.log_file_clear);
		clearButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				clearLogFiles();
			}
		});

		// handle the email button
		Button emailButton = (Button)findViewById(R.id.log_file_email);
		emailButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				emailLogFiles();
			}
		});

		// load log file 1
		loadLogFile(Log.getFile1(), R.string.no_file_1, file1Button, file2Button);
	}

	//******************************************************************************
	// loadLogFile
	//******************************************************************************
	private void loadLogFile(File logFile, int no_file_id, Button thisButton, Button otherButton)
	{
		Log.info("load log file: " + logFile.getName());
		thisButton.setSelected(true);
		otherButton.setSelected(false);
		if (logFile.exists())
		{
			try
			{
				BufferedReader reader = new BufferedReader(new FileReader(logFile));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null)
				{
					builder.append(line + "\n");
				}
				reader.close();
				textView.setText(builder.toString());
			}
			catch (Exception ex)
			{
				textView.setText(no_file_id);
			}
		}
		else
		{
			textView.setText(no_file_id);
		}
	}

	//******************************************************************************
	// clearLogFiles
	//******************************************************************************
	private void clearLogFiles()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.ok_to_clear_logs);
		alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Log.clear();
				loadLogFile(Log.getFile1(), R.string.no_file_1, file1Button, file2Button);
				dialog.dismiss();
			}
		});
		alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		alert.show();
	}

	//******************************************************************************
	// emailLogFiles
	//******************************************************************************
	private void emailLogFiles()
	{
		// get the email intent
		Log.info("email log files");
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");

		// set the to and subject fields
		intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { App.getStr(R.string.email_address) } );
		intent.putExtra(Intent.EXTRA_SUBJECT, App.getStr(R.string.app_name) + " " + App.getStr(R.string.log_files));

		// get the ZIP file
		File dir = new File(getApplicationContext().getFilesDir(), "logs");
		String appName = App.getStr(R.string.app_name).replaceAll("\\s+", "");
		File zipFile = new File(dir, appName + "LogFiles.zip");
		try
		{
			// create the ZIP file
			FileOutputStream output = new FileOutputStream(zipFile);
			ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(output));

			// write the log files to the ZIP file
			addFileToZip(zip, Log.getFile1());
			if (Log.getFile2().exists())
			{
				addFileToZip(zip, Log.getFile2());
			}

			// close the files
			zip.close();
			output.close();
		}
		catch (Exception ex)
		{
		}

		// attach the zip file
		String providerName = App.getStr(R.string.file_provider);
		Uri uri = FileProvider.getUriForFile(this, providerName, zipFile);
		intent.putExtra(Intent.EXTRA_STREAM, uri);
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		// start the activity
		startActivity(intent);
	}

	//******************************************************************************
	// addFileToZip
	//******************************************************************************
	private void addFileToZip(ZipOutputStream zip, File file)
	{
		try
		{
			// open the input file
			byte buffer[] = new byte[BUFFER_SIZE];
			FileInputStream input = new FileInputStream(file);
			BufferedInputStream bufferedInput = new BufferedInputStream(input, BUFFER_SIZE);

			// add a ZIP entry
			ZipEntry entry = new ZipEntry(file.getName());
			zip.putNextEntry(entry);

			// write the file to the ZIP
			int count;
			while ((count = bufferedInput.read(buffer, 0, BUFFER_SIZE)) != -1)
			{
				zip.write(buffer, 0, count);
			}

			// close the files
			bufferedInput.close();
			input.close();
		}
		catch (Exception ex)
		{
		}
	}
}
