package com.example.bcast;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug.FlagToString;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.example.bcast.volley.CustomListAdapter;
import com.example.bcast.volley.StreamDetails;

public class Watch extends Activity {

	private static final String TAG = "Watch";
	private List<StreamDetails> streamList;
	private CustomListAdapter adapter;
	private ListView listView;
	
//	private SurfaceView mSurfaceView;
//	private SurfaceHolder mSurfaceHolder;
//	private PowerManager.WakeLock mWakeLock;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_watch);
		
//		mSurfaceView = (SurfaceView) findViewById(R.id.camera_view);
		
//		mSurfaceHolder = mSurfaceView.getHolder();
		// Backwards compatibility with Android 2.*
//		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		streamList = new ArrayList<StreamDetails>();
		adapter = new CustomListAdapter(this, streamList);
		
		listView = (ListView) findViewById(R.id.available_videos);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(streamList.get(pos).getURL()));
				intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				intent.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			    startActivity(intent);
			}
		});
		populateList();
		
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}
	
	private void populateList() {
		JsonArrayRequest listRequest = new JsonArrayRequest("http://" + GlobalVariables.destAddress + ":" + GlobalVariables.JSON_PORT, 
		new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray response) {
				Log.i(TAG, "Number of videos: " + response.length());
				for(int i=0; i<response.length(); i++) {
					try {
						JSONObject obj = response.getJSONObject(i);
						StreamDetails details = new StreamDetails();
						details.setTitle(obj.getString("title"));
						details.setAuthor(obj.getString("author"));
						details.setThumbnailUrl(obj.getString("image"));
						details.setURL(obj.getString("url"));
						streamList.add(details);
					} catch(JSONException e) { }
				}
				adapter.notifyDataSetChanged();
			}
		},
		new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				Log.e(TAG, "Error on JSON request!");
			}
		});
		listRequest.setTag("Watch");
		GlobalVariables.requestQueue.add(listRequest);
	}
	
	/*@Override    
	public void onBackPressed() {
		Intent setIntent = new Intent(Intent.ACTION_MAIN);
		setIntent.addCategory(Intent.CATEGORY_HOME);
		setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(setIntent);
	}*/
}
