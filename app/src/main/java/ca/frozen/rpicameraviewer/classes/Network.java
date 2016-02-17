// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Network implements Comparable, Parcelable
{
	// local constants
	private final static String TAG = "Network";

	// instance variables
	public String name;
	public Source source;

	//******************************************************************************
	// Network
	//******************************************************************************
	public Network(String name, Source source)
	{
		this.name = name;
		this.source = source;
		//Log.d(TAG, "values: " + toString());
	}

	//******************************************************************************
	// Network
	//******************************************************************************
	public Network(String name)
	{
		this.name = name;
		this.source = new Source();
		//Log.d(TAG, "name: " + toString());
	}

	//******************************************************************************
	// Network
	//******************************************************************************
	public Network(Parcel in)
	{
		readFromParcel(in);
		//Log.d(TAG, "parcel: " + toString());
	}

	//******************************************************************************
	// Network
	//******************************************************************************
	public Network()
	{
		initialize();
		//Log.d(TAG, "init: " + toString());
	}

	//******************************************************************************
	// Network
	//******************************************************************************
	public Network(JSONObject obj)
	{
		try
		{
			name = obj.getString("name");
			source = new Source(obj.getJSONObject("source"));
		}
		catch (JSONException ex)
		{
			initialize();
		}
		//Log.d(TAG, "json: " + toString());
	}

	//******************************************************************************
	// initialize
	//******************************************************************************
	private void initialize()
	{
		name = "";
		source = new Source();
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(name);
		dest.writeParcelable(source, flags);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		name = in.readString();
		source = (Source) in.readParcelable(Source.class.getClassLoader());
	}

	//******************************************************************************
	// describeContents
	//******************************************************************************
	public int describeContents()
	{
		return 0;
	}

	//******************************************************************************
	// Parcelable.Creator
	//******************************************************************************
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
	{
		public Network createFromParcel(Parcel in)
		{
			return new Network(in);
		}
		public Network[] newArray(int size)
		{
			return new Network[size];
		}
	};

	//******************************************************************************
	// equals
	//******************************************************************************
	@Override
	public boolean equals(Object otherNetwork)
	{
		return compareTo(otherNetwork) == 0;
	}

	//******************************************************************************
	// compareTo
	//******************************************************************************
	@Override
	public int compareTo(Object otherNetwork)
	{
		int result = 1;
		if (otherNetwork instanceof Network)
		{
			Network network = (Network) otherNetwork;
			result = name.compareTo(network.name);
			if (result == 0)
			{
				result = source.compareTo(network.source);
			}
		}
		return result;
	}

	//******************************************************************************
	// toString
	//******************************************************************************
	@Override
	public String toString()
	{
		return name + ": " + source.toString();
	}

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("name", name);
			obj.put("source", source.toJson());
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	//******************************************************************************
	// getSource
	//******************************************************************************
	public Source getSource()
	{
		return Utils.getSettings().source.compound(source);
	}

	//******************************************************************************
	// getConnectionType
	//******************************************************************************
	public Source.ConnectionType getConnectionType() { return getSource().connectionType; }
}
