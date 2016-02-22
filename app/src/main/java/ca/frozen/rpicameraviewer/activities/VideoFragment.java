// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.HttpReader;
import ca.frozen.rpicameraviewer.classes.MulticastReader;
import ca.frozen.rpicameraviewer.classes.RawH264Reader;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.SpsParser;
import ca.frozen.rpicameraviewer.classes.TcpIpReader;
import ca.frozen.rpicameraviewer.classes.Utils;

public class VideoFragment extends Fragment implements TextureView.SurfaceTextureListener
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "VideoFragment";
	private final static float MIN_ZOOM = 0.1f;
	private final static float MAX_ZOOM = 10;

	// instance variables
	private Camera camera;
	private DecoderThread decoder;
	private TextureView textureView;
	private TextView nameView, messageView;
	private ScaleGestureDetector scaleDetector;
	private GestureDetector simpleDetector;
	private float scale = 1;
	private float panX = 0;
	private float panY = 0;
	private Runnable finishRunner;
	private Handler finishHandler;

	//******************************************************************************
	// newInstance
	//******************************************************************************
	public static VideoFragment newInstance(Camera camera)
	{
		VideoFragment fragment = new VideoFragment();

		Bundle args = new Bundle();
		args.putParcelable(CAMERA, camera);
		fragment.setArguments(args);

		return fragment;
	}

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);

		// load the settings, networks and cameras
		Utils.loadData();

		// get the camera object
		camera = getArguments().getParcelable(CAMERA);

		// create the gesture recognizers
		simpleDetector = new GestureDetector(getActivity(), new SimpleListener());
		scaleDetector = new ScaleGestureDetector(getActivity(), new ScaleListener());

		// create the finish handler and runnable
		finishHandler = new Handler();
		finishRunner = new Runnable()
		{
			@Override
			public void run()
			{
				getActivity().finish();
			}
		};
	}

	//******************************************************************************
	// onCreateView
	//******************************************************************************
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_video, container, false);
		view.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				simpleDetector.onTouchEvent(event);
				scaleDetector.onTouchEvent(event);
				return true;
			}
		});

		// configure the name
		Settings settings = Utils.getSettings();
		nameView = (TextView) view.findViewById(R.id.video_name);
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
		messageView = (TextView) view.findViewById(R.id.video_message);
		messageView.setTextColor(App.getClr(R.color.good_text));
		messageView.setText(R.string.initializing_video);

		// set the texture listener
		textureView = (TextureView) view.findViewById(R.id.video_surface);
		textureView.setSurfaceTextureListener(this);

		return view;
	}

	//******************************************************************************
	// onDestroy
	//******************************************************************************
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		finishHandler.removeCallbacks(finishRunner);
	}

	//******************************************************************************
	// onStart
	//******************************************************************************
	@Override
	public void onStart()
	{
		super.onStart();

		// create the decoder thread
		decoder = new DecoderThread();
		decoder.start();
	}

	//******************************************************************************
	// onStop
	//******************************************************************************
	@Override
	public void onStop()
	{
		super.onStop();

		if (decoder != null)
		{
			decoder.interrupt();
			decoder = null;
		}
	}

	//******************************************************************************
	// onSurfaceTextureAvailable
	//******************************************************************************
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
	{
		if (decoder != null)
		{
			decoder.setSurface(new Surface(surfaceTexture));
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
		if (decoder != null)
		{
			decoder.setSurface(null);
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
		private MediaCodec decoder = null;
		private MediaFormat format;
		private boolean decoding = false;
		private Surface surface;
		private Source source = null;
		private byte[] buffer = null;
		private RawH264Reader reader = null;
		private WifiManager.MulticastLock multicastLock = null;

		//******************************************************************************
		// setSurface
		//******************************************************************************
		public void setSurface(Surface surface)
		{
			this.surface = surface;
			if (decoder != null)
			{
				if (surface != null)
				{
					boolean newDecoding = decoding;
					if (decoding)
					{
						setDecodingState(false);
					}
					if (format != null)
					{
						try
						{
							decoder.configure(format, surface, null, 0);
						}
						catch (Exception ex) {}
						if (!newDecoding)
						{
							newDecoding = true;
						}
					}
					if (newDecoding)
					{
						setDecodingState(newDecoding);
					}
				}
				else if (decoding)
				{
					setDecodingState(false);
				}
			}
		}

		//******************************************************************************
		// setDecodingState
		//******************************************************************************
		private synchronized void setDecodingState(boolean newDecoding)
		{
			try
			{
				if (newDecoding != decoding && decoder != null)
				{
					if (newDecoding)
					{
						decoder.start();
					}
					else
					{
						decoder.stop();
					}
					decoding = newDecoding;
				}
			} catch (Exception ex) {}
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
				// get the multicast lock if necessary
				if (camera.getSource().connectionType == Source.ConnectionType.RawMulticast)
				{
					WifiManager wifi = (WifiManager) getActivity().getSystemService(App.getContext().WIFI_SERVICE);
					if (wifi != null)
					{
						multicastLock = wifi.createMulticastLock("rpicamlock");
						multicastLock.acquire();
					}
				}

				// create the decoder
				decoder = MediaCodec.createDecoderByType("video/avc");

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

				// read from the source
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
												setDecodingState(true);
												inputBuffers = decoder.getInputBuffers();
												hideMessage();
												gotSPS = true;
											}
											if (gotSPS && decoding)
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

							// add the byte to the NAL
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
					if (format != null && decoding)
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

			// close the reader
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (Exception ex) {}
				reader = null;
			}

			// stop the decoder
			if (decoder != null)
			{
				try
				{
					setDecodingState(false);
					decoder.release();
				}
				catch (Exception ex) {}
				decoder = null;
			}

			// release the multicast lock
			if (multicastLock != null)
			{
				try
				{
					if (multicastLock.isHeld())
					{
						multicastLock.release();
					}
				}
				catch (Exception ex) {}
				multicastLock = null;
			}
		}

		//******************************************************************************
		// hideMessage
		//******************************************************************************
		private void hideMessage()
		{
			getActivity().runOnUiThread(new Runnable()
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
			getActivity().runOnUiThread(new Runnable()
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
