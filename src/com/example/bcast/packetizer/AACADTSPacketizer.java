package com.example.bcast.packetizer;

import java.io.IOException;

import com.example.bcast.audio.AACStream;

import android.os.SystemClock;
import android.util.Log;

public class AACADTSPacketizer extends AbstractPacketizer implements Runnable {
	private static final String TAG = "AACADTSPacketizer";
	private static final boolean DEBUGGING = true;
	
	private final static int MAXPACKETSIZE = 1400;
	private Thread t;
	private int samplingRate = 8000;
	
	public AACADTSPacketizer() throws IOException {
		super();
	}

	public void setSamplingRate(int samplingRate) {
		this.samplingRate = samplingRate;
		mSocket.setClockFrequency(samplingRate);
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
			} catch (InterruptedException e) {}
			t = null;
		}
	}
	
	@Override
	public void run() {
		if(DEBUGGING) {
			Log.d(TAG, "AAC ADTS Packetizer started");
		}
		boolean protection;
		int frameLength, sum, length, nbau, nbpk, samplingRateIndex, profile;
		long oldtime = SystemClock.elapsedRealtime(), now = oldtime;
		byte[] header = new byte[8];
		
		try {
			while(!Thread.interrupted()) {
				while(true) {
					if( (mIs.read() & 0xFF) == 0xFF) {
						header[1] = (byte) mIs.read();
						if( (header[1] & 0xF0) == 0xF0) {
							break;
						}
					}
				}
				
				fill(header, 2, 5);
				
				protection = (header[1] & 0x01) > 0 ? true : false;
				frameLength = (header[3]&0x03) << 11 | 
						(header[4]&0xFF) << 3 | 
						(header[5]&0xFF) >> 5 ;
				frameLength -= (protection ? 7 : 9);
				
				nbau = (header[6] & 0x03) + 1;
				
				nbpk = frameLength / MAXPACKETSIZE + 1;
				
				if(!protection) {
					mIs.read(header, 0, 2);
				}
				
				samplingRate = AACStream.AUDIO_SAMPLING_RATES[(header[2]&0x3C) >> 2];
				profile = ( (header[2]&0xC0) >> 6 ) + 1 ;
				
				ts += 1024L*1000000000L/samplingRate;
				
				now = SystemClock.elapsedRealtime();
				if(mInterval > 0) {
					if(now - oldtime >= mInterval) {
						oldtime = now;
						mReport.send(System.nanoTime(), ts * samplingRate / 1000000000L);
					}
				}
				
				sum = 0;
				while(sum < frameLength) {
					mBuffer = mSocket.requestBuffer();
					mSocket.updateTimestamp(ts);
					
					if(frameLength - sum > MAXPACKETSIZE - RTP_HEADER_LENGTH - 4) {
						length = MAXPACKETSIZE - RTP_HEADER_LENGTH - 4;
					} else {
						length = frameLength - sum;
						mSocket.markNextPacket();
					}
					sum += length;
					fill(mBuffer, RTP_HEADER_LENGTH + 4, length);
					
					mBuffer[RTP_HEADER_LENGTH] = 0;
					mBuffer[RTP_HEADER_LENGTH + 1] = 0x10;
					
					mBuffer[RTP_HEADER_LENGTH + 2] = (byte) (frameLength >> 5);
					mBuffer[RTP_HEADER_LENGTH + 3] = (byte) (frameLength << 3);
					
					mBuffer[RTP_HEADER_LENGTH + 3] &= 0xF8;
					mBuffer[RTP_HEADER_LENGTH + 3] |= 0x00;
					
					send(RTP_HEADER_LENGTH + 4 + length);
				}
			}
		} catch (IOException e) {
		} catch (ArrayIndexOutOfBoundsException e) {
			if(DEBUGGING) {
				Log.e(TAG, "ArrayIndexOutOfBoundsException: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
				e.printStackTrace();
			}
		} catch (InterruptedException e) {}
		if(DEBUGGING) {
			Log.d(TAG, "AAC ADTS Packetizer stopped");
		}
	}
	
	private int fill(byte[] buffer, int offset, int length) throws IOException {
		int sum = 0, len;
		while(sum < length) {
			len = mIs.read(buffer, offset + sum, length - sum);
			if(len < 0) {
				throw new IOException("End of stream");
			} else {
				sum += len;
			}
		}
		return sum;
	}

}
