package com.example.bcast.session;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

import com.example.bcast.GlobalVariables;
import com.example.bcast.audio.AACStream;
import com.example.bcast.video.H264;
import com.example.bcast.video.MP4Config;

public class SendConfigurations extends AsyncTask<Object, Void, int[]> {
	private static final String TAG = "SendConfigurations";
	private static final boolean DEBUGGING = true;
	
	@Override
	protected int[] doInBackground(Object... params) {
		int[] ports = null;
		Session session = ((Session) params[0]).clone();
		if(session.getAudioTrack() instanceof AACStream/*session.getAudioTrack().getClass().getName().contains("AACStream")*/) {
			((AACStream)session.getAudioTrack()).setAudioRecord(null);
			if(DEBUGGING) {
				Log.d(TAG, session.getAudioTrack().getClass().getName());
				Log.d(TAG, "AudioRecord set to null");
			}
		}
		
		Socket s = null;
		ObjectOutputStream os = null;
		ObjectInputStream is = null;
		try {
			s = new Socket(GlobalVariables.destAddress, GlobalVariables.SESSION_PORT);
			os = new ObjectOutputStream(s.getOutputStream());
			is = new ObjectInputStream(s.getInputStream());
			
			os.writeObject(GlobalVariables.TITLE);
			os.writeObject(GlobalVariables.bufferLength);
			os.writeObject(session);
			os.writeObject(GlobalVariables.SSRC);
			
			if(session.getAudioTrack() != null) {
				os.writeObject(GlobalVariables.audioQuality);
				if(session.getAudioTrack() instanceof AACStream) {
					os.writeObject(GlobalVariables.audioConfig);
				}
			}
			
			if(session.getVideoTrack() != null) {
				os.writeObject(GlobalVariables.videoQuality);
				if(session.getVideoTrack() instanceof H264) {
//					os.writeObject(GlobalVariables.config);
					os.writeObject(GlobalVariables.config.getProfileLevel());
					os.writeObject(GlobalVariables.config.getB64PPS());
					os.writeObject(GlobalVariables.config.getB64SPS());
				}
			}
			
			ports = (int[])is.readObject();
			if(DEBUGGING) {
				Log.d(TAG, "The following ports were received: " + ports[0] + ", " + ports[1] + ", " + ports[2] + ", " + ports[3]);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			Log.e(TAG, "Server is offline / Wrong IP Address");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Log.e(TAG, "Error while parsing the ports array");
		} finally {
			try {
				os.flush();
				is.close();
				os.close();
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		return ports;
	}
	
}