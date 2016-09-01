package com.zome.android.webspidola;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URL;

/**
 * Created by Sasha on 8/2/2016.
 */
public 	class AudioRecordRunnable implements Runnable {

	private MediaPlayerService.FileIsReadyListener mFileIsReadyListener = null;

	public AudioRecordRunnable(String url, MediaPlayerService.FileIsReadyListener fileIsReadyListener) {
		mOriginalUrl = url;
		mFileIsReadyListener = fileIsReadyListener;
	}


	private String mPath;
	private String mExtention;
	private String mPrefix;
	private String mOriginalUrl = null;
	private String mAbsolutePath;
	private byte[] buffer;
	private BufferedOutputStream mBufferedOutputFile;
	private InputStream mInputStream;
	private boolean runAudioThread;
	private boolean mAudioRecordingPaused;
	private Thread mAudioThread;

	@Override
	public void run() {
		// Set the thread priority
		//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		// Audio
		try {
			mInputStream = new BufferedInputStream(new URL(mOriginalUrl).openStream());
		} catch (IOException e) {
			e.printStackTrace();
			mInputStream = null;
		}
		if (mInputStream != null) {
			long count = 0;
			int len;
			int maxCountWaitsForData = MediaPlayerService.MAX_COUNT_WAITS_FOR_DATA;
			buffer = new byte[1024];
			//_audio encoding loop
			while (runAudioThread) {
				if (!mAudioRecordingPaused) {
					try {
						try {
							len = mInputStream.read(buffer);
						} catch (SocketException socketException) {
							len = 0;
						}
						if (len > 0) {
							try {
								mBufferedOutputFile.write(buffer, 0, len);
								count += len;
								maxCountWaitsForData = MediaPlayerService.MAX_COUNT_WAITS_FOR_DATA;
								if (count > MediaPlayerService.mMaxRecordingSize) {
									closeOutputFile(MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_TO_NEXT_FILE);
									File outputFile = MediaPlayerService.prepareNewFile(mPath, mPrefix, mExtention);
									mAbsolutePath = outputFile.getAbsolutePath();
									mBufferedOutputFile = new BufferedOutputStream(new FileOutputStream(outputFile));
									count = 0;
								}
							} catch (IOException noSpace) {
								Throwable cause = noSpace.getCause();
								if (cause != null && cause.getClass().toString().contains("libcore.io.ErrnoException"))
									closeOutputFile(MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_STOP_NO_SPACE);
								else
									noSpace.printStackTrace();
							}
						} else if (len < 0) {
							if (MediaPlayerService.mIsConnected) {
								if (maxCountWaitsForData-- > 0)
									Thread.sleep(MediaPlayerService.WAIT_FOR_DATA);
								else {
									runAudioThread = false;
									MediaPlayerService.sendMessageToTheClients(mOriginalUrl, MediaPlayerService.BROADCAST_MESSAGE_EXTRA_END_BY_NO_DATA);
								}
							} else if (!MediaPlayerService.mIsConnectedOrConnecting)
								runAudioThread = false;
							else {
								MediaPlayerService.sendMessageToTheClients(mOriginalUrl, MediaPlayerService.BROADCAST_MESSAGE_EXTRA_PAUSE_BY_NO_CONNECTION);
								mAudioRecordingPaused = true;
								int waitForConnectivity = MediaPlayerService.MAX_COUNT_WAITS_FOR_CONNECTIVITY;
								while (!MediaPlayerService.mIsConnected) {
									if (--waitForConnectivity >= 0)
										Thread.sleep(MediaPlayerService.WAITS_FOR_CONNECTIVITY);
									else {
										runAudioThread = false;
										MediaPlayerService.sendMessageToTheClients(mOriginalUrl, MediaPlayerService.BROADCAST_MESSAGE_EXTRA_END_BY_NO_CONNECTION);
										break;
									}
								}
								mAudioRecordingPaused = false;
								if (MediaPlayerService.mIsConnected) {
									mInputStream = new BufferedInputStream(new URL(mOriginalUrl).openStream());
									MediaPlayerService.sendMessageToTheClients(mOriginalUrl, MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RESTORE_BY_CONNECTION);
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//Log.v(LOG_TAG,"recording? " + recording);
				}
			}
			try {
				mInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		closeOutputFile(MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_FINISHED);
	}

	private void closeOutputFile(int cause) {
		if(MediaPlayerService.closeOutputFile(mBufferedOutputFile, mPrefix, cause, mOriginalUrl, mAbsolutePath, mFileIsReadyListener))
			mBufferedOutputFile = null;
	}
	public boolean prepare(String path, String prefix, String ext) {
		boolean result = false;
		mPath = path;
		mPrefix = prefix;
		mExtention = ext;//+MediaPlayerService.EXT_MODIFIER;//to be deleted by closeOutputFile after setting tags
		File outputFile = MediaPlayerService.prepareNewFile(path,prefix, mExtention);
		try {
			mBufferedOutputFile = new BufferedOutputStream(new FileOutputStream(outputFile));
			mAbsolutePath = outputFile.getAbsolutePath();
			runAudioThread = true;
			mAudioRecordingPaused = false;
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
			mInputStream = null;
		}
		return result;
	}
	public void pauseRecording(boolean pause){
		mAudioRecordingPaused = pause;
	}
	public void finishRecording(){
		runAudioThread = false;
	}
	public int getCurrentState() {
		int status = -1;
		if(runAudioThread)
			status = mAudioRecordingPaused ? MediaPlayerService.AUDIO_RECORDING_PAUSE :MediaPlayerService. AUDIO_RECORDING_START;
		return status;
	}
	public void setFileIsReadyListener(MediaPlayerService.FileIsReadyListener listener) {
		mFileIsReadyListener = listener;
	}
}

