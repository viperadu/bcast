package com.example.bcast.packetizer;

import java.io.IOException;

import android.util.Log;

public class H264Packetizer extends AbstractPacketizer implements Runnable {
	public final static String TAG = "H264Packetizer";
	public final static boolean DEBUGGING = true;

	private final static int MAXPACKETSIZE = 1400;

	private Thread t = null;
	private int naluLength = 0;
	private long delay = 0, oldtime = 0;
	private Statistics stats = new Statistics();
	private byte[] sps = null, pps = null;
	private int count = 0;

	public H264Packetizer() throws IOException {
		super();
		mSocket.setClockFrequency(90000);
	}

	@Override
	public void start() throws IOException {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	@Override
	public void stop() {
		if (t != null) {
			t.interrupt();
			try {
				t.join(1000);
			} catch (InterruptedException e) {}
			t = null;
		}
	}
	
	public void setStreamParameters(byte[] pps, byte[] sps) {
		this.pps = pps;
		this.sps = sps;
	}
	
	public void run() {
		long duration = 0, delta2 = 0;
		if(DEBUGGING) {
			Log.d(TAG, "H264Packetizer started");
		}
		stats.reset();
		count = 0;
		try {
			byte buffer[] = new byte[4];
			while(!Thread.interrupted()) {
				while(mIs.read() != 'm');
				mIs.read(buffer,0,3);
				if(buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') {
					break;
				}
			}
		} catch (IOException e) {
			if(DEBUGGING) {
				Log.e(TAG, "Couldn't skip MP4 header");
			}
			return;
		}
		
		try {
			while(!Thread.interrupted()) {
				oldtime = System.nanoTime();
				send();
				duration = System.nanoTime() - oldtime;
				
				mDelta += duration / 1000000;
				if(mInterval > 0) {
					if(mDelta >= mInterval) {
						mReport.send(oldtime + duration, (ts/100) * 90 / 10000);
					}
				}
				
				delta2 += duration / 1000000;
				if(delta2 > 5000) {
					delta2 = 0;
					if(sps != null) {
						mBuffer = mSocket.requestBuffer();
						mSocket.markNextPacket();
						mSocket.updateTimestamp(ts);
						System.arraycopy(sps, 0, mBuffer, RTP_HEADER_LENGTH, sps.length);
						super.send(RTP_HEADER_LENGTH + sps.length);
					}
					if(pps != null) {
						mBuffer = mSocket.requestBuffer();
						mSocket.markNextPacket();
						mSocket.updateTimestamp(ts);
						System.arraycopy(pps, 0, mBuffer, RTP_HEADER_LENGTH, pps.length);
						super.send(RTP_HEADER_LENGTH + pps.length);
					}
				}
				
				stats.push(duration);
				delay = stats.average();
				
			}
		} catch(IOException e) {
		} catch(InterruptedException e) {}
		if(DEBUGGING) {
			Log.d(TAG, "H264Packetizer stopped");
		}
	}
	
	private void send() throws IOException, InterruptedException {
		int sum = 1, len = 0, type;
		byte[] header = new byte[5];
		fill(header, 0, 5);
		naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8 | (header[1] & 0xFF) << 16 | (header[0] & 0xFF) << 24;
		
		if(naluLength > 100000 || naluLength < 0) {
			resync();
		}
		
		type = header[4] & 0x1F;
		if(type == 7 || type == 8) {
			count++;
			if(count > 4) {
				sps = null;
				pps = null;
			}
		}
		ts += delay;
		if(naluLength <= MAXPACKETSIZE - RTP_HEADER_LENGTH - 2) {
			mBuffer = mSocket.requestBuffer();
			mBuffer[RTP_HEADER_LENGTH] = header[4];
			len = fill(mBuffer, RTP_HEADER_LENGTH + 1, naluLength - 1);
			mSocket.updateTimestamp(ts);
			mSocket.markNextPacket();
			super.send(naluLength + RTP_HEADER_LENGTH);
		} else {
			header[1] = (byte) (header[4] & 0x1F);
			header[1] += 0x80;
			header[0] = (byte) ((header[4] & 0x60) & 0xFF);
			header[0] += 28;
			
			while(sum < naluLength) {
				mBuffer = mSocket.requestBuffer();
				mBuffer[RTP_HEADER_LENGTH] = header[0];
				mBuffer[RTP_HEADER_LENGTH + 1] = header[1];
				mSocket.updateTimestamp(ts);
				if((len = fill(mBuffer, RTP_HEADER_LENGTH + 2, naluLength - sum > MAXPACKETSIZE - RTP_HEADER_LENGTH - 2 ? MAXPACKETSIZE - RTP_HEADER_LENGTH - 2 : naluLength - sum)) < 0) return; sum += len;
				if(sum >= naluLength) {
					mBuffer[RTP_HEADER_LENGTH + 1] += 0x40;
					mSocket.markNextPacket();
				}
				super.send(len + RTP_HEADER_LENGTH + 2);
				header[1] = (byte) (header[1] & 0x7F);
			}
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
	
	private void resync() throws IOException {
		byte[] header = new byte[5];
		int type;
		if(DEBUGGING) {
			Log.e(TAG, "Packetizer out of sync");
		}
		while(true) {
			header[0] = header[1];
			header[1] = header[2];
			header[2] = header[3];
			header[3] = header[4];
			header[4] = (byte) mIs.read();
			type = header[4] & 0x1F;
			if(type == 5 || type == 1) {
				naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8 | (header[1] * 0xFF) << 16 | (header[0] & 0xFF) << 24;
				if(naluLength > 0 && naluLength < 100000) {
					oldtime = System.nanoTime();
					if(DEBUGGING) {
						Log.e(TAG, "NAL unit found");
					}
					break;
				}
			}
		}
	}
}