// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.HttpReader;
import ca.frozen.rpicameraviewer.classes.MulticastReader;
import ca.frozen.rpicameraviewer.classes.RawH264Reader;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.SpsParser;
import ca.frozen.rpicameraviewer.classes.TcpIpReader;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class VideoActivity extends Activity implements TextureView.SurfaceTextureListener
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "VideoActivity";
	private final static float MIN_ZOOM = 0.1f;
	private final static float MAX_ZOOM = 10;

	// instance variables
	private Camera camera;
	private DecoderThread decoder;
	private TextureView textureView;
	private TextView nameView, messageView;
	private ScaleGestureDetector scaleDetector;
	private GestureDetector simpleListener;
	private float scale = 1;
	private float panX = 0;
	private float panY = 0;
	private Matrix matrix = new Matrix();
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

		// load the settings, networks and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = (Camera)data.getParcelable(CAMERA);

		// set the texture listener
		setContentView(R.layout.activity_video);
		textureView = (TextureView) findViewById(R.id.video_surface);
		textureView.setSurfaceTextureListener(this);

		// create the gesture recognizers
		simpleListener = new GestureDetector(this, new SimpleListener());
		scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

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
	// onTouchEvent
	//******************************************************************************
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		simpleListener.onTouchEvent(ev);
		scaleDetector.onTouchEvent(ev);
		return true;
	}

	//******************************************************************************
	// onSurfaceTextureAvailable
	//******************************************************************************
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
	{
		if (decoder != null)
		{
			if (decoder.init(new Surface(surfaceTexture)))
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
	// onSurfaceTextureSizeChanged
	//******************************************************************************
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height)
	{
	}

	//******************************************************************************
	// onSurfaceTextureDestroyed
	//******************************************************************************
	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
	{
		finishHandler.removeCallbacks(finishRunner);
		if (decoder != null)
		{
			decoder.interrupt();
		}
		return true;
	}

	//******************************************************************************
	// onSurfaceTextureUpdated
	//******************************************************************************
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
	{
	}

	//******************************************************************************
	// onBackPressed
	//******************************************************************************
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		decoder.interrupt();
	}

	//******************************************************************************
	// adjustPan
	//******************************************************************************
	private boolean adjustPan(float scale)
	{
		boolean adjusted = false;
		int w = textureView.getWidth();
		int h = textureView.getHeight();
		float dx = (w * scale - w) / 2;
		float dy = (h * scale - h) / 2;
		if (panX < -dx)
		{
			panX = -dx;
			adjusted = true;
		}
		if (panX > dx)
		{
			panX = dx;
			adjusted = true;
		}
		if (panY < -dy)
		{
			panY = -dy;
			adjusted = true;
		}
		if (panY > dy)
		{
			panY = dy;
			adjusted = true;
		}
		return adjusted;
	}

	////////////////////////////////////////////////////////////////////////////////
	// SimpleListener
	////////////////////////////////////////////////////////////////////////////////
	private class SimpleListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			if (scale > 1)
			{
				panX -= distanceX;
				panY -= distanceY;
				adjustPan(scale);
				textureView.setTranslationX(panX);
				textureView.setTranslationY(panY);
				return true;
			}
			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			scale = 1;
			textureView.setScaleX(scale);
			textureView.setScaleY(scale);
			panX = panY = 0;
			textureView.setTranslationX(panX);
			textureView.setTranslationY(panY);
			return true;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// ScaleListener
	////////////////////////////////////////////////////////////////////////////////
	private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener
	{
		float startScale = 1;

		@Override
		public boolean onScale(ScaleGestureDetector detector)
		{
			float newScale = startScale * detector.getScaleFactor();
			newScale = Math.max(MIN_ZOOM, Math.min(newScale, MAX_ZOOM));
			textureView.setScaleX(newScale);
			textureView.setScaleY(newScale);
			if (newScale > 1)
			{
				if (adjustPan(newScale))
				{
					textureView.setTranslationX(panX);
					textureView.setTranslationY(panY);
				}
			}
			else if (panX != 0 || panY != 0)
			{
				panX = panY = 0;
				textureView.setTranslationX(panX);
				textureView.setTranslationY(panY);
			}
			return false;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector)
		{
			startScale = scale;
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector)
		{
			float newScale = startScale * detector.getScaleFactor();
			scale = Math.max(0.1f, Math.min(newScale, 10));
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// DecoderThread
	////////////////////////////////////////////////////////////////////////////////
	private class DecoderThread extends Thread
	{
		// local constants
		private final static String TAG = "DecoderThread";
		private final static int BUFFER_TIMEOUT = 10000;
		private final static int FINISH_TIMEOUT = 5000;
		private final static int MULTICAST_BUFFER_SIZE = 16384;
		private final static int TCPIP_BUFFER_SIZE = 16384;
		private final static int HTTP_BUFFER_SIZE = 4096;
		private final static int NAL_SIZE_INC = 4096;
		private final static int MAX_READ_ERRORS = 300;

		// instance variables
		private MediaCodec decoder;
		private MediaFormat format;
		private Surface surface;
		private Source source = null;
		private byte[] buffer = null;
		private RawH264Reader reader = null;
		private WifiManager.MulticastLock multicastLock = null;

		//******************************************************************************
		// init
		//******************************************************************************
		public boolean init(Surface surface)
		{
			try
			{
				// save the surface
				this.surface = surface;

				// get the multicast lock if necessary
				if (camera.getSource().connectionType == Source.ConnectionType.RawMulticast)
				{
					WifiManager wifi = (WifiManager) getSystemService(App.getContext().WIFI_SERVICE);
					if (wifi != null)
					{
						multicastLock = wifi.createMulticastLock("rpicamlock");
						multicastLock.acquire();
					}
				}

				// create the decoder
				decoder = MediaCodec.createDecoderByType("video/avc");
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
			byte[] nal = new byte[NAL_SIZE_INC];
			int nalLen = 0;
			int numZeroes = 0;
			int numReadErrors = 0;
			long presentationTime = System.nanoTime() / 1000;
			boolean gotSPS = false;
			boolean gotHeader = false;
			ByteBuffer[] inputBuffers = null;

			try
			{
				// create the reader
				source = camera.getSource();
				if (source.connectionType == Source.ConnectionType.RawMulticast)
				{
					buffer = new byte[MULTICAST_BUFFER_SIZE];
					reader = new MulticastReader(source);
				}
				else if (source.connectionType == Source.ConnectionType.RawHttp)
				{
					buffer = new byte[HTTP_BUFFER_SIZE];
					reader = new HttpReader(source);
				}
				else
				{
					buffer = new byte[TCPIP_BUFFER_SIZE];
					reader = new TcpIpReader(source);
				}

				// read from the socket
				while (!Thread.interrupted())
				{
					// read from the stream
					int len = reader.read(buffer);
					//Log.d(TAG, String.format("len = %d", len));

					// process the input buffer
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
												inputBuffers = decoder.getInputBuffers();
												gotSPS = true;
											}
											if (gotSPS)
											{
												int index = decoder.dequeueInputBuffer(BUFFER_TIMEOUT);
												if (index >= 0)
												{
													ByteBuffer inputBuffer = inputBuffers[index];
													//ByteBuffer inputBuffer = decoder.getInputBuffer(index);
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
							decoder.releaseOutputBuffer(index, true);
						}
					}
				}
			}
			catch (Exception ex)
			{
				if (reader == null || !reader.isConnected())
				{
					setMessage(R.string.error_couldnt_connect);
					finishHandler.postDelayed(finishRunner, FINISH_TIMEOUT);
				}
				else
				{
					setMessage(R.string.error_lost_connection);
				}
				//Log.d(TAG, ex.toString());
				ex.printStackTrace();
			}

			try
			{
				// close the reader
				if (reader != null)
				{
					reader.close();
				}

				// stop the decoder
				if (decoder != null)
				{
					decoder.stop();
					decoder = null;
				}

				// release the multicast lock
				if (multicastLock != null)
				{
					if (multicastLock.isHeld())
					{
						multicastLock.release();
					}
					multicastLock = null;
				}
			}
			catch (Exception ex)
			{
			}
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
