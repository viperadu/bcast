package com.example.bcast.session;

import java.io.IOException;
import java.net.InetAddress;

import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;

import com.example.bcast.GlobalVariables;
import com.example.bcast.audio.AACStream;
import com.example.bcast.audio.AMRNBStream;
import com.example.bcast.audio.AudioQuality;
import com.example.bcast.audio.AudioStream;
import com.example.bcast.video.H263;
import com.example.bcast.video.H264;
import com.example.bcast.video.VideoQuality;
import com.example.bcast.video.VideoStream;

public class SessionBuilder {
	public final static String TAG = "SessionBuilder";
	// public final static int VIDEO_NONE = 0;
	// public final static int VIDEO_H264 = 1;
	// public final static int VIDEO_H263 = 2;
	// public final static int AUDIO_NONE = 0;
	// public final static int AUDIO_AMRNB = 3;
	// public final static int AUDIO_AAC = 5;

	private VideoQuality mVideoQuality = new VideoQuality();
	private AudioQuality mAudioQuality = new AudioQuality();
	private Context mContext;
	// private int mVideoEncoder = VIDEO_H264;
	private int mVideoEncoder = GlobalVariables.VideoEncoder;
	// private int mAudioEncoder = AUDIO_AMRNB;
	private int mAudioEncoder = GlobalVariables.AudioEncoder;
	private int mCamera = CameraInfo.CAMERA_FACING_BACK;
	private int mTimeToLive = 64;
	private boolean mFlash = false;
	private SurfaceHolder mSurfaceHolder = null;
	private InetAddress mOrigin = null;
	private InetAddress mDestination = null;

	private SessionBuilder() {
	}

	private static volatile SessionBuilder sInstance = null;

	public final static SessionBuilder getInstance() {
		if (sInstance == null) {
			synchronized (SessionBuilder.class) {
				if (sInstance == null) {
					SessionBuilder.sInstance = new SessionBuilder();
				}
			}
		}
		return sInstance;
	}

	public Session build() throws IOException {
		Session session;

		session = new Session();
		session.setContext(mContext);
		session.setOrigin(mOrigin);
		session.setDestination(mDestination);
		// session.setTimeToLive(mTimeToLive);
		// switch(mAudioEncoder) {
		switch (GlobalVariables.AudioEncoder) {
			case MediaRecorder.AudioEncoder.AAC:
				AACStream stream = new AACStream();
				session.addAudioTrack(stream);
				if (mContext != null) {
					stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
				}
				break;
			case MediaRecorder.AudioEncoder.AMR_NB:
				session.addAudioTrack(new AMRNBStream());
				break;
		}
		switch (GlobalVariables.VideoEncoder) {
			case MediaRecorder.VideoEncoder.H263:
				session.addVideoTrack(new H263(mCamera));
				break;
			case MediaRecorder.VideoEncoder.H264:
				H264 stream = new H264(mCamera);
				if (mContext != null) {
					stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
				}
				session.addVideoTrack(stream);
				break;
		}


		if (session.getAudioTrack() != null) {
			AudioStream audio = session.getAudioTrack();
			audio.setAudioQuality(AudioQuality.merge(mAudioQuality,
					audio.getAudioQuality()));
			audio.setDestinationPorts(GlobalVariables.ports[0], GlobalVariables.ports[1]);
		}
		
		if (session.getVideoTrack() != null) {
			VideoStream video = session.getVideoTrack();
			// video.setFlashState(mFlash);
			video.setVideoQuality(VideoQuality.merge(mVideoQuality,
					video.getVideoQuality()));
			video.setPreviewDisplay(mSurfaceHolder);
			video.setDestinationPorts(GlobalVariables.ports[2],GlobalVariables.ports[3]);
		}

		return session;
	}

	public SessionBuilder setContext(Context context) {
		mContext = context;
		return this;
	}

	public SessionBuilder setDestination(InetAddress destination) {
		mDestination = destination;
		return this;
	}

	public SessionBuilder setOrigin(InetAddress origin) {
		mOrigin = origin;
		return this;
	}

	public SessionBuilder setVideoEncoder(int encoder) {
		mVideoEncoder = encoder;
		return this;
	}

	public SessionBuilder setVideoQuality(VideoQuality quality) {
		mVideoQuality = VideoQuality.merge(quality, mVideoQuality);
		return this;
	}

	public SessionBuilder setAudioEncoder(int encoder) {
		mAudioEncoder = encoder;
		return this;
	}

	public SessionBuilder setAudioQuality(AudioQuality quality) {
		mAudioQuality = AudioQuality.merge(quality, mAudioQuality);
		return this;
	}

	public SessionBuilder setFlashEnabled(boolean enabled) {
		mFlash = enabled;
		return this;
	}

	public SessionBuilder setCamera(int camera) {
		mCamera = camera;
		return this;
	}

	public SessionBuilder setTimeToLive(int ttl) {
		mTimeToLive = ttl;
		return this;
	}

	public SessionBuilder setSurfaceHolder(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
		return this;
	}

	public Context getContext() {
		return mContext;
	}

	public InetAddress getDestination() {
		return mDestination;
	}

	public InetAddress getOrigin() {
		return mOrigin;
	}

	public int getAudioEncoder() {
		return mAudioEncoder;
	}

	public int getVideoEncoder() {
		return mVideoEncoder;
	}

	public int getCamera() {
		return mCamera;
	}

	public AudioQuality getAudioQuality() {
		return mAudioQuality;
	}

	public VideoQuality getVideoQuality() {
		return mVideoQuality;
	}

	// public boolean getFlashState() {
	// return mState;
	// }

	public SurfaceHolder getSurfaceHolder() {
		return mSurfaceHolder;
	}

	public int getTimeToLive() {
		return mTimeToLive;
	}

	public SessionBuilder clone() {
		return new SessionBuilder().setDestination(mDestination)
				.setOrigin(mOrigin).setSurfaceHolder(mSurfaceHolder)
				.setVideoQuality(mVideoQuality)
				.setAudioQuality(mAudioQuality)
				// .setFlashEnabled(mFlash)
				.setCamera(mCamera).setTimeToLive(mTimeToLive)
				.setAudioEncoder(mAudioEncoder).setVideoEncoder(mVideoEncoder)
				.setContext(mContext);

	}
}
