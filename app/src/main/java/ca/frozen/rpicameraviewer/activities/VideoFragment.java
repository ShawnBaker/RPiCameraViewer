// Copyright © 2016-2018 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import ca.frozen.library.classes.Log;
import ca.frozen.library.views.ZoomPanTextureView;
import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.R;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.Settings;
import ca.frozen.rpicameraviewer.classes.SpsParser;
import ca.frozen.rpicameraviewer.classes.TcpIpReader;
import ca.frozen.rpicameraviewer.classes.Utils;

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
	private final static float MIN_ZOOM = 1;
	private final static float MAX_ZOOM = 10;
	private final static int FADEOUT_TIMEOUT = 8000;
	private final static int FADEOUT_ANIMATION_TIME = 500;
	private final static int FADEIN_ANIMATION_TIME = 400;
	private final static int REQUEST_WRITE_EXTERNAL_STORAGE = 73;

	// instance variables
	private Camera camera;
	private boolean fullScreen;
	private DecoderThread decoder;
	private ZoomPanTextureView textureView;
	private ImageView overlayView;
	private TextView nameView, messageView;
	private Button closeButton, snapshotButton, frameRotateButton;
	private Runnable fadeInRunner, fadeOutRunner, finishRunner, startVideoRunner;
	private Handler fadeInHandler, fadeOutHandler, finishHandler, startVideoHandler;
	private OnFadeListener fadeListener;
	private Settings settings;

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

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// load the settings and cameras
		Utils.loadData();

		// get the parameters
		settings = Utils.getSettings();
		camera = getArguments().getParcelable(CAMERA);
		fullScreen = getArguments().getBoolean(FULL_SCREEN);
		Log.info("camera: " + camera.toString());

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
				closeButton.startAnimation(fadeInSnapshot);
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
				closeButton.startAnimation(fadeOutSnapshot);
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

		if (fullScreen)
		{
			DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener()
			{
				@Override
				public void onDisplayAdded(int displayId)
				{
					Log.info("Display #" + displayId + " added.");
				}

				@Override
				public void onDisplayChanged(int displayId)
				{
					Log.info("Display #" + displayId + " changed.");
					setControlMargins();
				}

				@Override
				public void onDisplayRemoved(int displayId)
				{
					Log.info("Display #" + displayId + " removed.");
				}
			};
			DisplayManager displayManager = (DisplayManager)getActivity().getSystemService(Context.DISPLAY_SERVICE);
			displayManager.registerDisplayListener(displayListener, null);
		}
	}

	//******************************************************************************
	// onCreateView
	//******************************************************************************
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_video, container, false);

		// configure the name
		nameView = view.findViewById(R.id.video_name);
		nameView.setText(camera.name);

		// initialize the message
		messageView = view.findViewById(R.id.video_message);
		messageView.setTextColor(App.getClr(R.color.good_text));
		messageView.setText(R.string.initializing_video);

		// set the texture listener
		textureView = view.findViewById(R.id.video_surface);
		textureView.setSurfaceTextureListener(this);
		textureView.setZoomRange(MIN_ZOOM, MAX_ZOOM);
		textureView.setRotation(settings.frameRotation);
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

		overlayView = view.findViewById(R.id.overlay_surface);

		if(settings.showSilouette)
		{
			overlayView.setVisibility(View.VISIBLE);
			overlayView.setRotation(settings.frameRotation);
		}
		else
			overlayView.setVisibility(View.GONE);

		// create the close button listener
		closeButton = view.findViewById(R.id.video_close);
		closeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				stop();
				getActivity().finish();
			}
		});

		// create the snapshot button listener
		snapshotButton = view.findViewById(R.id.video_snapshot);
		snapshotButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				int check = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if (check != PackageManager.PERMISSION_GRANTED)
				{
					Log.info("ask for external storage permission");
					requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
										REQUEST_WRITE_EXTERNAL_STORAGE);
				}
				else
				{
					takeSnapshot();
				}
			}
		});

		frameRotateButton = view.findViewById(R.id.frame_rotate);
        frameRotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateFrame();
            }
        });


		// adjust the controls to account for the navigation and status bars
		if (fullScreen)
		{
			setControlMargins();
		}

		return view;
	}

	//******************************************************************************
	// onRequestPermissionsResult
	//******************************************************************************
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE && grantResults.length > 0 &&
			grantResults[0] == PackageManager.PERMISSION_GRANTED)
		{
			Log.info("external storage permission granted");
			takeSnapshot();
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
	// setControlMargins
	//******************************************************************************
	public void setControlMargins()
	{
		Activity activity = getActivity();
		if (activity != null)
		{
			// get the margins accounting for the navigation and status bars
			Display display = activity.getWindowManager().getDefaultDisplay();
			float scale = getContext().getResources().getDisplayMetrics().density;
			int margin = (int)(5 * scale + 0.5f);
			int extra = Utils.getNavigationBarWidth(getContext());
			int rotation = display.getRotation();
			int leftMargin = margin;
			int rightMargin = margin;
			if (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270)
			{
				leftMargin += extra;
			}
			else
			{
				rightMargin += extra;
			}
			int topMargin = margin + Utils.getStatusBarHeight(getContext());

			// set the control margins
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)closeButton.getLayoutParams();
			lp.setMargins(leftMargin, topMargin, rightMargin, margin);
			lp = (ViewGroup.MarginLayoutParams)snapshotButton.getLayoutParams();
			lp.setMargins(leftMargin, margin, rightMargin, margin);
			lp = (ViewGroup.MarginLayoutParams)nameView.getLayoutParams();
			lp.setMargins(leftMargin, margin, rightMargin, margin);
		}
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
		// get the snapshot image
		Bitmap image = textureView.getBitmap();

		// save the image
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
		String name = camera.network + "_" + camera.name.replaceAll("\\s+", "") + "_" + sdf.format(new Date()) + ".jpg";
		Utils.saveImage(getActivity().getContentResolver(), image, name, null);
		Log.info("takeSnapshot: " + name);

		// play the shutter sound
		MediaActionSound sound = new MediaActionSound();
		sound.play(MediaActionSound.SHUTTER_CLICK);

		// display a message
		String msg = String.format(getString(R.string.image_saved), getString(R.string.app_name));
		Toast toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	public void rotateFrame()
    {
        textureView.setRotation(textureView.getRotation()+90);
		overlayView.setRotation(overlayView.getRotation()+90);
    }

    public void rotateFrame(int rotation)
	{
		textureView.setRotation(rotation);
		overlayView.setRotation(rotation);
	}

	//******************************************************************************
	// stop
	//******************************************************************************
	public void stop()
	{
		if (decoder != null)
		{
			messageView.setText(R.string.closing_video);
			messageView.setTextColor(App.getClr(R.color.good_text));
			messageView.setVisibility(View.VISIBLE);
			decoder.interrupt();
			try
			{
				decoder.join(TcpIpReader.IO_TIMEOUT * 2);
			}
			catch (Exception ex) {}
			decoder = null;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// DecoderThread
	////////////////////////////////////////////////////////////////////////////////
	private class DecoderThread extends Thread
	{
		// local constants
		private final static int FINISH_TIMEOUT = 5000;
		private final static int BUFFER_SIZE = 16384;
		private final static int NAL_SIZE_INC = 4096;
		private final static int MAX_READ_ERRORS = 300;

		// instance variables
		private MediaCodec decoder = null;
		private MediaFormat format;
		private boolean decoding = false;
		private Surface surface;
		private byte[] buffer = null;
		private ByteBuffer[] inputBuffers = null;
		private long presentationTime;
		private long presentationTimeInc = 66666;
		private TcpIpReader reader = null;
		private Handler startVideoHandler;
		private Runnable startVideoRunner;

		//******************************************************************************
		// setSurface
		//******************************************************************************
		void setSurface(Surface surface, Handler handler, Runnable runner)
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
		MediaFormat getMediaFormat()
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

			try
			{
				// create the decoder
				decoder = MediaCodec.createDecoderByType("video/avc");

				// create the reader
				buffer = new byte[BUFFER_SIZE];
				reader = new TcpIpReader(camera);
				if (!reader.isConnected())
				{
					throw new Exception();
				}

				// read until we're interrupted
				while (!isInterrupted())
				{
					// read from the stream
					int len = reader.read(buffer);
					if (isInterrupted()) break;

					// process the input buffer
					if (len > 0)
					{
						numReadErrors = 0;
						for (int i = 0; i < len && !isInterrupted(); i++)
						{
							// add the byte to the NAL
							if (nalLen == nal.length)
							{
								nal = Arrays.copyOf(nal, nal.length + NAL_SIZE_INC);
							}
							nal[nalLen++] = buffer[i];

							// look for a header
							if (buffer[i] == 0)
							{
								numZeroes++;
							}
							else
							{
								if (buffer[i] == 1 && numZeroes == 3)
								{
									if (nalLen > 4)
									{
										int nalType = processNal(nal, nalLen - 4);
										if (isInterrupted()) break;
										if (nalType == -1)
										{
											nal[0] = nal[1] = nal[2] = 0;
											nal[3] = 1;
										}
									}
									nalLen = 4;
								}
								numZeroes = 0;
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
					}

					// send an output buffer to the surface
					if (format != null && decoding)
					{
						if (isInterrupted()) break;
						MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
						int index;
						do
						{
							index = decoder.dequeueOutputBuffer(info, 0);
							if (isInterrupted()) break;
							if (index >= 0)
							{
								decoder.releaseOutputBuffer(index, true);
							}
							//Log.info(String.format("dequeueOutputBuffer index = %d", index));
						} while (index >= 0);
					}
				}
			}
			catch (Exception ex)
			{
				Log.error(ex.toString());
				if (reader == null || !reader.isConnected())
				{
					setMessage(R.string.error_couldnt_connect);
					finishHandler.postDelayed(finishRunner, FINISH_TIMEOUT);
				}
				else
				{
					setMessage(R.string.error_lost_connection);
				}
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
		}

		//******************************************************************************
		// processNal
		//******************************************************************************
		private int processNal(byte[] nal, int nalLen)
		{
			// get the NAL type
			int nalType = (nalLen > 4 && nal[0] == 0 && nal[1] == 0 && nal[2] == 0 && nal[3] == 1) ? (nal[4] & 0x1F) : -1;
			//Log.info(String.format("NAL: type = %d, len = %d", nalType, nalLen));

			// process the first SPS record we encounter
			if (nalType == 7 && !decoding)
			{
				SpsParser parser = new SpsParser(nal, nalLen);
				format = MediaFormat.createVideoFormat("video/avc", parser.width, parser.height);
				presentationTimeInc = 66666;
				presentationTime = System.nanoTime() / 1000;
				Log.info(String.format("SPS: %02X, %d x %d, %d", nal[4], parser.width, parser.height, presentationTimeInc));
				decoder.configure(format, surface, null, 0);
				setDecodingState(true);
				inputBuffers = decoder.getInputBuffers();
				hideMessage();
				startVideoHandler.post(startVideoRunner);
			}

			// queue the frame
			if (nalType > 0 && decoding)
			{
				int index = decoder.dequeueInputBuffer(0);
				if (index >= 0)
				{
					ByteBuffer inputBuffer = inputBuffers[index];
					//ByteBuffer inputBuffer = decoder.getInputBuffer(index);
					inputBuffer.put(nal, 0, nalLen);
					decoder.queueInputBuffer(index, 0, nalLen, presentationTime, 0);
					presentationTime += presentationTimeInc;
				}
				//Log.info(String.format("dequeueInputBuffer index = %d", index));
			}
			return nalType;
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
