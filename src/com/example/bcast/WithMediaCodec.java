package com.example.bcast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.bcast.audio.AudioQuality;
import com.example.bcast.session.SendConfigurations;
import com.example.bcast.session.Session;
import com.example.bcast.session.SessionBuilder;
import com.example.bcast.video.VideoQuality;

public class WithMediaCodec extends Activity {
	public final static String TAG = "WithMediaCodec";
	public final static boolean DEBUGGING = true;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private Session mSession;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_withmediacodec);

		mSurfaceView = (SurfaceView) findViewById(R.id.camera_view_2);
		mSurfaceHolder = mSurfaceView.getHolder();
		// Backwards compatibility with Android 2.*
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		SessionBuilder builder = SessionBuilder.getInstance().clone();
		builder.setSurfaceHolder(mSurfaceHolder);
		builder.setCamera(CameraInfo.CAMERA_FACING_BACK);
		try {
			builder.setDestination(InetAddress
					.getByName(GlobalVariables.destAddress));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		AudioQuality aQuality = null;
		if (GlobalVariables.audioQuality == null) {
			aQuality = AudioQuality.parseQuality("8-32000");
		} else {
			aQuality = GlobalVariables.audioQuality;
		}
		if(DEBUGGING) {
			Log.d(TAG, "Audio Quailty set to " + aQuality.bitRate + " bitrate and " + aQuality.samplingRate + " sampling rate.");
		}
		builder.setAudioQuality(aQuality).setAudioEncoder(
				GlobalVariables.AudioEncoder);

		VideoQuality vQuality = null;
		if (GlobalVariables.videoQuality == null) {
			vQuality = VideoQuality.parseQuality("200-20-320-240");
		} else {
			vQuality = GlobalVariables.videoQuality;
		}
		if(DEBUGGING) {
			Log.d(TAG, "Video Quality set to " + vQuality.resX + "x" + vQuality.resY + " at " + vQuality.framerate + " fps and " + vQuality.bitrate + " bitrate.");
		}
		builder.setVideoQuality(vQuality).setVideoEncoder(
				GlobalVariables.VideoEncoder);

		try {
			mSession = builder.build();
		} catch (IOException e) {
			if (GlobalVariables.DEBUGGING) {
				Log.e(TAG, "Error while setting up the session");
				e.printStackTrace();
				if (e.getMessage() != null) {
					Log.e(TAG, e.getMessage());
				}
			}
		}
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// TODO: set the value for TITLE
//		GlobalVariables.TITLE = "......";
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.start), 1);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.stop), 1);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.start:
			try {
				if (mSession != null) {
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					try {
						GlobalVariables.ports = new SendConfigurations().execute(mSession, GlobalVariables.config).get();
						mSession.start();
					} catch (InterruptedException e) {
						e.printStackTrace();
						Toast.makeText(getApplicationContext(), "Connection to server reset", Toast.LENGTH_SHORT).show();
					} catch (ExecutionException e) {
						e.printStackTrace();
						Toast.makeText(getApplicationContext(), "Connection to server reset", Toast.LENGTH_SHORT).show();
					}
				}
			} catch (IllegalStateException e) {
				if (GlobalVariables.DEBUGGING) {
					e.printStackTrace();
					if (e.getMessage() != null) {
						Log.e(TAG, "Error while starting the session");
						Log.e(TAG, e.getMessage());
					}
				}
			} catch (IOException e) {
				if (GlobalVariables.DEBUGGING) {
					e.printStackTrace();
					if (e.getMessage() != null) {
						Log.e(TAG, "Error while starting the session");
						Log.e(TAG, e.getMessage());
					}
				}
			}

			return true;
		case R.id.stop:
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			if (mSession.isStreaming()) {
				mSession.stop();
			}
			return true;
		case R.id.quit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}