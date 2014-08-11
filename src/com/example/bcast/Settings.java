package com.example.bcast;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class Settings extends FragmentActivity implements ActionBar.TabListener {
	private static final String TAG = "Settings";
	private ViewPager viewPager;
	private TabsPagerAdapter mAdapter;
	private ActionBar actionBar;
	private String[] tabs = { "Audio", "Video", "General" };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		getSupportedVideoOptions();
		printVideoOptions();
		
		viewPager = (ViewPager) findViewById(R.id.pager);
		actionBar = getActionBar();
		mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(mAdapter);
//		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);		

		// Adding Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name)
					.setTabListener(this));
		}

		/**
		 * on swiping the viewpager make respective tab selected
		 * */
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// on changing the page
				// make respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

	}
	
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		
	}
	
	public class TabsPagerAdapter extends FragmentPagerAdapter {
		public TabsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int index) {

			switch (index) {
			case 0:
				return new AudioFragment(getApplicationContext());
			case 1:
				return new VideoFragment(getApplicationContext());
			case 2:
				return new GeneralFragment(getApplicationContext());
			}

			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}

	}

	private void getSupportedVideoOptions() {
		Camera camera = Camera.open();
		Camera.Parameters cameraParameters = camera.getParameters();
		GlobalVariables.videoResolutions = cameraParameters
				.getSupportedVideoSizes();
		GlobalVariables.videoFramerates = new ArrayList<Integer>();
		List<int[]> fpsRanges = cameraParameters.getSupportedPreviewFpsRange();
		for (int[] fpsRange : fpsRanges) {
			if (fpsRange[0] == fpsRange[1]) {
				GlobalVariables.videoFramerates.add(fpsRange[0] / 1000);
			}
		}
		camera.release();
	}

	private void printVideoOptions() {
		for (Camera.Size size : GlobalVariables.videoResolutions) {
			Log.i(TAG, size.width + "x" + size.height);
		}
		for (int fpsRange : GlobalVariables.videoFramerates) {
			Log.i(TAG, fpsRange + " fps");
		}
	}
	
}



/*
 * 
	private Spinner video_resolution, video_framerate, audio_sampling_rate;
	private ToggleButton audio_enable, video_enable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_settings);

		TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
		// TabHost tabHost = getTabHost();
		tabHost.setup();

		TabSpec tab1 = tabHost.newTabSpec("audio_settings");
		tab1.setIndicator("Audio");
		tab1.setContent(R.layout.audio_settings);
		tabHost.addTab(tab1);

		TabSpec tab2 = tabHost.newTabSpec("video_settings");
		tab2.setIndicator("Video");
		tab2.setContent(R.layout.video_settings);
		tabHost.addTab(tab2);

		TabSpec tab3 = tabHost.newTabSpec("general_settings");
		tab3.setIndicator("General");
		tab3.setContent(R.layout.general_settings);
		tabHost.addTab(tab3);

		init();

		getSupportedVideoOptions();
		printVideoOptions();

		loadVideoOptions();
	}

	private void init() {
				
	}


	/*
	 * @Override public void onBuildHeaders(List<Header> target) {
	 * loadHeadersFromResource(R.xml.headers, target); }
	 * 
	 * public static class SettingsFragment extends PreferenceFragment {
	 * 
	 * @Override public void onCreate(Bundle savedInstanceState) {
	 * super.onCreate(savedInstanceState); Log.i("Settings", "Arguments: " +
	 * getArguments()); // addPreferencesFromResource(R.xml.settings2); } }
	 * 
	 * public static class AudioSettingsFragment extends PreferenceFragment {
	 * 
	 * @Override public void onCreate(Bundle savedInstanceState) {
	 * super.onCreate(savedInstanceState);
	 * addPreferencesFromResource(R.xml.audio_settings); } }
	 * 
	 * public static class VideoSettingsFragment extends PreferenceFragment {
	 * 
	 * @Override public void onCreate(Bundle savedInstanceState) {
	 * super.onCreate(savedInstanceState);
	 * addPreferencesFromResource(R.xml.video_settings); } }
	 */
