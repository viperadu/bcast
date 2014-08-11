package com.example.bcast.video;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;

import com.example.bcast.GlobalVariables;
import com.example.bcast.packetizer.H264Packetizer;

public class H264 extends VideoStream implements Serializable {
	private static final long serialVersionUID = 75172938245372591L;
	private SharedPreferences mSettings = null;
	private Semaphore mLock = new Semaphore(0);

	public H264() throws IOException {
		this(CameraInfo.CAMERA_FACING_BACK);
	}
	
	public H264(int cameraId) throws IOException {
		super(cameraId);
		setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mPacketizer = new H264Packetizer();
	}
	
	public void setPreferences(SharedPreferences preferences) {
		mSettings = preferences;
	}

	@Override
	public String generateSessionDescription() throws IllegalStateException, IOException {
		MP4Config config = null;
		if(GlobalVariables.config == null) {
			config = testH264();
			GlobalVariables.config = config;
		} else {
			config = GlobalVariables.config;
		}
		
//		Log.e("H264", "profileLevel=" + config.getProfileLevel());
//		Log.e("H264", "sps=" + config.getB64SPS());
//		Log.e("H264", "pps=" + config.getB64PPS());
		
		return "m=video " + String.valueOf(getDestinationPorts()[0]) + "RTP/AVP 96 \r\n" +
		"a=rtpmap:96 H264/90000\r\n" + 
		"a=fmtp:96 packetization-mode=1;profile-level-id=" + config.getProfileLevel() + 
		";sprop-parameter-sets=" + config.getB64SPS() + "," + config.getB64PPS() + ";\r\n";
	}
	
	public synchronized void start() throws IllegalStateException, IOException {
		MP4Config config = null;
		if(GlobalVariables.config == null) {
			GlobalVariables.config = testH264();
			Log.i(TAG, "profile level=" + GlobalVariables.config.getProfileLevel());
			Log.i(TAG, "sps=" + GlobalVariables.config.getB64SPS());
			Log.i(TAG, "pps=" + GlobalVariables.config.getB64PPS());
		} else {
			config = GlobalVariables.config;
		}
		
//		Log.e("H264", "profileLevel=" + config.getProfileLevel());
//		Log.e("H264", "sps=" + config.getB64SPS());
//		Log.e("H264", "pps=" + config.getB64PPS());
		
		byte[] pps = Base64.decode(GlobalVariables.config.getB64PPS(), Base64.NO_WRAP);
		byte[] sps = Base64.decode(GlobalVariables.config.getB64SPS(), Base64.NO_WRAP);
		((H264Packetizer)mPacketizer).setStreamParameters(pps, sps);
		super.start();
	}

	public MP4Config testH264() throws IllegalStateException, IOException {
//		if(mSettings != null) {
//			if(mSettings.contains("h264" + mQuality.framerate + "," + mQuality.resX + "," + mQuality.resY)) {
//				String[] s = mSettings.getString("h264" + mQuality.framerate + "," + mQuality.resX + "," + mQuality.resY, "").split(",");
//				return new MP4Config(s[0], s[1], s[2]);
//			}
//		}
		try {
			Thread.sleep(500);
		} catch(InterruptedException ignore) {}
		
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new IllegalStateException("External storage error!");
		}
		
		final String TESTFILE = Environment.getExternalStorageDirectory().getPath() + "/bcast-test.mp4";
		
		if(DEBUGGING) {
			Log.i(TAG, "Testing H264 support...Test file saved at: " + TESTFILE);
		}
//			boolean savedFlashState = mFlashState;
//			mFlashState = false;
		createCamera();
		if(mPreviewStarted) {
			lockCamera();
			try {
				mCamera.stopPreview();
			} catch(Exception e) {}
			mPreviewStarted = false;
		}
		
		try {
			Thread.sleep(200);
		} catch(InterruptedException e1) {
			e1.printStackTrace();
		}
		
		unlockCamera();
		
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setMaxDuration(1000);
		mMediaRecorder.setVideoEncoder(mVideoEncoder);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.setVideoSize(mQuality.resX, mQuality.resY);
		mMediaRecorder.setVideoFrameRate(mQuality.framerate);
		mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);
		mMediaRecorder.setOutputFile(TESTFILE);
		
		mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			public void onInfo(MediaRecorder mr, int what, int extra) {
				if(DEBUGGING) {
					Log.d(TAG, "MediaRecorder callback called!");
					if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
						Log.d(TAG, "MediaRecorder: max duration reached");
					} else if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
						Log.d(TAG, "MediaRecorder: max filesize reached");
					} else if(what == MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
						Log.d(TAG, "MediaRecorder: info unknown");
					} else {
						Log.d(TAG, "?.?");
					}
				}
				mLock.release();
			}
		});
		
		mMediaRecorder.prepare();
		mMediaRecorder.start();
		
		try {
			if(mLock.tryAcquire(1, TimeUnit.SECONDS)) {
				if(DEBUGGING) {
					Log.d(TAG, "MediaRecorder callback was called");
				}
				Thread.sleep(400);
			} else {
//				if(DEBUGGING) {
//					Log.d(TAG, "MediaRecorder callback was not called after 6 seconds");
//				}
			}
		} catch (InterruptedException e) {
			if(DEBUGGING) {
				e.printStackTrace();
			}
		} finally {
			try {
				mMediaRecorder.stop();
			} catch (Exception e) {}
			mMediaRecorder.release();
			mMediaRecorder = null;
			lockCamera();
		}
		MP4Config config = new MP4Config(TESTFILE);
		File file = new File(TESTFILE);
		if(!file.delete()) {
			if(DEBUGGING) {
				Log.e(TAG, "Temp file could not be deleted");
			}
		}
//		mFlashState = savedFlashState;
		if(DEBUGGING) {
			Log.i(TAG, "H264 test finished");
		}
		
		if(mSettings != null) {
			Editor editor = mSettings.edit();
			editor.putString("h264" + mQuality.framerate + "," + mQuality.resX + "," + mQuality.resY, config.getProfileLevel() + "," + config.getB64SPS() + "," + config.getB64PPS());
			editor.commit();
		}
		return config;
	}
	
	/*public MP4Config testH264(SurfaceView surfaceView) throws IllegalStateException, IOException {
		if(mSettings != null) {
			if(mSettings.contains("h264" + mQuality.framerate + "," + mQuality.resX + "," + mQuality.resY)) {
				String[] s = mSettings.getString("h264" + mQuality.framerate + "," + mQuality.resX + "," + mQuality.resY, "").split(",");
				return new MP4Config(s[0], s[1], s[2]);
			}
		}
		
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new IllegalStateException("External storage error!");
		}
		
		final String TESTFILE = Environment.getExternalStorageDirectory().getPath() + "/bcast-test.mp4";
		
		if(DEBUGGING) {
			Log.i(TAG, "Testing H264 support...Test file saved at: " + TESTFILE);
		}
//			boolean savedFlashState = mFlashState;
//			mFlashState = false;
		
		createCamera(surfaceView);

		lockCamera();
		try {
			mCamera.stopPreview();
		} catch(Exception e) {}
		
		try {
			Thread.sleep(5000);
		} catch(InterruptedException e1) {
			e1.printStackTrace();
		}
		
		unlockCamera();
		
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setMaxDuration(1000);
		mMediaRecorder.setVideoEncoder(mVideoEncoder);
		mMediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
		mMediaRecorder.setVideoSize(mQuality.resX, mQuality.resY);
		mMediaRecorder.setVideoFrameRate(mQuality.framerate);
		mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);
		mMediaRecorder.setOutputFile(TESTFILE);
		
		mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			public void onInfo(MediaRecorder mr, int what, int extra) {
				if(DEBUGGING) {
					Log.d(TAG, "MediaRecorder callback called!");
					if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
						Log.d(TAG, "MediaRecorder: max duration reached");
					} else if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
						Log.d(TAG, "MediaRecorder: max filesize reached");
					} else if(what == MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
						Log.d(TAG, "MediaRecorder: info unknown");
					} else {
						Log.d(TAG, "?.?");
					}
				}
				mLock.release();
			}
		});
		
		mMediaRecorder.prepare();
		mMediaRecorder.start();
		
		try {
			if(mLock.tryAcquire(6, TimeUnit.SECONDS)) {
				if(DEBUGGING) {
					Log.d(TAG, "MediaRecorder callback was called");
				}
				Thread.sleep(400);
			} else {
				if(DEBUGGING) {
					Log.d(TAG, "MediaRecorder callback was not called after 6 seconds");
				}
			}
		} catch (InterruptedException e) {
			if(DEBUGGING) {
				e.printStackTrace();
			}
		} finally {
			try {
				mMediaRecorder.stop();
			} catch (Exception e) {}
			mMediaRecorder.release();
			mMediaRecorder = null;
			lockCamera();
		}
		MP4Config config = new MP4Config(TESTFILE);
		File file = new File(TESTFILE);
		if(!file.delete()) {
			if(DEBUGGING) {
				Log.e(TAG, "Temp file could not be deleted");
			}
		}
//		mFlashState = savedFlashState;
		if(DEBUGGING) {
			Log.i(TAG, "H264 test finished");
		}
		
		if(mSettings != null) {
			Editor editor = mSettings.edit();
			editor.putString("h264" + mQuality.framerate + "," + mQuality.resX + "," + mQuality.resY, config.getProfileLevel() + "," + config.getB64SPS() + "," + config.getB64PPS());
			editor.commit();
		}
		return config;
	}*/

}
