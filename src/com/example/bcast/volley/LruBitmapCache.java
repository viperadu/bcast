package com.example.bcast.volley;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class LruBitmapCache extends LruCache<String, Bitmap> implements ImageCache {

	public static int getDefaultLruCacheSize() {
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;
		return cacheSize;
	}
	
	public LruBitmapCache() {
		this(getDefaultLruCacheSize());
	}
	
	public LruBitmapCache(int sizeInKiloBytes) {
		super(sizeInKiloBytes);
	}
	
	@Override
	protected int sizeOf(String key, Bitmap value) {
		return value.getRowBytes() * value.getHeight() / 1024;
	}

	@Override
	public Bitmap getBitmap(String arg0) {
		return get(arg0);
	}

	@Override
	public void putBitmap(String arg0, Bitmap arg1) {
		put(arg0, arg1);
	}

}
