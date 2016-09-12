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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

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
	//private RecordsListAdapter mRecordsListAdapter = null;

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
		showBatchPlayingProgress();
		/*ArrayList<String> expandedGroups = mSavedInstanceState != null ? mSavedInstanceState.getStringArrayList(EXPANDED_GROUPS_LIST):getExpandedGroups();
		mSavedInstanceState = null;
		initRecordedStationsView(expandedGroups);//Get recording content
		showBatchPlayingProgress();
		OnResumeDone = true;*/
	}
	@Override
	public void onPause(){
		super.onPause();
		/*if(playingProgressHandler!=null){
			playingProgressHandler.removeCallbacks(playingProgressThread);
			playingProgressHandler = null;
		}
		OnResumeDone = false;*/
	}
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser) {
			ArrayList<String> expandedGroups = mSavedInstanceState != null ? mSavedInstanceState.getStringArrayList(EXPANDED_GROUPS_LIST) : getExpandedGroups();
			mSavedInstanceState = null;
			initRecordedStationsView(expandedGroups);//Get recording content
			showBatchPlayingProgress();
			OnResumeDone = true;
		}
		else if(playingProgressHandler!=null){
			playingProgressHandler.removeCallbacks(playingProgressThread);
			playingProgressHandler = null;
			OnResumeDone = false;
			Log.e("MRF", "setUserVisibleHint:Not Visible:playingProgressHandler=null");
		}
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
		ExpandableListView recordingGroupsList = (ExpandableListView) mFragmentRootView.findViewById(R.id.recordings_list);
		if(recordingGroupsList != null) {
			RecordsListAdapter adapter = (RecordsListAdapter)recordingGroupsList.getExpandableListAdapter();
			if (adapter != null) {
				if (recordingGroupsList != null && recordingGroupsList.getAdapter() != null) {
					expandedGroups = new ArrayList<>();
					int groupCount = adapter.getGroupCount();
					for (int i = 0; groupCount > i; i++) {
						if (recordingGroupsList.isGroupExpanded(i))
							expandedGroups.add(String.valueOf(i));
					}
					if (expandedGroups.size() == 0)
						expandedGroups = null;
				}
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
			if (recordingGroupsList != null) {
				RecordsListAdapter adapter = (RecordsListAdapter) recordingGroupsList.getExpandableListAdapter();
				if (adapter != null) {
					int id;
					for (String groupId : expandedGroups) {
						if (adapter.getGroup(id = Integer.parseInt(groupId)) != null)
							recordingGroupsList.expandGroup(id);
					}
				}
			}
		}
	}
	private void showBatchPlayingProgress(){
		MediaPlayerService.BatchPlayingStat batchPlayingStat = MediaPlayerService.BatchPlayingStat.getInstance(null);
		if(batchPlayingStat.getCurrentPlaying()!= null && playingProgressHandler == null){//getCurrentPercentageInPlay();
			playingProgressHandler = new android.os.Handler();
			playingProgressHandler.postDelayed(playingProgressThread, 500);
			Log.e("MRF", "showBatchPlayingProgress:playingProgressHandler=new");
		}
	}
	//private MediaPlayerService.PlayingInfo mPreviousBatchPlayingInfo = null;
	private LinearLayout previousIndicatosHolder = null;
	private android.os.Handler playingProgressHandler;
	private Runnable playingProgressThread = new Runnable(){
		public void run(){
			MediaPlayerService.BatchPlayingStat batchPlayingStat = MediaPlayerService.BatchPlayingStat.getInstance(null);
			int position = batchPlayingStat.getCurrentPercentageInPlay();
			View playingIndicators = mFragmentRootView.findViewById(R.id.manage_recordings_controls);
			if(playingIndicators == null && previousIndicatosHolder != null && previousIndicatosHolder.getChildCount() == 2) {
				playingIndicators = previousIndicatosHolder.getChildAt(1);
			}
			if(playingIndicators!= null && playingIndicators.getParent()!= null ){
				if(playingIndicators.getParent() instanceof LinearLayout) {
					LinearLayout parent = (LinearLayout) playingIndicators.getParent();
					if(parent.getParent() == null){
						parent.removeView(playingIndicators);
						playingIndicators = null;
					}
				}
			}
			MediaPlayerService.PlayingInfo currentPlayingInfo;
			if((position >= 0) && (currentPlayingInfo = batchPlayingStat.getCurrentPlaying())!=null) {
				if(previousIndicatosHolder == null || playingIndicators == null || (currentPlayingInfo != null && !currentPlayingInfo.equalTo((MediaPlayerService.PlayingInfo) previousIndicatosHolder.getTag()))){
					if(previousIndicatosHolder != null && previousIndicatosHolder.getChildCount() == 2)
						previousIndicatosHolder.removeViewAt(1);
					LinearLayout currentView = (LinearLayout) findViewByInfo(currentPlayingInfo);
					if(currentView != null && mFragmentRootView.findViewById(R.id.manage_recordings_controls)== null) {
						currentView.addView(inflatePlayingIndicators());
						previousIndicatosHolder = currentView;
					}
				}
				if(playingIndicators!= null) {
					playingIndicators.setVisibility(View.VISIBLE);
					setProgressButtonVisibility(R.id.overall_previous, batchPlayingStat.isFirst());
					setProgressButtonVisibility(R.id.overall_next, batchPlayingStat.isLast());
					setProgressValue(R.id.overallProgressBar, batchPlayingStat.getCurrentPercentageInList());
					setProgressValue(R.id.progressBar, position);

					long duration = batchPlayingStat.getDuration();
					setTextValue(R.id.duration, MediaPlayerService.formatTimeString(duration));
					setTextValue(R.id.current_time, MediaPlayerService.formatTimeString(Math.round(duration * (position / 100.0d))));
					setTextValue(R.id.current_name, currentPlayingInfo==null?"":currentPlayingInfo.name);
				}
				if(playingProgressHandler ==null) {
					playingProgressHandler = new android.os.Handler();
					Log.e("MRF", "playingProgressThread:run:playingProgressHandler=new");
				}
				playingProgressHandler.postDelayed(this, 500);
			}
			else{
				Log.e("MRF", "playingProgressThread:run:position="+position+", playingIndicators="+playingIndicators);

				if(playingIndicators != null)
					playingIndicators.setVisibility(View.GONE);
			}
		}
	};
	private void setProgressButtonVisibility(int id, boolean hide) {
		TextView view = (TextView) mFragmentRootView.findViewById(id);
		if(view != null)
			view.setVisibility(hide ? View.INVISIBLE: View.VISIBLE);
	}
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
		ExpandableListView recordingGroupsList;
		if(playingInfo != null && (recordingGroupsList = (ExpandableListView) mFragmentRootView.findViewById(R.id.recordings_list)) != null) {
			RecordsListAdapter adapter = (RecordsListAdapter) recordingGroupsList.getExpandableListAdapter();
			if(adapter != null) {
				Integer groupId = playingInfo.getGroupId();
				if (groupId != null && groupId >= 0 && groupId < adapter.getGroupCount()) {
					if (!recordingGroupsList.isGroupExpanded(groupId))
						recordingGroupsList.expandGroup(groupId);
					SavedRecordsGroup group = adapter.getGroup(groupId);
					int childCount = group.size();
					View view;
					MediaPlayerService.PlayingInfo tag;
					for (int j = 0; childCount > j; j++) {
						if (adapter.getChild(groupId, j).equalTo(playingInfo)) {
							int viewCount = recordingGroupsList.getChildCount();
							for (int k = 0; viewCount > k; k++) {//View view = recordingGroupsList.findViewWithTag(playingInfo);
								if ((view = recordingGroupsList.getChildAt(k)) != null && (view instanceof LinearLayout)) {
									tag = (MediaPlayerService.PlayingInfo) view.getTag();
									if (playingInfo.equalTo(tag))
										return view;
								}
							}
						}
					}
					Log.e("MRF","Not founf uri="+playingInfo.uri+", groupId="+groupId+",groupSize="+childCount);
				}
				else
					Log.e("MRF","groupId="+groupId+", groupCount="+adapter.getGroupCount());
			}
			else
				Log.e("MRF","No adapter");
		}
		else
			Log.e("MRF","No R.id.recordings_list");

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
								group.putToList(mRecordingsGroups);
							}
						}
					}
				}
			}
			ExpandableListView recordingGroupsList = (ExpandableListView) mFragmentRootView.findViewById(R.id.recordings_list);
			recordingGroupsList.setAdapter(new RecordsListAdapter(getContext(), mRecordingsGroups));
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
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			MediaPlayerService.PlayingInfo childInfo = getChild(groupPosition, childPosition);
			View view = convertView;
			if (view == null) {
				view = mFragmentInflater.inflate(R.layout.template_recording, parent, false);//*android.R.layout.simple_list_item_1
			}
			view.setTag(childInfo);
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
		private Integer id = null;
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
		public void setGroupId(int id){
			this.id = id;
			for(MediaPlayerService.PlayingInfo info : this.savedRecords)
				info.setGroupId(id);
		}
		public Integer getGroupId(){return this.id;}
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
			}
		}

		public void putToList(List<SavedRecordsGroup> recordingsGroups) {
			int groupId = recordingsGroups.size();
			this.setGroupId(groupId);
			recordingsGroups.add(this);
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
