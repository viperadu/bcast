package com.example.bcast.packetizer;

import java.io.IOException;

import com.example.bcast.stream.MediaCodecInputStream;

import android.annotation.SuppressLint;
import android.media.MediaCodec.BufferInfo;
import android.os.SystemClock;
import android.util.Log;

public class AACLATMPacketizer extends AbstractPacketizer implements Runnable {
	private static final String TAG = "AACLATMPacketizer";
	private static final boolean DEBUGGING = true;
	
	private static final int MAXPACKETSIZE = 1400;
	private Thread t;
	private int samplingRate = 8000;
	
	public AACLATMPacketizer() throws IOException {
		super();
	}
	
	@Override
	public void start() throws IOException {
		if(t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	@Override
	public void stop() {
		if(t != null) {
			try {
				mIs.close();
			} catch(IOException ignore) {}
			t.interrupt();
			try {
				t.join();
			} catch(InterruptedException e) {}
			t = null;
		}
	}
	
	public void setSamplingRate(int samplingRate) {
//		this.samplingRate = samplingRate;
		mSocket.setClockFrequency(samplingRate);
	}
	
	@SuppressLint("NewApi")
	public void run() {
		if(DEBUGGING) {
			Log.d(TAG, "AAC LATM Packetizer started");
		}
		
		int length = 0;
//		long oldtime = SystemClock.elapsedRealtime(), now = oldtime;
		long oldts;
		BufferInfo bufferInfo;
		try {
			while(!Thread.interrupted()) {
				mBuffer = mSocket.requestBuffer();
				length = mIs.read(mBuffer, RTP_HEADER_LENGTH + 4, MAXPACKETSIZE - (RTP_HEADER_LENGTH + 4));
				if(length > 0) {
					bufferInfo = ((MediaCodecInputStream)mIs).getLastBufferInfo();
					oldts = ts;
					ts = bufferInfo.presentationTimeUs * 1000;
					if(oldts > ts) {
						mSocket.commitBuffer();
						continue;
					}
					
					
					
					mSocket.markNextPacket();
					mSocket.updateTimestamp(ts);
					
					mBuffer[RTP_HEADER_LENGTH] = 0;
					mBuffer[RTP_HEADER_LENGTH + 1] = 0x10;
					
					mBuffer[RTP_HEADER_LENGTH + 2] = (byte) (length >> 5);
					mBuffer[RTP_HEADER_LENGTH + 3] = (byte) (length << 3);
					
					mBuffer[RTP_HEADER_LENGTH + 3] &= 0xF8;
					mBuffer[RTP_HEADER_LENGTH + 3] |= 0x00;
					
					send(RTP_HEADER_LENGTH + length + 4);
				} else {
					mSocket.commitBuffer();
				}
			}
		} catch(IOException e) {
			if(DEBUGGING) {
				e.printStackTrace();
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			if(DEBUGGING) {
				Log.e(TAG, "ArrayIndexOutOfBoundsException: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
				e.printStackTrace();
			}
		} catch(InterruptedException ignore) {}
		Log.d(TAG, "AAC LATM Packetizer stopped");
	}

}
