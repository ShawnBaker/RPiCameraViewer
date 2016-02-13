package ca.frozen.rpicameraviewer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VideoActivity extends Activity implements SurfaceHolder.Callback
{
	// public constants
	public final static String CAMERA = "camera";

	// local constants
	private final static String TAG = "VideoActivity";

	// instance variables
	private Camera camera;
	private DecoderThread decoder;

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

		// create the surface view
		SurfaceView surfaceView = new SurfaceView(this);
		surfaceView.getHolder().addCallback(this);
		setContentView(surfaceView);

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = (Camera)data.getParcelable(CAMERA);

		// create the decoder thread
		decoder = new DecoderThread();
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

	private class DecoderThread extends Thread
	{
		// local constants
		private final static String TAG = "DecoderThread";
		private final static int SOCKET_TIMEOUT = 200;
		private final static int BUFFER_TIMEOUT = 10000;
		private final static int BUFFER_SIZE = 16384;
		private final static int NAL_SIZE_INC = 4096;

		// instance variables
		MediaCodec decoder;

		//******************************************************************************
		// init
		//******************************************************************************
		public boolean init(Surface surface, Camera camera)
		{
			try
			{
				decoder = MediaCodec.createDecoderByType("video/avc");
				MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 720);
				format.setInteger(MediaFormat.KEY_BIT_RATE, 1000000);
				format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
				//format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
				//format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
				decoder.configure(format, surface, null, 0);
				//decoder.configure(format, surface, null, 0);
				decoder.start();
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
			try
			{
				// try to connect to the device
				Socket socket = new Socket();
				InetSocketAddress socketAddress = new InetSocketAddress(camera.getAddress(), camera.getPort());
				socket.connect(socketAddress, SOCKET_TIMEOUT);

				// read from the socket
				InputStream stream = socket.getInputStream();
				byte[] buffer = new byte[BUFFER_SIZE];
				byte[] nal = new byte[NAL_SIZE_INC];
				int nalLen = 0;
				int numZeroes = 0;
				long presentationTime = System.nanoTime() / 1000;
				boolean gotSPS = false;
				boolean gotHeader = false;
				while (!Thread.interrupted())
				{
					int len = stream.read(buffer);
					//Log.d(TAG, String.format("len = %d", len));
					if (len > 0)
					{
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
								if (nalLen == 4)
								{
									if ((buffer[i] & 0x1F) == 7)
									{
										if (!gotSPS)
										{
//											decoder.start();
										}
										gotSPS = true;
										//Log.d(TAG, "SPS");
									}
								}
								if (nalLen == nal.length)
								{
									nal = Arrays.copyOf(nal, nal.length + NAL_SIZE_INC);
									//Log.d(TAG, String.format("NAL size: %d", nal.length));
								}
								nal[nalLen++] = buffer[i];
								if (nalLen == 5)
								{
									//Log.d(TAG, String.format("NAL: %02X %02X %02X %02X %02X", nal[0], nal[1], nal[2], nal[3], nal[4]));
								}
							}
						}
					}

					// send an output buffer to the surface
					MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
					int index = decoder.dequeueOutputBuffer(info, BUFFER_TIMEOUT);
					if (index >= 0)
					{
						decoder.getOutputBuffer(index);
						decoder.releaseOutputBuffer(index, true);
					}
				}

				// close things
				stream.close();
				socket.close();
			}
			catch (Exception ex)
			{
				String msg = ex.getMessage();
				Log.d(TAG, msg);
			}
			decoder.stop();
		}
	}
}
