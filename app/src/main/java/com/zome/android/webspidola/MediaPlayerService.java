package com.zome.android.webspidola;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.id3.GeobFrame;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.PrivFrame;
import com.google.android.exoplayer.metadata.id3.TxxxFrame;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.util.Util;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.NotSupportedException;
import com.zome.android.webspidola.player.DashRendererBuilder;
import com.zome.android.webspidola.player.EventLogger;
import com.zome.android.webspidola.player.ExoMediaPlayer;
import com.zome.android.webspidola.player.ExtractorRendererBuilder;
import com.zome.android.webspidola.player.HlsRendererBuilder;
import com.zome.android.webspidola.player.RecordableUriDataSource;
import com.zome.android.webspidola.player.SmoothStreamingRendererBuilder;
import com.zome.android.webspidola.player.SmoothStreamingTestMediaDrmCallback;
import com.zome.android.webspidola.player.WidevineTestMediaDrmCallback;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MediaPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {
	public static final int BROADCAST_MESSAGE_EXTRA_ADD_RADIO_NAME = 1;
	public static final int BROADCAST_MESSAGE_EXTRA_END_BY_NO_DATA = 2;
	public static final int BROADCAST_MESSAGE_EXTRA_PAUSE_BY_NO_CONNECTION = 3;
	public static final int BROADCAST_MESSAGE_EXTRA_END_BY_NO_CONNECTION = 4;
	public static final int BROADCAST_MESSAGE_EXTRA_RESTORE_BY_CONNECTION = 5;
	public static final int BROADCAST_MESSAGE_EXTRA_ON_PREPARED_FINISHED = 6;
	public static final int BROADCAST_MESSAGE_EXTRA_RECORD_TO_NEXT_FILE = 7;
	public static final int BROADCAST_MESSAGE_EXTRA_RECORD_STOP_NO_SPACE = 8;
	public static final int BROADCAST_MESSAGE_EXTRA_RECORD_FINISHED = 9;

	public static final int DEF_POS_TYPE = 0;
	public static final int DEF_POS_URL = 1;
	public static final int DEF_POS_LABEL = 2;
	public static final int DEF_POS_COUNT = 3;

	public static final String EXT_MODIFIER = "_w";
	public static final String DO_NOT_ADD_STATION = "ACCESS_ERROR";
	public static String applicationName;
	private static LocalBroadcastManager localBroadcastManager;
	private static Context mContext;
	private static boolean playerNeedsPrepare;
	private static MediaPlayerService mService;

	// Binder given to clients
	private final IBinder mBinder = new LocalMPBinder();
	public static final String CUSTOM_EVENT = "media-player-custom_event";
	public static final String CUSTOM_INTENT = "com-android-zome-webspidola-mediaservice";
	public static final String CUSTOM_EVENT_MESSAGE = "message";
	public static final String CUSTOM_EVENT_CATEGORY = "category";
	public static final String ACTION_PLAY = "com.zome.android.webspidola.action_play";
	public static final String ACTION_PAUSE = "com.zome.android.webspidola.action_pause";
	public static final String ACTION_EXIT = "com.zome.android.webspidola.action_exit";
	private final static int NO_CURRENT_HEADSET_STATE = -1;
	private final static int HEADSET_STATE_DISCONNECTED = 0;
	private final static int HEADSET_STATE_CONNECTED = 1;
	public final static String CURRENT_STATUS_IN_PAUSE = "Pause";
	public final static String CURRENT_STATUS_IN_PLAY = "Playing";
	public final static String CURRENT_STATUS_IN_STOP = "Stopped";
	public final static String CURRENT_STATUS_CHANGED = "Changed";
	public final static String CURRENT_STATUS_RESUMED = "Resume";
	public final static String CURRENT_STATUS_IN_PLAY_ERROR = "Error";

	public static final String SELECTED_STATION_DEFINITION = "SELECTED_STATION_DEFINITION";

	/*private final int IS_NULL = -1;
	private final int ON_STOP = 0;
	private final int ON_PAUSE = 1;
	private final int ON_STARTED = 2;
	private final int ON_PREPARE = 3;
	private final int ON_STOP_BY_ERROR = 4;
	*/
	private final static int NOTIFICATION_ID = 1;
	private final int NO_CONNECTIVITY_SET = -1;
	private static final Integer CONNECTIVITY_NO =null;
	private static final String TAG = "ExoPlayerService";

	//PlayerActivityHelper
	private static ExoMediaPlayer mediaPlayer = null;

	private static MediaController mediaController;
	private static EventLogger eventLogger;
	//PlayerActivityHelper

	private static NotificationManagerCompat mNotificationManager = null;
	//private String preparingUrl = null;
	//private static String currentlyPlayingUrl = null;
	//private static String selectedStationName = null;
	private static int mCurrentHeadsetState = NO_CURRENT_HEADSET_STATE;
	//private int mediaPlayerRegisteredState = IS_NULL;
	private WifiManager.WifiLock mWifiLoc = null;
	private ConnectivityManager mConnectManager = null;
	private static Integer mCurrentConnectivity = Integer.MAX_VALUE;
	private static TaskStackBuilder stackBuilder = null;
	private static long playerPosition;
	private int mCurrentConnectivityType =-1;
	public static boolean mMonitorHeadphones = true;
	public static boolean mMonitorBluetooth = false;
	public static boolean mMonitorPhoneCall = true;
	public static boolean mIsConnected = false;
	public static boolean mIsConnectedOrConnecting = false;
	public static boolean mDeviceStorageLow = false;
	public static int mPrefFontSizeForSearch;
	public static int mPrefFontSize;
	public static int mPrefPaddingTop;
	public static int mTheme;
	public static String mRootDirectoryForRecordings = "recordings";
	public static boolean mRecordsSortAscending = false;
	public static boolean mRecordsShortingNames = true;

	private static int mAudioLevelBeforePauseByHeadsetDisconnect;
	public static boolean mMonitorAudioLevelOnHeadphones = true;
	public static SharedPreferences mPreferences;
	public static final int  TAG_URI = R.string.TAG_URI;
	public static final int  TAG_TYPE = R.string.TAG_TYPE;
	public static PlayingInfo currentlyPlayingInfo;
	//Search frame data
	public static ArrayList<String[]> mStationForRestore = null;
	public static ArrayList<String[]> mNavigationsForRestore = null;
	public static String mSearchInputText = "";

	public static boolean topPositionOfPlayPause = true;
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void setPhoneCallMonitoring(boolean phoneCallMonitoring) {
		mMonitorPhoneCall = phoneCallMonitoring;
	}

	public HashMap<String,Integer> getRecordingMap() {
		AudioRecordRunnable audioRecord;
		HashMap<String,Integer> returnMap = new HashMap<>();
		if(isPlaying()){
			RecordableUriDataSource dataSourceForRecording = (RecordableUriDataSource) mediaPlayer.getDataSource();
			if(dataSourceForRecording != null && dataSourceForRecording.isRecording())
				returnMap.put(currentlyPlayingInfo.uri, isOnPause() ? MediaPlayerService.AUDIO_RECORDING_PAUSE : MediaPlayerService.AUDIO_RECORDING_START);
		}
		int currentStatus;
		Set<String> keys = mAudioRecordingMap.keySet();
		for(String url:keys)
			if((audioRecord = mAudioRecordingMap.get(url)) != null){
				try {
					if ((currentStatus = audioRecord.getCurrentState()) > -1)
						if(mAudioRecordingTreadMap.get(url).isAlive());
							returnMap.put(url, currentStatus);

				}catch(Exception e){}
			}
		return returnMap;
	}

	/*public String getCurrentStationUrl() {
		return currentlyPlayingUrl;
	}*/

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalMPBinder extends Binder {
		MediaPlayerService getService(){
			return MediaPlayerService.this;// Return this instance of LocalService so clients can call public methods
		}
	}

	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 * /
	public MediaPlayerService() {
		super("Media Player for Web Spidola");
	}*/
	private AudioManager mAudioManager;
	@Override
	public void onCreate() {
		super.onCreate();
		applicationName = getString(R.string.application_name);
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		mContext = getApplicationContext();
		mService = this;
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_EXIT);
		filter.addAction(ACTION_PAUSE);
		filter.addAction(ACTION_PLAY);
		filter.addAction(Intent.ACTION_HEADSET_PLUG);
		filter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
		filter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
		filter.addAction(Intent.ACTION_DIAL);
		filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		filter.addAction(Intent.ACTION_MEDIA_BUTTON);
		filter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
		filter.addAction(Intent.ACTION_ANSWER);
		filter.addAction(Intent.ACTION_CALL);
		filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		//filter.addAction(Intent.ACTION_SCREEN_OFF);
		//filter.addAction(Intent.ACTION_SCREEN_ON);

		/*LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ACTION_PAUSE));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ACTION_PLAY));*/
		this.registerReceiver(mMessageReceiver, filter);
		engageNotificationBarAsStopped();

		mediaController = new KeyCompatibleMediaController(this);
		//mediaController.setAnchorView(root);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		//mAudioManager.vol
	}
	private boolean tempPauseOnCall = false;
	@Override
	public void onAudioFocusChange(int focusChange) {
		if(mMonitorPhoneCall) {
			if (focusChange <= 0) {//LOSS -> PAUSE
				if (isPlaying()) {
					tempPauseOnCall = pausePlay();//message.append("Pause on connectivity lost");
				}
			} else {//GAIN -> PLAY
				if (tempPauseOnCall) {
					tempPauseOnCall = false;//message.append("Restart play on connectivity");
					reStartPlay();
				}
			}
		}
	}
	public void changeVolumeLevel(boolean up){
		//mAudioManager.
		mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND + AudioManager.FLAG_SHOW_UI);
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(mWifiLoc == null){//initialization
			mWifiLoc = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "WebSpidolaLoc");
			mWifiLoc.acquire();
			mConnectManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			//((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
		}
		if(intent != null)
			mMessageReceiver.onReceive(getApplicationContext(),intent);
		return super.onStartCommand(intent, flags, startId);
	}

	// Handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastNotificationReceiver mMessageReceiver = new BroadcastNotificationReceiver();
	private static String mAudioLevelBeforePauseByHeadsetDisconnectUrl = "";
	private void setAudioLevelBeforePauseByHeadsetDisconnectUrl() {
		if(mMonitorAudioLevelOnHeadphones) {
			mAudioLevelBeforePauseByHeadsetDisconnectUrl = currentlyPlayingInfo.uri;
			mAudioLevelBeforePauseByHeadsetDisconnect = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		}
	}
	private void restoreAudioLevelBeforePauseByHeadsetDisconnectUrl() {
		if(mMonitorAudioLevelOnHeadphones && mAudioLevelBeforePauseByHeadsetDisconnectUrl.equals(currentlyPlayingInfo.uri))
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioLevelBeforePauseByHeadsetDisconnect, AudioManager.FLAG_PLAY_SOUND + AudioManager.FLAG_SHOW_UI);
	}
	public class BroadcastNotificationReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			String action = intent.getAction();
			if (action != null) {
				switch (action) {
					case ACTION_EXIT:
						Log.e("MPS:onreceive",ACTION_EXIT);
						logAndBroadcast("i",ACTION_EXIT);
						if(mNotificationManager!=null)
							mNotificationManager.cancel(NOTIFICATION_ID);
						//prepareForStop();
						stopSelf();
						break;
					case ACTION_PAUSE:
						pausePlay();
						break;
					case ACTION_PLAY:
						reStartPlay();
						break;
					case Intent.ACTION_HEADSET_PLUG:
						int state = intent.getIntExtra("state", NO_CURRENT_HEADSET_STATE);
						if (mCurrentHeadsetState != NO_CURRENT_HEADSET_STATE) {
							if(mMonitorHeadphones)
							{
								if (state == HEADSET_STATE_DISCONNECTED) {
									if (mediaPlayer != null && isPlaying()) {
										setAudioLevelBeforePauseByHeadsetDisconnectUrl();
										pausePlay();
									}
								} else if (state == HEADSET_STATE_CONNECTED && isOnPause()) {
									reStartPlay();
									restoreAudioLevelBeforePauseByHeadsetDisconnectUrl();
									logAndBroadcast("i", "Headset '" + intent.getStringExtra("name") + "' connected");
								}
							}
						}
						mCurrentHeadsetState = state;
						break;
					case Intent.ACTION_DEVICE_STORAGE_LOW:
						mDeviceStorageLow = true;
						break;
					case Intent.ACTION_DEVICE_STORAGE_OK:
						mDeviceStorageLow = false;
						break;
					case Intent.ACTION_DIAL:
					case Intent.ACTION_MEDIA_BAD_REMOVAL:
					case Intent.ACTION_MEDIA_BUTTON:
					case Intent.ACTION_MY_PACKAGE_REPLACED:
						logAndBroadcast("i", "intentAction:" + action);
						break;
					/*case Intent.ACTION_SCO_AUDIO_STATE_UPDATED:// API level 8
					int state = Intent.getIntExtra(Intent.EXTRA_SCO_AUDIO_STATE, Intent.SCO_AUDIO_STATE_DISCONNECTED);
					if(state == Intent.SCO_AUDIO_STATE_CONNECTED)
						;//Bluetooth connected
					break;*/
					case ConnectivityManager.CONNECTIVITY_ACTION:
						if(mConnectManager != null) {
							boolean failOverConnection = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
							boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
							//String type = intent.getStringExtra(ConnectivityManager.EXTRA_NETWORK_TYPE);
							StringBuffer message = new StringBuffer("Connectivity:");
							if (failOverConnection)
								message.append("failOver-" + intent.getStringExtra(ConnectivityManager.EXTRA_REASON));
							if (noConnectivity)
								message.append(" noConnectivity");
							message.append(getConnectivityStatus());
							Integer newConnectivityState = noConnectivity ? CONNECTIVITY_NO : new Integer(mConnectManager.getActiveNetworkInfo().getType());
							NetworkInfo networkInfo = mConnectManager.getActiveNetworkInfo();
							if(networkInfo!= null) {//despite info in the intent
								mIsConnected = networkInfo.isConnected();
								mIsConnectedOrConnecting = networkInfo.isConnectedOrConnecting();
								int newConnectivityType = networkInfo.getType();
								if (mCurrentConnectivity != CONNECTIVITY_NO) {
									if (!mCurrentConnectivity.equals(newConnectivityState) || newConnectivityType != mCurrentConnectivityType) {
										if (pausePlay())
											restartPlayHandler.postDelayed(restartPlayRunnable, 1000);
									/*if (noConnectivity) {
										if (mediaPlayer != null && isPlaying()) {
											pausePlay();
											message.append("Pause on connectivity lost");
										}
									} else if (isOnPause()) {
										reStartPlay();
										message.append("Restart play on connectivity");
									}*/
									}
								}
								mCurrentConnectivity = newConnectivityState;
								mCurrentConnectivityType = newConnectivityType;
								logAndBroadcast("i", message.toString());
							}
						}
						break;
					default:
						break;
				}
			}
		}
	}

	public void prepareForStop() {
		if(mAudioManager != null) {
			mAudioManager.abandonAudioFocus(this);
			mAudioManager = null;
		}
		if(mMessageReceiver != null) {
			unregisterReceiver(mMessageReceiver);
			mMessageReceiver = null;
		}
		restartPlayHandler.removeCallbacks(restartPlayRunnable);
		if(mAudioRecordingMap != null) {
			Set<String> keys = mAudioRecordingMap.keySet();
			AudioRecordRunnable audioRecord;
			for (String url : keys)
				if ((audioRecord = mAudioRecordingMap.get(url)) != null) {
					try {
						if (audioRecord.getCurrentState() > -1 && mAudioRecordingTreadMap.get(url).isAlive())
							audioRecord.finishRecording();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			mAudioRecordingMap = null;
		}
		if(mediaPlayer!=null) {
			stopPlay(true);//releasePlayer();
			mediaPlayer = null;
		}
		if(mWifiLoc != null) {//initialized
			mWifiLoc.release();
			mWifiLoc=null;
		}
		if(mNotificationManager!=null) {
			mNotificationManager.cancel(NOTIFICATION_ID);
			mNotificationManager = null;
		}
		stopSelf();
		//onDestroy();
	}

	final Handler restartPlayHandler = new Handler();// Timeout Handler to Restart Play after some time
	final Runnable restartPlayRunnable = new Runnable() { @Override public void run() { reStartPlay(); }};// Restart Play after some time
	public void setHeadphoneMonitoring(boolean monitor){
		mMonitorHeadphones = monitor;
	}
	public void setAudioLevelMonitoring(boolean monitor){
		mMonitorAudioLevelOnHeadphones = monitor;
	}
	public void setBluetoothMonitoring(boolean monitor){
		mMonitorBluetooth = monitor;
	}
	private boolean isPlaying(){
		boolean playing = false;
		if(mediaPlayer != null){
			switch(mediaPlayer.getPlaybackState())
			{
				case ExoPlayer.STATE_PREPARING:
				case ExoPlayer.STATE_BUFFERING:
					playing = mediaPlayer.getPlayerControl().isPlaying();
					break;
				case ExoPlayer.STATE_READY:
					playing = mediaPlayer.getPlayWhenReady();
			}
		}
		return playing;
	}
	public boolean isOnPause() {
		boolean onPause = false;
		if(mediaPlayer != null){
			switch(mediaPlayer.getPlaybackState())
			{
				case ExoPlayer.STATE_PREPARING:
				case ExoPlayer.STATE_BUFFERING:
					onPause = !mediaPlayer.getPlayerControl().isPlaying();
					break;
				case ExoPlayer.STATE_READY:
					onPause = !mediaPlayer.getPlayWhenReady();
			}
		}
		return onPause;
	}
	public boolean isOnPrepare() {
		boolean preparing = false;
		if(mediaPlayer != null){
			switch(mediaPlayer.getPlaybackState())
			{
				case ExoPlayer.STATE_PREPARING:
				case ExoPlayer.STATE_BUFFERING:
					preparing = !mediaPlayer.getPlayerControl().isPlaying();
					if(preparing && mediaPlayer.getPlayWhenReady())
						preparing = false;
					break;
			}
		} //mediaPlayerRegisteredState = ON_STARTED;
		return preparing;
	}
	private boolean serviceJustStarted = true;
	public boolean isAutoPlayCase() {
		boolean theCase = false;
		if(serviceJustStarted) {
			serviceJustStarted = false;
			theCase = mediaPlayer == null;
		}
		return theCase;
	}
	private static void engageNotificationBar(String text, NotificationCompat.Action[] actions) {
		//Notification Area content
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mService/*this*/);
		int iconId = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.spidola64_sil_2:R.drawable.spidola64;
		mBuilder.setSmallIcon(iconId);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.spidolabigimg));
		mBuilder.setContentTitle(mContext.getString(R.string.application_name));
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		if(stackBuilder == null)
			stackBuilder = TaskStackBuilder.create(mService/*this*/);
		// Adds the back stack for the Intent (but not the Intent itself)
		//stackBuilder.addParentStack(NotificationAreaActivity.class);
		// Creates an explicit intent for an return to FavoriteStationsFrame in case of click
		Intent resultIntent = new Intent(mContext/*getApplicationContext()*/, MainTabbedActivity.class);
		stackBuilder.addNextIntent(resultIntent);// Adds the Intent that starts the Activity to the top of the stack
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		//Media Style
		NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();

		mediaStyle.setCancelButtonIntent(stackBuilder.getPendingIntent(1, PendingIntent.FLAG_CANCEL_CURRENT));
		mediaStyle.setShowActionsInCompactView(0);
		mBuilder.addAction(generateAction(R.drawable.close32, "Close", ACTION_EXIT));
		for(int i=0; actions.length>i;i++) {
			mediaStyle.setShowActionsInCompactView(i+1);
			mBuilder.addAction(actions[i]);
		}
		mediaStyle.setShowCancelButton(true);
		mBuilder.setStyle(mediaStyle);
		mBuilder.setContentText(text);
		Notification notification = mBuilder.build();
		notification.flags |= NotificationCompat.FLAG_NO_CLEAR;
		mNotificationManager = NotificationManagerCompat.from(mService/*this*/);//(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);//Notification Manager
		mNotificationManager.notify(NOTIFICATION_ID, notification);// mId allows you to update the notification later on.
		//End building Notification Area content
	}
	private static void engageNotificationBarAsPlaying(PlayingInfo info) {
		engageNotificationBarAsPlaying(info!=null?info.name:"Unknown station");
	}
	private static void engageNotificationBarAsPlaying(String text) {
		NotificationCompat.Action[] actions = {generateAction(R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)};
		mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_PLAY;
		engageNotificationBar("Playing:" + text, actions);
		/*try{
			actions[0].actionIntent.send();
		}catch(Exception e){
			Log.e("engageNotificationBarAsPlaying", e.toString());
		}*/
	}
	private static int mCurrentPlayingStatus;
	public int getCurrentPlayingStatus(){return mCurrentPlayingStatus;}
	public final static int CURRENT_PLAYING_STATUS_NONE = 0;
	public final static int CURRENT_PLAYING_STATUS_PLAY = 1;
	public final static int CURRENT_PLAYING_STATUS_PAUSE = 2;

	private static void engageNotificationBarAsPaused(PlayingInfo info) {
		engageNotificationBarAsPaused(info!=null?info.name:"Unknown station");
	}
	private static void engageNotificationBarAsPaused(String text) {
		NotificationCompat.Action[] actions = {generateAction(R.drawable.ic_media_play, "Play", ACTION_PLAY)};
		mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_PAUSE;
		engageNotificationBar("Paused:" + text, actions);
	}
	private static void engageNotificationBarAsConnecting(PlayingInfo info) {
		engageNotificationBarAsConnecting(info!=null?info.name:"Unknown station");
	}
	private static void engageNotificationBarAsConnecting(String text) {
		NotificationCompat.Action[] actions = {generateAction(R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)};
		mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_PLAY;
		engageNotificationBar("Connectiing to:"+ text, actions);
	}
	private static void engageNotificationBarAsStopped() {
		NotificationCompat.Action[] actions;
		if(currentlyPlayingInfo!=null ) {
			actions = new NotificationCompat.Action[]{generateAction(R.drawable.ic_media_play, "Play", ACTION_PLAY)};
			mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_NONE;
		}
		else {
			actions = new NotificationCompat.Action[]{};
			mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_NONE;
		}

		engageNotificationBar("Not Playing", actions);
	}
	private static NotificationCompat.Action generateAction(int icon, String title, String intentAction) {

		/* */Intent intent = new Intent(mContext/*getApplicationContext()*/, MediaPlayerService.class );
		intent.setAction(intentAction);
		PendingIntent pendingIntent = PendingIntent.getService(mContext/*getApplicationContext()*/, 2, intent, 0);
		/** /
		Intent intent = new Intent(getApplicationContext(), BroadcastNotificationReceiver.class );
		intent.setAction(intentAction);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 2, intent, 0);
		/**/
		NotificationCompat.Action action = new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
		return action;
	}	/**
	 * This method is invoked on the worker thread with a request to process.
	 * Only one Intent is processed at a time, but the processing happens on a
	 * worker thread that runs independently from other application logic.
	 * So, if this code takes a long time, it will hold up other requests to
	 * the same IntentService, but it will not hold up anything else.
	 * When all requests have been handled, the IntentService stops itself,
	 * so you should not call {@link #stopSelf}.
	 *
	 * @param intent The value passed to {@link
	 *               Context#startService(Intent)}.
	 * /
	@Override
	protected void onHandleIntent(Intent intent) {
		//????
	}*/
	/** methods for clients */
	private static boolean isMediaPlayerReadyToContinue(){
		//if (mediaPlayerRegisteredState != ON_STOP && mediaPlayerRegisteredState != ON_STOP_BY_ERROR && !isPlaying() && url == currentlyPlayingUrl) {
		return mediaPlayer != null && mediaPlayer.getPlaybackState() == ExoPlayer.STATE_READY && !mediaPlayer.getPlayerControl().isPlaying() && mediaPlayer.getRenderedUri().equals(Uri.parse(currentlyPlayingInfo.uri));
	}
	public static void reStartPlay() {
		if(isMediaPlayerReadyToContinue()){//mediaPlayerRegisteredState == ON_PAUSE) {
			//mediaPlayerRegisteredState = ON_STARTED;
			if(playerNeedsPrepare){
				mediaPlayer.prepare();
				playerNeedsPrepare = false;
			}
			mediaPlayer.getPlayerControl().start();//start-resume
			engageNotificationBarAsPlaying(currentlyPlayingInfo);
			logAndBroadcast("i", CURRENT_STATUS_RESUMED+" "+CURRENT_STATUS_IN_PLAY + " after pause");
			return;
		}
		else
			startPlay();
	}
	public static void startPlay() {
		if(currentlyPlayingInfo != null)//currentlyPlayingUrl != null && selectedStationName != null)
			startPlay(currentlyPlayingInfo);//, selectedStationName);
	}
	/*public static void startPlay(String url, String stationName){
		startPlay(new PlayingInfo((new StringBuffer(String.valueOf(Util.TYPE_OTHER)).append(' ').append(url).append(' ').append(stationName)).toString()));
	}*/
	public static void startPlay(PlayingInfo stationDef) {
		try{
			if(mediaPlayer != null) {
				if (isMediaPlayerReadyToContinue()) {
					//mediaPlayerRegisteredState = ON_STARTED;
					mediaPlayer.getPlayerControl().start();//start-resume
					engageNotificationBarAsPlaying(currentlyPlayingInfo.name);//selectedStationName);
					logAndBroadcast("i", "Start "+CURRENT_STATUS_IN_PLAY + " after pause");
					return;
				}
				else//releasePlayer();
					stopPlay(true);//logAndBroadcast("d", CURRENT_STATUS_IN_PLAY + " when mediaPlayer.getPlaybackState=" + (new Integer(mediaPlayer.getPlaybackState())).toString() + " ,mediaPlayer.getPlayerControl().isPlaying(="+mediaPlayer.getPlayerControl().isPlaying());

			}
			//else
				//mediaPlayerRegisteredState = IS_NULL;
		} catch (Exception eOnResume){
			logAndBroadcast("e", CURRENT_STATUS_RESUMED+" after pause " + eOnResume.getMessage());
		}
		if(stationDef.isValid()) {
			//Start or resume with new url
			//selectedStationName = stationDef.name;
			//currentlyPlayingUrl = stationDef.uri;
			currentlyPlayingInfo = stationDef;
			preparePlayer(true);//getStoppedMediaPlayer();
			//mediaPlayerRegisteredState = ON_PREPARE;
			engageNotificationBarAsConnecting(currentlyPlayingInfo);//selectedStationName);
			StringBuffer mess = new StringBuffer("Preparing");
			logAndBroadcast("o", mess.toString());
			showMessageInPopup(mess.append(": ").append(currentlyPlayingInfo.name));//selectedStationName));
		}
		else
			currentlyPlayingInfo = null;
	}
	private static Toast toastMessage;
	public static void showMessageInPopup(String message) {
		showMessageInPopup(new StringBuffer(message), Toast.LENGTH_LONG, false);
	}
	public static void showMessageInPopup(StringBuffer message) {
		showMessageInPopup(message, Toast.LENGTH_LONG, false);
	}
	public static void showMessageInPopup(StringBuffer message, int duration, boolean concatPrev) {
		if (toastMessage != null) {
			if (concatPrev)
				message.append('\n').append(toastMessage.getView().getTag().toString());
			toastMessage.cancel();
		}
		toastMessage = Toast.makeText(mContext, message, duration);
		toastMessage.getView().setTag(message);
		toastMessage.show();
		/*if (popupView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
			infoPopup.setContentView(layoutInflater.inflate(R.layout.popup, null));
		}
		else
			dismissInfoPopup();*/
		/*if (infoPopup.isShowing()) {
			//popupHideHandler.removeCallbacks(popupHideRunnable);
			infoPopup.dismiss();
		}*/
		/*TextView textView = (TextView) infoPopup.getContentView().findViewById(R.id.popup_text);
		textView.setText(message);*//*
		View anchor = selectedStation == null ? (View) findViewById(R.id.stations_radio_group) : selectedStation;
		infoPopup.showAsDropDown(anchor, 400, -10, Gravity.FILL);
		infoPopup.update(800, 200);
		popupHideHandler.postDelayed(popupHideRunnable, 10000);*/
	}

	public boolean pausePlay() {
		boolean paused;
		if (paused = (mediaPlayer != null && isPlaying())) {
			//mediaPlayerRegisteredState = ON_PAUSE;
			mediaPlayer.getPlayerControl().pause();//pause
			logAndBroadcast("i", CURRENT_STATUS_IN_PAUSE);
			engageNotificationBarAsPaused(currentlyPlayingInfo);//selectedStationName);
		}
		//else mediaPlayerRegisteredState = IS_NULL;
		return paused;
	}
	public static void stopPlay() {stopPlay(false);}
	public static void stopPlay(boolean noNotif) {
		if (mediaPlayer != null) {
			currentlyPlayingInfo = null;//selectedStationName = null;//mediaPlayerRegisteredState = state;
			if(!mediaPlayer.getPlayerControl().isPlaying())
				mediaPlayer.getPlayerControl().pause();

			releasePlayer();
			if(!noNotif)
				engageNotificationBarAsStopped();
			logAndBroadcast("i", CURRENT_STATUS_IN_STOP);
		}
	}
	public PlayingInfo whatIsPlaying(){
		boolean playing = mediaPlayer!=null && (isPlaying() || isMediaPlayerReadyToContinue());//mediaPlayerRegisteredState == ON_PAUSE));
		return playing && currentlyPlayingInfo!=null? currentlyPlayingInfo : null;
	}
	/*
	public String getCurrentStationName(){
		return selectedStationName;
	}*/
	private static void logAndBroadcast(String category, String mess){
		int extra = 0;
		switch(category) {
			case "e":Log.e("MPS:onError", mess);break;
			case "o":
				extra = BROADCAST_MESSAGE_EXTRA_ADD_RADIO_NAME;
			case "i":
				Log.i("MPS:onInfo", mess);
				break;
		}
		sendMessageToTheClients(mess, extra);
	}
	private static void sendMessageToTheClients(int command, String param) {
		sendMessageToTheClients(command+" "+param,0);
	}

	public static void sendMessageToTheClients(String msg, int extra) {
		Log.d("MPS:sender", "Broadcasting message:msg="+msg+",extra="+extra);
		Intent intent = new Intent(CUSTOM_EVENT);
		intent.putExtra(CUSTOM_EVENT_MESSAGE, msg);// Include some extra data.
		intent.putExtra(CUSTOM_EVENT_CATEGORY, extra);
		localBroadcastManager.sendBroadcast(intent);
	}
	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(this.getClass().getName(), "UNBIND");
		return true;
	}
	@Override
	public void onDestroy(){
		prepareForStop();
		if(mConnectManager != null)
			mConnectManager = null;
		logAndBroadcast("d", "onDestroy");
	}

	public StringBuffer getConnectivityStatus(){
		StringBuffer message = new StringBuffer();
		NetworkInfo networkInfo = mConnectManager.getActiveNetworkInfo();
		if(networkInfo != null){
			message.append('\n').append(networkInfo.getState())
					.append('\n').append(networkInfo.getTypeName())
					.append('\n').append(networkInfo.getExtraInfo());
		}
		else
			message.append(getString(R.string.network_not_available));
		return message;
	}
	private static ExoMediaPlayer.RendererBuilder getRendererBuilder(String contentUrl)  {
		return getRendererBuilder(Util.TYPE_OTHER, Uri.parse(contentUrl));
	}
	private static ExoMediaPlayer.RendererBuilder getRendererBuilder(int contentType, Uri contentUri) {
		return getRendererBuilder(contentType, contentUri, "");
	}
	private static ExoMediaPlayer.RendererBuilder getRendererBuilder(int contentType, Uri contentUri, String contentId) {
		return getRendererBuilder(contentType, contentUri, contentId, "");
	}
	private static ExoMediaPlayer.RendererBuilder getRendererBuilder(int contentType, Uri contentUri, String contentId, String provider) {
		String userAgent = Util.getUserAgent(mService/*this*/, mService.getString(R.string.exo_player_name));
		switch (contentType) {
			case Util.TYPE_SS:
				return new SmoothStreamingRendererBuilder(mService/*this*/, userAgent, contentUri.toString(),
						new SmoothStreamingTestMediaDrmCallback());
			case Util.TYPE_DASH:
				return new DashRendererBuilder(mService/*this*/, userAgent, contentUri.toString(),
						new WidevineTestMediaDrmCallback(contentId, provider));
			case Util.TYPE_HLS:
				return new HlsRendererBuilder(mService/*this*/, userAgent, contentUri.toString());
			case Util.TYPE_OTHER:
				ExtractorRendererBuilder renderer = new ExtractorRendererBuilder(mService/*this*/, userAgent, contentUri);
				return renderer;
			default:
				throw new IllegalStateException("Unsupported type: " + contentType);
		}
	}

	private Uri contentUri;
	private int contentType;
	private String contentId;
	private String provider;
	public final static int TRANSFER_COMMAND_SHOW_CONTROLS = 1;
	public final static int TRANSFER_COMMAND_BUTTONS_VISIBILITY =2;
	private final static int TRANSFER_COMMAND_UPDATE_TEXT =3;
	private final static int TRANSFER_COMMAND_SHUTTER_VIEW = 4;
	private final static int TRANSFER_COMMAND_ASPECT_RATIO =5;
	private final static int TRANSFER_COMMAND_SET_CUES = 6;
	private static void transfer(int command, String... params){
		String param = "";
		if(params != null && params.length > 0 && params[0]!= null)
			param = params[0];
		switch(command)
		{
			case TRANSFER_COMMAND_SHOW_CONTROLS:
				switch(param){
					case CURRENT_STATUS_IN_PLAY_ERROR:
						if(mCurrentConnectivity != CONNECTIVITY_NO)
							reStartPlay();
						else
							stopPlay();
						break;
					case CURRENT_STATUS_IN_PLAY:
						break;
					case CURRENT_STATUS_IN_PAUSE:
						break;
					case CURRENT_STATUS_IN_STOP:
						engageNotificationBarAsStopped();
						break;
				}
				logAndBroadcast("i", command + " " + param);
				break;
			case TRANSFER_COMMAND_BUTTONS_VISIBILITY:
				switch(param) {
					case CURRENT_STATUS_IN_STOP:
						engageNotificationBarAsStopped();
						break;
					case CURRENT_STATUS_IN_PLAY:
						engageNotificationBarAsPlaying(currentlyPlayingInfo);//selectedStationName);
						break;
					case CURRENT_STATUS_IN_PAUSE:
						engageNotificationBarAsPaused(currentlyPlayingInfo);//selectedStationName);
				}
				sendMessageToTheClients(command, param);
				break;
			case TRANSFER_COMMAND_UPDATE_TEXT:
				break;
			case TRANSFER_COMMAND_SHUTTER_VIEW://transfer("shutterView.setVisibility(View.GONE)");
				break;
			case TRANSFER_COMMAND_ASPECT_RATIO://transfer("videoFrame.setAspectRatio(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height)");
				break;
			case TRANSFER_COMMAND_SET_CUES://transfer("subtitleLayout.setCues(cues);");
				break;
		}
		//transfer command
	}

	private static void preparePlayer(boolean playWhenReady) {
		if (mediaPlayer == null) {
			ExoMediaPlayer.RendererBuilder renderer = getRendererBuilder(currentlyPlayingInfo.uri);
			mediaPlayer = new ExoMediaPlayer(renderer, Uri.parse(currentlyPlayingInfo.uri));
			//mediaPlayer.getCurrentPosition();mediaPlayer.getPlayerControl().getCurrentPosition();
			mediaPlayer.addListener(mediaPlayerListener);
			mediaPlayer.setCaptionListener(mediaPlayerCaptionListener);
			mediaPlayer.setMetadataListener(mediaPlayerMetadataListener);
			mediaPlayer.seekTo(playerPosition);
			playerNeedsPrepare = true;
			mediaController.setMediaPlayer(mediaPlayer.getPlayerControl());
			mediaController.setEnabled(true);
			eventLogger = new EventLogger();
			eventLogger.startSession();
			mediaPlayer.addListener(eventLogger);
			mediaPlayer.setInfoListener(eventLogger);
			mediaPlayer.setInternalErrorListener(eventLogger);
			//debugViewHelper = new DebugTextViewHelper(mediaPlayer, debugTextView);
			//debugViewHelper.start();
		}
		if (playerNeedsPrepare) {
			mediaPlayer.prepare();
			playerNeedsPrepare = false;
			//updateButtonVisibilities();
		}
		//mediaPlayer.setSurface(surfaceView.getHolder().getSurface());
		mediaPlayer.setPlayWhenReady(playWhenReady);
	}

	private static void releasePlayer() {
		if (mediaPlayer != null) {
			//debugViewHelper.stop();
			//debugViewHelper = null;
			playerPosition = mediaPlayer.getCurrentPosition();
			playerPosition = 0;
			mediaPlayer.release();
			playerNeedsPrepare = true;
			mediaPlayer = null;
			eventLogger.endSession();
			eventLogger = null;
		}
	}
	private static final class KeyCompatibleMediaController extends MediaController {

		private MediaController.MediaPlayerControl playerControl;

		public KeyCompatibleMediaController(Context context) {
			super(context);
		}

		@Override
		public void setMediaPlayer(MediaController.MediaPlayerControl playerControl) {
			super.setMediaPlayer(playerControl);
			this.playerControl = playerControl;
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			int keyCode = event.getKeyCode();
			if (playerControl.canSeekForward() && keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					playerControl.seekTo(playerControl.getCurrentPosition() + 15000); // milliseconds
					show();
				}
				return true;
			} else if (playerControl.canSeekBackward() && keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					playerControl.seekTo(playerControl.getCurrentPosition() - 5000); // milliseconds
					show();
				}
				return true;
			}
			return super.dispatchKeyEvent(event);
		}
	}
	public class PlayerControl implements MediaController.MediaPlayerControl {

		private final ExoPlayer exoPlayer;

		public PlayerControl(ExoPlayer exoPlayer) {
			this.exoPlayer = exoPlayer;
		}

		@Override
		public boolean canPause() {
			return true;
		}

		@Override
		public boolean canSeekBackward() {
			return true;
		}

		@Override
		public boolean canSeekForward() {
			return true;
		}

		@Override
		public int getAudioSessionId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getBufferPercentage() {
			return exoPlayer.getBufferedPercentage();
		}

		@Override
		public int getCurrentPosition() {
			return exoPlayer.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0
					: (int) exoPlayer.getCurrentPosition();
		}

		@Override
		public int getDuration() {
			return exoPlayer.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0
					: (int) exoPlayer.getDuration();
		}

		@Override
		public boolean isPlaying() {
			return exoPlayer.getPlayWhenReady();
		}

		@Override
		public void start() {
			exoPlayer.setPlayWhenReady(true);
		}

		@Override
		public void pause() {
			exoPlayer.setPlayWhenReady(false);
		}

		@Override
		public void seekTo(int timeMillis) {
			long seekPosition = exoPlayer.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0
					: Math.min(Math.max(0, timeMillis), getDuration());
			exoPlayer.seekTo(seekPosition);
		}

	}
	public static class BatchPlayingStat {
		private static BatchPlayingStat mBatchPlayingStat = null;
		private int mOverAllLength = 0;
		private ArrayList<PlayingInfo> mBatchPlayingDefinition = null;
		private int mBatchPlayingDefinitionIndex = -1;
		private int mOverallPosition = 0;

		private BatchPlayingStat() {
			this(new ArrayList<PlayingInfo>(0));
		}

		private BatchPlayingStat(ArrayList<PlayingInfo> batchDefinitions) {
			mBatchPlayingDefinition = batchDefinitions;
			mBatchPlayingDefinitionIndex = -1;
			mOverAllLength = 0;
			mOverallPosition = 0;
			for(PlayingInfo info:batchDefinitions)
				mOverAllLength+=info.length;
		}

		public PlayingInfo getNextPlayingInfo() {
			return getNextPlayingInfo(1, false);
		}

		public PlayingInfo getNextPlayingInfo(int delta) {
			return getNextPlayingInfo(delta, true);
		}

		public PlayingInfo getNextPlayingInfo(int delta, boolean jump) {
			PlayingInfo info = null;
			if (mBatchPlayingDefinition.size() > 0) {
				if(mBatchPlayingDefinitionIndex >= 0 && mBatchPlayingDefinition.size() > mBatchPlayingDefinitionIndex)
					mOverallPosition += (mBatchPlayingDefinition.get(mBatchPlayingDefinitionIndex)).length*(delta>0? 1:-1);
				if ((mBatchPlayingDefinitionIndex +=delta) >=0  && mBatchPlayingDefinition.size() > mBatchPlayingDefinitionIndex) {
					info = mBatchPlayingDefinition.get(mBatchPlayingDefinitionIndex);
					Log.i("MPS","getNextPlayingInfo: selected to play "+mBatchPlayingDefinitionIndex+" batch item out of "+mBatchPlayingDefinition.size());
					//(new Exception("getNextPlayingInfo: selected to play "+mBatchPlayingDefinitionIndex+" batch item out of "+mBatchPlayingDefinition.size())).printStackTrace();
				}
				else
					Log.i("MPS","getNextPlayingInfo: played last batch item out of "+mBatchPlayingDefinition.size());
			}
			if(info != null && jump){
				startPlay(info);
			}
			return info;
		}

		public static BatchPlayingStat getInstance(ArrayList<PlayingInfo> batchDefinitions) {
			if (batchDefinitions != null)
				mBatchPlayingStat = new BatchPlayingStat(batchDefinitions);
			else if (mBatchPlayingStat == null)
				mBatchPlayingStat = new BatchPlayingStat();
			return mBatchPlayingStat;
		}

		public int getCurrentPercentageInPlay() {
			int position = -1;
			if(mediaPlayer!=null ) {
				com.google.android.exoplayer.util.PlayerControl control = mediaPlayer.getPlayerControl();
				if ((mediaPlayer.isPlaying() || control.isPlaying()) && currentlyPlayingInfo.length > 0)
					position = Math.round(100.0f * control.getCurrentPosition() / control.getDuration());
				else
					Log.e("MPS","isPlaying="+ mediaPlayer.isPlaying()+", PlayingInfo.length=" + currentlyPlayingInfo.length);
			}
			else
				Log.e("MPS","No MediaPlayer");
			return position;
		}

		public int getCurrentPercentageInList() {
			int position = -1;
			if(mediaPlayer!=null && mediaPlayer.isPlaying() && mOverAllLength > 0) {
				position = Math.round(100.0f * (mOverallPosition + mediaPlayer.getCurrentPosition()) / mOverAllLength);
				mediaPlayer.getDuration();
			}
			return position;
		}
		public long getDuration() {
			long duration = 0;
			if(mediaPlayer!=null && mediaPlayer.isPlaying() && mOverAllLength > 0) {
				duration = mediaPlayer.getDuration();
			}
			return duration;
		}

		public void setCurrentPercentageInPlay(int progress) {
			if(mediaPlayer!=null && mediaPlayer.isPlaying() && currentlyPlayingInfo.length > 0) {
				com.google.android.exoplayer.util.PlayerControl control = mediaPlayer.getPlayerControl();
				int position = Math.round(0.01f * progress * control.getDuration());//currentlyPlayingInfo.length);
				control.seekTo(position);
				//mediaPlayer.seekTo(position);
				Log.e("MPS", "duration:"+formatTimeString(mediaPlayer.getDuration())+", set to :"+formatTimeString(position));
			}
		}

		public void seekByOverallControl(int delta) {
			PlayingInfo info = getNextPlayingInfo(delta);
			/*if(info != null) {
				setCurrentPercentageInPlay(100);
			}*/
		}
		public PlayingInfo getCurrentPlaying(){
			PlayingInfo info = null;
			if (mBatchPlayingDefinitionIndex >= 0 && mBatchPlayingDefinition.size() > mBatchPlayingDefinitionIndex)
				info = mBatchPlayingDefinition.get(mBatchPlayingDefinitionIndex);
			return info;
		}

		public boolean isFirst() {
			return (mBatchPlayingDefinitionIndex == 0);
		}
		public boolean isLast() {
			return (mBatchPlayingDefinitionIndex == mBatchPlayingDefinition.size()-1);
		}
	}
	public static String formatTimeString(long millis) {
		StringBuffer buf = new StringBuffer();

		int hours = (int) (millis / (1000 * 60 * 60));
		int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
		int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

		buf.append(String.format("%02d", hours))
			.append(":").append(String.format("%02d", minutes))
			.append(":").append(String.format("%02d", seconds));
		return buf.toString();
	}
	public static void startPlayingBatch(ArrayList<PlayingInfo> batchDefinitions){
		BatchPlayingStat batchPlayingStat = BatchPlayingStat.getInstance(batchDefinitions);
		PlayingInfo definition = batchPlayingStat.getNextPlayingInfo();
		if(definition != null)
			startPlay(definition);//Pause causes change in state, thus stop detected second time
	}

	private static ExoMediaPlayer.Listener mediaPlayerListener = new ExoMediaPlayer.Listener() {
		// ExoMediaPlayer.Listener implementation
		@Override
		public void onStateChanged(boolean playWhenReady, int playbackState) {
			/*if (playbackState == ExoPlayer.STATE_ENDED) {
				transfer(TRANSFER_COMMAND_SHOW_CONTROLS, CURRENT_STATUS_IN_STOP);
			}*/
			String text = "playWhenReady=" + playWhenReady + ", playbackState=";
			String status = "";
			switch (playbackState) {
				case ExoPlayer.STATE_BUFFERING:
					//mediaPlayerRegisteredState = ON_STARTED;
					status = CURRENT_STATUS_IN_PLAY;
					text += "buffering";
					break;
				case ExoPlayer.STATE_ENDED:
					//mediaPlayerRegisteredState = ON_STOP;
					status = CURRENT_STATUS_IN_STOP;
					text += "ended";
					startPlayingBatch(null);
					break;
				case ExoPlayer.STATE_IDLE:
					//mediaPlayerRegisteredState = ON_STOP;
					status = CURRENT_STATUS_IN_STOP;
					text += "idle";
					break;
				case ExoPlayer.STATE_PREPARING:
					//mediaPlayerRegisteredState = mediaPlayer.getPlayerControl().isPlaying()? ON_STARTED : ON_PREPARE;
					status = CURRENT_STATUS_IN_PLAY;
					text += "preparing";
					break;
				case ExoPlayer.STATE_READY:
					//mediaPlayerRegisteredState =mediaPlayer.getPlayWhenReady() ? BROADCAST_MESSAGE_EXTRA_BROADCAST_MESSAGE_EXTRA_ON_PREPARED_FINISHED : ON_PAUSE;
					status = currentlyPlayingInfo != null ? CURRENT_STATUS_IN_PLAY : CURRENT_STATUS_IN_STOP;
					text += "ready";
					break;
				default:
					text += "unknown";
					break;
			}
			transfer(TRANSFER_COMMAND_UPDATE_TEXT, text);//"playerStateTextView.setText(text)");
			transfer(TRANSFER_COMMAND_BUTTONS_VISIBILITY, status);//"updateButtonVisibilities()");
		}

		@Override
		public void onError(Exception e) {
			String errorString = null;
			String extra = "";
			if (e instanceof UnsupportedDrmException) {
				// Special case DRM failures.
				UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
				errorString = mService.getString(Util.SDK_INT < 18 ? R.string.error_drm_not_supported
						: unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
						? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
			} else if (e instanceof ExoPlaybackException) {
				if (e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
					// Special case for decoder initialization failures.
					MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
							(MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
					if (decoderInitializationException.decoderName == null) {
						if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
							errorString = mService.getString(R.string.error_querying_decoders);
						} else if (decoderInitializationException.secureDecoderRequired) {
							errorString = mService.getString(R.string.error_no_secure_decoder,
									decoderInitializationException.mimeType);
						} else {
							errorString = mService.getString(R.string.error_no_decoder,
									decoderInitializationException.mimeType);
						}
					} else {
						errorString = mService.getString(R.string.error_instantiating_decoder,
								decoderInitializationException.decoderName);
					}
				} else {
					String className = e.getCause().getClass().getName();
					final String signature = ".HttpDataSource$";
					int pos = className.indexOf(signature);
					String word = "";
					if (pos >0) {
						switch (className.substring(pos+signature.length())) {
							case "InvalidResponseCodeException":
								word = "temporary ";
							case "HttpDataSourceException":
								break;
						}
						StringBuffer mess = new StringBuffer("Url of station '");
						mess.append(currentlyPlayingInfo.name).append("' (").append(currentlyPlayingInfo.uri).append(") ").append(word).append("is not available");
						showMessageInPopup(mess);
						stopPlay();
						extra = DO_NOT_ADD_STATION;
					}
				}
			}
			if (errorString != null) {
				Toast.makeText(mContext/*getApplicationContext()*/, errorString, Toast.LENGTH_LONG).show();
			}
			playerNeedsPrepare = true;
			//mediaPlayerRegisteredState = ON_STOP_BY_ERROR;
			transfer(TRANSFER_COMMAND_BUTTONS_VISIBILITY, (String)null);//"updateButtonVisibilities()");
			transfer(TRANSFER_COMMAND_SHOW_CONTROLS, CURRENT_STATUS_IN_PLAY_ERROR, extra);//"showControls()");
		}

		@Override
		public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
									   float pixelWidthAspectRatio) {
			transfer(TRANSFER_COMMAND_SHUTTER_VIEW, Integer.toString(View.GONE));//"shutterView.setVisibility(View.GONE)");
			transfer(TRANSFER_COMMAND_ASPECT_RATIO, Float.toString(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height));//"videoFrame.setAspectRatio(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height)");
		}
	};
	private static ExoMediaPlayer.CaptionListener mediaPlayerCaptionListener= new ExoMediaPlayer.CaptionListener() {
		// ExoMediaPlayer.CaptionListener implementation
		@Override
		public void onCues(List<Cue> cues) {
			String[] sCues = new String[cues.size()];
			int i=0;
			for(Cue cue : cues)
				sCues[i++] = cue.text.toString();
			transfer(TRANSFER_COMMAND_SET_CUES, sCues);//"subtitleLayout.setCues(cues);");
		}
	};
	private static ExoMediaPlayer.Id3MetadataListener mediaPlayerMetadataListener = new ExoMediaPlayer.Id3MetadataListener() {
		// ExoMediaPlayer.MetadataListener implementation
		@Override
		public void onId3Metadata(List<Id3Frame> id3Frames) {
			for (Id3Frame id3Frame : id3Frames) {
				if (id3Frame instanceof TxxxFrame) {
					TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
					Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
							txxxFrame.description, txxxFrame.value));
				} else if (id3Frame instanceof PrivFrame) {
					PrivFrame privFrame = (PrivFrame) id3Frame;
					Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
				} else if (id3Frame instanceof GeobFrame) {
					GeobFrame geobFrame = (GeobFrame) id3Frame;
					Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
							geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
				} else {
					Log.i(TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
				}
			}
		}

	};

	public static File prepareNewFile(String path, String prefix, String ext) {
		File dest = null;
		FileOutputStream out = null;
		File sd = new File( getAvailableForStoreDirectory()+ "/"+path);
		boolean success = true;
		if (!sd.exists())
			success = sd.mkdirs();
		int count = 0;
		if(success)
		while (count <10) {
			String fileName = (prefix != null? prefix:"") +(new SimpleDateFormat("yyyyMMdd_HHmmss")).format(new Date()) +(count > 0 ? String.format(" (%1$d)", count) : "") + "."+ ext;
			if(!(dest = new File(sd, fileName)).exists())
				break;
		}
		return dest;
	}
	public static File getAvailableForStoreDirectory(){
		return mWriteAccessGrunted ? Environment.getExternalStorageDirectory() : Environment.getDataDirectory();
	}

	private static boolean mWriteAccessGrunted = true;
	private boolean mAudioAccessGrunted = true;
	public void informAboutPermission(int myPermissions, boolean grunted) {
		switch (myPermissions) {
			case MainTabbedActivity.MY_PERMISSIONS_REQUEST_WRITE:
				mWriteAccessGrunted = grunted;
				break;
			case MainTabbedActivity.MY_PERMISSIONS_REQUEST_MICROPHONE:
				mAudioAccessGrunted = grunted;
				break;
		}
	}
	public void handlingAudioRecording(int action, String path, String prefix) {
		handlingAudioRecording(mediaPlayer, action, path, prefix);
	}
	public void handlingAudioRecording(ExoMediaPlayer player, int action, String path, String prefix){
		if(player != null) {
			RecordableUriDataSource dataSourceForRecording = (RecordableUriDataSource) player.getDataSource();
			if (dataSourceForRecording != null) {
				switch (action) {
					case AUDIO_RECORDING_START:
						if(path != null)
							dataSourceForRecording.prepare(currentlyPlayingInfo.uri, path, prefix, "mp3", fileIsReadyListener);
						break;
					case AUDIO_RECORDING_RESTART:
						dataSourceForRecording.recordingStart();
						break;
					case AUDIO_RECORDING_PAUSE:
						dataSourceForRecording.recordingPause();
						break;
					case AUDIO_RECORDING_STOP:
						dataSourceForRecording.recordingStop();
						break;
				}
			}
		}
	}
	private FileIsReadyListener fileIsReadyListener= new FileIsReadyListener() {
		@Override
		public void onFileReady(String fileName, String title, int cause, String url) {
			(new Thread(new RunMediaScanner(url, fileName, title, cause))).start();
		}
	};
	private HashMap<String, AudioRecordRunnable> mAudioRecordingMap = new HashMap<>();
	private HashMap<String, Thread> mAudioRecordingTreadMap = new HashMap<>();
	public void handlingUrlStreaming(int action, PlayingInfo definition, String path) {
		String url = definition.uri, prefix = definition.name;
		AudioRecordRunnable audioRecordRunnable = mAudioRecordingMap.get(url);
		if(audioRecordRunnable == null)
			mAudioRecordingMap.put(url, audioRecordRunnable = new AudioRecordRunnable(url, fileIsReadyListener));
		switch (action) {
			case AUDIO_RECORDING_START:
				if(path != null) {
					if (audioRecordRunnable.prepare(path, prefix, "mp3")) {
						Thread thread = new Thread(audioRecordRunnable);
						mAudioRecordingTreadMap.put(url, thread);
						thread.start();
					}
				}
				break;
			case AUDIO_RECORDING_RESTART:
				audioRecordRunnable.pauseRecording(false);
				break;
			case AUDIO_RECORDING_PAUSE:
				audioRecordRunnable.pauseRecording(true);
				break;
			case AUDIO_RECORDING_STOP:
				audioRecordRunnable.finishRecording();
				mAudioRecordingTreadMap.put(url, null);
		}
	}
	public final static int AUDIO_RECORDING_RESTART = 3;
	public final static int AUDIO_RECORDING_STOP = 2;
	public final static int AUDIO_RECORDING_PAUSE = 1;
	public final static int AUDIO_RECORDING_START = 0;

	public final static int WAITS_FOR_CONNECTIVITY = 1000;
	public final static int MAX_COUNT_WAITS_FOR_CONNECTIVITY= 120;
	public final static int WAIT_FOR_DATA = 250;
	public final static int MAX_COUNT_WAITS_FOR_DATA = 4;

	public static int mMaxRecordingSize = 8 *1024 *1024;

	/**
	 *
	 */
/*
	public static String setID3TagsData(String absolutePath) {
		String[] split = absolutePath.split("/");//sss.../name/name_date.mp3w
		String tag = (split.length > 2 ? split[split.length - 2] : "");
		return setID3TagsData(1, absolutePath, tag);
	}
	public static String setID3TagsData(String absolutePath, String title) {
		return setID3TagsData(1, absolutePath, title);
	}
	public static String setID3TagsData(long count, String absolutePath, String title) {
		File fileToDelete = new File(absolutePath);
		if(count > 0 && absolutePath.endsWith(EXT_MODIFIER)) {
			Mp3File mp3file = null;
			ID3v1 id3v1Tag;
			try {
				mp3file = new Mp3File(absolutePath);
				if (mp3file.hasId3v1Tag()) {
					id3v1Tag = mp3file.getId3v1Tag();
				} else {
					// mp3 does not have an ID3v1 tag, let's create one..
					id3v1Tag = new ID3v1Tag();
					mp3file.setId3v1Tag(id3v1Tag);
				}
				id3v1Tag.setTitle(title);
				id3v1Tag.setArtist(applicationName);
				id3v1Tag.setAlbum(applicationName);
				id3v1Tag.setYear("2001");
				id3v1Tag.setGenre(12);
				id3v1Tag.setComment("Some comment");
				absolutePath = absolutePath.substring(0, absolutePath.length() - EXT_MODIFIER.length());
				mp3file.save(absolutePath);
			} catch (IOException e) {
				fileToDelete = null;
				e.printStackTrace();
			} catch (UnsupportedTagException e) {
				fileToDelete = null;
				e.printStackTrace();
			} catch (InvalidDataException e) {
				fileToDelete = null;
				e.printStackTrace();
			} catch (NotSupportedException e) {
				fileToDelete = null;
				e.printStackTrace();
			}
		}
		if(fileToDelete != null && fileToDelete.exists())
			fileToDelete.delete();
		return absolutePath;
	}
*/
	private class RunMediaScanner implements Runnable{
		public RunMediaScanner(String url, String fileName, String title, int cause){
			mFileName = fileName;
			mTitle = title;
			mCause = cause;
			mUrl = url;
		}
		private String mUrl;
		private String mFileName;
		private String mTitle;
		private int mCause;
		@Override
		public void run() {
			if(mFileName != null){
				sendMessageToTheClients(mUrl, mCause);
				//String newFileName = setID3TagsData(mFileName, mTitle);
				MediaScannerConnection.scanFile(getApplicationContext(), new String[]{mFileName}, null, scanCompleteListener);
			}
		}
	}
	private MediaScannerConnection.OnScanCompletedListener scanCompleteListener = new MediaScannerConnection.OnScanCompletedListener() {
		@Override
		public void onScanCompleted(String s, Uri uri) {
			//do nothing so far
		}
	};
	public static boolean closeOutputFile(BufferedOutputStream bufferedOutput, String title, int cause, String url, String fileName, FileIsReadyListener fileIsReadyListener){
		if(bufferedOutput != null)
			try {
				ID3v1 id3v1Tag = new ID3v1Tag();
				id3v1Tag.setTitle(title);
				id3v1Tag.setArtist(MediaPlayerService.applicationName);
				id3v1Tag.setAlbum(MediaPlayerService.applicationName);
				try {
					bufferedOutput.write(id3v1Tag.toBytes());
				} catch (NotSupportedException e) {
					e.printStackTrace();
				}
				bufferedOutput.flush();
				bufferedOutput.close();
				if(fileIsReadyListener != null)
					fileIsReadyListener.onFileReady(fileName, title, cause, url);
				bufferedOutput = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		return (bufferedOutput == null);
	}
	public interface FileIsReadyListener {
		void onFileReady(String fileName, String mPrefix, int cause, String url);
	}
	public static Button setButtonByDefinition(Button button, PlayingInfo def){
		button.setText(def.name);
		button.setTag(TAG_URI, def.uri);
		button.setTag(TAG_TYPE, def.type);
		button.setTextSize(TypedValue.COMPLEX_UNIT_PT, mPrefFontSize);
		return button;
	}
	/*
	public static PlayingInfo serializeSelectedStation(Button station){
		return serializeSelectedStation((String) station.getText(), station.getTag(TAG_URI).toString(),String.valueOf( station.getTag(TAG_TYPE)));
				//station.getTag(TAG_URI).toString(),
				//(String) station.getText(),
				//String.valueOf( station.getTag(TAG_TYPE)));
	}
	public static PlayingInfo serializeSelectedStation(String url, String label, String type){
		PlayingInfo def = new PlayingInfo(label, url,type);
		//def[DEF_POS_URL] = url;
		//def[DEF_POS_LABEL] =label;
		//def[DEF_POS_TYPE] =type;
		return def;
	}
	public static PlayingInfo deSerializePrefStationDefinition(String[] savedInstanceStringsArray) {
		return new PlayingInfo(savedInstanceStringsArray[2],savedInstanceStringsArray[1],savedInstanceStringsArray[0]);
	}
	public static PlayingInfo deSerializePrefStationDefinition(String savedPrefString) {
		PlayingInfo def = null;
		if (savedPrefString != null) {
			int pos = savedPrefString.indexOf(' ');
			if (pos > 0) {
				int pos1 = savedPrefString.indexOf(' ', pos + 1);
				if (pos1 > 0) {
					def = new PlayingInfo(savedPrefString.substring(pos1 + 1), savedPrefString.substring(pos + 1, pos1),savedPrefString.substring(0, pos));
					//def[DEF_POS_TYPE] = savedPrefString.substring(0, pos);
					//def[DEF_POS_URL] = savedPrefString.substring(pos + 1, pos1);
					//def[DEF_POS_LABEL] = savedPrefString.substring(pos1 + 1);
				}
			}
		}
		return def;
	}
	public static String serializePrefStationDefinition(PlayingInfo def){
		String res ;
		if(def != null)
			res = (new StringBuffer(def.type).append(' ').append(def.uri).append(' ').append(def.name)).toString();
		else
			res = null;
		return res;
	}
	*/
	public static class PlayingInfo implements Comparator<PlayingInfo> {

		public String name = null;
		public String contentId = "";
		public String provider = "";
		public String uri = null;
		public int type = Util.TYPE_OTHER;
		public long lastModified = 0;
		public long length = 0;
		private Integer groupId = null;
		//private Integer associatedViewId = null;
		//private Integer groupViewId = null;

		public PlayingInfo(String name, String uri, String type) {
			this(name, uri, safeConvertType(type),0);
		}

		public PlayingInfo(Button station) {
			this.name = (String) station.getText();
			this.uri = station.getTag(TAG_URI).toString();
		}

		private static int safeConvertType(String type){
			int intType = Util.TYPE_OTHER;
			try {
				intType = Integer.parseInt(type);
			}catch(Exception e) {
				e.printStackTrace();
			}
			return intType;
		}
		public PlayingInfo(String serializedPlayInfo) {
			if (serializedPlayInfo != null) {
				int pos = serializedPlayInfo.indexOf(' ');
				if (pos >= 0) {
					int pos1 = serializedPlayInfo.indexOf(' ', pos + 1);
					if (pos1 > 0) {
						this.name = serializedPlayInfo.substring(pos1 + 1);
						this.uri = serializedPlayInfo.substring(pos + 1, pos1);
						this.type = safeConvertType(serializedPlayInfo.substring(0, pos));
						return;
					}
				}
			}
			if(serializedPlayInfo.equals(("3 null "))){
				this.type = Util.TYPE_OTHER;
				this.uri = "http://oggvorbis.tb-stream.net:80/technobase.ogg";
				this.name = "Ogg Radio";
				return;
			}

			this.name = serializedPlayInfo;
			Exception e = new Exception("Wrong string to desirialize:"+serializedPlayInfo);
			e.printStackTrace();
		}
		public PlayingInfo(String[] playInfoStringArray) {
			if (playInfoStringArray != null && playInfoStringArray.length > 2) {
				this.name = playInfoStringArray[2];
				this.uri = playInfoStringArray[1];
				this.type = safeConvertType(playInfoStringArray[0]);
			}else{
				this.name = playInfoStringArray !=null ? playInfoStringArray[0] :"";
				Exception e = new Exception("Wrong string array to desirialize:" + (playInfoStringArray== null?"null":"length="+playInfoStringArray.length));
				e.printStackTrace();
			}
		}
		public PlayingInfo(String name, String uri, int type) {
			this(name, uri, type,0);
		}
		public PlayingInfo(String name, String uri, int type, long lastModifieds) {
			this(name, name.toLowerCase(Locale.US).replaceAll("\\s", ""), "", uri, type,lastModifieds);
		}

		public PlayingInfo(String name, String contentId, String provider, String uri, int type, long lastModified) {
			this.name = name;
			this.contentId = contentId;
			this.provider = provider;
			this.uri = uri;
			this.type = type;
			this.lastModified = lastModified;
		}
		public boolean isValid(){
			return (this.uri != null && !this.uri.isEmpty());
		}

		@Override
		public int compare(PlayingInfo t1, PlayingInfo t2) {
			if(t1.lastModified>t2.lastModified)
				return 1;
			else if(t2.lastModified>t1.lastModified)
				return -1;
			else
				return 0;
		}
		public boolean equalTo(PlayingInfo t1){
			return t1==null ? false : this.uri.equals(t1.uri);
		}
		@Override
		public Comparator<PlayingInfo> reversed() {
			return null;
		}
		public String serialize(){
			return String.valueOf(this.type) + " "+ this.uri+" "+ this.name;
		}
		public String[] serializeToStringArray(){
			String[] res = {String.valueOf(this.type), this.uri, this.name};
			return res;
		}
		public static PlayingInfo getInstance(String name, File file){
			long length;
			PlayingInfo item = null;
			if(file !=null && file.exists()&& file.isFile() && (length = file.length())>0){
				item = new PlayingInfo(name, "", "file", file.getAbsolutePath(), Util.TYPE_OTHER, file.lastModified());
				item.length = length;
			}
			return item;
		}

		public void setGroupId(int groupId) {
			this.groupId = groupId;
		}

		public Integer getGroupId() {
			return this.groupId;
		}

		public static PlayingInfo getInstance(String string) {
			return string != null && !string.startsWith("3 null ") ? new PlayingInfo(string) : null;
		}

		public static PlayingInfo getInstance(String[] stringArray) {
			return stringArray != null && stringArray.length>2 ? new PlayingInfo(stringArray):null;
		}

		public static PlayingInfo getInstance(Button radioStation) {
			return radioStation != null ? new PlayingInfo(radioStation): null;
		}
		/*
		public int setViewId(int viewId) {
			return (associatedViewId = viewId);
		}
		public Integer getViewId() {
			return associatedViewId;
		}

		public void setGroupViewId(int groupViewId) {
			this.groupViewId = groupViewId;
		}
		public Integer getGroupViewId() {
			return this.groupViewId;
		}
		*/
	}

}
