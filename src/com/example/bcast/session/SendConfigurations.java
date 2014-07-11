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
import com.example.bcast.video.MP4Config;

public class SendConfigurations extends AsyncTask<Object, Void, int[]> {
	private static final String TAG = "SendConfigurations";
	private static final boolean DEBUGGING = true;
	
	@Override
	protected int[] doInBackground(Object... params) {
//		Session session = ((Session) params[0]).clone();
//		Session session = new Session();
//		session.removeVideoTrack();
//		session.removeAudioTrack();
		int[] ports = null;
		Session session = ((Session) params[0]).clone();
		if(session.getAudioTrack().getClass().getName().contains("AACStream")) {
			((AACStream)session.getAudioTrack()).setAudioRecord(null);
			if(DEBUGGING) {
				Log.d(TAG, session.getAudioTrack().getClass().getName());
				Log.d(TAG, "AudioRecord set to null");
			}
		}
		MP4Config config = (MP4Config) params[1];
//		VideoQuality videoQuality = (VideoQuality) params[2];
//		AudioQuality audioQuality = (AudioQuality) params[3];
		
		Socket s = null;
		ObjectOutputStream os = null;
		ObjectInputStream is = null;
		try {
			s = new Socket(GlobalVariables.destAddress, GlobalVariables.SESSION_PORT);
			os = new ObjectOutputStream(s.getOutputStream());
			is = new ObjectInputStream(s.getInputStream());
			
			os.writeObject(GlobalVariables.TITLE);
			os.writeObject(session);			
			os.writeObject(config);
			os.writeObject(GlobalVariables.SSRC);
			
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