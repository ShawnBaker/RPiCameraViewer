// Copyright © 2017-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ca.frozen.library.classes.Log;
import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Utils;

public class LogFilesActivity extends AppCompatActivity
{
	// local constants
	private static final int BUFFER_SIZE = 16384;

	// instance variables
	private Button file1Button, file2Button;
	private ListView listView;

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

		// get the views
		listView = findViewById(R.id.log_list);
		file1Button = findViewById(R.id.log_file_1);
		file2Button = findViewById(R.id.log_file_2);

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
		Button clearButton = findViewById(R.id.log_file_clear);
		clearButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				clearLogFiles();
			}
		});

		// handle the email button
		Button emailButton = findViewById(R.id.log_file_email);
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
	private void loadLogFile(File logFile, int noFileId, Button thisButton, Button otherButton)
	{
		// configure the buttons
		Log.info("load log file: " + logFile.getName());
		thisButton.setSelected(true);
		otherButton.setSelected(false);

		// display the loading message
		ArrayList<String> loading = new ArrayList<>();
		loading.add(getString(R.string.loading));
		ArrayAdapter<String> loadingAdapter = new ArrayAdapter<>(this, R.layout.row_log, loading);
		listView.setAdapter(loadingAdapter);

		// load the list asynchronously
		new LogFileLoader(logFile, noFileId).execute();
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
		intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { getString(R.string.email_address) } );
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + getString(R.string.log_files));

		// get the ZIP file
		File dir = new File(getApplicationContext().getFilesDir(), "logs");
		String appName = getString(R.string.app_name).replaceAll("\\s+", "");
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
		String providerName = getString(R.string.file_provider);
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

	////////////////////////////////////////////////////////////////////////////////
	// LogFileLoader
	////////////////////////////////////////////////////////////////////////////////
	private class LogFileLoader extends AsyncTask<Void, Void, Void>
	{
		private File logFile;
		private int noFileId;
		private ArrayList<String> lines = new ArrayList<>();

		//******************************************************************************
		// LogFileLoader
		//******************************************************************************
		LogFileLoader(File logFile, int noFileId)
		{
			this.logFile = logFile;
			this.noFileId = noFileId;
		}

		//******************************************************************************
		// doInBackground
		//******************************************************************************
		@Override
		protected Void doInBackground(Void... params)
		{
			// get the list of lines
			if (logFile.exists())
			{
				try
				{
					Scanner s = new Scanner(logFile).useDelimiter("\n");
					while (s.hasNext())
					{
						lines.add(s.next());
					}
					s.close();
				}
				catch (Exception ex)
				{
					lines.add(getString(noFileId));
				}
			}
			else
			{
				lines.add(getString(noFileId));
			}
			return null;
		}

		//******************************************************************************
		// onPostExecute
		//******************************************************************************
		@Override
		protected void onPostExecute(Void unused)
		{
			// display the lines in the list
			ArrayAdapter<String> adapter = new ArrayAdapter<>(LogFilesActivity.this, R.layout.row_log, lines);
			listView.setAdapter(adapter);
			listView.setSelection(adapter.getCount() - 1);
		}
	}
}
