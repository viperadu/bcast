package com.example.bcast.packetizer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Random;

import com.example.bcast.GlobalVariables;
import com.example.bcast.rtcp.SenderReport;
import com.example.bcast.socket.RtpSocket;

public abstract class AbstractPacketizer {
	protected static final int RTP_HEADER_LENGTH = RtpSocket.RTP_HEADER_LENGTH;
	
	protected RtpSocket mSocket = null;
	protected SenderReport mReport = null;
	protected InputStream mIs = null;
	protected byte[] mBuffer;
	protected long ts = 0, mDelta = 0;
	protected long mInterval = 5000;
	
	public AbstractPacketizer() throws IOException {
//		int ssrc = new Random().nextInt();
		ts = new Random().nextInt();
		mSocket = new RtpSocket();
		mReport = new SenderReport();
		mSocket.setSSRC(GlobalVariables.SSRC);
		mReport.setSSRC(GlobalVariables.SSRC);
	}
	
	public RtpSocket getRtpSocket() {
		return mSocket;
	}
	
	public SenderReport getRtcpSocket() {
		return mReport;
	}
	
	public void setSSRC(int ssrc) {
		mSocket.setSSRC(ssrc);
		mReport.setSSRC(ssrc);
	}
	
	public int getSSRC() {
		return mSocket.getSSRC();
	}
	
	public void setInputStream(InputStream is) {
		mIs = is;
	}
	
//	public void setTimeToLive(int ttl) throws IOException {
//		mSocket.setTimeToLive(ttl);
//	}
	
	public void setDestination(InetAddress destAddr, int rtpPort, int rtcpPort) {
		mSocket.setDestination(destAddr, rtpPort);
		mReport.setDestination(destAddr, rtcpPort);
	}
	
	public void setSenderReportsInterval(long interval) {
		mInterval = interval;
	}
	
	public abstract void start() throws IOException;
	
	public abstract void stop();
	
	protected void send(int length) throws IOException {
		mSocket.commitBuffer(length);
		mReport.update(length);
	}
	
	protected static class Statistics {
		public final static String TAG = "Statistics";
		
		private int count = 700, c = 0;
		private float m = 0, q = 0;
		private long elapsed = 0;
		private long start = 0;
		private long duration = 0;
		private long period = 10000000000L;
		private boolean initOffset = false;
		
		public Statistics() {}
		
		public Statistics(int count, int period) {
			this.count = count;
			this.period = period;
		}
	
		public void reset() {
			initOffset = false;
			q = 0;
			m = 0;
			c = 0;
			elapsed = 0;
			start = 0;
			duration = 0;
		}
		
		public void push(long value) {
			elapsed += value;
			if(elapsed > period) {
				elapsed = 0;
				long now = System.nanoTime();
				if(!initOffset || (now - start < 0)) {
					start = now;
					duration = 0;
					initOffset = true;
				}
				
				value += (now - start) - duration;
			}
			if(c < 5) {
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
			long l = (long) m;
			duration += l;
			return l;
		}
	}
}
