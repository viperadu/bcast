package com.example.bcast.video;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

public class VideoQuality implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static final String TAG = "VideoQuality";
//	public final static VideoQuality DEFAULT_VIDEO_QUALITY = new VideoQuality(640, 480, 15, 500000);
	
	public int resX = 0;
	public int resY = 0;
	public int framerate = 0;
	public int bitrate = 0;
	public int orientation = 90;
	
	public VideoQuality() {}
	
	public VideoQuality(int resX, int resY, int framerate, int bitrate, int orientation) {
		this.resX = resX;
		this.resY = resY;
		this.framerate = framerate;
		this.bitrate = bitrate;
		this.orientation = orientation;
	}
	
	public VideoQuality(int resX, int resY, int framerate, int bitrate) {
		this.resX = resX;
		this.resY = resY;
		this.framerate = framerate;
		this.bitrate = bitrate;
	}
	
	public boolean equals(VideoQuality quality) {
		if (quality==null) return false;
		return (quality.resX == this.resX 				&
				quality.resY == this.resY 				&
				quality.framerate == this.framerate	&
				quality.bitrate == this.bitrate 		&
				quality.orientation == this.orientation);
	}
	
	public VideoQuality clone() {
		return new VideoQuality(resX, resY, framerate, bitrate);
	}
	
	public static VideoQuality parseQuality(String str) {
		VideoQuality quality = new VideoQuality(0,0,0,0);
		if (str != null) {
			String[] config = str.split("-");
			try {
				quality.bitrate = Integer.parseInt(config[0])*1000; // conversion to bit/s
				quality.framerate = Integer.parseInt(config[1]);
				quality.resX = Integer.parseInt(config[2]);
				quality.resY = Integer.parseInt(config[3]);
			}
			catch (IndexOutOfBoundsException ignore) {}
		}
		return quality;
	}
	
	public static VideoQuality merge(VideoQuality videoQuality, VideoQuality withVideoQuality) {
		if (withVideoQuality != null && videoQuality != null) {
			if (videoQuality.resX==0) videoQuality.resX = withVideoQuality.resX;
			if (videoQuality.resY==0) videoQuality.resY = withVideoQuality.resY;
			if (videoQuality.framerate==0) videoQuality.framerate = withVideoQuality.framerate;
			if (videoQuality.bitrate==0) videoQuality.bitrate = withVideoQuality.bitrate;
			if (videoQuality.orientation==90) videoQuality.orientation = withVideoQuality.orientation;
		}
		return videoQuality;
	}
	
	public static VideoQuality determineClosestSupportedResolution(Camera.Parameters parameters, VideoQuality quality) {
		VideoQuality v = quality.clone();
		int minDist = Integer.MAX_VALUE;
		String supportedSizesStr = "Supported resolutions: ";
		List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
		for (Iterator<Size> it = supportedSizes.iterator(); it.hasNext();) {
			Size size = it.next();
			supportedSizesStr += size.width+"x"+size.height+(it.hasNext()?", ":"");
			int dist = Math.abs(quality.resX - size.width);
			if (dist<minDist) {
				minDist = dist;
				v.resX = size.width;
				v.resY = size.height;
			}
		}
		Log.v(TAG, supportedSizesStr);
		if (quality.resX != v.resX || quality.resY != v.resY) {
			Log.v(TAG,"Resolution modified: "+quality.resX+"x"+quality.resY+"->"+v.resX+"x"+v.resY);
		}
		
		return v;
	}

	public static int[] determineMaximumSupportedFramerate(Camera.Parameters parameters) {
		int[] maxFps = new int[]{0,0};
		String supportedFpsRangesStr = "Supported frame rates: ";
		List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
		for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext();) {
			int[] interval = it.next();
			supportedFpsRangesStr += interval[0]/1000+"-"+interval[1]/1000+"fps"+(it.hasNext()?", ":"");
			if (interval[1]>maxFps[1] || (interval[0]>maxFps[0] && interval[1]==maxFps[1])) {
				maxFps = interval; 
			}
		}
		Log.v(TAG, supportedFpsRangesStr);
		return maxFps;
	} 

}
