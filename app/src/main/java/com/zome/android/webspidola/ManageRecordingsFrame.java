package com.zome.android.webspidola;
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zome.android.webspidola.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


/**
 * An activity for selecting from a number of savedRecords.
 */
public class ManageRecordingsFrame extends Fragment {
	private static final String EXPANDED_GROUPS_LIST = "EXPANDED_GROUPS_LIST";
	private static LayoutInflater mFragmentInflater;
	private static ViewGroup mFragmentContainer;
	private static Bundle mFragmentSavedInstanceState;
	private static View mFragmentRootView;
	private RecordsListAdapter mRecordsListAdapter = null;

	public static ManageRecordingsFrame newInstance(int sectionNumber, int columnCount){
		final ManageRecordingsFrame fragment = new ManageRecordingsFrame();
		final Bundle args = new Bundle();
		args.putInt(MainTabbedActivity.ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public static ManageRecordingsFrame newInstance(int sectionNumber) {
		return newInstance(sectionNumber, 1);
	}	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ManageRecordingsFrame() {
	}
	private List<SavedRecordsGroup> mRecordingsGroups;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		mFragmentInflater = inflater;
		mFragmentContainer = container;
		mFragmentSavedInstanceState =savedInstanceState;
		mFragmentRootView = inflater.inflate(R.layout.frame_manage_recordings, container, false);
		return mFragmentRootView;
	}
	public View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			switch(view.getId()){
				case R.id.overall_previous:
				case R.id.overall_next:
					MediaPlayerService.BatchPlayingStat batchPlayingStat = MediaPlayerService.BatchPlayingStat.getInstance(null);
					batchPlayingStat.seekByOverallControl(Integer.parseInt((String) view.getTag()));

					break;
			}
		}
	};
	private Bundle mSavedInstanceState = null;
	private boolean OnResumeDone = false;
	public void onResume(){
		super.onResume();
		ArrayList<String> expandedGroups = mSavedInstanceState != null ? mSavedInstanceState.getStringArrayList(EXPANDED_GROUPS_LIST):getExpandedGroups();
		mSavedInstanceState = null;
		initRecordedStationsView(expandedGroups);//Get recording content
		showBatchPlayingProgress();
		OnResumeDone = true;
	}
	@Override
	public void onPause(){
		super.onPause();
		if(customHandler!=null){
			customHandler.removeCallbacks(updateTimerThread);
			customHandler = null;
		}
		OnResumeDone = false;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		ArrayList<String> expandedGroups = getExpandedGroups();
		if(expandedGroups!=null)
			outState.putStringArrayList(EXPANDED_GROUPS_LIST, expandedGroups);
	}
	private ArrayList<String> getExpandedGroups(){
		ArrayList<String> expandedGroups = null;
		if(mRecordsListAdapter != null) {
			ExpandableListView recordingGroupsList = (ExpandableListView) mFragmentRootView.findViewById(R.id.recordings_list);
			if(recordingGroupsList != null && recordingGroupsList.getAdapter() != null){
				expandedGroups = new ArrayList<>();
				int groupCount = mRecordsListAdapter.getGroupCount();
				for (int i = 0; groupCount > i; i++) {
					if (recordingGroupsList.isGroupExpanded(i))
						expandedGroups.add(String.valueOf(i));
				}
				if (expandedGroups.size() == 0)
					expandedGroups = null;
			}
		}
		return expandedGroups;
	}
	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null && OnResumeDone)
			expandGroups(savedInstanceState.getStringArrayList(EXPANDED_GROUPS_LIST));
		else
			mSavedInstanceState = savedInstanceState;
	}

	private void expandGroups(ArrayList<String> expandedGroups){
		if(expandedGroups != null && expandedGroups.size()>0) {
			ExpandableListView recordingGroupsList = (ExpandableListView) mFragmentRootView.findViewById(R.id.recordings_list);
			int id;
			for (String groupId : expandedGroups) {
				if (mRecordsListAdapter.getGroup(id = Integer.parseInt(groupId)) != null)
					recordingGroupsList.expandGroup(id);
			}
		}
	}
	private void showBatchPlayingProgress(){
		MediaPlayerService.BatchPlayingStat batchPlayingStat = MediaPlayerService.BatchPlayingStat.getInstance(null);
		/*int position = batchPlayingStat.getCurrentPercentageInPlay();
		if(position >= 0){*/
			customHandler = new android.os.Handler();
			customHandler.postDelayed(updateTimerThread, 500);
		//}
	}
	private MediaPlayerService.PlayingInfo mPreviousBatchPlayingInfo = null;
	private android.os.Handler customHandler;
	private Runnable updateTimerThread = new Runnable(){
		public void run(){
			MediaPlayerService.BatchPlayingStat batchPlayingStat = MediaPlayerService.BatchPlayingStat.getInstance(null);
			int position = batchPlayingStat.getCurrentPercentageInPlay();
			View playingIndicators = mFragmentRootView.findViewById(R.id.manage_recordings_controls);
			LinearLayout previousView;
			if(playingIndicators == null && mPreviousBatchPlayingInfo != null) {
				previousView = (LinearLayout)findViewByInfo(mPreviousBatchPlayingInfo);
				if(previousView != null && previousView.getChildCount() == 2 )
					playingIndicators = previousView.getChildAt(1);
			}
			if(position >= 0) {
				MediaPlayerService.PlayingInfo currentPlayingInfo = batchPlayingStat.getCurrentPlaying();
				if(mPreviousBatchPlayingInfo == null || (currentPlayingInfo != null && !currentPlayingInfo.equalTo(mPreviousBatchPlayingInfo))){
					previousView = (LinearLayout) (mPreviousBatchPlayingInfo != null ? findViewByInfo(mPreviousBatchPlayingInfo):null);
					if(previousView != null && previousView.getChildCount() == 2)
						previousView.removeViewAt(1);
					LinearLayout currentView = (LinearLayout) findViewByInfo(mPreviousBatchPlayingInfo = currentPlayingInfo);
					if(currentView != null)
						currentView.addView(playingIndicators=inflatePlayingIndicators());
				}
				if(playingIndicators!= null) {
					playingIndicators.setVisibility(View.VISIBLE);
					setProgressValue(R.id.overallProgressBar, batchPlayingStat.getCurrentPercentageInList());
					setProgressValue(R.id.progressBar, position);

					long duration = batchPlayingStat.getDuration();
					setTextValue(R.id.duration, MediaPlayerService.formatTimeString(duration));
					setTextValue(R.id.current_time, MediaPlayerService.formatTimeString(Math.round(duration * (position / 100.0d))));
					setTextValue(R.id.current_name, currentPlayingInfo==null?"":currentPlayingInfo.name);
				}
				if(customHandler ==null)
					customHandler = new android.os.Handler();
				customHandler.postDelayed(this, 500);
			}
			else if(playingIndicators != null)
				playingIndicators.setVisibility(View.GONE);
		}
	};
	private void setTextValue(int id, String value) {
		TextView view = (TextView) mFragmentRootView.findViewById(id);
		if(view != null)
			view.setText(value);
	}
	private void setProgressValue(int id, int value) {
		SeekBar progress = (SeekBar) mFragmentRootView.findViewById(id);
		if(progress != null)
			progress.setProgress(value);
	}
	private View inflatePlayingIndicators() {
		LinearLayout playingIndicators = (LinearLayout) mFragmentInflater.inflate(R.layout.template_playing_indicators, mFragmentContainer, false);
		SeekBar progress = (SeekBar) playingIndicators.findViewById(R.id.overallProgressBar);
		progress.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		progress = (SeekBar) playingIndicators.findViewById(R.id.progressBar);
		progress.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		playingIndicators.findViewById(R.id.overall_previous).setOnClickListener(mOnClickListener);
		playingIndicators.findViewById(R.id.overall_next).setOnClickListener(mOnClickListener);
		return playingIndicators;
	}

	private View findViewByInfo(MediaPlayerService.PlayingInfo playingInfo) {
		View view = null;
		ExpandableListView recordingGroupsList = (ExpandableListView) mFragmentRootView.findViewById(R.id.recordings_list);
		expandGroupByPlayInfo(playingInfo, recordingGroupsList);
		int childCount = recordingGroupsList.getChildCount();
		MediaPlayerService.PlayingInfo tagInfo;
		for (int j = 0; childCount > j; j++) {
			view = recordingGroupsList.getChildAt(j);
			if (view instanceof LinearLayout) {
				tagInfo = (MediaPlayerService.PlayingInfo) view.getTag();
				if (tagInfo != null && tagInfo.equalTo(playingInfo)) {
					return view;
				}
			}
		}
		return null;
	}
	private SavedRecordsGroup expandGroupByPlayInfo(MediaPlayerService.PlayingInfo info, ExpandableListView recordingGroupsList){
		SavedRecordsGroup group;
		int count = mRecordsListAdapter.getGroupCount();
		int childCount;
		for(int i=0;count>i;i++){
			group = mRecordsListAdapter.getGroup(i);
			childCount = group.size();
			for(int j=0; childCount>j;j++)
				if(mRecordsListAdapter.getChild(i,j).equalTo(info)) {
					if(!recordingGroupsList.isGroupExpanded(i))
						recordingGroupsList.expandGroup(i);
					return group;
				}
		}
		return null;
	}
	private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if(fromUser) {
				MediaPlayerService.BatchPlayingStat batchPlayingStat = MediaPlayerService.BatchPlayingStat.getInstance(null);
				switch (seekBar.getId()) {
					case R.id.progressBar:
						batchPlayingStat.setCurrentPercentageInPlay(progress);
						break;
					case R.id.overallProgressBar:
						break;
				}
			}
		}
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}
	};
	public void initRecordedStationsView(){
		initRecordedStationsView(getExpandedGroups());
	}
	private void initRecordedStationsView(@Nullable ArrayList<String> expandedGroups) {
		mRecordingsGroups = new ArrayList<>();
		SavedRecordsGroup group;
		File sd = getSubDirectory(MediaPlayerService.getAvailableForStoreDirectory()+ "/"+MediaPlayerService.mRootDirectoryForRecordings);
		File subDir, file;
		String[] fileNames, subDirNames;
		HashMap<String,String[]> mapOfFiles = new HashMap<>();
		String name;
		if (sd!=null) {
			subDirNames = sd.list();
			int count = subDirNames.length;
			if (count > 0) {
				for (int i = 0; count > i; i++) {
					subDir = getSubDirectory(sd.getAbsolutePath() + "/" + subDirNames[i]);
					if (subDir != null) {
						fileNames = subDir.list();
						if (fileNames.length > 0) {
							group = new SavedRecordsGroup(subDirNames[i]);// +" ["+fileNames.length+"]");//mapOfFiles.put(subDirNames[i], fileNames);
							for (int j = 0; fileNames.length > j; j++) {
								if(fileNames[j].toLowerCase().endsWith(".mp3")) {
									name = MediaPlayerService.mRecordsShortingNames && fileNames[j].startsWith(subDirNames[i]) ? fileNames[j].substring(subDirNames[i].length()) : fileNames[j];
									file = new File(Uri.parse(subDir.getAbsolutePath() + "/" + fileNames[j]).getPath());
									group.add(MediaPlayerService.PlayingInfo.getInstance(name, file));//.getAbsolutePath(), Util.TYPE_OTHER, file.lastModified()));
								}
							}
							if(group.size()>0) {
								group.sort(MediaPlayerService.mRecordsSortAscending);
								group.modifyTitle();
								mRecordingsGroups.add(group);
							}
						}
					}
				}
			}
			ExpandableListView recordingGroupsList = (ExpandableListView) mFragmentRootView.findViewById(R.id.recordings_list);
			recordingGroupsList.setAdapter(mRecordsListAdapter = new RecordsListAdapter(getContext(), mRecordingsGroups));
			recordingGroupsList.setOnItemLongClickListener(new ExpandableListView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
					openAlertDialogToDelete(adapterView, view, i, l);
					return true;
				}
			});
			recordingGroupsList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
					//onRecordSelected(mRecordingsGroups.get(groupPosition).savedRecords.get(childPosition));
					if (!MainTabbedActivity.mBound)
						MediaPlayerService.showMessageInPopup("No Media Player Service");
					else{
						ArrayList<MediaPlayerService.PlayingInfo> recordDefinitions = new ArrayList<>();
						SavedRecordsGroup group = mRecordingsGroups.get(groupPosition);
						while(group.size() > childPosition){
							recordDefinitions.add(group.savedRecords.get(childPosition++));
						}
						if(recordDefinitions.size()>0) {
							MainTabbedActivity.mService.startPlayingBatch(recordDefinitions);
							showBatchPlayingProgress();
						}
					}
					return true;
				}
			});
			expandGroups(expandedGroups);
		}
	}
	private void openAlertDialogToDelete(AdapterView<?> adapterView, View view, int itemInd, long l) {
		final View toDelete = view;
		Object item = ((ExpandableListView) adapterView).getAdapter().getItem(itemInd);
		final ArrayList<String> urlsToDelete = new ArrayList<>();
		String title = null;
		if(item instanceof SavedRecordsGroup) {
			SavedRecordsGroup group = (SavedRecordsGroup) item;
			for(int j=0;group.savedRecords.size()>j;j++ )
				urlsToDelete.add(group.savedRecords.get(j).uri);
			title = group.title;
		}else if(item instanceof MediaPlayerService.PlayingInfo) {
			MediaPlayerService.PlayingInfo record = (MediaPlayerService.PlayingInfo) item;
			urlsToDelete.add(record.uri);
			title = record.name;
		}
		if(urlsToDelete.size()>0)
		{
			AlertDialog.Builder dialog = new AlertDialog.Builder(mFragmentContainer.getContext()/*FavoriteStationsFrame.this*/);//new AlertDialog.Builder(getApplicationContext(), R.style.AppTheme);
			dialog.setTitle("Delete Recording");
			title= "Would you like to delete "+(urlsToDelete.size() > 1 ? String.valueOf(urlsToDelete.size())+ " records of \n'"+title+ "'" : "recording\n'" + title + "'");
			dialog.setMessage(title);
			dialog.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dlg, int whichButton) {
					for(String url:urlsToDelete) {
						File rec = new File(url);
						if (rec.exists()) {
							rec.delete();
						}
					}
					initRecordedStationsView();
				}
			});
			dialog.setNegativeButton("Keep", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			dialog.show();
		}
	}

	private File getSubDirectory(String absPath) {
		File sd = new File( absPath);
		if (!sd.exists() || !sd.isDirectory())
			sd = null;
		return sd;

	}
	/*
	private void onRecordSelected(MediaPlayerService.PlayingInfo savedRecord) {
		Intent mpdIntent = new Intent(this, PlayerActivity.class)
				.setData(Uri.parse(savedRecord.uri))
				.putExtra(PlayerActivity.CONTENT_ID_EXTRA, savedRecord.contentId)
				.putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, savedRecord.type)
				.putExtra(PlayerActivity.PROVIDER_EXTRA, savedRecord.provider);
		startActivity(mpdIntent);
	}*/
	private static final class RecordsListAdapter extends BaseExpandableListAdapter {

		private final Context context;
		private final List<SavedRecordsGroup> savedRecordsGroups;

		public RecordsListAdapter(Context context, List<SavedRecordsGroup> savedRecordsGroups) {
			this.context = context;
			this.savedRecordsGroups = savedRecordsGroups;
		}

		@Override
		public MediaPlayerService.PlayingInfo getChild(int groupPosition, int childPosition) {
			return getGroup(groupPosition).savedRecords.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
								 View convertView, ViewGroup parent) {
			MediaPlayerService.PlayingInfo childInfo = getChild(groupPosition, childPosition);
			View view = convertView;
			if (view == null) {
				view = mFragmentInflater.inflate(R.layout.template_recording, parent, false);/*android.R.layout.simple_list_item_1*/
				view.setTag(childInfo);
			}
			((TextView) ((LinearLayout)view).getChildAt(0)).setText(childInfo.name);
			return view;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getGroup(groupPosition).savedRecords.size();
		}

		@Override
		public SavedRecordsGroup getGroup(int groupPosition) {
			return savedRecordsGroups.get(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View view = convertView;
			SavedRecordsGroup group = getGroup(groupPosition);
			if (view == null) {
				view = mFragmentInflater.inflate(R.layout.frame_manage_recordings_header, parent, false);
				//view.setId(group.setViewId(MainTabbedActivity.generateUniqueId()));
			}
			((TextView) view).setText(group.title);
			return view;
		}

		@Override
		public int getGroupCount() {
			return savedRecordsGroups.size();
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	}

	public static final class SavedRecordsGroup {

		public String title;
		public final List<MediaPlayerService.PlayingInfo> savedRecords;
		//private int viewId;

		public SavedRecordsGroup(String title) {
			this.title = title;
			this.savedRecords = new ArrayList<>();
		}

		public void addAll(MediaPlayerService.PlayingInfo[] savedRecords, boolean ascending) {
			Collections.addAll(this.savedRecords, savedRecords);
			if(this.savedRecords.size()>1) {
				this.sort(ascending);
				this.modifyTitle();
			}
		}
		public void modifyTitle(){
			this.title += " ("+this.savedRecords.size()+")";
		}
		public int size() {
			return savedRecords.size();
		}
		public void sort(final boolean ascending) {
			Collections.sort(this.savedRecords,
				new Comparator<MediaPlayerService.PlayingInfo>() {
					@Override
					public int compare(MediaPlayerService.PlayingInfo t1, MediaPlayerService.PlayingInfo t2) {
						int result;
						if (t1.lastModified > t2.lastModified)
							result = 1;
						else if (t2.lastModified > t1.lastModified)
							result = -1;
						else
							result = 0;
						if (ascending)
							result *= -1;
						return result;
					}
				}
			);
		}

		public void add(MediaPlayerService.PlayingInfo instance) {
			if(instance != null) {
				this.savedRecords.add(instance);
				//instance.setGroupViewId(this.viewId);
			}
		}
		/*
		public int setViewId(int viewId) {
			return(this.viewId = viewId);
		}
		public int getViewId() {
			return this.viewId;
		}*/
	}

}