package com.zome.android.webspidola;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class ManageFavoriteStationsFrame extends Fragment{
	private static LayoutInflater mFragmentInflater;
	private static ViewGroup mFragmentContainer;
	private static Bundle mFragmentSavedInstanceState;
	private static View mFragmentRootView;

	public ManageFavoriteStationsFrame() {
	}
	public static ManageFavoriteStationsFrame newInstance(int sectionNumber){
		final ManageFavoriteStationsFrame fragment = new ManageFavoriteStationsFrame();
		final Bundle args = new Bundle();
		args.putInt(MainTabbedActivity.ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mFragmentInflater = inflater;
		mFragmentContainer = container;
		mFragmentSavedInstanceState =savedInstanceState;
		mFragmentRootView = inflater.inflate(R.layout.frame_manage_stations, container, false);
	/*
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frame_manage_stations);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
*/
		FloatingActionButton fab = (FloatingActionButton) mFragmentRootView.findViewById(R.id.fab_edit);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Saving Configuration", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
				finishListEdit();
			}
		});
		fab.setOnTouchListener(MainTabbedActivity.onTouchListener);
		//getActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ArrayList<String[]> stationsList = FavoriteStationsFrame.deSerializeStationsList(FavoriteStationsFrame.preferences.getString(FavoriteStationsFrame.PREFERENCE_LIST_OF_STATIONS, null));
		initializeStationsList(stationsList);
		return mFragmentRootView;
	}
	private void finishListEdit(){
		LinearLayout list = (LinearLayout) mFragmentRootView.findViewById(R.id.scrollViewForStations);
		ArrayList<String[]> stationsDefinition = new ArrayList<>();
		CheckBox checkBox;
		LinearLayout row;
		String[] definition;
		for(int i =0; list.getChildCount()>i;i++){
			row = (LinearLayout) list.getChildAt(i);
			checkBox = ( CheckBox) row.getChildAt(0);
			if(checkBox.isChecked()){
				definition = (new MediaPlayerService.PlayingInfo(checkBox)).serializeToStringArray();
				/*definition[0] = checkBox.getText().toString();
				definition[1] = checkBox.getTag(FavoriteStationsFrame.TAG_URI).toString();
				definition[2] = checkBox.getTag(FavoriteStationsFrame.TAG_TYPE).toString();*/
				stationsDefinition.add(definition);
			}
		}
		Intent resultIntent = new Intent();
		resultIntent.putExtra(FavoriteStationsFrame.RETURN_ADDED_STATIONS_LIST, FavoriteStationsFrame.serializeStationsList(stationsDefinition));
		if (getActivity().getParent() == null) {
			getActivity().setResult(Activity.RESULT_OK, resultIntent);
		} else {
			getActivity().getParent().setResult(Activity.RESULT_OK, resultIntent);
		}
		FavoriteStationsFrame.commitStationsList(stationsDefinition, FavoriteStationsFrame.PREFERENCE_LIST_OF_STATIONS_CHANGED);
		//int position = MainTabbedActivity.mViewPager.getCurrentItem();
		MainTabbedActivity.mViewPager.setCurrentItem(MainTabbedActivity.FAVORITE_STATIONS, true);//getActivity().finish();
	}
	private void initializeStationsList(ArrayList<String[]> stationsDefinition) {
		LinearLayout list = (LinearLayout) mFragmentRootView.findViewById(R.id.scrollViewForStations);
		list.removeAllViews();
		CheckBox checkBox;
		LinearLayout row;
		View moveControl;
		String[] definition;
		int length = stationsDefinition.size();
		for (int i = 0; length > i; i++) {
			/*getLayoutInflater()*/mFragmentInflater.inflate(R.layout.template_checkbox, list);
			row = (LinearLayout)list.getChildAt(list.getChildCount() - 1);
			//row.setOnTouchListener(mOnTouchListener);
			checkBox = (CheckBox) row.getChildAt(0);
			//checkBox.setOnTouchListener(mOnTouchListener);
			setMoveContril(row.getChildAt(1));
			setMoveContril(row.getChildAt(2));
			MediaPlayerService.setButtonByDefinition(checkBox, new MediaPlayerService.PlayingInfo(stationsDefinition.get(i)));
		}
	}
	private View setMoveContril(View v){
		if(v != null && v instanceof TextView){
			TextView view = (TextView) v;
			view.setOnClickListener(moveControlClickListener);
			view.setTextSize(TypedValue.COMPLEX_UNIT_PT, MediaPlayerService.mPrefFontSize);
		}
		return v;
	}
	private View.OnClickListener moveControlClickListener = new View.OnClickListener(){

		@Override
		public void onClick(View view) {
			int delta = Integer.parseInt((String) view.getTag());
			ViewGroup row = (ViewGroup) view.getParent();
			ViewGroup parent = (ViewGroup) row.getParent();
			int i = 0;
			for (; parent.getChildCount() > i; i++) {
				if (parent.getChildAt(i) == row) {
					//if (delta < 0)
						i += delta;
					parent.removeView(row);
					if (i < 0)
						i = parent.getChildCount() - 1;
					else if (i >= parent.getChildCount())
						i = 0;
					parent.addView(row, i);
				}
			}
			ScrollView scroller = null;
			ViewParent v=parent.getParent();
			while(v != null && !(v instanceof ScrollView))
				v = v.getParent();
			if(v != null) {
				((ScrollView) v).scrollBy(0, -delta);
			}
		}
	};
	@Override
	public void onStart() {
		super.onStart();
		/*Intent intent = new Intent(this, MediaPlayerService.class);
		mFragmentContainer.getContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);*/
	}
	@Override
	public void onStop(){
		super.onStop();
		//unBindMPService();
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		//unBindMPService();
	}


	/*
	private boolean mBound = false;
	private MediaPlayerService mService;
	private void unBindMPService() {
		if(mBound){
			mFragmentContainer.getContext().unbindService(mServiceConnection);
			mBound = false;
		}
	}

	public ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			MediaPlayerService.LocalMPBinder binder = (MediaPlayerService.LocalMPBinder) service;
			mService = binder.getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			//Log.d("onServiceDisconnected", arg0.toString());
		}
	};*/
}
