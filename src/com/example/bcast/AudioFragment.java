package com.example.bcast;

import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.ToggleButton;
import com.example.bcast.audio.AACStream;

@SuppressLint("ValidFragment")
public class AudioFragment extends Fragment {

	private Context context;
	
	public AudioFragment(Context context) {
		this.context = context;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.audio_settings, container, false);
	
		Spinner audio_sampling_rate = (Spinner) rootView.findViewById(R.id.audio_spinner_sampling_rate);
		audio_sampling_rate
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						GlobalVariables.audioQuality.samplingRate = AACStream.AUDIO_SAMPLING_RATES[arg2];
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		ToggleButton audio_enable = (ToggleButton) rootView.findViewById(R.id.audio_enable_toggle);
		audio_enable.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {

			}
		});

		ArrayList<String> samplingRates = new ArrayList<String>();
		for (int i = 0; i < 7; i++) {
			samplingRates.add(AACStream.AUDIO_SAMPLING_RATES[i] + "");
		}
		ArrayAdapter<String> audioSamplingRatesAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_spinner_item,
				samplingRates);
		audioSamplingRatesAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		audio_sampling_rate.setAdapter(audioSamplingRatesAdapter);

		
		return rootView;
	}
}
