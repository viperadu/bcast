package com.example.bcast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bcast.audio.AACStream;
import com.example.bcast.audio.AudioQuality;
import com.example.bcast.session.SendConfigurations;
import com.example.bcast.session.Session;
import com.example.bcast.session.SessionBuilder;
import com.example.bcast.video.H264;
import com.example.bcast.video.VideoQuality;

public class Record extends Activity {
	public final static String TAG = "Record";
	public final static boolean DEBUGGING = true;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private Session mSession;
	
	private boolean duplicateTitle = false;
	private boolean errorNumberFormat = false;
	
	private final Context context = this;
	
	private Button button;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_record);

		mSurfaceView = (SurfaceView) findViewById(R.id.camera_view_2);
		mSurfaceHolder = mSurfaceView.getHolder();
		// Backwards compatibility with Android 2.*
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		SessionBuilder builder = SessionBuilder.getInstance().clone();
		builder.setSurfaceHolder(mSurfaceHolder);
		builder.setCamera(CameraInfo.CAMERA_FACING_BACK);
		try {
			builder.setDestination(InetAddress
					.getByName(GlobalVariables.destAddress));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		AudioQuality aQuality = null;
		if (GlobalVariables.audioQuality == null) {
			aQuality = AudioQuality.parseQuality("8-32000");
		} else {
			aQuality = GlobalVariables.audioQuality;
		}
		if(DEBUGGING) {
			Log.d(TAG, "Audio Quailty set to " + aQuality.bitRate + " bitrate and " + aQuality.samplingRate + " sampling rate.");
		}
		builder.setAudioQuality(aQuality).setAudioEncoder(
				GlobalVariables.AudioEncoder);

		VideoQuality vQuality = null;
		if (GlobalVariables.videoQuality == null) {
			vQuality = VideoQuality.parseQuality("2000-15-640-480");
		} else {
			vQuality = GlobalVariables.videoQuality;
		}
		if(DEBUGGING) {
			Log.d(TAG, "Video Quality set to " + vQuality.resX + "x" + vQuality.resY + " at " + vQuality.framerate + " fps and " + vQuality.bitrate + " bitrate.");
		}
		builder.setVideoQuality(vQuality).setVideoEncoder(
				GlobalVariables.VideoEncoder);

		try {
			mSession = builder.build();
		} catch (IOException e) {
			if (GlobalVariables.DEBUGGING) {
				Log.e(TAG, "Error while setting up the session");
				e.printStackTrace();
				if (e.getMessage() != null) {
					Log.e(TAG, e.getMessage());
				}
			}
		}
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		final TextView bcastNameLabel = new TextView(Record.this);
		bcastNameLabel.setText("Stream name:");
		
		final EditText bcastName = new EditText(Record.this);
		bcastName.setHint("Name of the broadcast");
		bcastName.setText("first");
		
		final TextView buffLengthLabel = new TextView(Record.this);
		buffLengthLabel.setText("Buffer size:");
		
		final EditText buffLength = new EditText(Record.this);
		buffLength.setHint("Buffering length (in seconds)");
		buffLength.setText("10");
		buffLength.setInputType(InputType.TYPE_CLASS_NUMBER);
		
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(bcastNameLabel);
		layout.addView(bcastName);
		layout.addView(buffLengthLabel);
		layout.addView(buffLength);
		
//		do {
			String message = "";
			if(duplicateTitle && !errorNumberFormat) {
				message = "Stream title already in use!\nChoose a different name for your stream.";
			} else if(!duplicateTitle && errorNumberFormat) {
				message = "Buffering length set is not a valid number!\nChoose a valid number.";
			} else {
				message = "Set the title for your stream.\nChoose the buffering length.";
			}

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
			alertDialogBuilder.setTitle("")
			.setMessage(message)
			.setView(layout)
			.setCancelable(false)
			.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					Editable valueTitle = bcastName.getText(); 
		            if(DEBUGGING) {
		            	Log.i(TAG, "Session title set to \"" + valueTitle.toString() + "\"");
		            }
		            GlobalVariables.TITLE = valueTitle.toString();
		            
		            Editable valueBufferLength = buffLength.getText();
		            if(DEBUGGING) {
		            	Log.i(TAG, "Buffer length set to \"" + valueBufferLength.toString() + "\"");
		            }
		            try {
		            	GlobalVariables.bufferLength = Integer.parseInt(valueBufferLength.toString());
		            	errorNumberFormat = false;
		            } catch (NumberFormatException e) {
		            	errorNumberFormat = true;
		            }
		            duplicateTitle = sessionSetUp();
				}
			  });
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
			button.setEnabled(false);
//		} while(!duplicateTitle && !errorNumberFormat);
	}

	private boolean sessionSetUp() {
		try {
			GlobalVariables.ports = new SendConfigurations().execute(mSession).get();
			if(GlobalVariables.ports == null) {
				if(DEBUGGING) {
					Log.e(TAG, "Error while retrieving the ports from the server");
				}
				// TODO: send to the server a notification that the ports
				// were not read by the application and request to re-send them
				return false;
			} else if(GlobalVariables.ports[0] == -1 && GlobalVariables.ports[1] == -1) {
				return false;
			}
			
			if(mSession.getAudioTrack() != null) {
				mSession.getAudioTrack().setDestinationPorts(GlobalVariables.ports[0], GlobalVariables.ports[1]);
			}
			
			if(mSession.getVideoTrack() != null) {
				mSession.getVideoTrack().setDestinationPorts(GlobalVariables.ports[2], GlobalVariables.ports[3]);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "Connection to server reset", Toast.LENGTH_SHORT).show();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "Connection to server reset", Toast.LENGTH_SHORT).show();
		}
		return true;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Thread videoTest = null, audioTest = null;
		if(GlobalVariables.config == null) {
			videoTest = new Thread() {
				@Override
				public void run() {
					extractMP4Parameters();
				}
			};
			videoTest.start();
		}
		if(GlobalVariables.audioConfig == 0) {
			audioTest = new Thread() {
				@Override
				public void run() {
					extractAACParameter();
				}
			};
			audioTest.start();
		}
		do {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while(!audioTest.isAlive() && !videoTest.isAlive());
		runOnUiThread(new Runnable() {
			public void run() {
				button.setEnabled(true);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.start), 1);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.stop), 1);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.start:
			try {
				if (mSession != null) {
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					mSession.start();
					
				}
			} catch (IllegalStateException e) {
				if (GlobalVariables.DEBUGGING) {
					e.printStackTrace();
					if (e.getMessage() != null) {
						Log.e(TAG, "Error while starting the session");
						Log.e(TAG, e.getMessage());
					}
				}
			} catch (IOException e) {
				if (GlobalVariables.DEBUGGING) {
					e.printStackTrace();
					if (e.getMessage() != null) {
						Log.e(TAG, "Error while starting the session");
						Log.e(TAG, e.getMessage());
					}
				}
			}

			return true;
		case R.id.stop:
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			if (mSession.isStreaming()) {
				mSession.stop();
			}
			return true;
		case R.id.quit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void extractAACParameter() {
		if(GlobalVariables.AudioEncoder == MediaRecorder.AudioEncoder.AAC) {
			try {
				GlobalVariables.audioConfig = ((AACStream)mSession.getAudioTrack()).getConfig();
				Log.i(TAG, "Audio config=" + GlobalVariables.audioConfig);
			} catch (IOException e) {
				if(GlobalVariables.DEBUGGING) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void extractMP4Parameters() {
		if(GlobalVariables.VideoEncoder == MediaRecorder.VideoEncoder.H264) {
			try {
				GlobalVariables.config = mSession.getConfig();
				Log.i(TAG, "Profile level=" + GlobalVariables.config.getProfileLevel());
				Log.i(TAG, "SPS=" + GlobalVariables.config.getB64SPS());
				Log.i(TAG, "PPS=" + GlobalVariables.config.getB64PPS());
			} catch (IOException e) {
				if(GlobalVariables.DEBUGGING) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (mSession.isStreaming()) {
			mSession.stop();
		}
	}
}