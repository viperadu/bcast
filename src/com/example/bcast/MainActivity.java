package com.example.bcast;

import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.bcast.audio.AudioQuality;
import com.example.bcast.video.MP4Config;
import com.example.bcast.video.VideoQuality;

public class MainActivity extends Activity implements OnClickListener {

//	SharedPreferences settings;
//	SharedPreferences.Editor editor;
	
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button b1 = (Button) findViewById(R.id.button_1);
		b1.setOnClickListener(this);
		Button b2 = (Button) findViewById(R.id.button_2);
		b2.setOnClickListener(this);
		
//		settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//		editor = settings.edit();
//		boolean firstTime = settings.getBoolean("FirstTime", true);
		boolean firstTime = true;
		
		if(firstTime) {
			Toast.makeText(getApplicationContext(), "First time initialisation.\nPlease wait", Toast.LENGTH_LONG).show();
			extractMP4Parameters();
		
			try {
				Class.forName("android.media.MediaCodec");
				GlobalVariables.MediaCodec = true;
//				editor.putBoolean("MediaCodec", true);
				if(GlobalVariables.DEBUGGING) {
					Log.i(GlobalVariables.TAG, "Android API is 4.2 or newer");
				}
			} catch (ClassNotFoundException e) {
				GlobalVariables.MediaCodec = false;
//				editor.putBoolean("MediaCodec", false);
				Log.i(GlobalVariables.TAG, "Android API is older than 4.2");
			}
			firstTime = false;
//			editor.putBoolean("FirstTime", firstTime);
		} else {
//			GlobalVariables.VideoEncoder = settings.getInt("VideoEncoder", 0);
			
//			GlobalVariables.AudioEncoder = settings.getInt("AudioEncoder", 0);
			
//			GlobalVariables.videoQuality = new VideoQuality();
//			GlobalVariables.videoQuality.bitrate = settings.getInt("VideoBitrate", 500000);
//			GlobalVariables.videoQuality.framerate = settings.getInt("VideoFramerate", 15);
//			GlobalVariables.videoQuality.orientation = settings.getInt("VideoOrientation", 90);
//			GlobalVariables.videoQuality.resX = settings.getInt("VideoResX", 640);
//			GlobalVariables.videoQuality.resY = settings.getInt("VideoResY", 480);
			
//			GlobalVariables.audioQuality = new AudioQuality();
//			GlobalVariables.audioQuality.bitRate = settings.getInt("AudioBitrate", 8000);
//			GlobalVariables.audioQuality.samplingRate = settings.getInt("AudioSamplingRate", 32000);
			
//			String profileLevel = settings.getString("ProfileLevel", "");
//			String PPS = settings.getString("PPS", "");
//			String SPS = settings.getString("SPS", "");
//			if(profileLevel.equals("") || PPS.equals("") || SPS.equals("")) {
//				if(GlobalVariables.DEBUGGING) {
//					Log.e(GlobalVariables.TAG, "Profile Level, PPS or SPS are null");
//				}
//				extractMP4Parameters();
//			} else {
//				GlobalVariables.config = new MP4Config(profileLevel, SPS, PPS);
//			}
			
		}
    }

	private void extractMP4Parameters() {
		// TODO: replace with a global variable instead of 1
		if(GlobalVariables.VideoEncoder == MediaRecorder.VideoEncoder.H264) {			
//			H264 h264;
//			try {
//				h264 = new H264();
				GlobalVariables.config = new MP4Config("42800c","Z0KADOkCg/I=","aM4G4g==");
//				GlobalVariables.config = h264.testH264(mSurfaceView);
//				editor.putString("ProfileLevel", GlobalVariables.config.getProfileLevel());
//				editor.putString("SPS", GlobalVariables.config.getB64SPS());
//				editor.putString("PPS", GlobalVariables.config.getB64PPS());
//			} catch (IOException e) {
//				if(GlobalVariables.DEBUGGING) {
//					e.printStackTrace();
//				}
//			}
		}
	}
    
    @Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.button_1:
			intent = new Intent(this, WithMediaRecorder.class);
			startActivity(intent);
			break;
		case R.id.button_2:
			intent = new Intent(this, WithMediaCodec.class);
			startActivity(intent);
			break;
		}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	
//    	if(!settings.getString("ProfileLevel", "").equals(GlobalVariables.config.getProfileLevel())) {
//    		editor.putString("ProfileLevel", GlobalVariables.config.getProfileLevel());
//    	}
//    	if(!settings.getString("SPS", "").equals(GlobalVariables.config.getB64SPS())) {
//    		editor.putString("SPS", GlobalVariables.config.getB64SPS());
//    	}
//    	if(!settings.getString("PPS", "").equals(GlobalVariables.config.getB64PPS())) {
//    		editor.putString("PPS", GlobalVariables.config.getB64PPS());
//    	}
    	

    	if(GlobalVariables.videoQuality != null) {
//	    	if(settings.getInt("VideoBitrate", 500000) != GlobalVariables.videoQuality.bitrate) {
//	    		editor.putInt("VideoBitrate", GlobalVariables.videoQuality.bitrate);
//	    	}
//	    	if(settings.getInt("VideoFramerate", 15) != GlobalVariables.videoQuality.framerate) {
//	    		editor.putInt("VideoFramerate", GlobalVariables.videoQuality.framerate);
//	    	}
//	    	if(settings.getInt("VideoOrientation", 90) != GlobalVariables.videoQuality.orientation) {
//	    		editor.putInt("VideoOrientation", GlobalVariables.videoQuality.orientation);
//	    	}
//	    	if(settings.getInt("VideoResX", 640) != GlobalVariables.videoQuality.resX) {
//	    		editor.putInt("VideoResX", GlobalVariables.videoQuality.resX);
//	    	}
//	    	if(settings.getInt("VideoResY", 90) != GlobalVariables.videoQuality.resY) {
//	    		editor.putInt("VideoResY", GlobalVariables.videoQuality.resY);
//	    	}
    	}
    	
//    	if(settings.getBoolean("MediaCodec", true) != GlobalVariables.MediaCodec) {
//    		editor.putBoolean("MediaCodec", GlobalVariables.MediaCodec);
//    	}
    	
//    	editor.commit();
    }

}
