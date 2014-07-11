package com.example.bcast.audio;

import java.io.Serializable;

public class AudioQuality implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final AudioQuality DEFAULT_AUDIO_QUALITY = new AudioQuality(8000, 32000);
	
	public int samplingRate;
	public int bitRate;
	
	public AudioQuality() {}
	
	public AudioQuality(int samplingRate, int bitRate) {
		this.samplingRate = samplingRate;
		this.bitRate = bitRate;
	}
	
	public boolean equals(AudioQuality quality) {
		if(quality == null) {
			return false;
		} else {
			return (quality.samplingRate == this.samplingRate & quality.bitRate == this.bitRate);
		}
	}
	
	public AudioQuality clone() {
		return new AudioQuality(samplingRate, bitRate);
	}
	
	public static AudioQuality parseQuality(String str) {
		AudioQuality quality = new AudioQuality(0, 0);
		if(str != null) {
			String[] config = str.split("-");
			try {
				quality.bitRate = Integer.parseInt(config[0]) * 1000;
				quality.samplingRate = Integer.parseInt(config[1]);
			} catch(IndexOutOfBoundsException ignore) {}
		}
		return quality;
	}
	
	public static AudioQuality merge(AudioQuality audioQuality, AudioQuality withAudioQuality) {
		if (withAudioQuality != null && audioQuality != null) {
			if (audioQuality.samplingRate==0) audioQuality.samplingRate = withAudioQuality.samplingRate;
			if (audioQuality.bitRate==0) audioQuality.bitRate = withAudioQuality.bitRate;
		}
		return audioQuality;
	}
}
