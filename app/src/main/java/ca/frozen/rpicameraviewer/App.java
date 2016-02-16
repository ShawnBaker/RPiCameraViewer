// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;

public class App extends Application
{
    // instance variables
    private static Context context;

    //******************************************************************************
    // onCreate
    //******************************************************************************
    @Override
    public void onCreate()
	{
        super.onCreate();
        context = this;
    }

    //******************************************************************************
    // onCreate
    //******************************************************************************
    public static Context getContext() { return context; }

    //******************************************************************************
    // getStr
    //******************************************************************************
    public static String getStr(int id) { return context.getResources().getString(id); }

	//******************************************************************************
	// getInt
	//******************************************************************************
	public static int getInt(int id)
	{
		return context.getResources().getInteger(id);
	}

	//******************************************************************************
    // getClr
    //******************************************************************************
    public static int getClr(int id)
    {
        return ContextCompat.getColor(context, id);
    }

    //******************************************************************************
    // error
    //******************************************************************************
    public static void error(Context context, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				dialog.cancel();
			}
		});
        builder.show();
    }

    //******************************************************************************
    // error
    //******************************************************************************
    public static void error(Context context, int id)
    {
        error(context, getStr(id));
    }

    //******************************************************************************
    // error
    //******************************************************************************
    public static void error(String message)
    {
        error(context, message);
    }

    //******************************************************************************
    // error
    //******************************************************************************
	public static void error(int id)
	{
		error(getStr(id));
	}
}
