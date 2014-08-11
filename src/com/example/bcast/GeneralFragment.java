package com.example.bcast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@SuppressLint("ValidFragment")
public class GeneralFragment extends Fragment {

	private Context context;
	
	public GeneralFragment(Context context) {
		this.context = context;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.general_settings, container, false);
		
		return rootView;
	}

}
