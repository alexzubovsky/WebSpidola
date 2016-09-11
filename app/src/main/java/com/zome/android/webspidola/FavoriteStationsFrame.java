package com.zome.android.webspidola;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.exoplayer.util.Util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

public class FavoriteStationsFrame extends Fragment {//AppCompatActivity {
	public static final int RESULT_SETTINGS = 1;
	public static final int RESULT_ADD_STATIONS = 2;
	public static final int RESULT_EDIT_STATIONS = 3;
	private static final int POSITION_RADIO = 0;
	private static final int POSITION_RECORD = 1;
	private static final int POSITION_PAUSE = 2;
	private static final int POSITION_STOP = 3;
	public static final String RETURN_ADDED_STATIONS_LIST = "RETURN_ADDED_STATIONS_LIST";
	public static final String PREFERENCE_LIST_OF_STATIONS = "LIST_OF_STATIONS";
	public static final String PREFERENCE_LIST_OF_STATIONS_CHANGED = "PREFERENCE_LIST_OF_STATIONS_CHANGED";
	public static final String PREFERENCE_LAST_SEARCH_URL = "PREFERENCE_LAST_SEARCH_URL";
	public static final String PREFERENCE_BEFORE_LAST_SEARCH_URL = "PREFERENCE_BEFORE_LAST_SEARCH_URL";
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
	private View selectedRadioStation = null;

	private static final boolean ACTION_STOP = true;
	public static final boolean ACTION_PLAY = false;

	private ArrayList<Integer> mRadioButtonIds;
	private float mTouchEventX = -1.0f;
	private View.OnTouchListener stationRowTouch = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			mTouchEventX = motionEvent.getX();
			return false;
		}
	};
	//Methods
	public static FavoriteStationsFrame newInstance(int sectionNumber){
		final FavoriteStationsFrame fragment = new FavoriteStationsFrame();
		final Bundle args = new Bundle();
		args.putInt(MainTabbedActivity.ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}
	public FavoriteStationsFrame(){}
	/*
	//@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		//super.onRestoreInstanceState(savedInstanceState);
		if(selectedRadioStation == null) {
			selectedStationTagOnResume = (String) savedInstanceState.getSerializable(MediaPlayerService.SELECTED_STATION_DEFINITION);
		}
	}
	*/
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//selectedStationTagOnResume = null;
		if(selectedRadioStation !=null)
			outState.putStringArray(MediaPlayerService.SELECTED_STATION_DEFINITION, (new MediaPlayerService.PlayingInfo((RadioButton) selectedRadioStation)).serializeToStringArray());//MediaPlayerService.serializeSelectedStation((RadioButton)selectedRadioStation));
	}
	//public static SharedPreferences preferences = null;
	private static LayoutInflater mFragmentInflater;
	private static ViewGroup mFragmentContainer;
	private static Bundle mFragmentSavedInstanceState;
	private static View mFragmentRootView;
	private static FavoriteStationsFrame mThis;
	private MediaPlayerService.PlayingInfo mSavedStationDefinition;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		mThis = this;
		mFragmentInflater = inflater;
		mFragmentContainer = container;
		mFragmentSavedInstanceState =savedInstanceState;
		//preferences = PreferenceManager.getDefaultSharedPreferences(getActivity()/*this*/);//this.getPreferences(Context.MODE_PRIVATE);
		MainTabbedActivity.reReadSettings();
		mFragmentRootView = mFragmentInflater.inflate(R.layout.frame_favorite_stations, mFragmentContainer,false);

		//Context mContext = getApplicationContext();
		//am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mSavedStationDefinition = MainTabbedActivity.getSavedStationDefinition(savedInstanceState);
		initializeStationsList(mSavedStationDefinition);
		getActivity().startService(new Intent(mFragmentContainer.getContext(), MediaPlayerService.class));//make sure service will not stop until stopService issued
		RadioGroup stationsRadioGroup = getStationsRadioGroup();
		stationsRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				View checkedRadio = getActivity().findViewById(checkedId);
				if (checkedRadio != null)
					selectStation(checkedRadio);
			}
		});
		return  mFragmentRootView;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser && getActivity()!=null) {
			if(MainTabbedActivity.mPreferences.getBoolean(FavoriteStationsFrame.PREFERENCE_LIST_OF_STATIONS_CHANGED, false)) {
				SharedPreferences.Editor prefEditor = MainTabbedActivity.mPreferences.edit();
				prefEditor.putBoolean(PREFERENCE_LIST_OF_STATIONS_CHANGED, false);
				prefEditor.apply();
				ArrayList<String[]> stationDefs = getStationsListFromPreferences();
				initializeStationsList(stationDefs, null);
			}
		}
	}
	private void initializeStationsList(MediaPlayerService.PlayingInfo currentPlayingStationDef) {
		initializeStationsList(getStationsListFromPreferences(), currentPlayingStationDef);
	}

	private void initializeStationsList(final ArrayList<String[]> stationsDefinition, MediaPlayerService.PlayingInfo currentPlayingStationDef) {
		if(currentPlayingStationDef == null){
			if(MainTabbedActivity.mBound)
				currentPlayingStationDef = MainTabbedActivity.mService.whatIsPlaying();
			if(currentPlayingStationDef == null)
				currentPlayingStationDef = MediaPlayerService.PlayingInfo.getInstance((Button) selectedRadioStation);
		}
		final String currentUrl
				= currentPlayingStationDef!= null&& currentPlayingStationDef.uri != null && !currentPlayingStationDef.uri.equals("null")
				? currentPlayingStationDef.uri
				: null;
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String testUrl = currentUrl;
				HashMap<String, Integer> runningRecordings = MainTabbedActivity.mBound ? MainTabbedActivity.mService.getRecordingMap() : null;

				RadioGroup stationsRadioGroup = getStationsRadioGroup();
				if(stationsRadioGroup.getChildCount()> 0){
					unCheckAllRadioButtonsBut(null);
					stationsRadioGroup.removeAllViews();
				}
				RadioButton button;
				LinearLayout row;
				String[] definition;
				RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
				int length = stationsDefinition.size();
				mRadioButtonIds = new ArrayList<>(length);
				int paddingLeft = 10;
				int paddingTop = MediaPlayerService.mPrefPaddingTop;
				int paddingBottom = new Double(paddingTop * 1.5).intValue();
				int paddingRight = 4;
				MediaPlayerService.PlayingInfo playingInfo;
				selectedRadioStation = null;
				int overallHeight = 0;
				int scrollY = 0;
				for (int i = 0; length > i; i++) {
					definition = stationsDefinition.get(i);
					mFragmentInflater.inflate(R.layout.template_radiobutton, stationsRadioGroup);
					row = (LinearLayout) stationsRadioGroup.getChildAt(stationsRadioGroup.getChildCount() - 1);
					button = (RadioButton) row.getChildAt(POSITION_RADIO);
					initRadioRowControl(row.getChildAt(POSITION_RECORD));
					initRadioRowControl(row.getChildAt(POSITION_PAUSE));
					initRadioRowControl(row.getChildAt(POSITION_STOP));
					MediaPlayerService.setButtonByDefinition(button, playingInfo = new MediaPlayerService.PlayingInfo(definition));//sets tags, text and font
					button.setId(MainTabbedActivity.generateUniqueId());
					mRadioButtonIds.add(button.getId());
					button.setLayoutParams(layoutParams);
					button.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
					button.setOnClickListener(radioRowControlClick);
					button.setOnLongClickListener(radioRowControlLongClick);
					button.setOnTouchListener(stationRowTouch);
					overallHeight += Math.abs(row.getLayoutParams().height);
					if (testUrl != null && testUrl.equalsIgnoreCase(stationsDefinition.get(i)[MediaPlayerService.DEF_POS_URL])) {
						((RadioButton)(selectedRadioStation = button)).setChecked(true);//stationsRadioGroup.check((selectedRadioStation = button).getId());
						testUrl = null;//to prevent farther checking
						scrollY = overallHeight;
					}
					setCurrentRecordingControls(button, runningRecordings != null ? runningRecordings.get(playingInfo.uri) : null);
					View divider = new View(mFragmentContainer.getContext(), null, R.style.Divider);
					divider.setLayoutParams(new RadioGroup.LayoutParams(LayoutParams.MATCH_PARENT, 1, 1f));
					stationsRadioGroup.addView(divider);
					overallHeight +=Math.abs(divider.getLayoutParams().height);
				}
				if(selectedRadioStation != null)
					stationsRadioGroup.requestChildFocus((LinearLayout)selectedRadioStation.getParent(), selectedRadioStation);
			}
		});
		//thread.start();
	}

	private LinearLayout.LayoutParams getCorrectedLayoutParamsForRadioButton(RadioButton button){
		LinearLayout.LayoutParams bParams = (LinearLayout.LayoutParams) button.getLayoutParams();
		float weight = 1.f;
		bParams.weight = 1f;
		return bParams;
	}
	private void initRadioRowControl(View control){
		control.setOnClickListener(radioRowControlClick);
	}

	private void unCheckAllRadioButtonsBut(RadioButton radio){// checkedId){
		Integer checkedId = radio != null ? radio.getId():null;
		RadioGroup radioGroup = getStationsRadioGroup();
		RadioButton checkedButton;// = (RadioButton) mFragmentRootView.findViewById(radioGroup.getCheckedRadioButtonId());
		boolean setChecked;
		for(int id:mRadioButtonIds) {
			checkedButton = (RadioButton) radioGroup.findViewById(id);
			if (checkedButton != null) {
				setChecked = (checkedId != null && id == checkedId);
				if (checkedButton.isChecked() != setChecked)
					checkedButton.setChecked(setChecked);
			}
		}
	}

	private View.OnClickListener radioRowControlClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if(v instanceof RadioButton){
				selectStation(v);
			}else {
				LinearLayout parent = (LinearLayout) v.getParent();
				String action =  (String) v.getTag();
				RadioButton button = (RadioButton) parent.getChildAt(POSITION_RADIO);
				View record = parent.getChildAt(POSITION_RECORD);
				View pause = parent.getChildAt(POSITION_PAUSE);
				View stop = parent.getChildAt(POSITION_STOP);
				int handlingAction = -1;
				switch (action) {
					case "record":
						handlingAction = stop.getVisibility() == View.GONE ? MediaPlayerService.AUDIO_RECORDING_START :MediaPlayerService.AUDIO_RECORDING_RESTART;
						setCurrentRecordingControls(MediaPlayerService.AUDIO_RECORDING_START, record, pause, stop);
						/*record.setVisibility(View.GONE);
						pause.setVisibility(View.VISIBLE);
						stop.setVisibility(View.VISIBLE);*/
						break;
					case "pause":
						setCurrentRecordingControls(handlingAction = MediaPlayerService.AUDIO_RECORDING_PAUSE, record, pause, stop);
						/*pause.setVisibility(View.GONE);
						record.setVisibility(View.VISIBLE);
						stop.setVisibility(View.VISIBLE);*/
						break;
					case "stop":
						setCurrentRecordingControls(handlingAction = MediaPlayerService.AUDIO_RECORDING_STOP, record, pause, stop);
						/*stop.setVisibility(View.GONE);
						record.setVisibility(View.GONE);
						pause.setVisibility(View.GONE);*/
						break;
					default:
						return;
				}
				MediaPlayerService.PlayingInfo definition = new MediaPlayerService.PlayingInfo(button);
				String url = definition.uri;
				String stationName  = definition.name;
				String path = MediaPlayerService.mRootDirectoryForRecordings+"/" +stationName;
				switch(handlingAction)
				{
					case MediaPlayerService.AUDIO_RECORDING_START:
					case MediaPlayerService.AUDIO_RECORDING_PAUSE:
					case MediaPlayerService.AUDIO_RECORDING_STOP:
					case MediaPlayerService.AUDIO_RECORDING_RESTART:
						if(MainTabbedActivity.mBound){
							/*if(button.isChecked())
								mService.handlingAudioRecording(handlingAction, path, stationName);
							else*/
							MainTabbedActivity.mService.handlingUrlStreaming(handlingAction, definition, path);
						}
				}
			}
		}
	};
	private MediaScannerConnection.OnScanCompletedListener scanCompleteListener = new MediaScannerConnection.OnScanCompletedListener() {
		@Override
		public void onScanCompleted(String s, Uri uri) {
			//do nothing so far
		}
	};
	private void setCurrentRecordingControls(View oneOfButtons, Integer currentRecordingStatus) {
		if(currentRecordingStatus != null) {
			LinearLayout parent = (LinearLayout) oneOfButtons.getParent();
			View record = parent.getChildAt(POSITION_RECORD);
			View pause = parent.getChildAt(POSITION_PAUSE);
			View stop = parent.getChildAt(POSITION_STOP);
			setCurrentRecordingControls(currentRecordingStatus, record, pause, stop);
		}
	}
	private void setCurrentRecordingControls(int state, View record, View pause,  View stop){
		switch(state){
			case MediaPlayerService.AUDIO_RECORDING_START:
				//handlingAction = stop.getVisibility() == View.GONE ? MediaPlayerService.AUDIO_RECORDING_START :MediaPlayerService.AUDIO_RECORDING_RESTART;
				record.setVisibility(View.GONE);
				boolean selectedForPlaying = false;//((LinearLayout) pause.getParent()).getChildAt(POSITION_RADIO).getTag(TAG_URI).equals(mService.getCurrentStationUrl());
				pause.setVisibility(selectedForPlaying ? View.GONE: View.VISIBLE);
				stop.setVisibility(View.VISIBLE);
				break;
			case MediaPlayerService.AUDIO_RECORDING_PAUSE:
				pause.setVisibility(View.GONE);
				record.setVisibility(View.VISIBLE);
				stop.setVisibility(View.VISIBLE);
				//handlingAction = MediaPlayerService.AUDIO_RECORDING_PAUSE;
				break;
			case MediaPlayerService.AUDIO_RECORDING_STOP:
				//handlingAction = MediaPlayerService.AUDIO_RECORDING_STOP;
				stop.setVisibility(View.GONE);
				record.setVisibility(View.GONE);
				pause.setVisibility(View.GONE);
				break;

		}

	}

	private View.OnLongClickListener radioRowControlLongClick = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			LinearLayout parent = (LinearLayout) v.getParent();
			if((mTouchEventX < 50.0f)/* && (v instanceof RadioButton) && ((RadioButton)v).isChecked()*/){
				RadioButton radio = (RadioButton) v;
				openAlertDialogToDelete(parent, (String) radio.getText(), (String) radio.getTag(MediaPlayerService.TAG_URI));
			}
			else {
				View record = parent.getChildAt(POSITION_RECORD);
				View pause = parent.getChildAt(POSITION_PAUSE);
				View stop = parent.getChildAt(POSITION_STOP);
				if (/*pause.getVisibility() == View.GONE && */stop.getVisibility() == View.GONE) {
					record.setVisibility(record.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
					return true;
				}
			}
			return false;
		}
	};

	private void openAlertDialogToDelete(View toDelete, String title, String url) {
		final View mToDelete = toDelete;
		final String mTitle = title;
		final String mUrl = url;
		AlertDialog.Builder dialog =new AlertDialog.Builder(mFragmentContainer.getContext()/*FavoriteStationsFrame.this*/);//new AlertDialog.Builder(getApplicationContext(), R.style.AppTheme);
		dialog.setTitle("Delete Station");
		dialog.setMessage("Would you like to delete station\n'" + mTitle + "'");
		dialog.setNeutralButton(/*DialogInterface.BUTTON_POSITIVE, */"Delete", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {
				ArrayList<String[]> list = getStationsListFromPreferences();
				for(String[] definition : list)
					if(definition[1].equals(mUrl)) {
						list.remove(definition);
						initializeStationsList(commitStationsList(list, null), null);
						break;
					}

			}
		});
		dialog.setNegativeButton(/*DialogInterface.BUTTON_NEGATIVE, */"Keep", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		dialog.show();
	}


	// Handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			int type = intent.getIntExtra(MediaPlayerService.CUSTOM_EVENT_CATEGORY, 0);
			String message = intent.getStringExtra(MediaPlayerService.CUSTOM_EVENT_MESSAGE);
			/*if(MediaPlayerService.ACTION_EXIT.equals(message))
				MainTabbedActivity.exitApp();*/
			switch(type){
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_ADD_RADIO_NAME:
					if(selectedRadioStation != null)
						message += " " +((RadioButton) selectedRadioStation).getText();
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_ON_PREPARED_FINISHED:
					//showPlayPauseButton();
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_END_BY_NO_CONNECTION:
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_END_BY_NO_DATA:
					putCurrentRecordingStatusToRadioButton(message, MediaPlayerService.AUDIO_RECORDING_STOP);
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_PAUSE_BY_NO_CONNECTION:
					putCurrentRecordingStatusToRadioButton(message, MediaPlayerService.AUDIO_RECORDING_PAUSE);
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RESTORE_BY_CONNECTION:
					putCurrentRecordingStatusToRadioButton(message, MediaPlayerService.AUDIO_RECORDING_START);
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_FINISHED:
					putCurrentRecordingStatusToRadioButton(message, MediaPlayerService.AUDIO_RECORDING_STOP);
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_TO_NEXT_FILE:
					//putCurrentRecordingStatusToRadioButton(message, MediaPlayerService.AUDIO_RECORDING_START);
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_STOP_NO_SPACE:
					putCurrentRecordingStatusToRadioButton(message, MediaPlayerService.AUDIO_RECORDING_STOP);
					break;
			}
			displayMessage("MA:receiver", message);
		}
	};
	private void putCurrentRecordingStatusToRadioButton(String url, int currentRecordingStatus) {
		RadioGroup stationsRadioGroup = getStationsRadioGroup();
		int stations = stationsRadioGroup.getChildCount();
		RadioButton radioButton;
		View child;
		for(int i=0;stations > i;i++) {
			child = stationsRadioGroup.getChildAt(i);
			if (child instanceof RadioButton)
				radioButton = (RadioButton) child;
			else if (child instanceof LinearLayout)
				radioButton = (RadioButton) ((LinearLayout) child).getChildAt(0);
			else
				continue;
			if (((String) radioButton.getTag(MediaPlayerService.TAG_URI)).equalsIgnoreCase(url)) {
				setCurrentRecordingControls(radioButton, currentRecordingStatus);
			}
		}

	}
	@Override
	public void onStart() {
		super.onStart();
		/*
		Intent intent = new Intent(this, MediaPlayerService.class);
		mFragmentContainer.getContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		*/
		//Log.d("onStart", "bindService:" + intent.toString());
	}

	@Override
	public void onStop() {
		super.onStop();
		//unBindMPService();
	}
	@Override
	public void onDestroy() {
		//unBindMPService();
		super.onDestroy();
	}
	@Override
	public void onResume(){
		super.onResume();
		MainTabbedActivity.reReadSettings();
		IntentFilter intentFilter = new IntentFilter(MediaPlayerService.CUSTOM_EVENT);
		Context context = mFragmentContainer.getContext();
		LocalBroadcastManager.getInstance(context/*this*/).registerReceiver(mMessageReceiver, intentFilter);
		Log.i("MA:OnResume", "registerReceiver(mMessageReceiver, intentFilter=" + intentFilter.toString() + ")");

		//context.bindService(new Intent(context, MediaPlayerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
	}
	@Override
	public void onPause(){
		LocalBroadcastManager.getInstance(mFragmentContainer.getContext()/*this*/).unregisterReceiver(mMessageReceiver);
		//unBindMPService();
		super.onPause();
	}
	public void synchronizeOnServiceConnected(MediaPlayerService mService){
		Log.d("FSF","onServiceConnected "+ mService.toString());
		MediaPlayerService.PlayingInfo currentPlayingStationInfo = mService.whatIsPlaying();
		String currentPlayingName = currentPlayingStationInfo != null ? currentPlayingStationInfo.name : null;
		HashMap<String, Integer> recordingMap = mService.getRecordingMap();
		RadioGroup stationsRadioGroup = getStationsRadioGroup();
		int stations = stationsRadioGroup.getChildCount();
		RadioButton radioButton;
		View row;
		selectedRadioStation = null;
		for(int i=0;stations > i;i++){
			row = stationsRadioGroup.getChildAt(i);
			if(row != null && row instanceof LinearLayout) {
				radioButton = (RadioButton) ((LinearLayout)row).getChildAt(POSITION_RADIO);
				if (radioButton.getText().equals(currentPlayingName)) {
					selectedRadioStation = radioButton;
					radioButton.setChecked(true);
				}
				else if(radioButton.isChecked())
					radioButton.setChecked(false);
				setCurrentRecordingControls(radioButton, recordingMap.get(radioButton.getTag(MediaPlayerService.TAG_URI)));
			}
		}
		//if(selectedRadioStation == null)
		//	stationsRadioGroup.clearCheck();
		/*
		synchronizePlayPauseButton();
		if (mService.isAutoPlayCase() && getBooleanPreference("autoplay_switch", false) && selectedRadioStation != null)
			doClickOnStopPlayButton(getPlayStopButton(), false);
		*/

	}
	/*boolean mBound = false;
	MediaPlayerService mService;
	private void unBindMPService() {
		if(mBound){
			mFragmentContainer.getContext()/*this* /.unbindService(mServiceConnection);
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
			synchronizeOnServiceConnected();
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.d("FSF","onServiceDisconnected "+arg0.toString());
		}
	};

*/

	/*
	private FloatingActionButton getPlayStopButton() {
		View fab = mFragmentRootView.findViewById(R.id.fab);
		if(fab == null)
			fab = getActivity().findViewById(R.id.fab);
		return  (FloatingActionButton)fab;
	}
	*/
	private RadioGroup getStationsRadioGroup() {
		return (RadioGroup) /*this*/mFragmentRootView.findViewById(R.id.stations_radio_group);
	}
	/*
	//@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getActivity().getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		Intent intent;
		switch (id) {
			case R.id.action_info:
				StringBuilder message = new StringBuilder();
				if(mBound)
					message.append(mService.getConnectivityStatus().toString());
				if(toastMessage != null)
					message.append('\n').append(toastMessage.getView().getTag().toString());
				showMessageInPopup(message.toString(), Toast.LENGTH_LONG * 10);
				return true;
			case R.id.action_settings:
				intent = new Intent(this, SettingsActivity.class);
				startActivityForResult(intent, RESULT_SETTINGS);
				return true;
			case R.id.action_add:
				intent = new Intent(/this, SearchForStationsFrame.class);
				startActivityForResult(intent, RESULT_ADD_STATIONS);
				return true;
			case R.id.action_edit:
				intent = new Intent(this, ManageFavoriteStationsFrame.class);
				startActivityForResult(intent, RESULT_EDIT_STATIONS);
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
	*/

	/**
	 * Called when clicked Add station button
	 *
	 * @param view
	 */
	public void addStationClick(View view) {
		//setContentView(R.layout.vtuner_view);
	}
	/*
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case RESULT_SETTINGS:
				if(resultCode == Activity.RESULT_OK)
					reReadSettings();
				break;
			case RESULT_ADD_STATIONS:
				if(MainTabbedActivity.mPreferences.getBoolean(FavoriteStationsFrame.PREFERENCE_LIST_OF_STATIONS_CHANGED, false)) {
					SharedPreferences.Editor prefEditor = MainTabbedActivity.mPreferences.edit();
					prefEditor.putBoolean(PREFERENCE_LIST_OF_STATIONS_CHANGED, false);
					prefEditor.apply();
					initializeStationsList(getStationsListFromPreferences(), null);
				}
				break;
			case RESULT_EDIT_STATIONS:
				if(resultCode == Activity.RESULT_OK) {
					ArrayList<MediaPlayerService.PlayingInfo> stationsList = deSerializeStationsList(data.getStringExtra(RETURN_ADDED_STATIONS_LIST));
					initializeStationsList(commitStationsList(stationsList, null),null);
				}
				break;
		}
	}
	*/
	public static ArrayList<String[]> commitStationsList(ArrayList<String[]> stationsList, String preferenceListOfStationsChanged) {
		if(stationsList.size() == 0)
			stationsList = getDefaultStationsList();
		SharedPreferences.Editor prefEditor = MainTabbedActivity.mPreferences.edit();
		prefEditor.putString(PREFERENCE_LIST_OF_STATIONS, serializeStationsList(stationsList));
		if(preferenceListOfStationsChanged != null)
			prefEditor.putBoolean(preferenceListOfStationsChanged, true);
		prefEditor.apply();
		return stationsList;
	}

	final static String PREF_TYPE_OTHER = Integer.toString(Util.TYPE_OTHER);
	private static ArrayList<String[]> getDefaultStationsList(){
		final String[][] aButtonsDef = {
				{PREF_TYPE_OTHER, "http://stream05.media.rambler.ru/echo.mp3", "Echo Moscow"},
				//{PREF_TYPE_OTHER, "http://95.79.31.115:8000/", "Echo Moscow LQ"},
				{PREF_TYPE_OTHER, "http://icecast.vgtrk.cdnvideo.ru/mayakfm_mp3_64kbps", "Mayak"},
				{PREF_TYPE_OTHER, "http://ags.abinet.com:8800/dr.mp3", "Davidzon Radio"},
				{PREF_TYPE_OTHER, "http://stream.kazancity.net:8000/27-mds", "MDS Station"},
				{PREF_TYPE_OTHER, "http://wioq-fm.akacast.akamaistream.net/7/247/20056/v1/auth.akacast.akamaistream.net/wioq-fm", "WIOQ FM"},
				{PREF_TYPE_OTHER, "http://shoutcast.internet-radio.org.uk:10272/;", "Shout cast UK"},
				{PREF_TYPE_OTHER, "http://oggvorbis.tb-stream.net:80/technobase.ogg", "Ogg Radio"}
		};
		ArrayList<String[]> stationsList = new ArrayList<>();
		for (int i = 0; aButtonsDef.length > i; i++) {
			stationsList.add(aButtonsDef[i]);
		}
		return stationsList;
	}
	public static ArrayList<String[]> getStationsListFromPreferences(){
		return getStationsListFromPreferences(null);
	}
	public static ArrayList<String[]> getStationsListFromPreferences(ArrayList<String[]> addStations){
		ArrayList<String[]> stationsList = deSerializeStationsList(MainTabbedActivity.mPreferences.getString(PREFERENCE_LIST_OF_STATIONS, null));
		if(stationsList.size() == 0) {
			commitStationsList(stationsList = getDefaultStationsList(), FavoriteStationsFrame.PREFERENCE_LIST_OF_STATIONS_CHANGED);
		}
		if(addStations == null)
			addStations = new ArrayList<>();
		addStations.addAll(stationsList);
		return addStations;

	}

	public static String serializeStationsList(ArrayList<String[]> list) {
		JSONArray array = new JSONArray();
		JSONArray subArray;
		for(String[] i : list){
			subArray = new JSONArray();
			for(int j=0;i.length> j; j++)
				subArray.put(i[j]);
			array.put(subArray.toString());
		}
		return array.toString();
	}

	public static ArrayList<String[]> deSerializeStationsList(String serializedArray) {
		ArrayList<String[]> result = new ArrayList<>();
		if(serializedArray != null && !serializedArray.isEmpty()) {
			try {
				JSONArray array = new JSONArray(serializedArray);
				JSONArray subArray;
				String[] sub;
				for (int i = 0; array.length() > i; i++) {
					subArray = new JSONArray((String) array.get(i));
					sub = new String[subArray.length()];
					for (int j = 0; sub.length > j; j++)
						sub[j] = subArray.getString(j);
					result.add(sub);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/*private void showUserSettings() {
		reReadSettings();
		setMonitoringToService();
		StringBuilder builder = new StringBuilder();
		Map<String,?> map = MainTabbedActivity.mPreferences.getAll();
		Set<String> keys = map.keySet();
		String s;
		for(String key:keys){
			builder.append("\n ").append(key).append('[').append(s=(map.get(key).getClass().getSimpleName())).append(":]");
			switch(s) {
				case "String":
					builder.append(MainTabbedActivity.mPreferences.getString(key, "NULL")); break;
				case "Integer":
					builder.append(MainTabbedActivity.mPreferences.getInt(key, -11111)); break;
				case "Boolean":
					builder.append(MainTabbedActivity.mPreferences.getBoolean(key, false)); break;
			}
		}
		/*
		builder.append("\n Theme: "+ sharedPrefs.getString("themes_list", "NULL"));
		builder.append("\n Font Size: "+ sharedPrefs.getString("FontSize", "NULL"));
		builder.append("\n Pading Top: "+ sharedPrefs.getString("addingTop", "NULL"));
		* /
		TextView settingsTextView = (TextView) mFragmentRootView.findViewById(R.id.textUserSettings);

		settingsTextView.setText(builder.toString());
		changeSettingsViewVisibility(View.VISIBLE);
	}*/
	/*
	private void showPlayPauseButton() {
		FloatingActionButton button = getPlayStopButton();
		if(button.getVisibility() != View.VISIBLE)
			button.setVisibility(View.VISIBLE);
	}
	*/
	/**
	 * Called To Play/Stop
	 *
	 * //@param button to toggle image
	 * @param doStop true then stop, false - toggle
	 * /
	public boolean doClickOnStopPlayButton(/*FloatingActionButton button, * /boolean doStop) {
		boolean result = false;
		if (MainTabbedActivity.mBound) {
			if (MainTabbedActivity.mService.whatIsPlaying() != null) {//am.abandonAudioFocus(afChangeListener);
				if(doStop)// Call a method from the LocalService.
					MainTabbedActivity.mService.stopPlay();
				else if(MainTabbedActivity.mService.isOnPause())
					MainTabbedActivity.mService.reStartPlay();
				else
					MainTabbedActivity.mService.pausePlay();
				result = true;
			} else {
				if(!(result = startPlaying())) {
					displayMessage("Playing did not start");
				}
			}
		}
		else
			displayMessage("Not Bound with the Media Player Service");
		//synchronizePlayPauseButton();//make sure button shows 'ready' to play
		return result;
	}*/
	/**
	 * makes sure plaing state correctly shown in button's image
	 * /
	private void synchronizePlayPauseButton()	{
		FloatingActionButton button = getPlayStopButton();
		if (selectedRadioStation != null) {
			if(button.getTag() == null )
				button.setTag("ic_media_play|ic_media_pause");
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
				button.setImageResource(MainTabbedActivity.mResources.getIdentifier(tags[1], "drawable", getActivity().getPackageName()));
			}
			if(mService.isOnPrepare())
				button.setVisibility(View.GONE);
			else if(button.getVisibility() != View.VISIBLE)
				button.setVisibility(View.VISIBLE);
		}else
			button.setVisibility(View.GONE);
	}*/
	/*
	private static String[] getUrlAndType(View v){
		String[] strArray = {null, null};
		if(v != null) {
			Object tag = v.getTag(TAG_URI);
			strArray[0] = tag != null ? tag.toString() : null;
			strArray[1] = v.getTag(TAG_TYPE).toString();
		}
		return strArray;
	}
	*/
	/*
	private boolean startPlaying() {
		boolean bRes = false;
		String message = null;
		if (selectedRadioStation != null) {
			MediaPlayerService.PlayingInfo definition = new MediaPlayerService.PlayingInfo((RadioButton)selectedRadioStation);
			if (definition != null) {
				if (MainTabbedActivity.mBound) {
					//getPlayStopButton().setVisibility(View.GONE);
					// Call a method from the LocalService.
					// However, if this call were something that might hang, then this request should
					// occur in a separate thread to avoid slowing down the activity performance.
					MainTabbedActivity.mService.startPlay(definition);
					MainTabbedActivity.saveSelectedStationToPreferences(definition);
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
		//getPlayStopButton().setVisibility(View.VISIBLE);
		return bRes;
	}
	*/
	private void displayMessage(String message) {
		displayMessage("i", "Start Playing", message);
	}

	private void displayMessage(String tag, String message) {
		displayMessage("i", tag, message);
	}

	private void displayMessage(String category, String tag, String message) {
		boolean showMess = false;
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
						//synchronizePlayPauseButton();
						break;
					case MediaPlayerService.ACTION_EXIT:
						//exitApp();
				}
				break;
			case "w":
				showMess = true;
				Log.w(tag, message);
				break;
			default:
				showMess = true;
				Log.e(tag, message);
		}
		if(showMess)
			showMessageInPopup(message);


	}
	private Toast toastMessage;
	public void showMessageInPopup(String message) {
		showMessageInPopup(message, Toast.LENGTH_LONG);
	}
	public void showMessageInPopup(String message, int duration) {
		if(toastMessage != null)
			toastMessage.cancel();
		toastMessage = Toast.makeText(/*getApplicationContext()*/mFragmentContainer.getContext(), message, duration);
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
		View anchor = selectedRadioStation == null ? (View) findViewById(R.id.stations_radio_group) : selectedRadioStation;
		infoPopup.showAsDropDown(anchor, 400, -10, Gravity.FILL);
		infoPopup.update(800, 200);
		popupHideHandler.postDelayed(popupHideRunnable, 10000);*/
	}

	public void selectStation(View view) {
		if (view != null && view instanceof RadioButton) {
			RadioButton radio = (RadioButton) view;
			unCheckAllRadioButtonsBut(radio);
			String selectedName = (String) radio.getText();
			if (MainTabbedActivity.mBound) {
				MediaPlayerService.PlayingInfo whatIsPlaying = MainTabbedActivity.mService.whatIsPlaying();
				if(whatIsPlaying != null){
					if(whatIsPlaying.name.equals(selectedName) || view.getTag(MediaPlayerService.TAG_URI).equals(whatIsPlaying.uri))
						return;
					else
						MainTabbedActivity.doClickOnStopPlayButton(ACTION_STOP);
				}
				MainTabbedActivity.setSelectedStation(view);
				MainTabbedActivity.doClickOnStopPlayButton(ACTION_PLAY);
			}
			selectedRadioStation = view;
		}
	}
	/*
	private void unselectStation() {
		RadioGroup stationsRadioGroup = getStationsRadioGroup();
		stationsRadioGroup.clearCheck();
		selectedRadioStation = null;
		MainTabbedActivity.doClickOnStopPlayButton(ACTION_STOP);

	}
	*/
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
	/*
	public void exitApp() {
		Activity thisActivity = getActivity();
		thisActivity.stopService(new Intent(thisActivity, MediaPlayerService.class));
		thisActivity.finish();
	}

	public void clickOnCloseSettingsView(View view) {
		changeSettingsViewVisibility(View.GONE);
	}
	*/

	public void changeSettingsViewVisibility(int visibility){
		View settingsView = getActivity().findViewById(R.id.settings_view);
		settingsView.setVisibility(visibility);
	}

}