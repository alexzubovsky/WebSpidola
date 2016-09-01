package com.zome.android.webspidola.player;

import android.content.Context;

import com.zome.android.webspidola.MediaPlayerService;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.HttpDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.upstream.UriDataSource;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A {@link UriDataSource} that supports multiple URI schemes. The supported schemes are:
 *
 * <ul>
 * <li>http(s): For fetching data over HTTP and HTTPS (e.g. https://www.something.com/media.mp4).
 * <li>file: For fetching data from a local file (e.g. file:///path/to/media/media.mp4, or just
 *     /path/to/media/media.mp4 because the implementation assumes that a URI without a scheme is a
 *     local file URI).
 * <li>asset: For fetching data from an asset in the application's apk (e.g. asset:///media.mp4).
 * <li>content: For fetching data from a content URI (e.g. content://authority/path/123).
 * </ul>
 */
public class RecordableUriDataSource implements UriDataSource {

	/**
	 * A listener for receiving notifications of timed text.
	 */
	private final DefaultUriDataSource defaultDataSource;

	/**
	 * Constructs a new instance.
	 * <p>
	 * The constructed instance will not follow cross-protocol redirects (i.e. redirects from HTTP to
	 * HTTPS or vice versa) when fetching remote data. Cross-protocol redirects can be enabled by
	 * using {@link #RecordableUriDataSource(Context, TransferListener, String, boolean)} and passing
	 * {@code true} as the final argument.
	 *
	 * @param context A context.
	 * @param userAgent The User-Agent string that should be used when requesting remote data.
	 */
	public RecordableUriDataSource(Context context, String userAgent) {
		defaultDataSource = new DefaultUriDataSource(context, userAgent);
	}

	/**
	 * Constructs a new instance.
	 * <p>
	 * The constructed instance will not follow cross-protocol redirects (i.e. redirects from HTTP to
	 * HTTPS or vice versa) when fetching remote data. Cross-protocol redirects can be enabled by
	 * using {@link #RecordableUriDataSource(Context, TransferListener, String, boolean)} and passing
	 * {@code true} as the final argument.
	 *
	 * @param context A context.
	 * @param listener An optional {@link TransferListener}.
	 * @param userAgent The User-Agent string that should be used when requesting remote data.
	 */
	public RecordableUriDataSource(Context context, TransferListener listener, String userAgent) {
		defaultDataSource = new DefaultUriDataSource(context, listener, userAgent);
	}

	/**
	 * Constructs a new instance, optionally configured to follow cross-protocol redirects.
	 *
	 * @param context A context.
	 * @param listener An optional {@link TransferListener}.
	 * @param userAgent The User-Agent string that should be used when requesting remote data.
	 * @param allowCrossProtocolRedirects Whether cross-protocol redirects (i.e. redirects from HTTP
	 *     to HTTPS and vice versa) are enabled when fetching remote data..
	 */
	public RecordableUriDataSource(Context context, TransferListener listener, String userAgent,
								   boolean allowCrossProtocolRedirects) {
		defaultDataSource = new DefaultUriDataSource(context, listener, userAgent, allowCrossProtocolRedirects);
	}

	/**
	 * Constructs a new instance, using a provided {@link HttpDataSource} for fetching remote data.
	 *
	 * @param context A context.
	 * @param listener An optional {@link TransferListener}.
	 * @param httpDataSource {@link UriDataSource} to use for non-file URIs.
	 */
	public RecordableUriDataSource(Context context, TransferListener listener,
								   UriDataSource httpDataSource) {
		defaultDataSource = new DefaultUriDataSource(context, listener, httpDataSource);
	}

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		return defaultDataSource.open(dataSpec);
	}
	@Override
	public String getUri() {
		return defaultDataSource.getUri();
	}

	@Override
	public void close() throws IOException {
		defaultDataSource.close();
	}

	@Override
	public int read(byte[] buffer, int offset, int readLength) throws IOException {
		int result = defaultDataSource.read(buffer, offset, readLength);
		if(result > 0 && result == readLength)
			writeToBuffer(buffer, offset, readLength);
		return result;
	}
	private void writeToBuffer(byte[] buffer, int offset, int readLength) {
		if (mRecordToOutputStream && mBufferedOutputStream != null) {
			try {
				mBufferedOutputStream.write(buffer, offset, readLength);
				mRecordedCount += readLength;
				//maxCountWaitsForData = MediaPlayerService.MAX_COUNT_WAITS_FOR_DATA;
				if (mRecordedCount > MediaPlayerService.mMaxRecordingSize) {
					closeOutputFile(MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_TO_NEXT_FILE);
					createOutputStream();
				}
			} catch (IOException e) {
				Throwable cause = e.getCause();
				if(cause != null && cause.getClass().toString().contains("libcore.io.ErrnoException"))
					closeOutputFile(MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_STOP_NO_SPACE);
				else
					e.printStackTrace();
			}
		}
	}
	private void closeOutputFile(int cause) {
		if(MediaPlayerService.closeOutputFile(mBufferedOutputStream, mPrefix, cause, mSignature, mAbsolutePath, mFileIsReadyListener))
			mBufferedOutputStream = null;
	}
	private boolean createOutputStream(){
		File outputFile = MediaPlayerService.prepareNewFile(mPath, mPrefix, mExtention);
		mAbsolutePath = outputFile.getAbsolutePath();
		try {
			mBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mBufferedOutputStream = null;
		}
		mRecordedCount = 0;
		return mBufferedOutputStream != null;
	}
	public void prepare(String url, String path, String prefix, String ext, MediaPlayerService.FileIsReadyListener fileIsReadyListener) {
		boolean result = false;
		mSignature = url;
		mFileIsReadyListener = fileIsReadyListener;
		mPath = path;
		mPrefix = prefix;
		mExtention = ext;//+MediaPlayerService.EXT_MODIFIER;//to be deleted by closeOutputFile after setting tags
		mRecordToOutputStream = createOutputStream();
	}
	public void recordingStart(){
		mRecordToOutputStream = true;
		mRecordedCount = 0;
	}
	public void recordingPause(){
		mRecordToOutputStream = false;
	}
	public void recordingStop(){
		mRecordToOutputStream = false;
		closeOutputFile(MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_FINISHED);
	}

	public void setFileIsReadyListener(MediaPlayerService.FileIsReadyListener listener) {
		mFileIsReadyListener = listener;
	}
	//private FileOutputStream mFileOutputStream = null;
	private BufferedOutputStream mBufferedOutputStream = null;
	private boolean mRecordToOutputStream = false;
	private int mRecordedCount;
	private MediaPlayerService.FileIsReadyListener mFileIsReadyListener = null;
	private String mAbsolutePath;
	private String mPath;
	private String mPrefix;
	private String mExtention;
	private String mSignature;

	public boolean isRecording() {
		return mRecordToOutputStream;
	}
}
