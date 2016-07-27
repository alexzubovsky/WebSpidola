package com.example.android.webspidola;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.android.webspidola.player.DashRendererBuilder;
import com.example.android.webspidola.player.EventLogger;
import com.example.android.webspidola.player.ExoMediaPlayer;
import com.example.android.webspidola.player.ExtractorRendererBuilder;
import com.example.android.webspidola.player.HlsRendererBuilder;
import com.example.android.webspidola.player.SmoothStreamingRendererBuilder;
import com.example.android.webspidola.player.SmoothStreamingTestMediaDrmCallback;
import com.example.android.webspidola.player.WidevineTestMediaDrmCallback;
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

import java.util.List;

public class MediaPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {


	private boolean playerNeedsPrepare;

	// Binder given to clients
	private final IBinder mBinder = new LocalMPBinder();
	public static final String CUSTOM_EVENT = "media-player-service-name";
	public static final String CUSTOM_INTENT = "com-example-android-webspidola-mediaservice";
	public static final String CUSTOM_EVENT_MESSAGE = "message";
	public static final String CUSTOM_EVENT_CATEGORY = "category";
	public static final int ADD_RADIO_NAME = 1;
	public static final int ON_PREPARED_FINISHED = 2;
	public static final String ACTION_PLAY = "com.example.android.webspidola.action_play";
	public static final String ACTION_PAUSE = "com.example.android.webspidola.action_pause";
	public static final String ACTION_EXIT = "com.example.android.webspidola.action_exit";
	private final static int NO_CURRENT_HEADSET_STATE = -1;
	private final static int HEADSET_STATE_DISCONNECTED = 0;
	private final static int HEADSET_STATE_CONNECTED = 1;
	public final static String CURRENT_STATUS_IN_PAUSE = "Pause";
	public final static String CURRENT_STATUS_IN_PLAY = "Playing";
	public final static String CURRENT_STATUS_IN_STOP = "Stopped";
	public final static String CURRENT_STATUS_CHANGED = "Changed";
	public final static String CURRENT_STATUS_IN_PLAY_ERROR = "Error";
	/*private final int IS_NULL = -1;
	private final int ON_STOP = 0;
	private final int ON_PAUSE = 1;
	private final int ON_STARTED = 2;
	private final int ON_PREPARE = 3;
	private final int ON_STOP_BY_ERROR = 4;
	*/
	private final int NOTIFICATION_ID = 1;
	private final int NO_CONNECTIVITY_SET = -1;
	private final Integer CONNECTIVITY_NO =null;
	private static final String TAG = "ExoPlayerService";

	//PlayerActivityHelper
	private ExoMediaPlayer mediaPlayer = null;

	private MediaController mediaController;
	private EventLogger eventLogger;
	//PlayerActivityHelper

	private NotificationManagerCompat mNotificationManager = null;
	private String currentlyPlayingUrl = null;
	private String preparingUrl = null;
	private String selectedStationName = null;
	private int mCurrentHeadsetState = NO_CURRENT_HEADSET_STATE;
	//private int mediaPlayerRegisteredState = IS_NULL;
	private WifiManager.WifiLock mWifiLoc = null;
	private ConnectivityManager mConnectManager = null;
	private Integer mCurrentConnectivity = Integer.MAX_VALUE;
	private TaskStackBuilder stackBuilder = null;
	private long playerPosition;
	private int mCurrentConnectivityType =-1;
	private boolean mMonitorHeadphones = true;
	private boolean mMonitorBluetooth = false;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
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
	}
	private boolean tempPauseOnCall = false;
	@Override
	public void onAudioFocusChange(int focusChange) {
		if(focusChange<=0) {//LOSS -> PAUSE
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
	public class BroadcastNotificationReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			String action = intent.getAction();
			if (action != null) {
				switch (action) {
					case ACTION_EXIT:
						logAndBroadcast("i",ACTION_EXIT);
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
									if (mediaPlayer != null && isPlaying())
										pausePlay();
								} else if (state == HEADSET_STATE_CONNECTED && isOnPause()) {
									reStartPlay();
									logAndBroadcast("i", "Headset '" + intent.getStringExtra("name") + "' connected");
								}
							}
						}
						mCurrentHeadsetState = state;
						break;
					case Intent.ACTION_DEVICE_STORAGE_LOW:
					case Intent.ACTION_DEVICE_STORAGE_OK:
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
						break;
					default:
						break;
				}
			}
		}
	}

	final Handler restartPlayHandler = new Handler();// Timeout Handler to Restart Play after some time
	final Runnable restartPlayRunnable = new Runnable() { @Override public void run() { reStartPlay(); }};// Restart Play after some time
	public void setHeadphoneMonitoring(boolean monitor){
		mMonitorHeadphones = monitor;
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
	private void engageNotificationBar(String text, NotificationCompat.Action[] actions) {
		//Notification Area content
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setSmallIcon(R.drawable.spidola64);
		mBuilder.setContentTitle("Web Spidola");
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		if(stackBuilder == null)
			stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		//stackBuilder.addParentStack(NotificationAreaActivity.class);
		// Creates an explicit intent for an return to MainActivity in case of click
		Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
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
		mNotificationManager = NotificationManagerCompat.from(this);//(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);//Notification Manager
		mNotificationManager.notify(NOTIFICATION_ID, notification);// mId allows you to update the notification later on.
		//End building Notification Area content
	}
	private void engageNotificationBarAsPlaying(String text) {
		NotificationCompat.Action[] actions = {generateAction(R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)};
		engageNotificationBar("Playing:" + text, actions);
		/*try{
			actions[0].actionIntent.send();
		}catch(Exception e){
			Log.e("engageNotificationBarAsPlaying", e.toString());
		}*/
	}
	private int mCurrentPlayingStatus;
	public int getCurrentPlayingStatus(){return mCurrentPlayingStatus;}
	public final static int CURRENT_PLAYING_STATUS_NONE = 0;
	public final static int CURRENT_PLAYING_STATUS_PLAY = 1;
	public final static int CURRENT_PLAYING_STATUS_PAUSE = 2;
	private void engageNotificationBarAsPaused(String text) {
		NotificationCompat.Action[] actions = {generateAction(R.drawable.ic_media_play, "Play", ACTION_PLAY)};
		mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_PLAY;
		engageNotificationBar("Paused:" + text, actions);
	}
	private void engageNotificationBarAsConnecting(String text) {
		NotificationCompat.Action[] actions = {generateAction(R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)};
		mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_PAUSE;
		engageNotificationBar("Connectiing to:"+ text, actions);
	}
	private void engageNotificationBarAsStopped() {
		NotificationCompat.Action[] actions;
		if(selectedStationName!=null ) {
			actions = new NotificationCompat.Action[]{generateAction(R.drawable.ic_media_play, "Play", ACTION_PLAY)};
			mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_PLAY;
		}
		else {
			actions = new NotificationCompat.Action[]{};
			mCurrentPlayingStatus = CURRENT_PLAYING_STATUS_NONE;
		}

		engageNotificationBar("Not Playing", actions);
	}
	private NotificationCompat.Action generateAction( int icon, String title, String intentAction ) {

		/* */Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class );
		intent.setAction(intentAction);
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 2, intent, 0);
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
	private boolean isMediaPlayerReadyToContinue(){
		//if (mediaPlayerRegisteredState != ON_STOP && mediaPlayerRegisteredState != ON_STOP_BY_ERROR && !isPlaying() && url == currentlyPlayingUrl) {
		return mediaPlayer != null && mediaPlayer.getPlaybackState() == ExoPlayer.STATE_READY && !mediaPlayer.getPlayerControl().isPlaying() && mediaPlayer.getRenderedUri().equals(Uri.parse(currentlyPlayingUrl));
	}
	public void reStartPlay() {
		if(isMediaPlayerReadyToContinue()){//mediaPlayerRegisteredState == ON_PAUSE) {
			//mediaPlayerRegisteredState = ON_STARTED;
			if(playerNeedsPrepare){
				mediaPlayer.prepare();
				playerNeedsPrepare = false;
			}
			mediaPlayer.getPlayerControl().start();//start-resume
			engageNotificationBarAsPlaying(selectedStationName);
			logAndBroadcast("i", "Resume "+CURRENT_STATUS_IN_PLAY + " after pause");
			return;
		}
		else
			startPlay();
	}
	public void startPlay() {
		if(currentlyPlayingUrl != null && selectedStationName != null)
			startPlay(currentlyPlayingUrl, selectedStationName);
	}
	public void startPlay(String url, String stationName){
		startPlay(url, stationName, Util.TYPE_OTHER);
	}
	public void startPlay(String url, String stationName, int type){
		try{
			if(mediaPlayer != null) {
				if (isMediaPlayerReadyToContinue()) {
					//mediaPlayerRegisteredState = ON_STARTED;
					mediaPlayer.getPlayerControl().start();//start-resume
					engageNotificationBarAsPlaying(selectedStationName);
					logAndBroadcast("i", "Start "+CURRENT_STATUS_IN_PLAY + " after pause");
					return;
				}
				else
					logAndBroadcast("d", CURRENT_STATUS_IN_PLAY + " when mediaPlayer.getPlaybackState=" + (new Integer(mediaPlayer.getPlaybackState())).toString() + "mediaPlayer.getPlayerControl().isPlaying(="+mediaPlayer.getPlayerControl().isPlaying());
				releasePlayer();
			}
			//else
				//mediaPlayerRegisteredState = IS_NULL;
		} catch (Exception eOnResume){
			logAndBroadcast("e", "Resume after pause " + eOnResume.getMessage());
		}
		//Start or resume with new url
		selectedStationName = stationName;
		currentlyPlayingUrl = url;
		preparePlayer(true);//getStoppedMediaPlayer();
		//mediaPlayerRegisteredState = ON_PREPARE;
		engageNotificationBarAsConnecting(selectedStationName);
		logAndBroadcast("o", "Preparing");
		/*try {
			selectedStationName = stationName;
			currentlyPlayingUrl = null;

			mediaPlayer.reset();
			mediaPlayer.setDataSource(preparingUrl = url);
			mediaPlayer.setLooping(false);//No Looping for streaming content
			mediaPlayer.prepareAsync();
			mediaPlayerRegisteredState = ON_PREPARE;
			engageNotificationBarAsConnecting(selectedStationName);
			logAndBroadcast("o", "Preparing");
		} catch (IllegalArgumentException | IOException e) {
			logAndBroadcast("e", e.getMessage());
		}*/
	}

	public boolean pausePlay() {
		boolean paused;
		if (paused = (mediaPlayer != null && isPlaying())) {
			//mediaPlayerRegisteredState = ON_PAUSE;
			mediaPlayer.getPlayerControl().pause();//pause
			logAndBroadcast("i", CURRENT_STATUS_IN_PAUSE);
			engageNotificationBarAsPaused(selectedStationName);
		}
		//else mediaPlayerRegisteredState = IS_NULL;
		return paused;
	}
	public void stopPlay() {
		if (mediaPlayer != null) {
			selectedStationName = null;//mediaPlayerRegisteredState = state;
			mediaPlayer.getPlayerControl().pause();
			releasePlayer();
			engageNotificationBarAsStopped();
			logAndBroadcast("i", CURRENT_STATUS_IN_STOP);
		}
	}
	public String whatIsPlaying(){
		boolean playing = mediaPlayer!=null && (isPlaying() || isMediaPlayerReadyToContinue());//mediaPlayerRegisteredState == ON_PAUSE));
		return playing ? selectedStationName : null;
	}
	private void logAndBroadcast(String category, String mess){
		int extra = 0;
		switch(category) {
			case "e":Log.e("onError", mess);break;
			case "o":
				extra = ADD_RADIO_NAME;
			case "i":
				Log.i("onInfo", mess);
				break;
		}
		sendMessageToTheClients(mess, extra);
	}

	private void sendMessageToTheClients(String msg, int extra) {
		Log.d("sender", "Broadcasting message:msg="+msg+",extra="+extra);
		Intent intent = new Intent(CUSTOM_EVENT);
		intent.putExtra(CUSTOM_EVENT_MESSAGE, msg);// Include some extra data.
		intent.putExtra(CUSTOM_EVENT_CATEGORY, extra);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(this.getClass().getName(), "UNBIND");
		return true;
	}
	@Override
	public void onDestroy(){
		mAudioManager.abandonAudioFocus(this);
		unregisterReceiver(mMessageReceiver);
		restartPlayHandler.removeCallbacks(restartPlayRunnable);
		if(mediaPlayer!=null) {
			releasePlayer();
		}
		if(mWifiLoc != null) {//initialized
			mWifiLoc.release();
			mWifiLoc=null;
		}
		if(mNotificationManager!=null) {
			mNotificationManager.cancel(NOTIFICATION_ID);
			mNotificationManager = null;
		}
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
	private ExoMediaPlayer.RendererBuilder getRendererBuilder(String contentUrl)  {
		return getRendererBuilder(Util.TYPE_OTHER, Uri.parse(contentUrl));
	}
	private ExoMediaPlayer.RendererBuilder getRendererBuilder(int contentType, Uri contentUri) {
		return getRendererBuilder(contentType, contentUri, "");
	}
	private ExoMediaPlayer.RendererBuilder getRendererBuilder(int contentType, Uri contentUri, String contentId) {
		return getRendererBuilder(contentType, contentUri, contentId, "");
	}
	private ExoMediaPlayer.RendererBuilder getRendererBuilder(int contentType, Uri contentUri, String contentId, String provider) {
		String userAgent = Util.getUserAgent(this, getString(R.string.exo_player_name));
		switch (contentType) {
			case Util.TYPE_SS:
				return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
						new SmoothStreamingTestMediaDrmCallback());
			case Util.TYPE_DASH:
				return new DashRendererBuilder(this, userAgent, contentUri.toString(),
						new WidevineTestMediaDrmCallback(contentId, provider));
			case Util.TYPE_HLS:
				return new HlsRendererBuilder(this, userAgent, contentUri.toString());
			case Util.TYPE_OTHER:
				return new ExtractorRendererBuilder(this, userAgent, contentUri);
			default:
				throw new IllegalStateException("Unsupported type: " + contentType);
		}
	}

	private Uri contentUri;
	private int contentType;
	private String contentId;
	private String provider;
	private final static int TRANSFER_COMMAND_SHOW_CONTROLS = 1;
	private final static int TRANSFER_COMMAND_BUTTONS_VISIBILITY =2;
	private final static int TRANSFER_COMMAND_UPDATE_TEXT =3;
	private final static int TRANSFER_COMMAND_SHUTTER_VIEW = 4;
	private final static int TRANSFER_COMMAND_ASPECT_RATIO =5;
	private final static int TRANSFER_COMMAND_SET_CUES = 6;
	private void transfer(int command, String... params){
		String param = (params != null && params.length > 0 ? params[0] :"");
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
				logAndBroadcast("e", command + " " + param);
				break;
			case TRANSFER_COMMAND_BUTTONS_VISIBILITY:
				switch(param) {
					case CURRENT_STATUS_IN_STOP:
						engageNotificationBarAsStopped();
						break;
					case CURRENT_STATUS_IN_PLAY:
						engageNotificationBarAsPlaying(selectedStationName);
						break;
					case CURRENT_STATUS_IN_PAUSE:
						engageNotificationBarAsPaused(selectedStationName);
				}
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
	private void preparePlayer(boolean playWhenReady) {
		if (mediaPlayer == null) {
			mediaPlayer = new ExoMediaPlayer(getRendererBuilder(currentlyPlayingUrl), Uri.parse(currentlyPlayingUrl));
			mediaPlayer.addListener(new ExoMediaPlayer.Listener() {
				// ExoMediaPlayer.Listener implementation
				@Override
				public void onStateChanged(boolean playWhenReady, int playbackState) {
					if (playbackState == ExoPlayer.STATE_ENDED) {
						transfer(TRANSFER_COMMAND_SHOW_CONTROLS, CURRENT_STATUS_IN_STOP);
					}
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
							//mediaPlayerRegisteredState =mediaPlayer.getPlayWhenReady() ? ON_PREPARED_FINISHED : ON_PAUSE;
							status = currentlyPlayingUrl != null ? CURRENT_STATUS_IN_PLAY : CURRENT_STATUS_IN_STOP;
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
					if (e instanceof UnsupportedDrmException) {
						// Special case DRM failures.
						UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
						errorString = getString(Util.SDK_INT < 18 ? R.string.error_drm_not_supported
								: unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
								? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
					} else if (e instanceof ExoPlaybackException
							&& e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
						// Special case for decoder initialization failures.
						MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
								(MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
						if (decoderInitializationException.decoderName == null) {
							if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
								errorString = getString(R.string.error_querying_decoders);
							} else if (decoderInitializationException.secureDecoderRequired) {
								errorString = getString(R.string.error_no_secure_decoder,
										decoderInitializationException.mimeType);
							} else {
								errorString = getString(R.string.error_no_decoder,
										decoderInitializationException.mimeType);
							}
						} else {
							errorString = getString(R.string.error_instantiating_decoder,
									decoderInitializationException.decoderName);
						}
					}
					if (errorString != null) {
						Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
					}
					playerNeedsPrepare = true;
					//mediaPlayerRegisteredState = ON_STOP_BY_ERROR;
					transfer(TRANSFER_COMMAND_BUTTONS_VISIBILITY, null);//"updateButtonVisibilities()");
					transfer(TRANSFER_COMMAND_SHOW_CONTROLS, CURRENT_STATUS_IN_PLAY_ERROR);//"showControls()");
				}

				@Override
				public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
											   float pixelWidthAspectRatio) {
					transfer(TRANSFER_COMMAND_SHUTTER_VIEW, Integer.toString(View.GONE));//"shutterView.setVisibility(View.GONE)");
					transfer(TRANSFER_COMMAND_ASPECT_RATIO, Float.toString(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height));//"videoFrame.setAspectRatio(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height)");
				}
			});
			mediaPlayer.setCaptionListener(new ExoMediaPlayer.CaptionListener() {
				// ExoMediaPlayer.CaptionListener implementation
				@Override
				public void onCues(List<Cue> cues) {
					String[] sCues = new String[cues.size()];
					int i=0;
					for(Cue cue : cues)
						sCues[i++] = cue.text.toString();
					transfer(TRANSFER_COMMAND_SET_CUES, sCues);//"subtitleLayout.setCues(cues);");
				}
			});
			mediaPlayer.setMetadataListener(new ExoMediaPlayer.Id3MetadataListener() {
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

			});
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

	private void releasePlayer() {
		if (mediaPlayer != null) {
			//debugViewHelper.stop();
			//debugViewHelper = null;
			playerPosition = mediaPlayer.getCurrentPosition();
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
}
