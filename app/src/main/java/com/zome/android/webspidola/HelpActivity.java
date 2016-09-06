package com.zome.android.webspidola;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {

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
	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		Toolbar toolbar = (Toolbar) findViewById(R.id.help_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.help_container);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		int currentItem = getIntent().getIntExtra(ARG_SECTION_NUMBER, 0);
		mViewPager.setCurrentItem(currentItem, true);

		/*
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
		});
		*/
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_help, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_help_exit) {
			this.finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	public final static int HELP_SECTION_FAVORITES = 1;
	public final static int HELP_SECTION_RECORDINGS =2;
	public final static int HELP_SECTION_SEARCH = 3;
	public final static int HELP_SECTION_MANAGE = 4;
	public final static int HELP_SECTION_SETTINGS = 5;
	public final static int HELP_SECTION_SETTINGS_1 = 6;
	public final static int HELP_SECTION_SETTINGS_2 = 7;
	public final static int HELP_SECTION_SETTINGS_3 = 0;
	public final static int HELP_SECTIONS_NUMBER = 8;
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a PlaceholderFragment (defined as a static inner class below).
			return PlaceholderFragment.newInstance(position + 1);
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return HELP_SECTIONS_NUMBER;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case HELP_SECTION_FAVORITES:
					return "SECTION "+String.valueOf(position);
				case HELP_SECTION_RECORDINGS:
					return "SECTION "+String.valueOf(position);
				case HELP_SECTION_SEARCH:
					return "SECTION "+String.valueOf(position);
				case HELP_SECTION_MANAGE:
					return "SECTION "+String.valueOf(position);
				case HELP_SECTION_SETTINGS:
					return "SECTION "+String.valueOf(position);
				case HELP_SECTION_SETTINGS_1:
					return "SECTION "+String.valueOf(position);
				case HELP_SECTION_SETTINGS_2:
					return "SECTION "+String.valueOf(position);
				case HELP_SECTION_SETTINGS_3:
					return "SECTION "+String.valueOf(position);
			}
			return null;
		}
	}
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "help_section_number";

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		/**
		 * Returns a new instance of this fragment for the given section
		 * number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_help, container, false);
			int sectionNumber = this.getArguments().getInt(ARG_SECTION_NUMBER);
			TextView textView = (TextView) rootView.findViewById(R.id.help_section_label);
			textView.setText(getString(R.string.help_section_title, getHelpSectionTitle(sectionNumber)));
			ImageView image= (ImageView) rootView.findViewById(R.id.help_section_image);
			image.setImageResource(getImgaeResourceIdBySection(sectionNumber));
			return rootView;
		}

		private String getHelpSectionTitle(int sectionNumber) {
			switch(sectionNumber){
				case HELP_SECTION_RECORDINGS:
					return getString(R.string.help_recordings);
				case HELP_SECTION_SEARCH:
					return getString(R.string.help_search);
				case HELP_SECTION_MANAGE:
					return getString(R.string.help_manage);
				case HELP_SECTION_SETTINGS:
					return getString(R.string.help_settings);
				case HELP_SECTION_SETTINGS_1:
					return getString(R.string.help_settings_1);
				case HELP_SECTION_SETTINGS_2:
					return getString(R.string.help_settings_2);
				case HELP_SECTION_SETTINGS_3:
					return getString(R.string.help_settings_3);
				case HELP_SECTION_FAVORITES:
				default:
					return getString(R.string.help_favorites);
			}
		}

		private static int getImgaeResourceIdBySection(int sectionNumber) {
			switch(sectionNumber){
				case HELP_SECTION_RECORDINGS:
					return R.drawable.help_recordings;
				case HELP_SECTION_SEARCH:
					return R.drawable.help_search;
				case HELP_SECTION_MANAGE:
					return R.drawable.help_manage;
				case HELP_SECTION_SETTINGS:
					return R.drawable.help_settings;
				case HELP_SECTION_SETTINGS_1:
					return R.drawable.help_settings_1;
				case HELP_SECTION_SETTINGS_2:
					return R.drawable.help_settings_2;
				case HELP_SECTION_SETTINGS_3:
					return R.drawable.help_settings_3;
				case HELP_SECTION_FAVORITES:
				default:
					return R.drawable.help_favorites;
			}
		}
	}
}
