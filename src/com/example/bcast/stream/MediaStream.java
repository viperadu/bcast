package com.example.bcast.stream;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import com.example.bcast.GlobalVariables;
import com.example.bcast.packetizer.AbstractPacketizer;

public abstract class MediaStream implements Stream {
	
	protected static final String TAG = "MediaStream";
	protected static final boolean DEBUGGING = true;
	
	public static final int mMediaRecorderMode = 0x00;
	public static final int mMediaCodecMode = 0x01;
	
	protected AbstractPacketizer mPacketizer;
	
	protected MediaRecorder mMediaRecorder;
	protected MediaCodec mMediaCodec;
	
	protected boolean mStreaming = false;
	protected int mEncodingMode = mMediaRecorderMode;
	protected static int mSuggestedEncodingMode = mMediaRecorderMode;
	private LocalServerSocket mLocalServerSocket = null;
	protected LocalSocket mReceiver, mSender = null;
	private int mSocketId;
	
	protected int mRtpPort = 0, mRtcpPort = 0;
	protected InetAddress mDestination;
	
	static {
		try {
			Class.forName("android.media.MediaCodec");
			mSuggestedEncodingMode = mMediaCodecMode;
			GlobalVariables.MediaCodec = true;
			if(DEBUGGING) {
				Log.i(TAG, "Android API is 4.2 or newer");
			}
		} catch (ClassNotFoundException e) {
			mSuggestedEncodingMode = mMediaRecorderMode;
			GlobalVariables.MediaCodec = false;
			Log.i(TAG, "Android API is older than 4.2");
		}
	}
	
	public MediaStream() {
		mEncodingMode = mSuggestedEncodingMode;
	}
	
	public void setMode(int mode) {
		mEncodingMode = mode;
	}
	
	@Override
	public void start() throws IllegalStateException, IOException {
		if(mDestination == null) {
			throw new IllegalStateException("No destination IP set");
		}
		if(mRtpPort <= 0 || mRtcpPort <= 0) {
			throw new IllegalStateException("No destination RTP/RTCP ports set");
		}
		switch(mEncodingMode) {
		case mMediaRecorderMode:
			encodeWithMediaRecorder();
			break;
		case mMediaCodecMode:
			encodeWithMediaCodec();
			break;
		}
	}

	@Override
	@SuppressLint("NewApi")
	public void stop() {
		if(mStreaming) {
			mPacketizer.stop();
			try {
				if(mEncodingMode == mMediaRecorderMode) {
					mMediaRecorder.stop();
					mMediaRecorder.release();
					mMediaRecorder = null;
				} else {
					mMediaCodec.stop();
					mMediaCodec.release();
					mMediaCodec = null;
				}
				closeSockets();
			} catch (Exception e) {}
			mStreaming = false;
		}
	}
	
	protected abstract void encodeWithMediaRecorder() throws IOException;
	protected abstract void encodeWithMediaCodec() throws IOException;

//	@Override
//	public void setTimeToLive(int ttl) {
//		mPacketizer.setTimeToLive(ttl);
//	}

	@Override
	public void setDestinationAddress(InetAddress dest) {
		mDestination = dest;
	}

	@Override
	public void setDestinationPorts(int dport) {
		mRtpPort = dport;
		mRtcpPort = dport + 1;
	}
	
	public void setDestinationPorts(int rtpPort, int rtcpPort) {
		mRtpPort = rtpPort;
		mRtcpPort = rtcpPort;
	}

	@Override
	public int[] getLocalPorts() {
		return new int[] {
				this.mPacketizer.getRtpSocket().getLocalPort(),
				this.mPacketizer.getRtcpSocket().getLocalPort()
		};
	}

	@Override
	public int[] getDestinationPorts() {
		return new int[] {
				mRtpPort,
				mRtcpPort
		};
	}
//	public abstract int[] getDestinationPorts();

	@Override
	public int getSSRC() {
		return getPacketizer().getSSRC();
	}

	@Override
	public long getBitrate() {
		if(mStreaming) {
			return mPacketizer.getRtpSocket().getBitrate();
		} else {
			return 0;
		}
	}

	@Override
	public boolean isStreaming() {
		return mStreaming;
	}
	
	public AbstractPacketizer getPacketizer() {
		return mPacketizer;
	}
	
	protected void createSockets() throws IOException {
		final String LOCAL_ADDR = "bcast-";
		for(int i=0; i<100; i++) {
			try {
				mSocketId = new Random().nextInt();
				mLocalServerSocket = new LocalServerSocket(LOCAL_ADDR + mSocketId);
				break;
			} catch (IOException e) {}
		}
		
		mReceiver = new LocalSocket();
		mReceiver.connect(new LocalSocketAddress(LOCAL_ADDR + mSocketId));
		mReceiver.setReceiveBufferSize(1000000); //500000
		mSender = mLocalServerSocket.accept();
		mSender.setSendBufferSize(1000000); //500000
	}
	
	protected void closeSockets() {
		try {
			mSender.close();
			mSender = null;
			mReceiver.close();
			mReceiver = null;
			mLocalServerSocket.close();
			mLocalServerSocket = null;
		} catch (Exception e) {}
	}

	public abstract String generateSessionDescription() throws IllegalStateException, IOException;
}
