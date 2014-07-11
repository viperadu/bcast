package com.example.bcast.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;

@SuppressLint("NewApi")
public class MediaCodecInputStream extends InputStream {
	public final String TAG = "MediaCodecInputStream";
	public final boolean DEBUGGING = true;
	
	private MediaCodec mMediaCodec = null;
	private BufferInfo mBufferInfo = new BufferInfo();
	private ByteBuffer[] mBuffers = null;
	private ByteBuffer mBuffer = null;
	private int mIndex = -1;
	private boolean mClosed = false;
	
	public MediaFormat mMediaFormat;
	
	public MediaCodecInputStream(MediaCodec mediaCodec) {
		mMediaCodec = mediaCodec;
		mBuffers = mMediaCodec.getOutputBuffers();
	}
	
	@Override
	public void close() {
		mClosed = true;
	}
	
	@Override
	public int read() throws IOException {
		return 0;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int min = 0;
		if(mClosed) {
			throw new IOException("The InputStream was closed");
		}
		try {
			if(mBuffer == null || mBufferInfo.size - mBuffer.position() <= 0) {
				while(!Thread.interrupted()) {
					mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 100000);
					if(mIndex >= 0) {
						mBuffer = mBuffers[mIndex];
						mBuffer.position(0);
						break;
					} else if(mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						mBuffers = mMediaCodec.getOutputBuffers();
					} else if(mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						mMediaFormat = mMediaCodec.getOutputFormat();
					} else if(mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
						return 0;
					} else {
						if(DEBUGGING) {
							Log.e(TAG, "Message: " + mIndex);
						}
						return 0;
					}
				}
			}			
			if(length < mBufferInfo.size - mBuffer.position()) {
				min = length;
			} else {
				min = mBufferInfo.size - mBuffer.position();
			}
			mBuffer.get(buffer, offset, min);
			if(mBufferInfo.size >= mBuffer.position()) {
				mMediaCodec.releaseOutputBuffer(mIndex, false);
			}
		} catch (RuntimeException e) {
			if(DEBUGGING) {
				e.printStackTrace();
			}
		}
		return min;
	}

	public int available() {
		if(mBuffer != null) {
			return mBufferInfo.size - mBuffer.position();
		} else {
			return 0;
		}
	}
	
	public BufferInfo getLastBufferInfo() {
		return mBufferInfo;
	}
}
