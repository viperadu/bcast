package com.example.bcast.audio;

import java.io.IOException;

import android.media.MediaRecorder;
import android.util.Log;

import com.example.bcast.GlobalVariables;
import com.example.bcast.stream.MediaStream;

public abstract class AudioStream extends MediaStream {
	
	protected int mAudioSource;
	protected int mOutputFormat;
	protected int mAudioEncoder;
	public AudioQuality mQuality = GlobalVariables.audioQuality;// = AudioQuality.DEFAULT_AUDIO_QUALITY.clone();
	
	public AudioStream() {
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	}
	
	public void setAudioSource(int audioSource) {
		mAudioSource = audioSource;
	}
	
	public void setAudioSamplingRate(int samplingRate) {
		mQuality.samplingRate = samplingRate;
	}
	
	public void setAudioEncodingBitRate(int bitRate) {
		mQuality.bitRate = bitRate;
	}
	
	public void setAudioQuality(AudioQuality quality) {
		mQuality = quality;
	}
	
	public AudioQuality getAudioQuality() {
		return mQuality;
	}
	
	protected void setAudioEncoder(int audioEncoder) {
		mAudioEncoder = audioEncoder;
	}
	
	protected void setOutputFormat(int outputFormat) {
		mOutputFormat = outputFormat;
	}

	@Override
	protected void encodeWithMediaRecorder() throws IOException {
		createSockets();
		if(DEBUGGING) {
			Log.v(TAG, "Requested audio with " + mQuality.bitRate / 1000 + " kbps at " + mQuality.samplingRate / 1000 + " KHz");
		}
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setAudioSource(mAudioSource);
		mMediaRecorder.setOutputFormat(mOutputFormat);
		mMediaRecorder.setAudioEncoder(mAudioEncoder);
		mMediaRecorder.setAudioChannels(1);
		mMediaRecorder.setAudioSamplingRate(mQuality.samplingRate);
		mMediaRecorder.setAudioEncodingBitRate(mQuality.bitRate);
		
		mMediaRecorder.setOutputFile(mSender.getFileDescriptor());
		
		mMediaRecorder.prepare();
		mMediaRecorder.start();
		
		try {
//			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setDestination(mDestination, GlobalVariables.ports[0], GlobalVariables.ports[1]);
			mPacketizer.setInputStream(mReceiver.getInputStream());
			mPacketizer.start();
			mStreaming = true;
		} catch(IOException e) {
			stop();
			throw new IOException("Local sockets error. Audio failed");
		}
	}

}
