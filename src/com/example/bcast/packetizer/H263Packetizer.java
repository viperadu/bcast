package com.example.bcast.packetizer;

import java.io.IOException;

import android.util.Log;

public class H263Packetizer extends AbstractPacketizer implements Runnable {
	public static final String TAG = "H263Packetizer";
	public static final boolean DEBUGGING = true;
	private final static int MAXPACKETSIZE = 1400;
	private Statistics stats = new Statistics();

	private Thread t;

	public H263Packetizer() throws IOException {
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
		try {
			mIs.close();
		} catch (IOException e) {
		}
		t.interrupt();
		t = null;
	}

	@Override
	public void run() {
		long time, duration = 0;
		int i = 0, j = 0, tr;
		boolean firstFragment = true;
		byte[] nextBuffer;
		stats.reset();
		try {
			skipHeader();
		} catch (IOException e) {
			if (DEBUGGING) {
				Log.e(TAG, "Couldn't skip MP4 header");
				return;
			}
		}

		try {
			while (!Thread.interrupted()) {
				if (j == 0) {
					mBuffer = mSocket.requestBuffer();
				}
				mSocket.updateTimestamp(ts);

				mBuffer[RTP_HEADER_LENGTH] = 0;
				mBuffer[RTP_HEADER_LENGTH + 1] = 0;

				time = System.nanoTime();
				if (fill(RTP_HEADER_LENGTH + j + 2, MAXPACKETSIZE
						- RTP_HEADER_LENGTH - j - 2) < 0) {
					return;
				}
				duration += System.nanoTime() - time;
				j = 0;
				for (i = RTP_HEADER_LENGTH + 2; i < MAXPACKETSIZE - 1; i++) {
					if (mBuffer[i] == 0 && mBuffer[i + 1] == 0
							&& (mBuffer[i + 2] & 0xFC) == 0x80) {
						j = i;
						break;
					}
				}
				tr = (mBuffer[i + 2] & 0x03) << 6
						| (mBuffer[i + 3] & 0xFF) >> 2;
				if (firstFragment) {
					mBuffer[RTP_HEADER_LENGTH] = 4;
					firstFragment = false;
				} else {
					mBuffer[RTP_HEADER_LENGTH] = 0;
				}
				if (j > 0) {
					mDelta += duration / 1000000;
					if (mInterval > 0) {
						if (mDelta >= mInterval && duration / 1000000 > 10) {
							mDelta = 0;
							mReport.send(System.nanoTime(), ts * 90 / 1000000);
						}
					}
					stats.push(duration);
					ts += stats.average();
					duration = 0;
					mSocket.markNextPacket();
					send(j);
					nextBuffer = mSocket.requestBuffer();
					System.arraycopy(mBuffer, j + 2, nextBuffer,
							RTP_HEADER_LENGTH + 2, MAXPACKETSIZE - j - 2);
					mBuffer = nextBuffer;
					j = MAXPACKETSIZE - j - 2;
					firstFragment = true;
				} else {
					send(MAXPACKETSIZE);
				}
			}
		} catch (IOException e) {
		} catch (InterruptedException e) {
		}

		if (DEBUGGING) {
			Log.d(TAG, "H263 Packetizer stopped");
		}
	}
	
	private int fill(int offset, int length) throws IOException {
		int sum = 0, len;
		while(sum < length) {
			len = mIs.read(mBuffer, offset + sum, length - sum);
			if(len < 0) {
				throw new IOException("End of stream");
			} else {
				sum += len;
			}
		}
		return sum;
	}
	
	private void skipHeader() throws IOException {
		byte[] buffer = new byte[3];
		while(true) {
			while(mIs.read() != 'm');
			mIs.read(buffer, 0, 3);
			if(buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') {
				break;
			}
		}
	}
}
