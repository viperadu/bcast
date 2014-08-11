package com.example.bcast;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.example.bcast.audio.AudioQuality;
import com.example.bcast.video.VideoQuality;
import com.example.bcast.volley.LruBitmapCache;

public class MainActivity extends Activity implements OnClickListener {

//	SharedPreferences settings;
//	SharedPreferences.Editor editor;
	
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        GlobalVariables.SSRC = new Random().nextInt();

        Button b1 = (Button) findViewById(R.id.button_1);
		b1.setOnClickListener(this);
		Button b2 = (Button) findViewById(R.id.button_2);
		b2.setOnClickListener(this);
		Button b3 = (Button) findViewById(R.id.button_3);
		b3.setOnClickListener(this);
		
		init();
		
		GlobalVariables.videoQuality = VideoQuality.parseQuality("2000-15-1280-720");
		GlobalVariables.audioQuality = AudioQuality.parseQuality("8-32000");
		
//		settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//		editor = settings.edit();
//		boolean firstTime = settings.getBoolean("FirstTime", true);
		boolean firstTime = true;
		
		if(firstTime) {
//			Toast.makeText(getApplicationContext(), "First time initialisation.\nPlease wait", Toast.LENGTH_LONG).show();
//			extractMP4Parameters();
		
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

    private void init() {
    	GlobalVariables.videoFramerates = new ArrayList<Integer>();
    	GlobalVariables.videoResolutions = new ArrayList<Size>();
    	GlobalVariables.requestQueue = Volley.newRequestQueue(getApplicationContext());
    	GlobalVariables.imageLoader = new ImageLoader(GlobalVariables.requestQueue, new LruBitmapCache());
    }
    
    @Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
			case R.id.button_1:
				intent = new Intent(this, Watch.class);
				startActivity(intent);
				break;
			case R.id.button_2:
				intent = new Intent(this, Record.class);
				startActivity(intent);
				break;
			case R.id.button_3:
				intent = new Intent(this, Settings.class);
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
