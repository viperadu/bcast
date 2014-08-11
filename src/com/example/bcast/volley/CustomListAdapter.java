package com.example.bcast.volley;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.example.bcast.GlobalVariables;
import com.example.bcast.R;

public class CustomListAdapter extends BaseAdapter {

	private Activity mActivity;
	private LayoutInflater mLayoutInflater;
	private List<StreamDetails> mStreamList;
	
	public CustomListAdapter(Activity activity, List<StreamDetails> streamList) {
		this.mActivity = activity;
		this.mStreamList = streamList;
	}
	
	@Override
	public int getCount() {
		return mStreamList.size();
	}

	@Override
	public Object getItem(int pos) {
		return mStreamList.get(pos);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		if(mLayoutInflater == null) {
			mLayoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		if(convertView == null) {
			convertView = mLayoutInflater.inflate(R.layout.list_row, null);
		}
		NetworkImageView thumbnail = (NetworkImageView)convertView.findViewById(R.id.thumbnail);
		TextView title = (TextView) convertView.findViewById(R.id.title);
		TextView author = (TextView) convertView.findViewById(R.id.author);
		
		StreamDetails streamDetails = mStreamList.get(pos);
		
		thumbnail.setImageUrl(streamDetails.getThumbnailUrl(), GlobalVariables.imageLoader);
		title.setText(streamDetails.getTitle());
		author.setText(streamDetails.getAuthor());
		
		
		return convertView;
	}
}
