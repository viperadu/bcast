package com.example.bcast.video;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.example.bcast.GlobalVariables;
import com.example.bcast.stream.MediaCodecInputStream;
import com.example.bcast.stream.MediaStream;

@SuppressLint("NewApi")
public abstract class VideoStream extends MediaStream {
	protected final static String TAG = "VideoStream";
	
	public VideoQuality mQuality = GlobalVariables.videoQuality;//VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
	protected SurfaceHolder.Callback mSurfaceHolderCallback;
	protected SurfaceHolder mSurfaceHolder = null;
	protected int mVideoEncoder, mCameraId = 0;
	protected Camera mCamera;
	protected boolean mCameraOpenedManually = true;
//	protected boolean mFlashState = false;
	protected boolean mSurfaceReady = false;
	protected boolean mUnlocked = false;
	protected boolean mPreviewStarted = false;
	
	public VideoStream() {
		this(CameraInfo.CAMERA_FACING_BACK);
	}

	public VideoStream(int cameraId) {
		super();
		setCamera(cameraId);
		setMode(mMediaRecorderMode);
	}
	
	public VideoStream(int cameraId, int mode) {
		super();
		setCamera(cameraId);
		setMode(mode);
	}
	
	public void setCamera(int cameraId) {
		CameraInfo cameraInfo = new CameraInfo();
		int numberOfCameras = Camera.getNumberOfCameras();
		for(int i=0; i<numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if(cameraInfo.facing == cameraId) {
				this.mCameraId = i;
				break;
			}
		}
	}
	
	public void switchCamera() throws RuntimeException, IOException {
		if(Camera.getNumberOfCameras() == 1) {
			throw new IllegalStateException("Phone only has one camera");
		}
		boolean streaming = mStreaming;
		boolean previewing = mCamera != null && mCameraOpenedManually;
		if(mCameraId == CameraInfo.CAMERA_FACING_BACK) {
			mCameraId = CameraInfo.CAMERA_FACING_FRONT;
		} else {
			mCameraId = CameraInfo.CAMERA_FACING_BACK;
		}
		setCamera(mCameraId);
		stopPreview();
		if(previewing) {
			startPreview();
		}
		if(streaming) {
			start();
		}
	}
	
	public int getCamera() {
		return mCameraId;
	}
	
	public synchronized void setPreviewDisplay(SurfaceHolder surfaceHolder) {
		if(mSurfaceHolderCallback != null && mSurfaceHolder != null) {
			mSurfaceHolder.removeCallback(mSurfaceHolderCallback);
		}
		if(surfaceHolder != null) {
			mSurfaceHolderCallback = new Callback() {
				
				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
					mSurfaceReady = false;
					stopPreview();
					if(DEBUGGING) {
						Log.d(TAG, "Surface destroyed");
					}
				}
				
				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					mSurfaceReady = true;
				}
				
				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
					if(DEBUGGING) {
						Log.d(TAG, "Surface changed");
					}
				}
			};
			mSurfaceHolder = surfaceHolder;
			mSurfaceHolder.addCallback(mSurfaceHolderCallback);
			mSurfaceReady = true;
		}
	}
	
//	public synchronized void setFlashState(boolean state) {
//		mFlashState = state;
//		if(mCamera != null) {
//			if(mStreaming && mEncodingMode == mMediaRecorderMode) {
//				lockCamera();
//			}
//			
//			Parameters parameters = mCamera.getParameters();
//			
//			if(parameters.getFlashMode() == null) {
//				throw new RuntimeException("Can't turn the flash on!");
//			} else {
//				if(mFlashState) {
//					parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
//				} else {
//					parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
//				}
//				try {
//					mCamera.setParameters(parameters);
//				} catch (RuntimeException e) {
//					throw new RuntimeException("Cannot turn the flash on");
//				}
//			}
//			
//			if(mStreaming && mEncodingMode == mMediaRecorderMode) {
//				unlockCamera();
//			}
//		}
//	}
	
//	public void toggleFlash() {
//		setFlashState(!mFlashState);
//	}
	
//	public boolean getFlashState() {
//		return mFlashState;
//	}
	
	public void setVideoSize(int width, int height) {
		if(mQuality.resX != width || mQuality.resY != height) {
			mQuality.resX = width;
			mQuality.resY = height;
		}
	}
	
	public void setVideoFramerate(int rate) {
		if(mQuality.framerate != rate) {
			mQuality.framerate = rate;
		}
	}
	
	public void setVideoEncodingBitrate(int bitrate) {
		if(mQuality.bitrate != bitrate) {
			mQuality.bitrate = bitrate;
		}
	}
	
	public void setVideoQuality(VideoQuality videoQuality) {
		if(!mQuality.equals(videoQuality)) {
			mQuality = videoQuality;
		}
	}
	
	public VideoQuality getVideoQuality() {
		return mQuality;
	}
	
	public synchronized void start() throws IllegalStateException, IOException {
		if(!mPreviewStarted) {
			mCameraOpenedManually = false;
		}
		super.start();
	}
	
	public synchronized void stop() {
		if(mCamera != null) {
			if(mEncodingMode == mMediaRecorderMode) {
				mCamera.setPreviewCallback(null);
			}
			super.stop();
			if(!mCameraOpenedManually) {
				destroyCamera();
			} else {
				try {
					startPreview();
				} catch(RuntimeException e) {
					if(DEBUGGING) {
						e.printStackTrace();
					}
				} catch(IOException e) {
					if(DEBUGGING) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public synchronized void startPreview() throws RuntimeException, IOException {
		if(!mPreviewStarted) {
			createCamera();
			try {
				mCamera.startPreview();
				mPreviewStarted = true;
				mCameraOpenedManually = true;
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}
		}
	}
	
	public synchronized void stopPreview() {
		mCameraOpenedManually = false;
		stop();
	}
	
	protected void setVideoEncoder(int videoEncoder) {
		this.mVideoEncoder = videoEncoder;
	}
	
	@Override
	protected void encodeWithMediaRecorder() throws IOException {
		createSockets();
		createCamera();
		if(mPreviewStarted) {
			lockCamera();
			try {
				mCamera.stopPreview();
			} catch(Exception e) {}
			mPreviewStarted = false;
		}
		unlockCamera();
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setVideoEncoder(mVideoEncoder);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.setVideoSize(mQuality.resX, mQuality.resY);
		mMediaRecorder.setVideoFrameRate(mQuality.framerate);
		mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);
		
		mMediaRecorder.setOutputFile(mSender.getFileDescriptor());
		
		mMediaRecorder.prepare();
		mMediaRecorder.start();
		
Log.i(TAG, "MediaRecorder started with: " + mQuality.resX + "x" + mQuality.resY + " at " + mQuality.framerate + " fps and " + mQuality.bitrate + " bitrate.");
		
		try {
//			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setDestination(mDestination, GlobalVariables.ports[2], GlobalVariables.ports[3]);
			mPacketizer.setInputStream(mReceiver.getInputStream());
			mPacketizer.start();
			mStreaming = true;
		} catch(IOException e) {
			stop();
			throw new IOException("Start failed");
		}
	}

	@Override
	protected void encodeWithMediaCodec() throws IOException {
		createCamera();
		if(!mPreviewStarted) {
			try {
				mCamera.startPreview();
				mPreviewStarted = true;
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}
		}
		mMediaCodec = MediaCodec.createEncoderByType("video/avc");
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mQuality.resX, mQuality.resY);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 4);
		mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mMediaCodec.start();
		
		final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
		
		mCamera.setPreviewCallback(new Camera.PreviewCallback() {
			
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				long now = System.nanoTime() / 1000, timeout = 1000000 / mQuality.framerate;
				int bufferIndex = mMediaCodec.dequeueInputBuffer(timeout);
				
				if(bufferIndex >= 0) {
					inputBuffers[bufferIndex].clear();
					inputBuffers[bufferIndex].put(data, 0, data.length);
					mMediaCodec.queueInputBuffer(bufferIndex, 0, data.length, System.nanoTime() / 1000, 0);
				} else {
					if(DEBUGGING) {
						Log.e(TAG, "No buffer available");
					}
				}
			}
		});
		
		try {
//			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setDestination(mDestination, GlobalVariables.ports[2], GlobalVariables.ports[3]);
			mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
			mPacketizer.start();
			mStreaming = true;
		} catch (IOException e) {
			stop();
			throw new IOException ("MediaCodec start failed");
		}
	}
	
	public abstract String generateSessionDescription() throws IllegalStateException, IOException;
	
	protected synchronized void createCamera() throws RuntimeException, IOException {
		if(mSurfaceHolder == null || mSurfaceHolder.getSurface() == null || !mSurfaceReady) {
			if(GlobalVariables.DEBUGGING) {
				if(mSurfaceHolder == null) Log.e(TAG, "mSurfaceHolder is null");
				if(mSurfaceHolder.getSurface() == null) Log.e(TAG, "surface is null");
				if(!mSurfaceReady) Log.e(TAG, "surface not ready");
			}
			throw new IllegalStateException("Invalid surface!");
		}
		
		if(mCamera == null) {
			mCamera = Camera.open(mCameraId);
			mUnlocked = false;
			mCamera.setErrorCallback(new Camera.ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					if(error == Camera.CAMERA_ERROR_SERVER_DIED) {
						if(DEBUGGING) {
							Log.e(TAG, "Media server died");
						}
						mCameraOpenedManually = false;
						stop();
					} else {
						if(DEBUGGING) {
							Log.e(TAG, "Error unknown with the camera: " + error);
						}
					}
				}
			});
			Parameters parameters = mCamera.getParameters();
			
			//TODO: delete this, only for debugging
			List<int[]> a = parameters.getSupportedPreviewFpsRange();
			for(int[] list : a) {
				Log.i(TAG, "[" + list[0] + "-" + list[1] + "]");
			}
			
			
			if(mEncodingMode == mMediaCodecMode) {
				getClosestSupportedQuality(parameters);
				parameters.setPreviewFormat(ImageFormat.YV12);
				parameters.setPreviewSize(mQuality.resX, mQuality.resY);
//				parameters.setPreviewFrameRate(mQuality.framerate);
				parameters.setPreviewFpsRange(mQuality.framerate * 1000, mQuality.framerate * 1000);
			} else {
				parameters.setPreviewSize(mQuality.resX, mQuality.resY);
				parameters.setPreviewFpsRange(mQuality.framerate * 1000, mQuality.framerate * 1000);
			}
			
//			if(mFlashState) {
//				if(parameters.getFlashMode() == null) {
//					throw new IllegalStateException("Can't turn the flash on");
//				} else {
//					parameters.setFlashMode(mFlashState ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
//				}
//			}
			
			try {
				mCamera.setParameters(parameters);
				mCamera.setDisplayOrientation(mQuality.orientation);
				mCamera.setPreviewDisplay(mSurfaceHolder);
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			} catch (IOException e) {
				destroyCamera();
				throw e;
			}
		}
	}
	
	protected synchronized void createCamera(SurfaceView surfaceView) throws RuntimeException, IOException {
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if(surfaceHolder == null || surfaceHolder.getSurface() == null) {
			if(GlobalVariables.DEBUGGING) {
				if(surfaceHolder == null) Log.e(TAG, "surfaceHolder is null");
				if(surfaceHolder.getSurface() == null) Log.e(TAG, "surface is null");
			}
			throw new IllegalStateException("Invalid surface!");
		}
		
		if(mCamera == null) {
			mCamera = Camera.open(mCameraId);
			mUnlocked = false;
			mCamera.setErrorCallback(new Camera.ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					if(error == Camera.CAMERA_ERROR_SERVER_DIED) {
						if(DEBUGGING) {
							Log.e(TAG, "Media server died");
						}
						mCameraOpenedManually = false;
						stop();
					} else {
						if(DEBUGGING) {
							Log.e(TAG, "Error unknown with the camera: " + error);
						}
					}
				}
			});
			Parameters parameters = mCamera.getParameters();
			
			if(mEncodingMode == mMediaCodecMode) {
				getClosestSupportedQuality(parameters);
				parameters.setPreviewFormat(ImageFormat.YV12);
				parameters.setPreviewSize(mQuality.resX, mQuality.resY);
//				parameters.setPreviewFrameRate(mQuality.framerate);
				parameters.setPreviewFpsRange(mQuality.framerate, mQuality.framerate);
			}
			
//			if(mFlashState) {
//				if(parameters.getFlashMode() == null) {
//					throw new IllegalStateException("Can't turn the flash on");
//				} else {
//					parameters.setFlashMode(mFlashState ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
//				}
//			}
			
			try {
				mCamera.setParameters(parameters);
				mCamera.setDisplayOrientation(mQuality.orientation);
				mCamera.setPreviewDisplay(surfaceHolder);
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			} catch (IOException e) {
				destroyCamera();
				throw e;
			}
		}
	}
	
	protected synchronized void destroyCamera() {
		if(mCamera != null) {
			if(mStreaming) {
				super.stop();
			}
			lockCamera();
			mCamera.stopPreview();
			try {
				mCamera.release();
			} catch (Exception e) {
				if(DEBUGGING) {
					if(e.getMessage() == null) {
						Log.e(TAG, "Unknown error");
					} else {
						Log.e(TAG, e.getMessage());
					}
				}
			}
			mCamera = null;
			mUnlocked = false;
			mPreviewStarted = false;
		}
	}
	
	private void checkMediaCodecAPI() {
		for(int i= MediaCodecList.getCodecCount() - 1; i >= 0; i--) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if(codecInfo.isEncoder()) {
				MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
				for(int j=0; j < capabilities.colorFormats.length; j++) {
					int format = capabilities.colorFormats[i];
					if(DEBUGGING) {
						Log.e(TAG, codecInfo.getName() + " with color format " + format);
					}
				}
				/*for (int j = 0; j < capabilities.profileLevels; j++) {
				int format = capabilities.colorFormats[j];
				Log.e(TAG, codecInfo.getName()+" with color format " + format);           
				}*/
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void getClosestSupportedQuality(Camera.Parameters parameters) {
		String supportedSizesStr = "Supported resolutions: ";
		List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
		for(Iterator<Size> it = supportedSizes.iterator(); it.hasNext();) {
			Size size = it.next();
			supportedSizesStr += size.width + "x" + size.height + (it.hasNext() ? ", " : "");
		}
		if(DEBUGGING) {
			Log.v(TAG, supportedSizesStr);
		}
		String supportedFrameRatesStr = "Supported frame rates: ";
		List<Integer> supportedFrameRates = parameters.getSupportedPreviewFrameRates();
		for(Iterator<Integer> it = supportedFrameRates.iterator(); it.hasNext();) {
			supportedFrameRatesStr += it.next() + "fps" + (it.hasNext() ? ", " : "");
		}
		
		int minDist = Integer.MAX_VALUE, newFps = mQuality.framerate;
		if(!supportedFrameRates.contains(mQuality.framerate)) {
			for(Iterator<Integer> it = supportedFrameRates.iterator(); it.hasNext();) {
				int fps = it.next();
				int dist = Math.abs(fps - mQuality.framerate);
				if(dist < minDist) {
					minDist = dist;
					newFps = fps;
				}
			}
			if(DEBUGGING) {
				Log.v(TAG, "Framerate modified: " + mQuality.framerate + " -> " + newFps);
			}
		}
	}
	
	protected void lockCamera() {
		if(mUnlocked) {
			if(DEBUGGING) {
				Log.d(TAG, "Locking camera");
			}
			try {
				mCamera.reconnect();
			} catch(Exception e) {
				if(DEBUGGING) {
					Log.e(TAG, e.getMessage());
				}
			}
			mUnlocked = false;
		}
	}

	protected void unlockCamera() {
		if(!mUnlocked) {
			if(DEBUGGING) {
				Log.d(TAG, "Unlocking camera");
			}
			try {
				mCamera.unlock();
			} catch (Exception e) {
				if(DEBUGGING) {
					Log.e(TAG, e.getMessage());
				}
			}
			mUnlocked = true;
		}
	}
	
//	@Override
//	public int[] getDestinationPorts() {
//		return new int[] {
//			GlobalVariables.ports[2],
//			GlobalVariables.ports[3]
//		};
//	}
	
}
