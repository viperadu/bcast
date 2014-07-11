package com.example.bcast;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class WithMediaRecorder extends Activity {

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private PowerManager.WakeLock mWakeLock;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_withmediarecorder);
		
		mSurfaceView = (SurfaceView) findViewById(R.id.camera_view);
		
		mSurfaceHolder = mSurfaceView.getHolder();
		// Backwards compatibility with Android 2.*
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mWakeLock.acquire();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if(mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
	
	@Override    
	public void onBackPressed() {
		Intent setIntent = new Intent(Intent.ACTION_MAIN);
		setIntent.addCategory(Intent.CATEGORY_HOME);
		setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(setIntent);
	}
}
