package com.example.bcast.video;

import java.io.Serializable;

public class VideoQuality implements Serializable {
	public final static VideoQuality DEFAULT_VIDEO_QUALITY = new VideoQuality(640, 480, 15, 500000);
	
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
}
