package com.example.bcast.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.example.bcast.GlobalVariables;
import com.example.bcast.packetizer.AACADTSPacketizer;
import com.example.bcast.packetizer.AACLATMPacketizer;
import com.example.bcast.stream.MediaCodecInputStream;

public class AACStream extends AudioStream implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String TAG = "AACStream";
	public static final boolean DEBUGGING = true;
	private boolean configured = false;

	private static final String[] AUDIO_OBJECT_TYPES = { "NULL", // 0
			"AAC Main", // 1
			"AAC LC (Low Complexity)", // 2
			"AAC SSR (Scalable Sample Rate)", // 3
			"AAC LTP (Long Term Prediction)" // 4
	};

	public static final int[] AUDIO_SAMPLING_RATES = { 
			96000, // 0
			88200, // 1
			64000, // 2
			48000, // 3
			44100, // 4
			32000, // 5
			24000, // 6
			22050, // 7
			16000, // 8
			12000, // 9
			11025, // 10
			8000, // 11
			7350, // 12
			-1, // 13
			-1, // 14
			-1, // 15
	};

	private String mSessionDescription;
//	private int mActualSamplingRate;
	private int mProfile, mSamplingRateIndex, mChannel, mConfig;
	private SharedPreferences mSettings = null;
	private AudioRecord mAudioRecord = null;
	private Thread mThread = null;

	public AACStream() throws IOException {
		super();

		if (!AACStreamingSupported()) {
			if (DEBUGGING) {
				Log.e(TAG, "AAC not supported on this phone");
			}
			throw new AACNotSupportedException();
		} else {
			if (DEBUGGING) {
				Log.d(TAG, "AAC supported on this phone");
			}
		}

		/*if (mEncodingMode == mMediaRecorderMode) {
			mPacketizer = new AACADTSPacketizer();
		} else {
			mPacketizer = new AACLATMPacketizer();
		}*/
	}
	
	public void setAudioRecord(AudioRecord ar) {
		this.mAudioRecord = ar;
	}

	private static boolean AACStreamingSupported() {
		if (android.os.Build.VERSION.SDK_INT < 14) {
			return false;
		}
		try {
			MediaRecorder.OutputFormat.class.getField("AAC_ADTS");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void setPreferences(SharedPreferences prefs) {
		mSettings = prefs;
	}

	public void start() throws IllegalStateException, IOException {
		configure();
		if(!mStreaming) {
			super.start();
		}
	}
	
	public int getConfig() throws IllegalStateException, IOException {
		if(!configured) {
			configure();
		}
		return mConfig;
	}
	
	public synchronized void configure() throws IllegalStateException, IOException {
		super.configure();
		int i=0;
		for(; i<AUDIO_SAMPLING_RATES.length; i++) {
			if(AUDIO_SAMPLING_RATES[i] == mQuality.samplingRate) {
				mSamplingRateIndex = i;
				break;
			}
		}
		
		if(i > 12) {
			mQuality.samplingRate = 16000;
		}
		
		if (mPacketizer==null) {
			if (mEncodingMode == mMediaRecorderMode) {
				mPacketizer = new AACADTSPacketizer();
			} else { 
				mPacketizer = new AACLATMPacketizer();
			}		
		}
		
		if (mEncodingMode == mMediaRecorderMode) {
			testADTS();
			mSessionDescription = "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
					"a=rtpmap:96 mpeg4-generic/"+mQuality.samplingRate+"\r\n"+
					"a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config="+Integer.toHexString(mConfig)+"; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";
		} else {
			mProfile = 2; // AAC LC
			mChannel = 1;
			mConfig = mProfile<<11 | mSamplingRateIndex<<7 | mChannel<<3;

			mSessionDescription = "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
					"a=rtpmap:96 mpeg4-generic/"+mQuality.samplingRate+"\r\n"+
					"a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config="+Integer.toHexString(mConfig)+"; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";			
		}
		configured = true;
	}

	@Override
	protected void encodeWithMediaRecorder() throws IOException {
		testADTS();
		((AACADTSPacketizer) mPacketizer).setSamplingRate(mQuality.samplingRate);
		super.encodeWithMediaRecorder();
	}

	@Override
	@SuppressLint({ "InlinedApi", "NewApi" })
	protected void encodeWithMediaCodec() throws IOException {
//		int i = 0;
//		for (; i < AUDIO_SAMPLING_RATES.length; i++) {
//			if (AUDIO_SAMPLING_RATES[i] == mQuality.samplingRate) {
//				break;
//			}
//		}
//		if (i > 12)
//			mQuality.samplingRate = 24000;

		final int bufferSize = AudioRecord.getMinBufferSize(
				mQuality.samplingRate, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT) * 2;
		((AACLATMPacketizer) mPacketizer)
				.setSamplingRate(mQuality.samplingRate);

		mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				mQuality.samplingRate, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		mMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
		MediaFormat format = new MediaFormat();
		format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
		format.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitRate);
		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
		format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mQuality.samplingRate);
		format.setInteger(MediaFormat.KEY_AAC_PROFILE,
				MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
		mMediaCodec.configure(format, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		mAudioRecord.startRecording();
		mMediaCodec.start();

		final MediaCodecInputStream inputStream = new MediaCodecInputStream(
				mMediaCodec);
		final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				int len = 0, bufferIndex = 0;
				try {
					while (!Thread.interrupted()) {
						bufferIndex = mMediaCodec.dequeueInputBuffer(10000);
						if (bufferIndex >= 0) {
							inputBuffers[bufferIndex].clear();
							len = mAudioRecord.read(inputBuffers[bufferIndex],
									bufferSize);
							if (len == AudioRecord.ERROR_INVALID_OPERATION
									|| len == AudioRecord.ERROR_BAD_VALUE) {
								Log.e(TAG,
										"An error occured with the AudioRecord API !");
							} else {
								// Log.v(TAG,"Pushing raw audio to the decoder: len="+len+" bs: "+inputBuffers[bufferIndex].capacity());
								mMediaCodec.queueInputBuffer(bufferIndex, 0,
										len, System.nanoTime() / 1000, 0);
							}
						}
					}
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
				// Log.e(TAG,"Thread 1 over");
			}
		});

		mThread.start();

		try {
			// mReceiver.getInputStream contains the data from the camera
			// the packetizer encapsulates this stream in an RTP stream and send
			// it over the network
//			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setDestination(mDestination, GlobalVariables.ports[0], GlobalVariables.ports[1]);
			mPacketizer.setInputStream(inputStream);
			mPacketizer.start();
			mStreaming = true;
		} catch (IOException e) {
			stop();
			throw new IOException(
					"Something happened with the local sockets :/ Start failed !");
		}
	}
	
	public synchronized void stop() {
		if (mStreaming) {
			if (mEncodingMode == mMediaCodecMode) {
				Log.d(TAG, "Interrupting threads...");
				mThread.interrupt();
				mAudioRecord.stop();
				mAudioRecord.release();
				mAudioRecord = null;
			}
			super.stop();
		}
	}
	
	public String generateSessionDescription() throws IllegalStateException, IOException {
		/*if (mEncodingMode == mMediaRecorderMode) {

			testADTS();
			// TODO: streamType always 5 ? profile-level-id always 15 ?
			return "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
			"a=rtpmap:96 mpeg4-generic/"+mActualSamplingRate+"\r\n"+
			"a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config="+Integer.toHexString(mConfig)+"; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";

		} else {
			
			for (int i=0;i<AUDIO_SAMPLING_RATES.length;i++) {
				if (AUDIO_SAMPLING_RATES[i] == mQuality.samplingRate) {
					mSamplingRateIndex = i;
					break;
				}
			}
			mProfile = 2; // AAC LC
			mChannel = 1;
			mConfig = mProfile<<11 | mSamplingRateIndex<<7 | mChannel<<3;

			return "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
			"a=rtpmap:96 mpeg4-generic/"+mQuality.samplingRate+"\r\n"+
			"a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config="+Integer.toHexString(mConfig)+"; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";			
		}*/
		if (mSessionDescription == null)
			throw new IllegalStateException(
					"You need to call configure() first !");
		return mSessionDescription;
	}
	
	@SuppressLint("InlinedApi")
	private void testADTS() throws IllegalStateException, IOException {

		setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		try {
			Field name = MediaRecorder.OutputFormat.class.getField("AAC_ADTS");
			setOutputFormat(name.getInt(null));
		}
		catch (Exception ignore) {
			setOutputFormat(6);
		}
		
		// Checks if the user has supplied an exotic sampling rate
		int i=0;
		for (;i<AUDIO_SAMPLING_RATES.length;i++) {
			if (AUDIO_SAMPLING_RATES[i] == mQuality.samplingRate) {
				break;
			}
		}
		// If he did, we force a reasonable one: 16 kHz
		if (i>12) {
			Log.e(TAG,"Not a valid sampling rate: "+mQuality.samplingRate);
			mQuality.samplingRate = 16000;
		}
		
		if (mSettings!=null) {
			if (mSettings.contains("aac-"+mQuality.samplingRate)) {
				String[] s = mSettings.getString("aac-"+mQuality.samplingRate, "").split(",");
				mQuality.samplingRate = Integer.valueOf(s[0]);
				mConfig = Integer.valueOf(s[1]);
				mChannel = Integer.valueOf(s[2]);
				return;
			}
		}

		final String TESTFILE = Environment.getExternalStorageDirectory().getPath()+"/bcast-test.adts";

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new IllegalStateException("No external storage or external storage not ready !");
		}

		// The structure of an ADTS packet is described here: http://wiki.multimedia.cx/index.php?title=ADTS

		// ADTS header is 7 or 9 bytes long
		byte[] buffer = new byte[9];

		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setAudioSource(mAudioSource);
		mMediaRecorder.setOutputFormat(mOutputFormat);
		mMediaRecorder.setAudioEncoder(mAudioEncoder);
		mMediaRecorder.setAudioChannels(1);
		mMediaRecorder.setAudioSamplingRate(mQuality.samplingRate);
		mMediaRecorder.setAudioEncodingBitRate(mQuality.bitRate);
		mMediaRecorder.setOutputFile(TESTFILE);
		mMediaRecorder.setMaxDuration(1000);
		mMediaRecorder.prepare();
		mMediaRecorder.start();

		// We record for 1 sec
		// TODO: use the MediaRecorder.OnInfoListener
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}

		mMediaRecorder.stop();
		mMediaRecorder.release();
		mMediaRecorder = null;

		File file = new File(TESTFILE);
		RandomAccessFile raf = new RandomAccessFile(file, "r");

		// ADTS packets start with a sync word: 12bits set to 1
		while (true) {
			if ( (raf.readByte()&0xFF) == 0xFF ) {
				buffer[0] = raf.readByte();
				if ( (buffer[0]&0xF0) == 0xF0) break;
			}
		}

		raf.read(buffer,1,5);

		mSamplingRateIndex = (buffer[1]&0x3C)>>2 ;
		mProfile = ( (buffer[1]&0xC0) >> 6 ) + 1 ;
		mChannel = (buffer[1]&0x01) << 2 | (buffer[2]&0xC0) >> 6 ;
		mQuality.samplingRate = AUDIO_SAMPLING_RATES[mSamplingRateIndex];

		// 5 bits for the object type / 4 bits for the sampling rate / 4 bits for the channel / padding
		mConfig = mProfile<<11 | mSamplingRateIndex<<7 | mChannel<<3;

		Log.i(TAG,"MPEG VERSION: " + ( (buffer[0]&0x08) >> 3 ) );
		Log.i(TAG,"PROTECTION: " + (buffer[0]&0x01) );
		Log.i(TAG,"PROFILE: " + AUDIO_OBJECT_TYPES[ mProfile ] );
		Log.i(TAG,"SAMPLING FREQUENCY: " + mQuality.samplingRate );
		Log.i(TAG,"CHANNEL: " + mChannel );

		raf.close();

		if (mSettings!=null) {
			Editor editor = mSettings.edit();
			editor.putString("aac-"+mQuality.samplingRate, mQuality.samplingRate+","+mConfig+","+mChannel);
			editor.commit();
		}

		if (!file.delete()) Log.e(TAG,"Temp file could not be erased");

	}
}
