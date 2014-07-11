package com.example.bcast.packetizer;

import java.io.IOException;

import android.util.Log;

public class AMRNBPacketizer extends AbstractPacketizer implements Runnable {
	public static final String TAG = "AMRNBPacketizer";
	public static final boolean DEBUGGING = true;
	
	private final int AMR_HEADER_LENGTH = 6;
	private static final int AMR_FRAME_HEADER_LENGTH = 1;
	private static final int[] sFrameBits = {95, 103, 118, 134, 148, 159, 204, 244};
	private int samplingRate = 8000;
	
	private Thread t;

	public AMRNBPacketizer() throws IOException {
		super();
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
		try {
			mIs.close();
		} catch (IOException ignore) {}
		t.interrupt();
		t = null;
	}
	
	@Override
	public void run() {
		int frameLength, frameType;
		long now = System.nanoTime(), oldtime = now;
		byte[] header = new byte[AMR_HEADER_LENGTH];
		
		try {
			fill(header, 0, AMR_HEADER_LENGTH);
			if(header[5] != '\n') {
				if(DEBUGGING) {
					Log.e(TAG, "Bad AMRNB header. Codec not supported by device!");
					return;
				}
			}
			
			while(!Thread.interrupted()) {
				mBuffer = mSocket.requestBuffer();
				mBuffer[RTP_HEADER_LENGTH] = (byte) 0xF0;
				
				fill(mBuffer, RTP_HEADER_LENGTH + 1, AMR_FRAME_HEADER_LENGTH);
				
				frameType = (Math.abs(mBuffer[RTP_HEADER_LENGTH + 1]) >> 3) & 0x0F;
				frameLength = (sFrameBits[frameType] + 7) / 8;
				
				fill(mBuffer, RTP_HEADER_LENGTH + 2, frameLength);
				
				ts += 160L * 1000000000L / samplingRate;
				mSocket.updateTimestamp(ts);
				mSocket.markNextPacket();
				
				now = System.nanoTime();
				mDelta += (now - oldtime) / 1000000;
				oldtime = now;
				
				if(mInterval > 0) {
					if(mDelta >= mInterval) {
						mDelta = 0;
						mReport.send(now, ts * samplingRate / 1000000000L);
					}
				}
				send(RTP_HEADER_LENGTH + 1 + AMR_FRAME_HEADER_LENGTH + frameLength);
			}
			
		} catch(IOException e) {
		} catch(InterruptedException e) {}
		
		if(DEBUGGING) {
			Log.d(TAG, "AMRNB packetizer stopped");
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
