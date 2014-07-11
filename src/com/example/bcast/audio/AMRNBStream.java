package com.example.bcast.audio;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;

import android.annotation.TargetApi;
import android.media.MediaRecorder;
import android.os.Build;

import com.example.bcast.packetizer.AMRNBPacketizer;

@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class AMRNBStream extends AudioStream implements Serializable {
	private static final long serialVersionUID = -3434013960770199674L;
	public static final String TAG = "AMRNBStream";
	public static final boolean DEBUGGING = true;
	
	public AMRNBStream() throws IOException {
		super();
		mPacketizer = new AMRNBPacketizer();
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		
		try {
			Field deprecatedName = MediaRecorder.OutputFormat.class.getField("RAW_AMR");
			setOutputFormat(deprecatedName.getInt(null));
		} catch (Exception e) {
			setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
		}
		
		setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		setAudioSamplingRate(mQuality.samplingRate);
	}
	
	public String generateSessionDescription() {
		return "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
				"a=rtpmap:96 AMR/8000\r\n" +
				"a=fmtp:96 octet-align=1;\r\n";
	}

	@Override
	protected void encodeWithMediaCodec() throws IOException {
		super.encodeWithMediaRecorder();
	}
}
