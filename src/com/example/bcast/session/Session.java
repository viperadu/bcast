package com.example.bcast.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.bcast.GlobalVariables;
import com.example.bcast.audio.AudioStream;
import com.example.bcast.stream.Stream;
import com.example.bcast.video.H264;
import com.example.bcast.video.MP4Config;
import com.example.bcast.video.VideoStream;

public class Session implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public final static String TAG = "Session";
	
	private static Object sLock = new Object();
	
	private InetAddress mOrigin;
	private InetAddress mDestination;
//	private int mTimeToLive = 64;
	private long mTimestamp;
	private Context mContext = null;
//	private WifiManager.MulticastLock mLock = null;
	
	private AudioStream mAudioStream = null;
	private VideoStream mVideoStream = null;
	
	
	public Session() {
		this(null, null);
		try {
			mOrigin = InetAddress.getLocalHost();
		} catch (Exception ignore){
			mOrigin = null;
		}
	}
	
	public Session (InetAddress origin, InetAddress destination) {
		long uptime = System.currentTimeMillis();
		mDestination = destination;
		mOrigin = origin;
		mTimestamp = (uptime / 1000) << 32 & (((uptime - ((uptime / 1000) * 1000)) >> 32 ) / 1000);
	}
	
	public Session clone() {
		Session s = new Session(mOrigin, mDestination);
		s.mAudioStream = this.mAudioStream;
		s.mVideoStream = this.mVideoStream;
		s.mContext = null;
		return s;
	}
	
	public void addAudioTrack(AudioStream track) {
		mAudioStream = track;
	}
	
	public void addVideoTrack(VideoStream track) {
		mVideoStream = track;
	}
	
	public void removeAudioTrack() {
		mAudioStream = null;
	}
	
	public void removeVideoTrack() {
		mVideoStream = null;
	}
	
	public AudioStream getAudioTrack() {
		return mAudioStream;
	}
	
	public VideoStream getVideoTrack() {
		return mVideoStream;
	}
	
	public void setContext(Context context) {
		mContext = context;
	}
	
	public void setOrigin(InetAddress origin) {
		mOrigin = origin;
	}
	
	public void setDestination(InetAddress destination) throws IllegalStateException {
		mDestination = destination;
	}
	
//	public void setTimeToLive(int ttl) {
//		mTimeToLive = ttl;
//	}
	
	public String getSessionDescription() throws IllegalStateException, IOException {
		if(mDestination == null) {
			throw new IllegalStateException("destination is null");
		}
		synchronized(sLock) {
			StringBuilder sessionDescription = new StringBuilder();
			sessionDescription.append("v=0\r\n");
			sessionDescription.append("o=- "+mTimestamp+" "+mTimestamp+" IN IP4 "+(mOrigin==null?"127.0.0.1":mOrigin.getHostAddress())+"\r\n");
			sessionDescription.append("s=Unnamed\r\n");
			sessionDescription.append("i=N/A\r\n");
			sessionDescription.append("c=IN IP4 "+mDestination.getHostAddress()+"\r\n");
			// t=0 0 means the session is permanent (we don't know when it will stop)
			sessionDescription.append("t=0 0\r\n");
			sessionDescription.append("a=recvonly\r\n");
			if (mAudioStream != null) {
				sessionDescription.append(mAudioStream.generateSessionDescription());
				sessionDescription.append("a=control:trackID="+0+"\r\n");
			}
			if (mVideoStream != null) {
				sessionDescription.append(mVideoStream.generateSessionDescription());
				sessionDescription.append("a=control:trackID="+1+"\r\n");
			}			
			return sessionDescription.toString();
		}
	}
	
	public InetAddress getDestination() {
		return mDestination;
	}
	
	public boolean trackExists(int id) {
		if(id == 0) {
			return mAudioStream!=null;
		} else {
			return mVideoStream!= null;
		}
	}
	
	public Stream getTrack(int id) {
		if(id == 0) {
			return mAudioStream;
		} else {
			return mVideoStream;
		}
	}
	
	public long getBitrate() {
		long sum = 0;
		if(mAudioStream != null) {
			sum += mAudioStream.getBitrate();
		}
		if(mVideoStream != null) {
			sum += mVideoStream.getBitrate();
		}
		return sum;
	}
	
	public boolean isStreaming() {
		if((mAudioStream != null && mAudioStream.isStreaming()) || (mVideoStream!= null && mVideoStream.isStreaming())) {
			return true;
		} else {
			return false;
		}
	}
	
	public MP4Config getConfig() throws IOException {
		if(mVideoStream instanceof H264) {
			return ((H264) mVideoStream).testH264();
		} else {
			return null;
		}
	}
	
	public void start(int id) throws IllegalStateException, IOException {
		synchronized(sLock) {
			Stream stream = id == 0 ? mAudioStream : mVideoStream;
			if(stream != null && !stream.isStreaming()) {
//				stream.setTimeToLive(mTimeToLive);
				stream.setDestinationAddress(mDestination);
				stream.start();
			}
		}
	}
	
	public void start() throws IllegalStateException, IOException {
		synchronized(sLock) {
//			if(mDestination.isMulticastAddress()) {
//				if(mContext != null) {
//					WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
//					if(wifi != null) {
//						mLock = wifi.createMulticastLock("com.example.bcast.streaming");
//						mLock.acquire();
//					}
//				}
//			}
			start(0);
			start(1);
		}
	}
	
	public void stop(int id) {
		synchronized (sLock) {
//			if(mLock != null) {
//				if(mLock.isHeld()) {
//					mLock.release();
//				}
//				mLock = null;
//			}
			Stream stream = id == 0 ? mAudioStream : mVideoStream;
			if(stream != null) {
				stream.stop();
			}
		}
	}
	
	public void stop() {
		stop(0);
		stop(1);
	}
	
	public void flush() {
		synchronized(sLock) {
			if(mVideoStream != null) {
				mVideoStream.stop();
				mVideoStream = null;
			}
			if(mAudioStream != null) {
				mAudioStream.stop();
				mAudioStream = null;
			}
			
		}
	}
}

