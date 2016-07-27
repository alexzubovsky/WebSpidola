package com.example.android.webspidola;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer.util.Util;

import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
	private static final int RESULT_SETTINGS = 1;

	//AudioManager am;
	/**
	 * Media playback buttons such as play, pause, stop, skip, and previous are available on some handsets and many connected or wireless headsets.
	 * Whenever a user presses one of these hardware keys, the system broadcasts an intent with the ACTION_MEDIA_BUTTON action.
	 * To respond to media button clicks, you need to register a BroadcastReceiver in your manifest that listens for this action broadcast
	 * The receiver implementation itself needs to extract which key was pressed to cause the broadcast.
	 * The Intent includes this under the EXTRA_KEY_EVENT key, while the KeyEvent class includes a list KEYCODE_MEDIA_* static constants
	 * that represents each of the possible media buttons, such as KEYCODE_MEDIA_PLAY_PAUSE and KEYCODE_MEDIA_NEXT
	 */
	RemoteControlReceiver rcr = new RemoteControlReceiver();//see http://developer.android.com/training/managing-audio/volume-playback.html
	private View selectedStation = null;

	final String[][] aButtonsDef = {
			{"Echo Moscow", "http://stream05.media.rambler.ru/echo.mp3", new Integer(Util.TYPE_OTHER).toString()},
			{"Echo Moscow LQ", "http://95.79.31.115:8000/", new Integer(Util.TYPE_OTHER).toString()},
			{"Echo Moscow LQ DNS", "http://054026.nn.ru:8000/", new Integer(Util.TYPE_OTHER).toString()},
			{"Mayak", "http://icecast.vgtrk.cdnvideo.ru/mayakfm_mp3_64kbps", new Integer(Util.TYPE_OTHER).toString()},
			{"Davidzon Radio", "http://ags.abinet.com:8800/dr.mp3", new Integer(Util.TYPE_OTHER).toString()},
			{"MDS Radio", "http://stream.kazancity.net:8000/27-mds", new Integer(Util.TYPE_OTHER).toString()},
			{"WIOQ FM", "http://wioq-fm.akacast.akamaistream.net/7/247/20056/v1/auth.akacast.akamaistream.net/wioq-fm", new Integer(Util.TYPE_OTHER).toString()},
			{"Shout cast UK", "http://shoutcast.internet-radio.org.uk:10272/;", new Integer(Util.TYPE_OTHER).toString()},
			{"Ogg Radio", "http://oggvorbis.tb-stream.net:80/technobase.ogg", new Integer(Util.TYPE_OTHER).toString()}
			//{"MDS Radio Station", "http://tunein.com/radio/Mds-station-s144846"},
	};
	/*View popupView = null;
	final PopupWindow infoPopup = new PopupWindow();
	final Runnable popupHideRunnable = new Runnable() {
		@Override
		public void run() {
			dismissInfoPopup();
		}
	};
	final Handler popupHideHandler = new Handler();// Hide after some seconds
	*/
	private static final boolean ACTION_STOP = true;
	private static final boolean ACTION_PLAY = false;
	public static final String HEADPHONE_MONITOR = "headphone_switch";
	private static final String SELECTED_STATION = "SelectedStation";
	private static final String FONT_SIZE = "FontSize";
	private static final String PADDING_TOP = "PaddingTop";
	private MediaPlayerService mService;
	private boolean mBound = false;
	private int RB_fontSize;
	private int RB_paddingTop =20;

	//Methods
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(selectedStation!=null)
			outState.putSerializable(SELECTED_STATION, selectedStation.getTag(TAG_URI).toString());
		outState.putInt(FONT_SIZE, RB_fontSize);
		outState.putInt(PADDING_TOP, RB_paddingTop);
	}
	private static final int  TAG_URI = R.string.TAG_URI;
	private static final int  TAG_TYPE = R.string.TAG_TYPE;
	private SharedPreferences preferences;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = getPlayStopButton();
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				doClickOnStopPlayButton((FloatingActionButton) view, ACTION_PLAY);
			}
		});
		//Context mContext = getApplicationContext();
		//am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);//this.getPreferences(Context.MODE_PRIVATE);
		RB_fontSize = geteIntegerPreference(FONT_SIZE, 11);//preferences.getInt(FONT_SIZE, 11);
		RB_paddingTop = geteIntegerPreference(PADDING_TOP, 20);//preferences.getInt(PADDING_TOP, 20);
		String currentPlayingStationUrl = null;
		if(savedInstanceState!= null)
		{
			currentPlayingStationUrl = savedInstanceState.getString(SELECTED_STATION);
			String prefStation = preferences.getString(SELECTED_STATION,null);
			if(currentPlayingStationUrl != null && prefStation != null && !prefStation.equals(currentPlayingStationUrl)) {
				saveSelectedStationToPreferences(currentPlayingStationUrl);
			}
			else if(prefStation != null)
				currentPlayingStationUrl = prefStation;
		}
		else
			currentPlayingStationUrl = preferences.getString(SELECTED_STATION, null);

		RadioGroup stationsRadioGroup = getStationsRadioGroup();
		RadioButton button;
		int count = 0;
		for(int j=0;5>j;j++) {//to fiil list
			for (int i = 0; aButtonsDef.length > i; i++) {
				button = (RadioButton) getLayoutInflater().inflate(R.layout.template_radiobutton, null);
				button.setText(aButtonsDef[i][0]);
				button.setTag(TAG_URI, aButtonsDef[i][1]);
				button.setTag(TAG_TYPE, aButtonsDef[i][2]);
				button.setId(count * 2);
				RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f);
				button.setLayoutParams(layoutParams);
				button.setTextSize(TypedValue.COMPLEX_UNIT_PT, RB_fontSize);
				button.setPadding(10, RB_paddingTop,4, new Double(RB_paddingTop*1.5).intValue());
				stationsRadioGroup.addView(button);
				if(currentPlayingStationUrl != null){
					if(currentPlayingStationUrl.equalsIgnoreCase(aButtonsDef[i][1])) {
						selectedStation = stationsRadioGroup.getChildAt(stationsRadioGroup.getChildCount() - 1);
						stationsRadioGroup.check(selectedStation.getId());
						currentPlayingStationUrl = null;
					}
				}
				View divider = new View(this, null, R.style.Divider);
				divider.setLayoutParams(new RadioGroup.LayoutParams(LayoutParams.MATCH_PARENT, 1, 1f));
				divider.setId(count * 2 + 1);
				stationsRadioGroup.addView(divider);
				count++;
			}
		}
		//set click listener
		stationsRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				View checkedRadio = findViewById(checkedId);
				selectStation(checkedRadio);
			}
		});
		startService(new Intent(this, MediaPlayerService.class));//make sure service will not stop until stopService issued
	}

	private void saveSelectedStationToPreferences(String currentPlayingStationUrl) {
		SharedPreferences.Editor ed = preferences.edit();
		ed.putString(SELECTED_STATION, currentPlayingStationUrl);
		ed.commit();
	}
	private int geteIntegerPreference(String preferenceName, int value) {
		boolean exists = preferences.contains(preferenceName);
		int savedValue = preferences.getInt(preferenceName, value);
		if(!exists) {
			SharedPreferences.Editor ed = preferences.edit();
			ed.putInt(preferenceName, savedValue);
			ed.commit();
		}
		return savedValue;
	}
	private boolean getBooleanPreference(String preferenceName, boolean value) {
		boolean exists = preferences.contains(preferenceName);
		boolean savedValue = preferences.getBoolean(preferenceName, value);
		if(!exists) {
			SharedPreferences.Editor ed = preferences.edit();
			ed.putBoolean(preferenceName, savedValue);
			ed.commit();
		}
		return savedValue;
	}

	@Override
	protected void onResume(){
		super.onResume();
		IntentFilter intentFilter = new IntentFilter(MediaPlayerService.CUSTOM_EVENT);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter);
		Log.i("OnResume", "registerReceiver(mMessageReceiver, intentFilter=" + intentFilter.toString() + ")");
	}
	// Handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			int type = intent.getIntExtra(MediaPlayerService.CUSTOM_EVENT_CATEGORY, 0);
			String message = intent.getStringExtra(MediaPlayerService.CUSTOM_EVENT_MESSAGE);
			switch(type){
				case MediaPlayerService.ADD_RADIO_NAME:
					message += " " +((RadioButton) selectedStation).getText();
					break;
				case MediaPlayerService.ON_PREPARED_FINISHED:
					showPlayPauseButton();
					break;
			}
			displayMessage("receiver", message);
		}
	};
	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = new Intent(this, MediaPlayerService.class);
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		Log.d("onStart", "bindService:" + intent.toString());
	}

	@Override
	protected void onStop() {
		super.onStop();
		unBindMPService();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unBindMPService();
	}
	@Override
	protected void onPause(){
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onPause();
	}
	private void unBindMPService() {
		if(mBound){
			unbindService(mServiceConnection);
			mBound = false;
		}
	}
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			MediaPlayerService.LocalMPBinder binder = (MediaPlayerService.LocalMPBinder) service;
			mService = binder.getService();
			mBound = true;
			Log.d("onServiceConnected", mService.toString());
			String currentPlayingStationName = mService.whatIsPlaying();
			if(currentPlayingStationName != null && selectedStation == null){
				RadioGroup stationsRadioGroup = getStationsRadioGroup();
				int stations = stationsRadioGroup.getChildCount();
				RadioButton radioButton;
				View child;
				for(int i=0;stations > i;i++){
					if((child = stationsRadioGroup.getChildAt(i)) instanceof RadioButton && (radioButton = (RadioButton) child).getText() == currentPlayingStationName){
						selectedStation = child;
						stationsRadioGroup.check(radioButton.getId());
						break;
					};
				}

			}
			synchronizePlayPauseButton();
			if (mService.isAutoPlayCase() && getBooleanPreference("autoplay_switch", false) && selectedStation != null)
				doClickOnStopPlayButton(getPlayStopButton(), false);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.d("onServiceDisconnected", arg0.toString());
		}
	};
	private FloatingActionButton getPlayStopButton() {
		return (FloatingActionButton) findViewById(R.id.fab);
	}

	private RadioGroup getStationsRadioGroup() {
		return (RadioGroup) findViewById(R.id.stations_radio_group);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch (id) {
			case R.id.action_info:
				StringBuffer message = new StringBuffer();
				if(mBound)
					message.append(mService.getConnectivityStatus().toString());
				if(toastMessage != null)
					message.append('\n').append(toastMessage.getView().getTag().toString());
				showMessageInPopup(message.toString(), Toast.LENGTH_LONG * 10);
				return true;
			case R.id.action_settings:
				Intent i = new Intent(this, SettingsActivity.class);
				startActivityForResult(i, RESULT_SETTINGS);
				return true;
			case R.id.action_add:
				addStationClick(null);
				return true;
			case R.id.action_exit:
				exitApp();
				return true;
			case R.id.action_volume_down:
			case R.id.action_volume_up:
				if(mBound)
					mService.changeVolumeLevel(id==R.id.action_volume_up);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Called when clicked Add station button
	 *
	 * @param view
	 */
	public void addStationClick(View view) {

	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case RESULT_SETTINGS:
				showUserSettings();
				break;

		}

	}
	private void setMonitoringToService() {
		if(mBound) {
			mService.setHeadphoneMonitoring(getBooleanPreference("headphone_switch", true));
			mService.setBluetoothMonitoring(getBooleanPreference("bluetooth_switch", false));
		}

	}
	private void showUserSettings() {
		setMonitoringToService();
		StringBuilder builder = new StringBuilder();
		Map<String,?> map = preferences.getAll();
		Set<String> keys = map.keySet();
		String s;
		for(String key:keys){
			builder.append("\n ").append(key).append('[').append(s=(map.get(key).getClass().getSimpleName())).append(":]");
			switch(s) {
				case "String":
					builder.append(preferences.getString(key, "NULL")); break;
				case "Integer":
					builder.append(preferences.getInt(key, -11111)); break;
				case "Boolean":
					builder.append(preferences.getBoolean(key, false)); break;
			}
		}
		/*
		builder.append("\n Theme: "+ sharedPrefs.getString("themes_list", "NULL"));
		builder.append("\n Font Size: "+ sharedPrefs.getString("FontSize", "NULL"));
		builder.append("\n Pading Top: "+ sharedPrefs.getString("addingTop", "NULL"));
		*/
		TextView settingsTextView = (TextView) findViewById(R.id.textUserSettings);

		settingsTextView.setText(builder.toString());
		changeSettingsViewVisibility(View.VISIBLE);
	}
	/**
	 * makes sure plaing state correctly shown in button's image
	 */
	private void synchronizePlayPauseButton()	{
		FloatingActionButton button = getPlayStopButton();
		if (selectedStation != null) {
			String tag = button.getTag().toString();
			String[] tags = tag.split("\\|");
			boolean isPauseShown = tags[0].indexOf("play") > 0;//means currently playing
			int currentlyPlaying = mService.getCurrentPlayingStatus();//(mService.whatIsPlaying() != null && !mService.isOnPause()) || mService.isOnPrepare();
			boolean doSwitch = false;
			switch(currentlyPlaying){
				case MediaPlayerService.CURRENT_PLAYING_STATUS_NONE:
					if(!isPauseShown)
						doSwitch = true;
					break;
				case MediaPlayerService.CURRENT_PLAYING_STATUS_PAUSE:
					if(isPauseShown)
						doSwitch = true;
					break;
				case MediaPlayerService.CURRENT_PLAYING_STATUS_PLAY:
					if(!isPauseShown)
						doSwitch = true;
					break;
			}
			if (doSwitch) {
				String newTag = tags[1] + "|" + tags[0];
				button.setTag(newTag);
				button.setImageResource(getResources().getIdentifier(tags[1], "drawable", getPackageName()));
			}
			if(mService.isOnPrepare())
				button.setVisibility(View.GONE);
			else if(button.getVisibility() != View.VISIBLE)
				button.setVisibility(View.VISIBLE);
		}else
			button.setVisibility(View.GONE);
	}
	private void showPlayPauseButton() {
		FloatingActionButton button = getPlayStopButton();
		if(button.getVisibility() != View.VISIBLE)
			button.setVisibility(View.VISIBLE);
	}
	public boolean doClickOnStopPlayButton(boolean doStop) {
		return doClickOnStopPlayButton(getPlayStopButton(), doStop);
	}
	/**
	 * Called To Play/Stop
	 *
	 * @param button to toggle image
	 * @param doStop true then stop, false - toggle
	 */
	public boolean doClickOnStopPlayButton(FloatingActionButton button, boolean doStop) {
		boolean result = false;
		if (mBound) {
			if (mService.whatIsPlaying() != null) {//am.abandonAudioFocus(afChangeListener);
				if(doStop)// Call a method from the LocalService.
					mService.stopPlay();
				else if(mService.isOnPause())
					mService.reStartPlay();
				else
					mService.pausePlay();
				result = true;
			} else {
				if(!(result = startPlaying())) {
					displayMessage("Playing did not start");
				}
				/*else{// Request audio focus for playback
					int result = am.requestAudioFocus(afChangeListener,
							AudioManager.STREAM_MUSIC,// Use the music stream.
							AudioManager.AUDIOFOCUS_GAIN// Request permanent focus.
					);

					if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
						am.registerAudioDeviceCallback(rcr);
						///play/ Start playback.
					}
				}*/

			}
		}
		else
			displayMessage("Not Bound with the Media Player Service");
		synchronizePlayPauseButton();//make sure button shows 'ready' to play
		return result;
	}

	private boolean startPlaying() {
		boolean bRes = false;
		String message = null;
		if (selectedStation != null) {
			Object tag = selectedStation.getTag(TAG_URI);
			String url = tag != null ? tag.toString():null;
			int type = Integer.parseInt(selectedStation.getTag(TAG_TYPE).toString());
			if (url != null) {
				if (mBound) {
					getPlayStopButton().setVisibility(View.GONE);
					// Call a method from the LocalService.
					// However, if this call were something that might hang, then this request should
					// occur in a separate thread to avoid slowing down the activity performance.
					mService.startPlay(url, ((RadioButton) selectedStation).getText().toString(), type);
					saveSelectedStationToPreferences(url);
					bRes= true;
				}
				else {
					message = "No Media Player Service";
				}
			} else {
				message = "No URL for selected station";
			}
		} else
			message = "No selected station";
		if (!bRes) {
			unselectStation();
			displayMessage(message != null ? message : "No message");
		}
		getPlayStopButton().setVisibility(View.VISIBLE);
		return bRes;
	}

	private void displayMessage(String message) {
		displayMessage("i", "Start Playing", message);
	}

	private void displayMessage(String tag, String message) {
		displayMessage("i", tag, message);
	}

	private void displayMessage(String category, String tag, String message) {
		switch (category.toLowerCase()) {
			case "d":
				Log.d(tag, message);
				break;
			case "i":
				Log.i(tag, message);
				switch(message.split(" ")[0]){
					case MediaPlayerService.CURRENT_STATUS_IN_PAUSE:
					case MediaPlayerService.CURRENT_STATUS_IN_PLAY:
					case MediaPlayerService.CURRENT_STATUS_IN_STOP:
					case MediaPlayerService.CURRENT_STATUS_CHANGED:
						synchronizePlayPauseButton();
						break;
					case MediaPlayerService.ACTION_EXIT:
						exitApp();
				}
				break;
			case "w":
				Log.w(tag, message);
				break;
			default:
				Log.e(tag, message);
		}
		showMessageInPopup(message);

	}
	private Toast toastMessage;
	public void showMessageInPopup(String message) {
		showMessageInPopup(message, Toast.LENGTH_LONG);
	}
	public void showMessageInPopup(String message, int duration) {
		if(toastMessage != null)
			toastMessage.cancel();
		toastMessage = Toast.makeText(getApplicationContext(), message, duration);
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

	public void selectStation(View view) {
		if (mBound) {
			String whatIsPlaying =  mService.whatIsPlaying();
			if(selectedStation == null) {
				selectedStation = view;
				if(whatIsPlaying != null && ((RadioButton) selectedStation).getText() == whatIsPlaying )
					return;
			}
			else if (whatIsPlaying != null && selectedStation.getTag(TAG_URI) == view.getTag(TAG_URI) && whatIsPlaying == ((RadioButton) view).getText()){
				selectedStation = view;
				return;
			}
			if(whatIsPlaying != null)
				doClickOnStopPlayButton(ACTION_STOP);
			selectedStation = view;
			doClickOnStopPlayButton(ACTION_PLAY);
		}
	}

	private void unselectStation() {
		RadioGroup stationsRadioGroup = getStationsRadioGroup();
		stationsRadioGroup.clearCheck();
		selectedStation = null;
		doClickOnStopPlayButton(ACTION_STOP);

	}


	/*
	public void dismissInfoPopup() {
		try {
			if (infoPopup != null && infoPopup.isShowing()) {
				popupHideHandler.removeCallbacks(popupHideRunnable);
				infoPopup.dismiss();
			}
		}catch(java.lang.IllegalArgumentException e){}
	}
	*/
	public void exitApp() {
		stopService(new Intent(this, MediaPlayerService.class));
		this.finish();
	}

	public void clickOnCloseSettingsView(View view) {
		changeSettingsViewVisibility(View.GONE);
	}
	public void changeSettingsViewVisibility(int visibility){
		View settingsView = findViewById(R.id.settings_view);
		settingsView.setVisibility(visibility);
	}
}