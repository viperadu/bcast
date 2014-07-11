package com.example.bcast.video;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;

import android.util.Base64;
import android.util.Log;

public class MP4Config implements Serializable {
	private static final long serialVersionUID = 1L;
	private MP4Parser mp4Parser = null;
	private String mProfileLevel, mPPS, mSPS;

	public MP4Config(String profile, String sps, String pps) {
		mp4Parser = null;
		mProfileLevel = profile;
		mPPS = pps;
		mSPS = sps;
	}

	public MP4Config(String path) throws IOException, FileNotFoundException {
		StsdBox stsdBox;
		mp4Parser = new MP4Parser(path);
		try {
			mp4Parser.parse();
		} catch (IOException e) {
		}

		stsdBox = mp4Parser.getStsdBox();
		mPPS = stsdBox.getB64PPS();
		mSPS = stsdBox.getB64SPS();
		mProfileLevel = stsdBox.getProfileLevel();

		mp4Parser.close();
	}

	public String getProfileLevel() {
		return mProfileLevel;
	}

	public String getB64PPS() {
		return mPPS;
	}

	public String getB64SPS() {
		return mSPS;
	}
}

class MP4Parser implements Serializable {
	public static final String TAG = "MP4Parser";
	public static final boolean DEBUGGING = true;

	private HashMap<String, Long> boxes = new HashMap<String, Long>();
	private long pos = 0;
	private final RandomAccessFile file;

	public MP4Parser(final String path) throws IOException,
			FileNotFoundException {
		this.file = new RandomAccessFile(new File(path), "r");
	}

	public void parse() throws IOException {
		long length = 0;
		try {
			length = file.length();
		} catch (IOException e) {
			throw new IOException("Wrong size");
		}
		try {
			parse("", length);
		} catch (IOException e) {
			throw new IOException("Parse error: malformed MP4 file");
		}
	}

	public void close() {
		try {
			file.close();
		} catch (IOException e) {
		}
	}

	public long getBoxPos(String box) throws IOException {
		Long r = boxes.get(box);
		if (r == null) {
			throw new IOException("Box not found: " + box);
		}
		return boxes.get(box);
	}

	public StsdBox getStsdBox() throws IOException {
		try {
			// TODO: delete this try catch block
			try {
				File f = new File("/moov/trak/mdia/minf/stbl/stsd/avc1/avcC");
				if(f.exists()) {
					Log.i(TAG, "File /moov/trak/mdia/minf/stbl/stsd/avc1/avcC exists!");
				} else {
					Log.i(TAG, "File /moov/trak/mdia/minf/stbl/stsd/avc1/avcC could not be found :(");
				}
			} catch (Exception e) {
				Log.e(TAG, "Error while trying to open the file /moov/trak/mdia/minf/stbl/stsd/avc1/avcC");
			}
			return new StsdBox(file, getBoxPos("/moov/trak/mdia/minf/stbl/stsd"));
		} catch (IOException e) {
			throw new IOException("stsd box could not be found");
		}
	}

	private void parse(String path, long len) throws IOException {
		byte[] buffer = new byte[8];
		String name = "";
		long sum = 0, newlen = 0;
		if (!path.equals("")) {
			boxes.put(path, pos - 8);
		}
		while (sum < len) {
			file.read(buffer, 0, 8);
			sum += 8;
			pos += 8;
			if (validBoxName(buffer)) {
				ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
				newlen = byteBuffer.getInt() - 8;
				if (newlen < 0 || newlen == 1061109559) {
					throw new IOException();
				}
				name = new String(buffer, 4, 4);
				if (DEBUGGING) {
					Log.d(TAG, "Atom -> name: " + name + " newlen: " + newlen
							+ " pos: " + pos);
				}
				sum += newlen;
				parse(path + '/' + name, newlen);
			} else {
				if (len < 8) {
					file.seek(file.getFilePointer() - 8 + len);
					sum += len - 8;
				} else {
					int skipped = file.skipBytes((int) (len - 8));
					if (skipped < ((int) (len - 8))) {
						throw new IOException();
					}
					pos += len - 8;
					sum += len - 8;
				}
			}
		}
	}

	private boolean validBoxName(byte[] buffer) {
		for (int i = 0; i < 4; i++) {
			if ((buffer[i + 4] < 'a' || buffer[i + 4] > 'z')
					&& (buffer[i + 4] < '0' || buffer[i + 4] > '9')) {
				return false;
			}
		}
		return true;
	}
}

class StsdBox {
	private RandomAccessFile fis;
	private byte[] buffer = new byte[4];
	private long pos = 0;

	private byte[] pps;
	private byte[] sps;
	private int spsLength, ppsLength;

	public StsdBox(RandomAccessFile fis, long pos) {
		this.fis = fis;
		this.pos = pos;

		findBoxAvcc();
		findSPSandPPS();
	}

	public String getProfileLevel() {
		return toHexString(sps, 1, 3);
	}

	public String getB64PPS() {
		return Base64.encodeToString(pps, 0, ppsLength, Base64.NO_WRAP);
	}

	public String getB64SPS() {
		return Base64.encodeToString(sps, 0, spsLength, Base64.NO_WRAP);
	}

	private boolean findSPSandPPS() {
		try {
			fis.skipBytes(7);
			spsLength = 0xFF & fis.readByte();
			sps = new byte[spsLength];
			fis.read(sps, 0, spsLength);
			fis.skipBytes(2);
			ppsLength = 0xFF & fis.readByte();
			pps = new byte[ppsLength];
			fis.read(pps, 0, ppsLength);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private boolean findBoxAvcc() {
		try {
			fis.seek(pos + 8);
			while (true) {
				while (fis.read() != 'a')
					;
				fis.read(buffer, 0, 3);
				if (buffer[0] == 'v' && buffer[1] == 'c' && buffer[2] == 'C') {
					break;
				}
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	static private String toHexString(byte[] buffer, int start, int len) {
		String c;
		StringBuilder s = new StringBuilder();
		for (int i = start; i < start + len; i++) {
			c = Integer.toHexString(buffer[i] & 0xFF);
			s.append(c.length() < 2 ? "0" + c : c);
		}
		return s.toString();
	}
}
