// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.SpsParser;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class VideoActivity extends Activity implements SurfaceHolder.Callback
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "VideoActivity";

	// instance variables
	private Camera camera;
	private DecoderThread decoder;
	private TextView nameView, messageView;
	private Runnable finishRunner;
	private Handler finishHandler;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);

		// set the full screen and landscape modes
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		// load the settings, networks and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = (Camera)data.getParcelable(CAMERA);

		// get the surface view
		setContentView(R.layout.activity_video);
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.video_surface);
		surfaceView.getHolder().addCallback(this);

		// configure the name
		Settings settings = Utils.getSettings();
		nameView = (TextView) findViewById(R.id.video_name);
		nameView.setText(camera.name);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		switch (settings.cameraNamePosition)
		{
			case TopLeft:
				params.gravity =  Gravity.TOP | Gravity.LEFT;
				break;
			case TopRight:
				params.gravity =  Gravity.TOP | Gravity.RIGHT;
				break;
			case BottomLeft:
				params.gravity =  Gravity.BOTTOM | Gravity.LEFT;
				break;
			case BottomRight:
				params.gravity =  Gravity.BOTTOM | Gravity.RIGHT;
				break;
			default:
				nameView.setVisibility(View.GONE);
				break;
		}
		nameView.setLayoutParams(params);
		nameView.setTextColor(settings.cameraNameColor);

		// initialize the message
		messageView = (TextView) findViewById(R.id.video_message);
		messageView.setTextColor(App.getClr(R.color.good_text));
		messageView.setText(R.string.initializing_video);

		// create the decoder thread
		decoder = new DecoderThread();

		// create the finish handler and runnable
		finishHandler = new Handler();
		finishRunner = new Runnable()
		{
			@Override
			public void run()
			{
				finish();
			}
		};
	}

	//******************************************************************************
	// surfaceCreated
	//******************************************************************************
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
	}

	//******************************************************************************
	// surfaceChanged
	//******************************************************************************
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height)
	{
		if (decoder != null)
		{
			if (decoder.init(holder.getSurface(), camera))
			{
				decoder.start();
			}
			else
			{
				decoder = null;
			}
		}
	}

	//******************************************************************************
	// surfaceDestroyed
	//******************************************************************************
	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		finishHandler.removeCallbacks(finishRunner);
		if (decoder != null)
		{
			decoder.interrupt();
		}
	}

	//******************************************************************************
	// surfaceDestroyed
	//******************************************************************************
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		decoder.interrupt();
	}

	////////////////////////////////////////////////////////////////////////////////
	// DecoderThread
	////////////////////////////////////////////////////////////////////////////////
	private class DecoderThread extends Thread
	{
		// local constants
		private final static String TAG = "DecoderThread";
		private final static int SOCKET_TIMEOUT = 200;
		private final static int BUFFER_TIMEOUT = 10000;
		private final static int FINISH_TIMEOUT = 5000;
		private final static int BUFFER_SIZE = 16384;
		private final static int NAL_SIZE_INC = 4096;
		private final static int MAX_READ_ERRORS = 300;

		// instance variables
		private MediaCodec decoder;
		private MediaFormat format;
		private Surface surface;

		//******************************************************************************
		// init
		//******************************************************************************
		public boolean init(Surface surface, Camera camera)
		{
			try
			{
				decoder = MediaCodec.createDecoderByType("video/avc");
				this.surface = surface;
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}

			return true;
		}

		//******************************************************************************
		// run
		//******************************************************************************
		@Override
		public void run()
		{
			Socket socket = new Socket();
			try
			{
				// try to connect to the device
				Source source = camera.getSource();
				InetSocketAddress socketAddress = new InetSocketAddress(source.address, source.port);
				socket.connect(socketAddress, SOCKET_TIMEOUT);

				// read from the socket
				InputStream stream = socket.getInputStream();
				byte[] buffer = new byte[BUFFER_SIZE];
				byte[] nal = new byte[NAL_SIZE_INC];
				int nalLen = 0;
				int numZeroes = 0;
				int numReadErrors = 0;
				long presentationTime = System.nanoTime() / 1000;
				boolean gotSPS = false;
				boolean gotHeader = false;
				while (!Thread.interrupted())
				{
					int len = stream.read(buffer);
					//Log.d(TAG, String.format("len = %d", len));
					if (len > 0)
					{
						numReadErrors = 0;
						for (int i = 0; i < len; i++)
						{
							if (buffer[i] == 0)
							{
								numZeroes++;
							}
							else
							{
								if (buffer[i] == 1)
								{
									if (numZeroes == 3)
									{
										if (gotHeader)
										{
											nalLen -= numZeroes;
											if (!gotSPS && (nal[numZeroes + 1] & 0x1F) == 7)
											{
												//Log.d(TAG, String.format("SPS: %d = %02X %02X %02X %02X %02X", nalLen, nal[0], nal[1], nal[2], nal[3], nal[4]));
												SpsParser parser = new SpsParser(nal, nalLen);
												int width = (source.width != 0) ? source.width : parser.width;
												int height = (source.height != 0) ? source.height : parser.height;
												//Log.d(TAG, String.format("SPS: size = %d x %d", width, height));
												format = MediaFormat.createVideoFormat("video/avc", width, height);
												if (source.fps != 0)
												{
													format.setInteger(MediaFormat.KEY_FRAME_RATE, source.fps);
												}
												if (source.bps != 0)
												{
													format.setInteger(MediaFormat.KEY_BIT_RATE, source.bps);
												}
												decoder.configure(format, surface, null, 0);
												decoder.start();
												hideMessage();
												gotSPS = true;
											}
											if (gotSPS)
											{
												int index = decoder.dequeueInputBuffer(BUFFER_TIMEOUT);
												if (index >= 0)
												{
													ByteBuffer inputBuffer = decoder.getInputBuffer(index);
													inputBuffer.put(nal, 0, nalLen);
													decoder.queueInputBuffer(index, 0, nalLen, presentationTime, 0);
													presentationTime += 66666;
												}
												//Log.d(TAG, String.format("NAL: %d  %d", nalLen, index));
											}
										}
										for (int j = 0; j < numZeroes; j++)
										{
											nal[j] = 0;
										}
										nalLen = numZeroes;
										gotHeader = true;
									}
								}
								numZeroes = 0;
							}
							if (gotHeader)
							{
								if (nalLen == nal.length)
								{
									nal = Arrays.copyOf(nal, nal.length + NAL_SIZE_INC);
									//Log.d(TAG, String.format("NAL size: %d", nal.length));
								}
								nal[nalLen++] = buffer[i];
							}
						}
					}
					else
					{
						numReadErrors++;
						if (numReadErrors >= MAX_READ_ERRORS)
						{
							setMessage(R.string.error_lost_connection);
							break;
						}
						//Log.d(TAG, "len == 0");
					}

					// send an output buffer to the surface
					if (format != null)
					{
						MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
						int index = decoder.dequeueOutputBuffer(info, BUFFER_TIMEOUT);
						if (index >= 0)
						{
							decoder.getOutputBuffer(index);
							decoder.releaseOutputBuffer(index, true);
						}
					}
				}

				// close things
				stream.close();
				socket.close();
			}
			catch (Exception ex)
			{
				if (socket == null || !socket.isConnected())
				{
					setMessage(R.string.error_couldnt_connect);
					finishHandler.postDelayed(finishRunner, FINISH_TIMEOUT);
				}
				else
				{
					setMessage(R.string.error_lost_connection);
				}
				Log.d(TAG, ex.toString());
				ex.printStackTrace();
			}
			decoder.stop();
		}

		//******************************************************************************
		// hideMessage
		//******************************************************************************
		private void hideMessage()
		{
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					messageView.setVisibility(View.GONE);
				}
			});
		}

		//******************************************************************************
		// setMessage
		//******************************************************************************
		private void setMessage(final int id)
		{
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					messageView.setText(id);
					messageView.setTextColor(App.getClr(R.color.bad_text));
					messageView.setVisibility(View.VISIBLE);
				}
			});
		}
	}
}
