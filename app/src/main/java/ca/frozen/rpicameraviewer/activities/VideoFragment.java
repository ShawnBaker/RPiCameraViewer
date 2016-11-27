// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.HttpReader;
import ca.frozen.rpicameraviewer.classes.MulticastReader;
import ca.frozen.rpicameraviewer.classes.RawH264Reader;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.SpsParser;
import ca.frozen.rpicameraviewer.classes.TcpIpReader;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.library.views.ZoomPanTextureView;

public class VideoFragment extends Fragment implements TextureView.SurfaceTextureListener
{
	// public interfaces
	public interface OnFadeListener
	{
		void onStartFadeIn();
		void onStartFadeOut();
	}

	// public constants
	public final static String CAMERA = "camera";
	public final static String FULL_SCREEN = "full_screen";

	// local constants
	private final static String TAG = "VideoFragment";
	private final static float MIN_ZOOM = 0.1f;
	private final static float MAX_ZOOM = 10;
	private final static int FADEOUT_TIMEOUT = 8000;
	private final static int FADEOUT_ANIMATION_TIME = 500;
	private final static int FADEIN_ANIMATION_TIME = 400;
	private final static int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

	// instance variables
	private Camera camera;
	private boolean fullScreen;
	private DecoderThread decoder;
	private ZoomPanTextureView textureView;
	private TextView nameView, messageView;
	private Button snapshotButton;
	private Runnable fadeInRunner, fadeOutRunner, finishRunner, startVideoRunner;
	private Handler fadeInHandler, fadeOutHandler, finishHandler, startVideoHandler;
	private OnFadeListener fadeListener;

	//******************************************************************************
	// newInstance
	//******************************************************************************
	public static VideoFragment newInstance(Camera camera, boolean fullScreen)
	{
		VideoFragment fragment = new VideoFragment();

		Bundle args = new Bundle();
		args.putParcelable(CAMERA, camera);
		args.putBoolean(FULL_SCREEN, fullScreen);
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

		// load the settings and cameras
		Utils.loadData();

		// get the parameters
		camera = getArguments().getParcelable(CAMERA);
		fullScreen = getArguments().getBoolean(FULL_SCREEN);

		// create the fade in handler and runnable
		fadeInHandler = new Handler();
		fadeInRunner = new Runnable()
		{
			@Override
			public void run()
			{
				Animation fadeInName = new AlphaAnimation(0, 1);
				fadeInName.setDuration(FADEIN_ANIMATION_TIME);
				fadeInName.setFillAfter(true);
				Animation fadeInSnapshot = new AlphaAnimation(0, 1);
				fadeInSnapshot.setDuration(FADEIN_ANIMATION_TIME);
				fadeInSnapshot.setFillAfter(true);
				nameView.startAnimation(fadeInName);
				snapshotButton.startAnimation(fadeInSnapshot);
				fadeListener.onStartFadeIn();
			}
		};

		// create the fade out handler and runnable
		fadeOutHandler = new Handler();
		fadeOutRunner = new Runnable()
		{
			@Override
			public void run()
			{
				Animation fadeOutName = new AlphaAnimation(1, 0);
				fadeOutName.setDuration(FADEOUT_ANIMATION_TIME);
				fadeOutName.setFillAfter(true);
				Animation fadeOutSnapshot = new AlphaAnimation(1, 0);
				fadeOutSnapshot.setDuration(FADEOUT_ANIMATION_TIME);
				fadeOutSnapshot.setFillAfter(true);
				nameView.startAnimation(fadeOutName);
				snapshotButton.startAnimation(fadeOutSnapshot);
				fadeListener.onStartFadeOut();
			}
		};

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

		// create the start video handler and runnable
		startVideoHandler = new Handler();
		startVideoRunner = new Runnable()
		{
			@Override
			public void run()
			{
				MediaFormat format = decoder.getMediaFormat();
				int videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
				int videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
				textureView.setVideoSize(videoWidth, videoHeight);
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

		// configure the name
		nameView = (TextView)view.findViewById(R.id.video_name);
		nameView.setText(camera.name);

		// initialize the message
		messageView = (TextView)view.findViewById(R.id.video_message);
		messageView.setTextColor(App.getClr(R.color.good_text));
		messageView.setText(R.string.initializing_video);

		// set the texture listener
		textureView = (ZoomPanTextureView)view.findViewById(R.id.video_surface);
		textureView.setSurfaceTextureListener(this);
		textureView.setZoomRange(MIN_ZOOM, MAX_ZOOM);
		textureView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent e)
			{
				switch (e.getAction())
				{
					case MotionEvent.ACTION_DOWN:
						stopFadeOutTimer();
						break;
					case MotionEvent.ACTION_UP:
						if (e.getPointerCount() == 1)
						{
							startFadeOutTimer(false);
						}
						break;
				}
				return false;
			}
		});

		// create the snapshot button
		snapshotButton = (Button)view.findViewById(R.id.video_snapshot);
		snapshotButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				int check = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if (check != PackageManager.PERMISSION_GRANTED)
				{
					ActivityCompat.requestPermissions(getActivity(),
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							REQUEST_WRITE_EXTERNAL_STORAGE);
				}
				else
				{
					takeSnapshot();
				}
			}
		});

		// move the snapshot button over to account for the navigation bar
		if (fullScreen)
		{
			float scale = getContext().getResources().getDisplayMetrics().density;
			int margin = (int)(5 * scale + 0.5f);
			int extra = Utils.getNavigationBarHeight(getContext(), Configuration.ORIENTATION_LANDSCAPE);
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)snapshotButton.getLayoutParams();
			lp.setMargins(margin, margin, margin + extra, margin);
		}

		return view;
	}

	//******************************************************************************
	// onRequestPermissionsResult
	//******************************************************************************
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE)
		{
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
			{
				takeSnapshot();
			}
		}
	}

	//******************************************************************************
	// onAttach
	//******************************************************************************
	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		try
		{
			Activity activity = (Activity) context;
			fadeListener = (OnFadeListener) activity;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(context.toString() + " must implement OnFadeListener");
		}
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
	// onPause
	//******************************************************************************
	@Override
	public void onPause()
	{
		super.onPause();
		stopFadeOutTimer();
	}

	//******************************************************************************
	// onResume
	//******************************************************************************
	@Override
	public void onResume()
	{
		super.onResume();
		if (snapshotButton.getVisibility() == View.VISIBLE)
		{
			startFadeOutTimer(false);
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
			decoder.setSurface(new Surface(surfaceTexture), startVideoHandler, startVideoRunner);
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
			decoder.setSurface(null, null, null);
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
	// startFadeIn
	//******************************************************************************
	public void startFadeIn()
	{
		stopFadeOutTimer();
		fadeInHandler.removeCallbacks(fadeInRunner);
		fadeInHandler.post(fadeInRunner);
		startFadeOutTimer(true);
	}

	//******************************************************************************
	// startFadeOutTimer
	//******************************************************************************
	private void startFadeOutTimer(boolean addFadeInTime)
	{
		fadeOutHandler.removeCallbacks(fadeOutRunner);
		fadeOutHandler.postDelayed(fadeOutRunner, FADEOUT_TIMEOUT + (addFadeInTime ? FADEIN_ANIMATION_TIME : 0));
	}

	//******************************************************************************
	// stopFadeOutTimer
	//******************************************************************************
	private void stopFadeOutTimer()
	{
		fadeOutHandler.removeCallbacks(fadeOutRunner);
	}

	//******************************************************************************
	// takeSnapshot
	//******************************************************************************
	private void takeSnapshot()
	{
		Bitmap image = textureView.getBitmap();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
		String name = camera.network + "_" + camera.name.replaceAll("\\s+", "") + "_" + sdf.format(new Date()) + ".jpg";
		Utils.saveImage(getActivity().getContentResolver(), image, name, null);
		MediaActionSound sound = new MediaActionSound();
		sound.play(MediaActionSound.SHUTTER_CLICK);
		Toast toast = Toast.makeText(getActivity(), App.getStr(R.string.image_saved), Toast.LENGTH_SHORT);
		toast.show();
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
		private Handler startVideoHandler;
		private Runnable startVideoRunner;

		//******************************************************************************
		// setSurface
		//******************************************************************************
		public void setSurface(Surface surface, Handler handler, Runnable runner)
		{
			this.surface = surface;
			this.startVideoHandler = handler;
			this.startVideoRunner = runner;
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
		// getMediaFormat
		//******************************************************************************
		public MediaFormat getMediaFormat()
		{
			return format;
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
				if (camera.source.connectionType == Source.ConnectionType.RawMulticast)
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
				source = camera.getCombinedSource();
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
				if (!reader.isConnected())
				{
					throw new Exception();
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
												startVideoHandler.post(startVideoRunner);
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
