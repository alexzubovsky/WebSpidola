package com.zome.android.webspidola;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//import android.support.v4.view.ViewPager;

public class MainTabbedActivity extends AppCompatActivity implements View.OnTouchListener{//} implements ManageRecordingsFrame.OnListFragmentInteractionListener{

	public static final int FAVORITE_STATIONS = 0;
	public static final int MANAGE_RECORDINGS = 1;
	public static final int SEARCH_FOR_STATIONS = 2;
	public static final int MANAGE_STATIONS = 3;
	public static final int FRAMES_COUNT = 4;
	public static Resources mResources;
	private static Context mContext;
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link FragmentPagerAdapter} derivative, which will keep every
	 * loaded fragment in memory. If this becomes too memory intensive, it
	 * may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	public static ViewPager mViewPager;

	public static SharedPreferences mPreferences;
	private static MediaPlayerService.PlayingInfo mSavedStationDefinition = null;
	public static final String ARG_SECTION_NUMBER = "section_number";
	private static boolean mExitDeclared =false;
	private static Activity mThisActivity;

	public static void setSelectedStation(View view) {
		if(view instanceof Button)
			mSavedStationDefinition = new MediaPlayerService.PlayingInfo((Button) view);
	}


	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends android.support.v4.app.FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a PlaceholderFragment (defined as a static inner class below).
			switch (position) {
				//case 0:return PlaceholderFragment.newInstance(position + 1);//defaultFragment();
				case MANAGE_RECORDINGS:
					return ManageRecordingsFrame.newInstance(position);
				case SEARCH_FOR_STATIONS:
					return SearchForStationsFrame.newInstance(position);
				case MANAGE_STATIONS:
					return ManageFavoriteStationsFrame.newInstance(position);
				case FAVORITE_STATIONS:
				default:
					return FavoriteStationsFrame.newInstance(0);//
			}
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return FRAMES_COUNT;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case FAVORITE_STATIONS:
					return "Favorites";
				case SEARCH_FOR_STATIONS:
					return "Search";
				case MANAGE_STATIONS:
					return "Manage";
				case MANAGE_RECORDINGS:
					return "Recording";
			}
			return null;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 * /
	 * public static class PlaceholderFragment extends Fragment {
	 * /**
	 * The fragment argument representing the section number for this
	 * fragment.
	 * /
	 * private static final String ARG_SECTION_NUMBER = "section_number";
	 * <p/>
	 * /**
	 * Returns a new instance of this fragment for the given section
	 * number.
	 * /
	 * public static PlaceholderFragment newInstance(int sectionNumber) {
	 * PlaceholderFragment fragment = new PlaceholderFragment();
	 * Bundle args = new Bundle();
	 * args.putInt(ARG_SECTION_NUMBER, sectionNumber);
	 * fragment.setArguments(args);
	 * return fragment;
	 * }
	 *
	 * @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	 * int layoutId;
	 * switch(getArguments().getInt(ARG_SECTION_NUMBER)){
	 * case 0:
	 * layoutId = R.layout.frame_favorite_stations; break;
	 * case 1:
	 * layoutId = R.layout.frame_search_stations; break;
	 * case 3:
	 * layoutId = R.layout.frame_manage_stations; break;
	 * default:
	 * layoutId = R.layout.fragment_main_tabbed;
	 * }
	 * View rootView = inflater.inflate(layoutId, container, false);
	 * <p/>
	 * return rootView;
	 * }
	 * }
	 */
	private static FloatingActionButton floatingActionButton;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("MTA:onCreate:",(savedInstanceState!=null?savedInstanceState.getClass().getSimpleName():""));
		mContext = getApplicationContext();
		mResources = getResources();
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);//this.getPreferences(Context.MODE_PRIVATE);
		if(mExitDeclared) {
			if(savedInstanceState != null) {
				exitApp();
				return;
			}else
				mExitDeclared = false;
		}
		RECORDS_SORT_ASCENDING = mContext.getString(R.string.records_sort_ascending);
		RECORDS_SHORTING_NAMES = getString(R.string.records_shorting_names);
		reReadSettings(true);
		onActivityCreateSetTheme(mThisActivity = this);
		checkPermissionsOnInitialization();
		this.setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.main_tabbed_toolbar);
		setSupportActionBar(toolbar);
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.



		// Set up the ViewPager with the sections adapter.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.main_tabbed_container);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
			@Override
			public void onPageSelected(int position) {
				if (position < 0)
					mViewPager.setCurrentItem(FRAMES_COUNT - 1);
				else if (position > FRAMES_COUNT)
					mViewPager.setCurrentItem(0);
			}
			@Override
			public void onPageScrollStateChanged(int state) {
				int currentPage = mViewPager.getCurrentItem();       //ViewPager Type
				if (currentPage == FRAMES_COUNT -1 || currentPage == 0) {
					previousState = currentState;
					currentState = state;
					if (previousState == 1 && currentState == 0) {
						mViewPager.setCurrentItem(currentPage == 0 ? FRAMES_COUNT -1: 0);
					}
				}
			}
		});
		//////////////////
		TabLayout tabLayout = (TabLayout)findViewById(R.id.mainTabsBar);
		mViewPager.setOffscreenPageLimit(1);
		mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
		tabLayout.setupWithViewPager(mViewPager);
		///////////////////

		floatingActionButton = (FloatingActionButton)findViewById(R.id.fab_play_pause);
		floatingActionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(!doClickOnStopPlayButton() && !mBound)
					registerBroadcastReceivers("FAB:onClick", true);
			}
		});
		//floatingActionButton.setOnLongClickListener(fabMovingSupport);
		floatingActionButton.setOnTouchListener(onTouchListener);
		mSavedStationDefinition = getSavedStationDefinition(savedInstanceState);
		//registerBroadcastReceiver("onCreate:"+(savedInstanceState!=null?savedInstanceState.getClass().getSimpleName():""));
		setUpAdMob();
		//mViewPager.setOnSystemUiVisibilityChangeListener(onSystemUiVisibilityChangeListener);

		Log.e("MTA:onCreate1:",(savedInstanceState!=null?savedInstanceState.getClass().getSimpleName():""));
	}
	private static final boolean adMobShallBeShown = false;
	private void setUpAdMob(){
		int adMobCorrection = 0;
		float coeff = 0.98f;
		if(adMobShallBeShown) {
			AdView adMob = (AdView) findViewById(R.id.ad_mob_view);
			MobileAds.initialize(getApplicationContext(), "ca-app-pub-5265061023469633~6963313508");
			AdRequest adRequest = new AdRequest.Builder().build();
			adMob.loadAd(adRequest);
			adMobCorrection = AdSize.BANNER.getHeight();
			coeff = 1.5f;
		}
		TypedValue tv = new TypedValue();
		if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)){
			int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
			TabLayout tabLayout = (TabLayout) findViewById(R.id.mainTabsBar);
			//if(tabLayout.getVisibility()==View.VISIBLE)
			//	actionBarHeight+=tabLayout.getHeight();
			mViewPager.setPadding(mViewPager.getPaddingLeft(),mViewPager.getPaddingTop(),mViewPager.getPaddingRight(),Math.round(coeff*actionBarHeight) + adMobCorrection);
		}
	}
	/*
	private void setupTabItem(TabItem v){
		v.setOnClickListener(tabItemOnClickListener);
		((TextView)v).setText(mSectionsPagerAdapter.getPageTitle(Integer.parseInt((String)v.getTag())));
	}*/
	/*public View.OnLongClickListener fabMovingSupport = new View.OnLongClickListener(){
		@Override
		public boolean onLongClick(View view) {
			view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),view.getPaddingRight(),view.getPaddingBottom()+16);
			FloatingActionButton fab = (FloatingActionButton)view;
			Log.e("fabMovingSupport", String.valueOf(fab.getTop()));
			return false;
		}
	};*/
	private static Float previousX = null;
	private static Float previousY = null;
	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		return onTouchImplementation(view, motionEvent);
	}

	public static View.OnTouchListener onTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			return onTouchImplementation(view, motionEvent);
		}
	};
	private static boolean onTouchImplementation(View view, MotionEvent motionEvent) {
		Log.e("onTouch", view.getClass().getSimpleName() + "," + motionEvent.getAction());
		if (view instanceof FloatingActionButton) {
			FloatingActionButton fab = (FloatingActionButton) view;
			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_MOVE:
					if (previousX != null && previousY != null) {
						//fab.animate().translationYBy(previousY - motionEvent.getY()).translationXBy(previousX - motionEvent.getX());
						Log.e("onTouch", view.getClass().getSimpleName() + ",X:" + (previousX - motionEvent.getX())+ ",Y:"+(previousY - motionEvent.getY()));
					}//else
						break;
				case MotionEvent.ACTION_DOWN:
					previousY = motionEvent.getY();
					previousX = motionEvent.getX();
					break;
				case MotionEvent.ACTION_UP:
					if (previousX != null && previousY != null) {
						fab.animate().translationYBy(motionEvent.getY()- previousY).translationXBy(motionEvent.getX()- previousX );
						Log.e("onUp", view.getClass().getSimpleName() + ",X:" + (motionEvent.getX()- previousX)+ ",Y:"+(motionEvent.getY()- previousY));
					}
					previousY = null;
					previousX = null;
			}
		}
		return false;
	}
	private int previousState, currentState;
	private void showPlayPauseButton() {
		FloatingActionButton button = getPlayStopButton();
		if (button.getVisibility() != View.VISIBLE)
			button.setVisibility(View.VISIBLE);
	}

	public boolean doClickOnStopPlayButton() {
		boolean doStop = false;
		if (mBound) {
			/*if (mService.whatIsPlaying() != null && !mService.isOnPause())
				doStop = true;*/
			doClickOnStopPlayButton(doStop);
		}
		return false;
	}

	/**
	 * Called To Play/Stop
	 *
	 //* @param button to toggle image
	 * @param doStop true then stop, false - toggle
	 */
	public static boolean doClickOnStopPlayButton(/*FloatingActionButton button, */boolean doStop) {
		boolean result = false;
		if (mBound) {
			if (mService.whatIsPlaying() != null) {//am.abandonAudioFocus(afChangeListener);
				if (doStop)// Call a method from the LocalService.
					mService.stopPlay();
				else if (mService.isOnPause())
					mService.reStartPlay();
				else
					mService.pausePlay();
				result = true;
			} else {
				if(!(result = startPlaying())) {
					//displayMessage("Playing did not start");
				}
			}
		}
		//synchronizePlayPauseButton();//make sure button shows 'ready' to play
		return result;
	}

	/**
	 * makes sure plaing state correctly shown in button's image
	 */
	private void synchronizePlayPauseButton() {
		FloatingActionButton button = getPlayStopButton();
		int currentPlayingStatus = mService.getCurrentPlayingStatus();
		if (mService.whatIsPlaying() != null) {
			boolean isOnPrepare = mService.isOnPrepare();
			switch (currentPlayingStatus) {
				case MediaPlayerService.CURRENT_PLAYING_STATUS_NONE:
				case MediaPlayerService.CURRENT_PLAYING_STATUS_PAUSE:
					button.setImageResource(R.drawable.ic_media_play);
					break;
				case MediaPlayerService.CURRENT_PLAYING_STATUS_PLAY:
					button.setImageResource(R.drawable.ic_media_pause);
					break;
				default:
					button.setImageResource(isOnPrepare ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
			}
			/*if (isOnPrepare)
				button.setVisibility(View.GONE);
			else if (button.getVisibility() != View.VISIBLE)
				button.setVisibility(View.VISIBLE);*/
		} else
			button.setImageResource(R.drawable.ic_media_play);//button.setVisibility(View.GONE);
		if (button.getVisibility() != View.VISIBLE)
			button.setVisibility(View.VISIBLE);
	}

	private static FloatingActionButton getPlayStopButton() {
		return floatingActionButton;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if(menu != null) {
			setMenuItemVisibility(menu.findItem(R.id.sort_ascending), MediaPlayerService.mRecordsSortAscending);
			setMenuItemVisibility(menu.findItem(R.id.shortening_names), MediaPlayerService.mRecordsShortingNames);
		}
		return super.onMenuOpened(featureId, menu);
	}
	private void setMenuItemVisibility(MenuItem item, boolean checked)
	{
		if(item != null){
			boolean show = mViewPager.getCurrentItem() == MANAGE_RECORDINGS;
			item.setVisible(show);
			if (show)
				item.setChecked(checked);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		initManageRecordingMenuItem(menu, R.id.sort_ascending);
		initManageRecordingMenuItem(menu, R.id.shortening_names);
		return true;
	}
	private void initManageRecordingMenuItem(Menu menu,int id){
		MenuItem item = menu.findItem(id);
		if(item != null) {
			switch(id){
				case R.id.sort_ascending:
					item.setChecked(MediaPlayerService.mRecordsSortAscending);
					break;
				case R.id.shortening_names:
					item.setChecked(MediaPlayerService.mRecordsShortingNames);
					break;
				default:
					return;

			}
			item.setOnMenuItemClickListener(mOnMenuItemClickListener);
		}

	}
	private MenuItem.OnMenuItemClickListener mOnMenuItemClickListener = new MenuItem.OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick(MenuItem menuItem) {
			SharedPreferences.Editor ed = mPreferences.edit();
			switch(menuItem.getItemId()) {
				case R.id.sort_ascending:
					MediaPlayerService.mRecordsSortAscending = !MediaPlayerService.mRecordsSortAscending;
					ed.putBoolean(RECORDS_SORT_ASCENDING, MediaPlayerService.mRecordsSortAscending);
					break;
				case R.id.shortening_names:
					MediaPlayerService.mRecordsShortingNames = !MediaPlayerService.mRecordsShortingNames;
					ed.putBoolean(RECORDS_SHORTING_NAMES, MediaPlayerService.mRecordsShortingNames);
					break;
				default:
					return false;
			}

			ed.commit();
			if (mViewPager.getCurrentItem() == MANAGE_RECORDINGS)
				((ManageRecordingsFrame) mSectionsPagerAdapter.getItem(MANAGE_RECORDINGS)).initRecordedStationsView();
			return false;
		}
	};
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
				if(mBound) {
					StringBuffer message = new StringBuffer();
					message.append(mService.getConnectivityStatus().toString());
					MediaPlayerService.showMessageInPopup(message, Toast.LENGTH_LONG * 10, true);
				}
				return true;
			case R.id.action_settings:
				intent = new Intent(this, SettingsActivity.class);
				startActivityForResult(intent, FavoriteStationsFrame.RESULT_SETTINGS);
				return true;
			/*case R.id.action_add:
				intent = new Intent(this, SearchForStationsFrame.class);
				startActivityForResult(intent, FavoriteStationsFrame.RESULT_ADD_STATIONS);
				return true;*/
			case R.id.action_help:
				intent = new Intent(this, HelpActivity.class);
				intent.putExtra(HelpActivity.ARG_SECTION_NUMBER, mViewPager.getCurrentItem());
				startActivity(intent);
				return true;
			/*case R.id.action_help1:
				intent = new Intent(this, HelpTabbedActivity.class);
				intent.putExtra(HelpTabbedActivity.PlaceholderFragment.ARG_SECTION_NUMBER, mViewPager.getCurrentItem());
				startActivity(intent);
				return true;*/
			case android.R.id.home:
			case R.id.action_exit:
				Handler delayedPost = new Handler();
				delayedPost.postDelayed(new Runnable() {
					@Override
					public void run() {
						exitApp();
					}
				}, 100);//exitApp();
				return true;
			case R.id.action_volume_down:
			case R.id.action_volume_up:
				if(mBound)
					mService.changeVolumeLevel(id==R.id.action_volume_up);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
		this.startActivity(intent);
		//this.onKeyDown(KeyEvent.KEYCODE_HOME, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME));
		//mExitDeclared = true;
		//finish();
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode){
			case KeyEvent.KEYCODE_BACK:
				//return super.onKeyDown(KeyEvent.KEYCODE_HOME, event);//Do something here;
			case KeyEvent.KEYCODE_HOME:
				break;
		}
		return super.onKeyDown(keyCode, event);
	}
	@Override
	public void onStart() {
		super.onStart();
		registerBroadcastReceivers("onStart");
	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterBroadcastReceivers("onStop");
			//unBindMPService();
		//unregisterBroadcastReceiver("onStop");
	}
	@Override
	public void onPause() {
		super.onPause();
		Log.e("MTA:onPause:","");
		//unBindMPService();
	};

	@Override
	public void onResume() {
		super.onResume();
		reReadSettings();

		Log.e("MTA:onResume:","");
		//bindMPService();
		/*
		IntentFilter intentFilter = new IntentFilter(MediaPlayerService.CUSTOM_EVENT);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter);*/
	}
	/*
	private View.OnSystemUiVisibilityChangeListener onSystemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener(){
		@Override
		public void onSystemUiVisibilityChange(int visible) {
			if(visible == View.VISIBLE) {
				adjustScrollingHeight();
			}
		}
	};
	*/

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e("MTA:onDestroy:","");
		//unBindMPService();
		//unregisterBroadcastReceiver("onDestroy");
	}
	public static boolean mBound = false;
	public static MediaPlayerService mService;


	public ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			MediaPlayerService.LocalMPBinder binder = (MediaPlayerService.LocalMPBinder) service;
			mService = binder.getService();
			mBound = true;
			if (mService.isAutoPlayCase() && getBooleanPreference(getString(R.string.autoplay_switch), false) && mSavedStationDefinition != null)
				doClickOnStopPlayButton(/*getPlayStopButton(), */false);
			synchronizePlayPauseButton();
			FragmentManager fm = getSupportFragmentManager();
			if (fm != null) {
				List<Fragment> fragments = fm.getFragments();
				if(fragments != null) {
					for (Fragment fragment : fragments) {
						if (fragment != null) {
							if (fragment instanceof FavoriteStationsFrame) {
								((FavoriteStationsFrame) fragment).synchronizeOnServiceConnected(mService);// do something
								Log.e("MTA:onServ.Conn.", "FSF:synchronizeOnServiceConnected");
								break;
							}
						}
					}
				}
			}
			Log.e("MTA:onServ.Conn.", className.toString());
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.e("MTA:onServ.Disc.", arg0.toString());
		}
	};

	private boolean intentDataDecoder(String code1, String code2){
		boolean ret = false;
		if(code1 != null) {
			switch (code1.split(" ")[0]) {
				case MediaPlayerService.CUSTOM_EVENT:
					ret = intentDataDecoder(code2, null);
					break;
				case MediaPlayerService.ACTION_EXIT:
					exitApp();
					ret = true;
					break;
				case MediaPlayerService.ACTION_PAUSE:
					doClickOnStopPlayButton(true);//pausePlay();
					ret=true;
					break;
				case MediaPlayerService.ACTION_PLAY:
					doClickOnStopPlayButton(false);//reStartPlay();
					ret = true;
					break;
				case MediaPlayerService.CURRENT_STATUS_IN_PAUSE:// = "Pause";
				case MediaPlayerService.CURRENT_STATUS_IN_PLAY:// = "Playing";
				case MediaPlayerService.CURRENT_STATUS_IN_STOP:// = "Stopped";
				case MediaPlayerService.CURRENT_STATUS_CHANGED://= "Changed";
				case MediaPlayerService.CURRENT_STATUS_IN_PLAY_ERROR:// = "Error";
				case MediaPlayerService.CURRENT_STATUS_RESUMED://Resume
					synchronizePlayPauseButton();
					break;
			}
		}
		return ret;
	}
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			int type = intent.getIntExtra(MediaPlayerService.CUSTOM_EVENT_CATEGORY, 0);
			String message = intent.getStringExtra(MediaPlayerService.CUSTOM_EVENT_MESSAGE);// Get extra data included in the Intent
			Log.e("MTA:onReceive", (action!=null?action+"|":"")+message);
			if(intentDataDecoder(action, message))
				return;
			int pos = message.indexOf(' ');
			if(pos > 0 && pos < message.length() - 1) {
				String code1 = message.substring(0, pos-1);
				String code2 = null;
				if(pos < message.length() - 2) {
					code2 = code1;
					code1 = message.substring(pos + 1);
				}
				if (intentDataDecoder(code1, code2))
					return;
				else {
					int code = 0;
					String command = message.substring(0, pos);
					try {
						code = Integer.parseInt(command);
						String param = message.substring(pos + 1);
						switch (code) {
							//case MediaPlayerService.TRANSFER_COMMAND_SHOW_CONTROLS:
							case MediaPlayerService.TRANSFER_COMMAND_BUTTONS_VISIBILITY:
								switch (param) {
									case MediaPlayerService.CURRENT_STATUS_IN_STOP:
									case MediaPlayerService.CURRENT_STATUS_IN_PAUSE:
									case MediaPlayerService.CURRENT_STATUS_IN_PLAY:
									case MediaPlayerService.CURRENT_STATUS_CHANGED:
										synchronizePlayPauseButton();
										break;
									case MediaPlayerService.ACTION_EXIT:
										exitApp();
										break;
								}
								break;
							case MediaPlayerService.TRANSFER_COMMAND_SHOW_CONTROLS:
								switch (param) {
									case MediaPlayerService.CURRENT_STATUS_IN_PLAY:
									case MediaPlayerService.CURRENT_STATUS_IN_PAUSE:
									case MediaPlayerService.CURRENT_STATUS_IN_STOP:
										synchronizePlayPauseButton();
										break;
									case MediaPlayerService.CURRENT_STATUS_IN_PLAY_ERROR:
										break;
								}
						}
						return;
					} catch (Exception e) {
						if(!(e instanceof NumberFormatException))
							e.printStackTrace();
					}
				}
				//if(extra.equals(MediaPlayerService.DO_NOT_ADD_STATION))
				//	askWhenrAddingStation(mService.getCurrentStationUrl(), mService.getCurrentStationName(),3);
				switch (type) {
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_ADD_RADIO_NAME:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_ON_PREPARED_FINISHED:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_END_BY_NO_CONNECTION:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_END_BY_NO_DATA:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_PAUSE_BY_NO_CONNECTION:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RESTORE_BY_CONNECTION:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_FINISHED:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_TO_NEXT_FILE:
						break;
					case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_RECORD_STOP_NO_SPACE:
						break;
					default:
				}
			}
		};
	};
	private static int getIntegerPreference(String preferenceName, int value) {
		boolean exists = mPreferences.contains(preferenceName);
		int savedValue = Integer.parseInt(mPreferences.getString(preferenceName, String.valueOf(value)));
		if(!exists) {
			SharedPreferences.Editor ed = mPreferences.edit();
			ed.putString(preferenceName, String.valueOf(savedValue));
			ed.commit();
		}
		return savedValue;
	}
	private static boolean getBooleanPreference(String preferenceName, boolean value) {
		boolean exists = mPreferences.contains(preferenceName);
		boolean savedValue = mPreferences.getBoolean(preferenceName, value);
		if(!exists) {
			SharedPreferences.Editor ed = mPreferences.edit();
			ed.putBoolean(preferenceName, savedValue);
			ed.commit();
		}
		return savedValue;
	}
	public void exitApp() {
		this.stopService(new Intent(this, MediaPlayerService.class));
		/*if (mBound) {
			mService.prepareForStop();
			mBound = false;
		}*/
		mExitDeclared = true;
		this.finish();
		Log.e("MTA","exitApp");
	}
	private static boolean startPlaying() {
		boolean bRes = false;
		String message = null;
		if (mSavedStationDefinition != null) {
			if (mBound) {
				getPlayStopButton().setVisibility(View.GONE);
				// Call a method from the LocalService.
				// However, if this call were something that might hang, then this request should
				// occur in a separate thread to avoid slowing down the activity performance.
				mService.startPlay(mSavedStationDefinition);//((RadioButton) mSelectedStationUrl).getText().toString(), type);
				saveSelectedStationToPreferences(mSavedStationDefinition);
				bRes= true;
			}
			else {
				message = "No Media Player Service";
			}
		} else
			message = "No selected station";
		getPlayStopButton().setVisibility(View.VISIBLE);
		return bRes;
	}
	public static void saveSelectedStationToPreferences(MediaPlayerService.PlayingInfo currentPlayingStationDef) {
		SharedPreferences.Editor ed = mPreferences.edit();
		ed.putString(MediaPlayerService.SELECTED_STATION_DEFINITION, currentPlayingStationDef.serialize());
		ed.commit();
	}
	public static MediaPlayerService.PlayingInfo getSavedStationDefinition(Bundle savedInstanceState){
		MediaPlayerService.PlayingInfo prefDef = new MediaPlayerService.PlayingInfo(mPreferences.getString(MediaPlayerService.SELECTED_STATION_DEFINITION, null));
		if(!prefDef.isValid())
			prefDef = null;
		if(savedInstanceState!= null)
		{
			MediaPlayerService.PlayingInfo instanceDef = new MediaPlayerService.PlayingInfo(savedInstanceState.getStringArray(MediaPlayerService.SELECTED_STATION_DEFINITION));
			String	prefStation = prefDef!= null ? prefDef.uri : null;
			if(instanceDef != null && prefStation != null && !prefStation.equals(instanceDef.uri)) {
				saveSelectedStationToPreferences(prefDef = instanceDef);				;
			}
		}
		return prefDef;
	}

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		super.onSaveInstanceState(outState, outPersistentState);
		if (mSavedStationDefinition != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				outPersistentState.putStringArray(MediaPlayerService.SELECTED_STATION_DEFINITION, mSavedStationDefinition.serializeToStringArray());
			}
		}
	}

	public static void reReadSettings() {
		reReadSettings(false);
	}

	private static String RECORDS_SORT_ASCENDING;
	private static String RECORDS_SHORTING_NAMES;
	public static void reReadSettings(boolean ignoreForFirstRun) {
		final String FONT_SIZE_FOR_SEARCH = mResources.getString(R.string.font_size_for_search_value);
		final String FONT_SIZE = mResources.getString(R.string.font_size_value);
		final String PADDING_TOP = mResources.getString(R.string.space_size_value);
		final String HEADPHONE_MONITOR = mResources.getString(R.string.headphone_switch);
		final String AUTOPLAY_MONITOR = mResources.getString(R.string.autoplay_switch);
		final String BLUETOOTH_MONITOR = mResources.getString(R.string.bluetooth_switch);
		final String PHONECALL_MONITOR = mResources.getString(R.string.phonecall_switch);
		final String MAX_FILE_SIZE = mResources.getString(R.string.max_file_size);
		final String AUDIOLEVEL_MONITOR = mResources.getString(R.string.audiolevel_switch);
		final String THEME = mResources.getString(R.string.themes_list);
		final String ROOT_DIRECTORY_FOR_RECORDINGS = mContext.getString(R.string.root_directory_for_recordings);
		MediaPlayerService.mPrefFontSizeForSearch = getIntegerPreference(FONT_SIZE_FOR_SEARCH, 11);
		MediaPlayerService.mPrefFontSize = getIntegerPreference(FONT_SIZE, 11);//preferences.getInt(FONT_SIZE, 11);
		MediaPlayerService.mPrefPaddingTop = getIntegerPreference(PADDING_TOP, 5);//preferences.getInt(PADDING_TOP, 20);

		MediaPlayerService.mMonitorPhoneCall = getBooleanPreference(PHONECALL_MONITOR, true);
		MediaPlayerService.mMonitorBluetooth = getBooleanPreference(BLUETOOTH_MONITOR, true);
		MediaPlayerService.mMonitorHeadphones = getBooleanPreference(HEADPHONE_MONITOR, true);
		MediaPlayerService.mMonitorAudioLevelOnHeadphones = getBooleanPreference(AUDIOLEVEL_MONITOR, true);
		MediaPlayerService.mMaxRecordingSize = getIntegerPreference(MAX_FILE_SIZE,8)*(1024*1024);
		MediaPlayerService.mTheme = Integer.parseInt(mPreferences.getString(THEME, "0"));
		MediaPlayerService.mRootDirectoryForRecordings = mPreferences.getString(ROOT_DIRECTORY_FOR_RECORDINGS, MediaPlayerService.mRootDirectoryForRecordings);
		MediaPlayerService.mRecordsSortAscending = getBooleanPreference(RECORDS_SORT_ASCENDING, false);
		if(!ignoreForFirstRun)
			setCurrentTheme(MediaPlayerService.mTheme);
	}

	private static void setCurrentTheme(int themeId) {
		changeToTheme(mThisActivity,themeId);
	}

	//permissions
	public static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
	public static final int MY_PERMISSIONS_REQUEST_WRITE = 2;
	public static final int MY_PERMISSIONS_REQUEST_MICROPHONE = 3;
	private static final int PERMISSION_REQUEST_REJECTED = -1;
	private static final int PERMISSION_REQUEST_WAITING = 0;
	private static final int PERMISSION_REQUEST_GRUNTED = 1;
	private boolean mWriteAccessGrunted = true;
	private boolean mAudioAccessGrunted = true;

	private File getAvailableForStoreDirectory(){
		return mWriteAccessGrunted ? Environment.getExternalStorageDirectory() : Environment.getDataDirectory();
	}
	private void checkPermissionsOnInitialization(){
		switch (getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_WRITE)) {
			case PERMISSION_REQUEST_REJECTED:// Show an expanation to the user *asynchronously* -- don't block
				mWriteAccessGrunted = false;// this thread waiting for the user's response! After the user
				break;// No access - continue without wait.
			case PERMISSION_REQUEST_WAITING:
				mWriteAccessGrunted = false;// this thread waiting for the user's response! After the user
				break;
			case PERMISSION_REQUEST_GRUNTED:
				mWriteAccessGrunted = true;
				break;
		}
	}
	private int getPermission(String permission, int myPermissionRequestId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (mContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				// Should we show an explanation?
				if (shouldShowRequestPermissionRationale(permission)) {
					return PERMISSION_REQUEST_REJECTED;
					// Show an expanation to the user *asynchronously* -- don't block
					// this thread waiting for the user's response! After the user
					// sees the explanation, try again to request the permission.
				} else {
					// No explanation needed, we can request the permission. myPermissionRequestId is an
					requestPermissions(new String[]{permission}, myPermissionRequestId);// app-defined int constant. The callback method gets the result of the request.
					return PERMISSION_REQUEST_WAITING;
				}
			}
		}
		return PERMISSION_REQUEST_GRUNTED;
	}
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_WRITE:

				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {// If request is cancelled, the result arrays are empty.
					mWriteAccessGrunted = true;// permission was granted, Do the contacts-related task you need to do.
				} else {
					mWriteAccessGrunted = false;// permission denied! Disable the functionality that depends on this permission.
				}
				informMediaPlayerServiceAboutPermission(MY_PERMISSIONS_REQUEST_WRITE, mWriteAccessGrunted);
				break;
			case MY_PERMISSIONS_REQUEST_MICROPHONE: {

				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {// If request is cancelled, the result arrays are empty.
					mAudioAccessGrunted = true;// permission was granted, Do the contacts-related task you need to do.
				} else {
					mAudioAccessGrunted = false;// permission denied! Disable the functionality that depends on this permission.
				}
				informMediaPlayerServiceAboutPermission(MY_PERMISSIONS_REQUEST_MICROPHONE, mAudioAccessGrunted);
				break;
			}
			// other 'case' lines to check for other
			// permissions this app might request
			/*case MY_PERMISSIONS_REQUEST_CAMERA:

				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {// If request is cancelled, the result arrays are empty.
					;//continueOnCreate();// permission was granted, Do the contacts-related task you need to do.
				} else {// permission denied! Disable the functionality that depends on this permission.
					mCameraoAccessGrunted = false;
					setContentView(R.layout.no_permission_message);// this thread waiting for the user's response! After the user
				}
				break;*/
		}
	}

	private void informMediaPlayerServiceAboutPermission(int myPermissions, boolean grunted) {
		if(mBound)
			mService.informAboutPermission(myPermissions, grunted);
	}
	private void registerBroadcastReceivers(String src) {
		registerBroadcastReceivers(src, false);
	}
	private void registerBroadcastReceivers(String src, boolean serviceOnly) {
		Log.e("MTA:regBR", src);
		IntentFilter filter = new IntentFilter();
		filter.addAction(MediaPlayerService.ACTION_EXIT);
		filter.addAction(MediaPlayerService.ACTION_PAUSE);
		filter.addAction(MediaPlayerService.ACTION_PLAY);
		filter.addAction(MediaPlayerService.CUSTOM_EVENT);
		if(!serviceOnly) {
			try {
				LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try{
			Intent intent = new Intent(this, MediaPlayerService.class);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
			else
				getApplicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		}catch(Exception e){e.printStackTrace();};
	}
	private void unregisterBroadcastReceivers(String src) {
		Log.e("MTA:unregBR", src);
		if (mBound) {
			mBound = false;
			try{
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					unbindService(mServiceConnection);
				else
					getApplicationContext().unbindService(mServiceConnection);
			}catch(Exception e){e.printStackTrace();}
		}
		try{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		}catch(Exception e){e.printStackTrace();};
	}
	//public class Utils
	//{
	private static int mCurrentTheme = 0;
	public final static int THEME_DARK = 0;
	public final static int THEME_MEDIUM = 1;
	public final static int THEME_LIGHT = 2;
	/**
	 * Set the theme of the Activity, and restart it by creating a new Activity of the same type.
	 */
	public static void changeToTheme(Activity activity, int theme)
	{
		if(mCurrentTheme != theme) {
			mCurrentTheme = theme;
			activity.finish();
			activity.startActivity(new Intent(activity, activity.getClass()));
		}
	}
	/** Set the theme of the activity, according to the configuration. */
	public static void onActivityCreateSetTheme(Activity activity)
	{
		switch (mCurrentTheme)
		{
			default:THEME_DARK:
				activity.setTheme(R.style.DarkTheme);
				break;
			case THEME_MEDIUM:
				activity.setTheme(R.style.MediumTheme);
				break;
			case THEME_LIGHT:
				activity.setTheme(R.style.LightTheme);
				break;
		}
	}
	//}
	public static int generateUniqueId(){
		int id;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
			id = View.generateViewId();
		else
			id = generateViewId();
		return id;
	}
	// for API less then
	/**
	 * Generate a value suitable for use in View.setId().
	 * This value will not collide with ID values generated at build time by aapt for R.id.
	 *
	 * @return a generated ID value
	 */
	public static int generateViewId() {
		do {
			final int result = sNextGeneratedId.get();
			// aapt-generated IDs have the high byte nonzero; clamp to the range under that.
			int newValue = result + 1;
			if (newValue > 0x00FFFFFF){
				newValue = 1; // Roll over to 1, not 0.
			}
			if (sNextGeneratedId.compareAndSet(result, newValue)) {
				return result;
			}
		}while(true);
	}
	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
}
