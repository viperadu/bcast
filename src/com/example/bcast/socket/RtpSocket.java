package com.example.bcast.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;
import android.util.Log;

public class RtpSocket implements Runnable {
	public static final String TAG = "RtpSocket";
	public static final boolean DEBUGGING = true;
	
	public static final int RTP_HEADER_LENGTH = 12;
	public static final int MTU = 1500;
	
	private DatagramSocket mSocket;
//	private MulticastSocket mSocket;
	private DatagramPacket[] mPackets;
	private byte[][] mBuffers;
	private long[] mTimestamps;
	
	private Semaphore mBufferRequested, mBufferCommitted;
	private Thread mThread;
	
	private long mCacheSize;
	private long mClock = 0;
	private long mOldTimestamp = 0;
	private long mTime = 0, mOldTime = 0;
	private long mBitRate = 0, mOctetCount = 0;
	private int mSSRC, mSeq = 0, mPort = -1;
	private int mBufferCount, mBufferIn, mBufferOut;
	
	public RtpSocket() throws IOException {
		mCacheSize = 400;
		mBufferCount = 600; //300
		mBufferIn = 0;
		mBufferOut = 0;
		mBuffers = new byte[mBufferCount][];
		mPackets = new DatagramPacket[mBufferCount];
		mTimestamps = new long[mBufferCount];
		mBufferRequested = new Semaphore(mBufferCount);
		mBufferCommitted = new Semaphore(0);
		
		for(int i=0; i<mBufferCount; i++) {
			mBuffers[i] = new byte[MTU];
			mPackets[i] = new DatagramPacket(mBuffers[i], 1);
			
			mBuffers[i][0] = (byte) Integer.parseInt("10000000", 2);
			mBuffers[i][1] = (byte) 96;
		}
		
		mSocket = new DatagramSocket();
		Log.i(TAG, "Socket port=" + mSocket.getLocalPort());
//		mSocket = new MulticastSocket();
		mTime = mOldTime = SystemClock.elapsedRealtime();
	}
	
	public void close() {
		mSocket.close();
	}
	
	public void setSSRC(int ssrc) {
		mSSRC = ssrc;
		for(int i=0; i<mBufferCount; i++) {
			setLong(mBuffers[i], ssrc, 8, 12);
		}
	}
	
	public int getSSRC() {
		return mSSRC;
	}
	
	public void setClockFrequency(long clock) {
		mClock = clock;
	}
	
	public void setCacheSize(long cacheSize) {
		mCacheSize = cacheSize;
	}
	
//	public void setTimeToLive(int ttl) throws IOException {
//		mSocket.setTimeToLive(ttl);
//	}
	
	public void setDestination(InetAddress destAddr, int dport) {
		mPort = dport;
		for(int i=0; i<mBufferCount; i++) {
			mPackets[i].setPort(dport);
			mPackets[i].setAddress(destAddr);
		}
	}
	
	public int getPort() {
		return mPort;
	}
	
	public int getLocalPort() {
		return mSocket.getLocalPort();
	}
	
	public byte[] requestBuffer() throws InterruptedException {
		mBufferRequested.acquire();
		mBuffers[mBufferIn][1] &= 0x7F;
		return mBuffers[mBufferIn];
	}
	
	public void commitBuffer() throws IOException {

		if (mThread == null) {
			mThread = new Thread(this);
			mThread.start();
		}
		
		if (++mBufferIn>=mBufferCount) mBufferIn = 0;
		mBufferCommitted.release();

	}	

	
	public void commitBuffer(int length) throws IOException {
		updateSequence();
		mPackets[mBufferIn].setLength(length);
		mOctetCount += length;
		mTime = SystemClock.elapsedRealtime();
		if(mTime - mOldTime > 1500) {
			mBitRate = mOctetCount * 8000 / (mTime - mOldTime);
			mOctetCount = 0;
			mOldTime = mTime;
		}
		
		mBufferCommitted.release();
		if(++mBufferIn >= mBufferCount) {
			mBufferIn = 0;
		}
		if(mThread == null) {
			mThread = new Thread(this);
			mThread.start();
		}
	}
	
	public long getBitrate() {
		return mBitRate;
	}
	
	private void updateSequence() {
		setLong(mBuffers[mBufferIn], ++mSeq, 2, 4);
	}
	
	public void updateTimestamp(long timestamp) {
		mTimestamps[mBufferIn] = timestamp;
		setLong(mBuffers[mBufferIn], (timestamp/100L) * (mClock/1000L) / 10000L, 4, 8);
	}
	
	public void markNextPacket() {
		mBuffers[mBufferIn][1] |= 0x80;
	}
	
	@Override
	public void run() {
//		Statistics stats = new Statistics(50, 3000);
		try {
			Thread.sleep(mCacheSize);
			long delta = 0;
			while(mBufferCommitted.tryAcquire(4, TimeUnit.SECONDS)) {
				if(mOldTimestamp != 0) {
					if((mTimestamps[mBufferOut] - mOldTimestamp) > 0) {
//						stats.push(mTimestamps[mBufferOut] - mOldTimestamp);
//						long d = stats.average() / 1000000;
//						Thread.sleep(d)
					}
					delta += mTimestamps[mBufferOut] - mOldTimestamp;
					if(delta > 500000000 || delta < 0) {
						delta = 0;
					}
				}
				mOldTimestamp = mTimestamps[mBufferOut];
				
				// TODO: de vazut aici ce facem pt reconectat in cazul in care pica conexiunea
				mSocket.send(mPackets[mBufferOut]);
				
				
				if(++mBufferOut >= mBufferCount) {
					mBufferOut = 0;
				}
				mBufferRequested.release();
			}
		} catch (Exception e) {
			if(DEBUGGING) {
				e.printStackTrace();
			}
		}
		mThread = null;
	}
	
	private void setLong(byte[] buffer, long n, int begin, int end) {
		for(end--; end >= begin; end--) {
			buffer[end] = (byte) (n%256);
			n >>= 8;
		}
	}
	
	protected static class Statistics {
		public final static String TAG = "Statistics";
		
		private int count = 500, c = 0;
		private float m = 0, q = 0;
		private long elapsed = 0;
		private long start = 0;
		private long duration = 0;
		private long period = 6000000000L;
		private boolean initOffset = false;
		
		public Statistics(int count, long period) {
			this.count = count;
			this.period = period * 1000000L;
		}
		
		public void push(long value) {
			duration += value;
			elapsed += value;
			if(elapsed > period) {
				elapsed = 0;
				long now = System.nanoTime();
				if(!initOffset || (now - start < 0 )) {
					start = now;
					duration = 0;
					initOffset = true;
				}
				value -= (now - start) - duration;
			}
			if(c < 40) {
				c++;
				m = value;
			} else {
				m = (m * q + value) / (q + 1);
				if(q < count) {
					q++;
				}
			}
		}
		
		public long average() {
			long l = (long) m - 2000000;
			if(l > 0) {
				return l;
			} else {
				return 0;
			}
		}
	}
}