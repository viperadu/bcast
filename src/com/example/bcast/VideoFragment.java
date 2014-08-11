package com.example.bcast;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

@SuppressLint("ValidFragment")
public class VideoFragment extends Fragment {
	private Context context;
	
	public VideoFragment(Context context) {
		this.context = context;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.video_settings, container,
				false);
		
		ToggleButton video_enable = (ToggleButton) rootView
				.findViewById(R.id.video_enable_toggle);
		video_enable.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {

			}
		});

		Spinner video_resolution = (Spinner) rootView
				.findViewById(R.id.video_spinner_resolution);
		video_resolution
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						Camera.Size size = GlobalVariables.videoResolutions
								.get(arg2);
						GlobalVariables.videoQuality.resX = size.width;
						GlobalVariables.videoQuality.resY = size.height;
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		Spinner video_framerate = (Spinner) rootView
				.findViewById(R.id.video_spinner_framerate);
		video_framerate.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				GlobalVariables.videoQuality.framerate = GlobalVariables.videoFramerates
						.get(arg2);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		ArrayList<String> supportedVideoSizes = new ArrayList<String>();
		for (Camera.Size size : GlobalVariables.videoResolutions) {
			String s = size.width + "x" + size.height;
			supportedVideoSizes.add(s);
		}
		ArrayAdapter<String> videoResolutionAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_spinner_item,
				supportedVideoSizes);
		videoResolutionAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		video_resolution.setAdapter(videoResolutionAdapter);

		ArrayList<String> supportedFramerates = new ArrayList<String>();
		for (int i : GlobalVariables.videoFramerates) {
			String s = i + " fps";
			supportedFramerates.add(s);
		}
		ArrayAdapter<String> videoFramerateAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_spinner_item,
				supportedFramerates);
		videoFramerateAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		video_framerate.setAdapter(videoFramerateAdapter);

		return rootView;
	}
}
