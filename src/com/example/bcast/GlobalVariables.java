package com.example.bcast;

import java.util.List;

import android.annotation.SuppressLint;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.example.bcast.audio.AudioQuality;
import com.example.bcast.video.MP4Config;
import com.example.bcast.video.VideoQuality;

@SuppressLint("InlinedApi") public class GlobalVariables {
//	public static final String destAddress = "192.168.1.3"; // Home WiFi
//	public static final String destAddress = "192.168.1.5"; // Home ethernet
//	public static final String destAddress = "10.0.0.85";  //LUNO ethernet static
//	public static final String destAddress = "192.168.137.1"; //LUNO broken WiFi
//	public static final String destAddress = "10.0.0.52"; //LUNO ethernet DHCP
	
	
	public static final String destAddress = "192.168.0.3";	// Birmingham WiFi
//	public static final String destAddress = "192.168.0.8";	// Birmingham Ethernet
	
	public static final int VIDEO_PORT = 25123;
	public static final int SESSION_PORT = 25120;
	public static final int AUDIO_PORT = 25121;
	public static final int JSON_PORT = 25118;
	public static final boolean DEBUGGING = true;
	public static final String TAG = "bcast";

	public volatile static int audioConfig = 0;
	public volatile static MP4Config config = null;
	
	public static String TITLE;
	public static int[] ports = new int[]{0, 0, 0, 0};
	
	public static int SSRC;
	public static VideoQuality videoQuality;
	public static AudioQuality audioQuality;
	
	public static int VideoEncoder = MediaRecorder.VideoEncoder.H264;
	public static int AudioEncoder = MediaRecorder.AudioEncoder.AAC;
	
	public static List<Size> videoResolutions;
	public static List<Integer> videoFramerates;
	
	public static ImageLoader imageLoader;
	public static RequestQueue requestQueue;
	
	public static boolean MediaCodec = true;

	private static boolean error = false;

	public static int bufferLength;
	
/*	public static boolean sendObjects(final Session se, final MP4Config config) throws IOException {
		error = false;
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					Socket s = new Socket(InetAddress.getByName(destAddress), SESSION_PORT);
					ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
					BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
					
//					os.writeObject(se);
//					os.writeObject(config);
//					os.writeObject("A");
//					os.writeObject(1);
					
//					Session ses = new Session();
//					ses.mDestination = mDestination;
//					ses.mOrigin = mOrigin;
//					ses.mTimestamp = mTimestamp;
//					ses.mTimeToLive = mTimeToLive;
//					ses.mVideoStream = null;
//					ses.setContext(null);
					
//					String line = "";
//					line = br.readLine();
//					os.flush();
//					MP4Config config = new MP4Config("42800c", "Z0KADOkCg/I=", "pps=aM4G4g==");
//					
//					br.readLine();
					os.flush();
					os.close();
					br.close();
					s.close();
				} catch(IOException e) {
					error = true;
					if(e.getMessage() != null) {
						Log.e(TAG, "Error while sending the objects");
						Log.e(TAG, e.getMessage());
					}
				}
			}
		};
		t.start();
		return !error;
	}
//	public static void getMP4Config() {
//		config = testH264();
//		//send MP4 config
//		Thread t = new Thread() {
//			@Override
//			public void run() {
//				if(!Thread.interrupted()) {
//					try {
//						Socket s = new Socket(mDestination, GlobalVariables.DEFAULT_TCP_PORT);
//						ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
//						BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
//						os.writeObject(config);
//						String line = "";
//						line = br.readLine();
//					} catch (IOException e) {
//						if(DEBUGGING) {
//							Log.e(TAG, "Error when sending the MP4Config");
//						}
//					}
//				}
//			}
//		};
//		t.start();
//	}*/
}
