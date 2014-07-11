package com.example.bcast.video;

import java.io.IOException;
import java.io.Serializable;

import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;

import com.example.bcast.packetizer.H263Packetizer;

public class H263 extends VideoStream implements Serializable {
	private static final long serialVersionUID = -8892153046515407578L;

	public H263() throws IOException {
		this(CameraInfo.CAMERA_FACING_BACK);
	}
	
	public H263(int cameraId) throws IOException {
		super(cameraId);
		setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		mPacketizer = new H263Packetizer();
	}
	
	@Override
	public String generateSessionDescription() throws IllegalStateException,
			IOException {
		return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
				"a=rtpmap:96 H263-1998/90000\r\n";
	}

}
