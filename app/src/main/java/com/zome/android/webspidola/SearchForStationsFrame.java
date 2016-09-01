package com.zome.android.webspidola;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zome.android.webspidola.R;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SearchForStationsFrame extends android.support.v4.app.Fragment{//AppCompatActivity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * Some older devices needs a small delay between UI widget updates
	 * and a change of the status and navigation bar.
	 */
	private static final int UI_ANIMATION_DELAY = 300;
	private static final String STARTING_URL = "http://vtuner.com/setupapp/guide/asp/BrowseStations/StartPage.asp?sBrowseType=Language";
	private static final String[] START_OVER_DEFINOTION = {STARTING_URL,"Start Over"};
	private static String SEARCH_URL;
	private final Handler mHideHandler = new Handler();
	private View mContentView;
	private final Runnable mHidePart2Runnable = new Runnable() {
		@SuppressLint("InlinedApi")
		@Override
		public void run() {
			// Delayed removal of status and navigation bar

			// Note that some of these constants are new as of API 16 (Jelly Bean)
			// and API 19 (KitKat). It is safe to use them, as they are inlined
			// at compile-time and do nothing on earlier devices.
			mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		}
	};
	/*
	//private View mControlsView;
	private final Runnable mShowPart2Runnable = new Runnable() {
		@Override
		public void run() {
			// Delayed display of UI elements
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.show();
			}
			mControlsView.setVisibility(View.VISIBLE);
		}
	};
	//private boolean mVisible;
	private final Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hide();
		}
	};
	*/
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	/*
	private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};
	*/
	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;
	private String mLastAnchorName = "empty";

	private static LayoutInflater mFragmentInflater;
	private static ViewGroup mFragmentContainer;
	private static Bundle mFragmentSavedInstanceState;
	private static View mFragmentRootView;
	private static ProgressDialog mProgressIndicator;

	public SearchForStationsFrame() {
	}
	public static SearchForStationsFrame newInstance(int sectionNumber){
		final SearchForStationsFrame fragment = new SearchForStationsFrame();
		final Bundle args = new Bundle();
		args.putInt(MainTabbedActivity.ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){//protected void onCreate(Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);//super.onCreate(savedInstanceState);
		mFragmentInflater = inflater;
		mFragmentContainer = container;
		mFragmentSavedInstanceState =savedInstanceState;
		//getActivity().startService(new Intent(mFragmentContainer.getContext(), MediaPlayerService.class));//make sure service will not stop until stopService issued

		final String javaScriptA_mod = MainTabbedActivity.mResources.getString(R.string.js_get_anchors_struct);// ="var a=document.getElementsByTagName('A');var s=[];if(a!=null)for(var i=0;a.length>i;i++)s.push(a[i].innerText+'='+a[i].href);s.join('_|_|_')";
		final String javaScriptA = MainTabbedActivity.mResources.getString(R.string.js_get_anchors);// ="var a=document.getElementsByTagName('A');var s=[];if(a!=null)for(var i=0;a.length>i;i++)s.push(a[i].innerText+'='+a[i].href);s.join('_|_|_')";
		final String javaScriptHTML =MainTabbedActivity.mResources.getString(R.string.js_get_outerhtml);
		SEARCH_URL = MainTabbedActivity.mResources.getString(R.string.search_stations_url);
		mFragmentRootView = inflater.inflate(R.layout.frame_search_stations, mFragmentContainer,false);/*
		setContentView(R.layout.frame_search_stations);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		*/
		/*
		mVisible = true;
		mControlsView = findViewById(R.id.fullscreen_content_controls);
		mContentView = findViewById(R.id.fullscreen_content);


		// Set up the user interaction to manually show or hide the system UI.
		mContentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggle();
			}
		});
		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		Button button = (Button) findViewById(R.id.add_stations_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResultAndFinish();
			}
		});
		button.setOnTouchListener(mDelayHideTouchListener);
		*/
		mProgressIndicator = new ProgressDialog(getContext());
		mProgressIndicator.setTitle("Loading");
		mProgressIndicator.setMessage("Wait while loading...");

		//mExistedStations=
		ArrayList<String[]> existed = FavoriteStationsFrame.getStationsListFromPreferences();
		for(String[] definition : existed)
			mExistedStations.put(definition[1], definition[0]);

		final WebView myWebView = (WebView) mFragmentRootView.findViewById(R.id.webview);
		myWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				goToUrl(extractUrl(request), view);
				return true;
			}
			public void onPageFinished(WebView view, String url)
			{
				triggerProgressIndicator(false);// To dismiss the dialog

				if(url.length() == 0)
					goToUrl(STARTING_URL, myWebView);
				else if(url.substring(0,url.length()-1).toLowerCase().endsWith(".mp")){
					myWebView.goBack();
				}else {
					String url2 = FavoriteStationsFrame.preferences.getString(FavoriteStationsFrame.PREFERENCE_BEFORE_LAST_SEARCH_URL, STARTING_URL);
					String url1 = getUrlFromPreferences();
					if(!url.equals(url1) && !url.equals(STARTING_URL) && notDownloadingUrl(url)) {
						String[] s1={FavoriteStationsFrame.PREFERENCE_BEFORE_LAST_SEARCH_URL, url1};
						String[] s2={FavoriteStationsFrame.PREFERENCE_LAST_SEARCH_URL, url};
						String[][] s3 = {s1,s2};
						putStartingUrl(s3);
					}
  		      		/* This call inject JavaScript into the page which just finished loading. */
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						//myWebView.evaluateJavascript(javaScriptHTML, new ValueCallback<String>() {
						myWebView.evaluateJavascript(javaScriptA_mod, new ValueCallback<String>() {
							@Override
							public void onReceiveValue(String s) {
								processResultHtml(s);
							}
						});
						/*
						myWebView.evaluateJavascript(javaScriptA, new ValueCallback<String>() {
							@Override
							public void onReceiveValue(String s) {
								processResultHtml(s);
							}
						});*/
					}
					else
						myWebView.loadUrl("javascript:window.HTMLOUT.processHTML(document.documentElement.outerHTML);");
				}
			}
			public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request){
				String url = extractUrl(request);
				if(mHashMapOfAnchors.containsKey(url))
					mLastAnchorName = mHashMapOfAnchors.get(url);
				return null;
			}
		});
		/*
		myWebView.registerHandler("submitFromWeb", new BridgeHandler() {
			@Override
			public void handler(String data, CallBackFunction function) {
				Log.i("TAG", "handler = submitFromWeb, data from web = " + data);
				function.onCallBack("submitFromWeb exe, response data from Java");
			}
		});
		myWebView.seDefaultHandler(new DefaultHandler());
		WebViewJavascriptBridge.callHandler(
				'submitFromWeb'
				, {'param': str1}
		, function(responseData) {
			document.getElementById("show").innerHTML = "send get responseData from java, data = " + responseData
		}
		);
		*/
		/* WebViewClient must be set BEFORE calling loadUrl! */
		if(savedInstanceState == null)
			goToUrl(getUrlFromPreferences());
		else{
			restorePageContent(savedInstanceState);
			setOverPage(mStationForRestore, mNavigationsForRestore);
		}
		myWebView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String URL, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				if(mHashMapOfAnchors != null) {
					String url = URL.toLowerCase();
					if (url.endsWith(".m3u")) {
						String[] split = url.split("m3u");
						if (split.length > 1) {
							String code = "id=" + split[1].substring(0, split[1].length() - 1);
							Set<String> keys = mHashMapOfAnchors.keySet();
							for (String key : keys)
								if (key.endsWith(code)) {
									mLastAnchorName = mHashMapOfAnchors.get(key);
									break;
								}
						}
						new DownloadWebpageTask().execute(url);
					}
				}else
					goToUrl(getUrlFromPreferences());
			}
		});
		/* JavaScript must be enabled  */
		myWebView.getSettings().setJavaScriptEnabled(true);

		/* Register a new JavaScript interface called HTMLOUT */
		myWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client = new GoogleApiClient.Builder(/*this*/getContext()).addApi(AppIndex.API).build();

		EditText searchText = (EditText) mFragmentRootView.findViewById(R.id.sSearchInput);
		searchText.setTextSize(TypedValue.COMPLEX_UNIT_PT, (float) (MediaPlayerService.mPrefFontSizeForSearch*1.2));
		ImageButton searchButton = (ImageButton) mFragmentRootView.findViewById(R.id.sSearchInput_button);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchInputClick(view);
			}
		});
		return  mFragmentRootView;
	}
	private String getUrlFromPreferences (){
		return correctDownloadingUrl(FavoriteStationsFrame.preferences.getString(FavoriteStationsFrame.PREFERENCE_LAST_SEARCH_URL, STARTING_URL));
	}
	private void putStartingUrl(String[][] arrays) {
		SharedPreferences.Editor edit = FavoriteStationsFrame.preferences.edit();
		for(int i=0;arrays.length>i;i++)
			edit.putString(arrays[i][0], arrays[i][1]);
		edit.commit();
	}

	private String correctDownloadingUrl(String url) {
		return  notDownloadingUrl(url) ? url :STARTING_URL;
	}

	private boolean notDownloadingUrl(String url) {
		return !url.contains("?link=1&id=");
	}

	private String extractUrl(WebResourceRequest request) {
		String url;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			url =request.getUrl().toString();
		} else
			url = request.toString();
		return url;
	}
	private HashMap<String,String> mHashMapOfAnchors = new HashMap<>();;
	//private final static String javaScriptHTML ="String(document.documentElement.outerHTML.indexOf('meta name=\"'))+' '+document.documentElement.outerHTML";
	/* An instance of this class will be registered as a JavaScript interface */
	class MyJavaScriptInterface{
		@JavascriptInterface
		@SuppressWarnings("unused")
		public void processHTML(String html){
			processResultHtml(html);
		}
	}
	private static final String delim0 ="__aaa__aaa__";
	private static final String delim1 ="__bbb__bbb__";
	private static final String delim2 ="__ccc__ccc__";
	private ArrayList<String[]> deserializeJS(String src){
		ArrayList<String[]> res = new ArrayList<String[]>();
		if(!src.isEmpty()) {
			String[] split = src.split(delim1), splitTemp;
			for (int j = 0; split.length > j; j++) {
				if(!split[j].isEmpty())
					res.add(split[j].split(delim0));
			}
		}
		return res;
	}
	private String cleareUnicodes(String src){
		int pos;
		int posStart = 0;
		StringBuffer result = new StringBuffer(src.length());
		while((pos=src.indexOf("\\u", posStart))> posStart){
			result.append(src.substring(posStart, pos));
			pos+=2;
			result.append((char)Integer.parseInt(src.substring(pos,posStart = pos+4),16));
		}
		if(posStart < src.length())
			result.append(src.substring(posStart));
		return result.toString();
	}
	private void processResultHtml(String html){
		if(html == null)
			html = "'empty'";
		else {
			if(html.length()>2) {
				mHashMapOfAnchors = new HashMap<>();
				String str = cleareUnicodes(html.substring(1, html.length() - 1));
				String[] split = str.split(delim2);
				if(split.length < 2 || split[1].split(delim1).length == 1) {
					putStartingUrl(reSetPrefLastUrl);/*
					WebView myWebView = (WebView) findViewById(R.id.webview);
					myWebView.loadUrl(STARTING_URL);*/
					Log.e("VT:error", html);
				}
				else {
					ArrayList<String[]> a = deserializeJS(split[1]);
					int linkCount = setOverPage(deserializeJS(split[0]), a);
					//Log.e("VT:OK", printArrayList(a));
					if(linkCount == 0)
						putStartingUrl(reSetPrefLastUrl);
				}
			}
		}
		//Log.e("WebSpidola:JavaScript", html.replace("_|_|_", "\n"));
	}
	private static final String[][] reSetPrefLastUrl ={{FavoriteStationsFrame.PREFERENCE_LAST_SEARCH_URL, STARTING_URL}};

	private String printArrayList(ArrayList<String[]> a) {
		int length = a.size();
		String[] b;
		StringBuffer buffer = new StringBuffer(length*40);
		for(int i=0;length>i;i++) {
			b = a.get(i);
			for (int j = 0; b.length > j; j++)
				buffer.append(' ').append(b[j]);
			buffer.append('\n');
		}
		return buffer.toString();
	}

	private HashSet<String> makeBannedStationsList(String[] array){
		HashSet<String> bannedLabels = new HashSet<>();
		for(int i=0;array.length>i;i++)
			bannedLabels.add(array[i].toLowerCase());
		return bannedLabels;
	}
	private boolean existsInBannedStationsList(String[] src){
		final String[] array = MainTabbedActivity.mResources.getStringArray(R.array.banned_anchors_labels);
		final HashSet<String> bannedLabels = makeBannedStationsList(array);
		boolean result = bannedLabels.contains(src[1].toLowerCase());
		if(!result){
			for(int i=0;array.length>i;i++) {
				if(result = src[0].toLowerCase().endsWith(array[i]))
					break;
			}
		}
		return result;
	}
	public void searchInputClick(View view){
		final EditText search = (EditText) mFragmentRootView.findViewById(R.id.sSearchInput);
		try {
			InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),  InputMethodManager.HIDE_NOT_ALWAYS);
		} catch (Exception e) {
			// TODO: handle exception
		}
		goToUrl(SEARCH_URL+search.getText().toString());
	}
	private int setOverPage(ArrayList<String[]> strings, ArrayList<String[]> strings1) {
		mStationForRestore = new ArrayList<>();
		mNavigationsForRestore = new ArrayList<>();
		int linksCount = 0, length;
		String[] definition, defCopy;
		HashMap<String,String> existingAnchors = new HashMap<>();
		LinearLayout row;
		Button button;
		String url, label;
		LinearLayout stationsAnchorsGroup;
		//Stations
		stationsAnchorsGroup = (LinearLayout) mFragmentRootView.findViewById(R.id.stations_anchors_group);
		stationsAnchorsGroup.removeAllViews();
		length = strings.size();
		TextView stationData;
		for (int i = 0; length > i; i++) {
			definition = strings.get(i);
			if (definition.length == 5) {
				defCopy= new String[definition.length];
				/*getLayoutInflater()*/mFragmentInflater.inflate(R.layout.template_station_anchors, stationsAnchorsGroup);
				row = (LinearLayout) stationsAnchorsGroup.getChildAt(stationsAnchorsGroup.getChildCount() - 1);
				button = (Button) row.getChildAt(0);
				button.setText(label=defCopy[1]=definition[1]);
				button.setTag(url=defCopy[0]=definition[0]);
				button.setTextSize(TypedValue.COMPLEX_UNIT_PT, MediaPlayerService.mPrefFontSizeForSearch);
				if(mExistedStations.containsKey(url))
					button.setEnabled(false);
				else {
					button.setOnClickListener(initiateLinkLoad);
					button.setOnLongClickListener(initiateDeleteOfLink);
				}
				for (int j = 2; definition.length > j; j++) {
					try {
						stationData= ((TextView) row.getChildAt(j - 1));
						stationData.setText(defCopy[j]=definition[j]);
						stationData.setTextSize(TypedValue.COMPLEX_UNIT_PT, MediaPlayerService.mPrefFontSizeForSearch);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
				}
				mStationForRestore.add(defCopy);
				linksCount++;
			}
		}
		//Navigation
		stationsAnchorsGroup = (LinearLayout) mFragmentRootView.findViewById(R.id.navigation_anchors_group);
		stationsAnchorsGroup.removeAllViews();
		strings1.add(0, START_OVER_DEFINOTION);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			strings1.sort(comparatorForLabels);
		}
		length = strings1.size();
		for(int i = 0; length > i; i++) {
			/*getLayoutInflater()*/mFragmentInflater.inflate(R.layout.template_general_anchors, stationsAnchorsGroup);
			row = (LinearLayout) stationsAnchorsGroup.getChildAt(stationsAnchorsGroup.getChildCount() - 1);
			if(i==0){
				row.setPadding(row.getPaddingLeft(), 12/*row.getPaddingTop()*/, row.getPaddingRight(), row.getPaddingBottom());
			}
			int width;
			int inRow = Integer.parseInt(row.getTag().toString());
			for (int j = 0; j < inRow; j++) {
				while(i<length && ((definition = strings1.get(i)).length <2 || existingAnchors.containsKey(definition[0]) || existsInBannedStationsList(definition)))
					i++;
				button = (Button) row.getChildAt(j);
				if(i<length) {
					definition = strings1.get(i);
					defCopy= new String[definition.length];
					existingAnchors.put(url=defCopy[0]=definition[0], label=defCopy[1]=definition[1]);
					button.setText(label);
					button.setTag(url);
					button.setTextSize(TypedValue.COMPLEX_UNIT_PT, MediaPlayerService.mPrefFontSizeForSearch);
					button.setOnClickListener(initiateLinkLoad);
					linksCount++;
					mNavigationsForRestore.add(defCopy);
				}else
					button.setVisibility(View.GONE);
				if(j < inRow - 2)
					i++;
			}
		}
		if(linksCount>0)
			mFragmentRootView.findViewById(R.id.stations_anchors_scroller).scrollTo(0,0);
		return linksCount;
	}
	private ArrayList<String[]> mStationForRestore;
	private ArrayList<String[]> mNavigationsForRestore;
	private Comparator comparatorForLabels = new Comparator() {
		@Override
		public int compare(Object o, Object t1) {
			if(o instanceof String[] && t1 instanceof String[])
				return compare((String[]) o, (String[]) t1);
			return 0;
		}
		public int compare(String[] o, String[] t1) {
			return (o.length > 1 && t1.length > 1 ? o[1].compareTo(t1[1]) : 0);
		}
	};
	private View.OnClickListener initiateLinkLoad = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			view.setBackgroundColor(0x888888);
			String url = (String)view.getTag();
			mHashMapOfAnchors.put(url, mLastAnchorName = (String) ((Button)view).getText());
			goToUrl(url);
		}
	};
	private String findKeyByValue(String value){
		String foundKey = null;
		for(String key : mExistedStations.keySet())
			if(value.equals(mExistedStations.get(key))){
				foundKey = key;
				break;
			}
		return foundKey;
	}
	private View.OnLongClickListener initiateDeleteOfLink = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(final View view) {
			final String url = (String)view.getTag();
			final String label = (String) ((Button) view).getText();
			final String foundKey =findKeyByValue(label);
			if(foundKey != null){
				askOnAddingStation(foundKey,label,2);
				/*
				AlertDialog.Builder dialog =new AlertDialog.Builder(SearchForStationsFrame.this);//new AlertDialog.Builder(getApplicationContext(), R.style.AppTheme);
				dialog.setTitle("Manage Stations");
				dialog.setMessage("Would you like to not add station with the label\n'" + label + "'\n");
				dialog.setNeutralButton("Let it be Added", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
				dialog.setNegativeButton("Do not add it", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int whichButton) {
						mHashMapOfAnchors.remove(foundKey);
					}
				});
				dialog.show();
				*/
			}
			return false;
		}
	};
	private void goToUrl(String url){
		goToUrl(url, null);
	}
	private void goToUrl(String url, WebView webView){
		if(webView == null)
			webView = (WebView) mFragmentRootView.findViewById(R.id.webview);;
		Log.e("URL:",url);
		triggerProgressIndicator(true);
		webView.loadUrl(url);
		if(url.startsWith(SEARCH_URL))
			((EditText)mFragmentRootView.findViewById(R.id.sSearchInput)).setText(url.substring(SEARCH_URL.length()));
	}
	private boolean mProgressIndicatorState = false;
	private void triggerProgressIndicator() {
		triggerProgressIndicator(mProgressIndicatorState);
	}
	private void triggerProgressIndicator(boolean show){
		mProgressIndicatorState = show;
		if(!show)
			mProgressIndicator.dismiss();
		else{
			if(MainTabbedActivity.mViewPager.getCurrentItem() == getArguments().getInt(MainTabbedActivity.ARG_SECTION_NUMBER))
				mProgressIndicator.show();
		}
	}
	//private View lastClicked = null;
	@Override
	public void onStart() {
		super.onStart();
		/*Intent intent = new Intent(this, MediaPlayerService.class);
		mFragmentContainer.getContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);*/

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"SearchForStationsFrame Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.zome.android.webspidola/http/host/path")
		);
		AppIndex.AppIndexApi.start(client, viewAction);
	}

	@Override
	public void onPause() {
		super.onPause();
		setResultAndFinish();
	}

	@Override
	public void onStop() {
		super.onStop();
		//unBindMPService();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"SearchForStationsFrame Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.zome.android.webspidola/http/host/path")
		);
		AppIndex.AppIndexApi.end(client, viewAction);
		client.disconnect();

		setResultAndFinish();
	}
	private void setResultAndFinish(){
		if(!mAddedStations.isEmpty()) {
			FavoriteStationsFrame.commitStationsList(FavoriteStationsFrame.getStationsListFromPreferences(mAddedStations), FavoriteStationsFrame.PREFERENCE_LIST_OF_STATIONS_CHANGED);
			for(int i=mAddedStations.size() -1;i>=0;i--)
				mAddedStations.remove(i);
		}
	}
	public class MyWebViewClient extends WebView {
		public MyWebViewClient(Context context) {
			super(context);
		}
	}

	private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			// params comes from the execute() call: params[0] is the url.
			String res;
			try {
				res = downloadM3U(urls[0]);
			} catch (IOException e) {
				e.printStackTrace();
				res = "Unable to retrieve web page. URL may be invalid.";
			}
			return res;
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(final String result) {
			String existed = mExistedStations.get(result);
			if (existed == null)
				addDefinition(result);
			else {
				askOnAddingStation(result, existed, 1);
			}
		}
	}
	private void askOnAddingStation(final String url, final String name, final int type){
		AlertDialog.Builder dialog =new AlertDialog.Builder(mFragmentContainer.getContext()/*SearchForStationsFrame.this*/);//new AlertDialog.Builder(getApplicationContext(), R.style.AppTheme);
		String[] strings;
		switch(type) {
			case 3:
				strings = new String[]{getString(R.string.manage_stations_title), String.format(getString(R.string.inaccessible_station_text),name),getString(R.string.delete_station_button),getString(R.string.keep_sation_button)};break;
			case 2:
				strings = new String[]{getString(R.string.manage_stations_title),String.format(getString(R.string.manage_station_text),name),getString(R.string.add_station_button),getString(R.string.not_add_station_button)};break;
			case 1:
			default:
				strings = new String[]{getString(R.string.duplicate_station_button),String.format(getString(R.string.duplicate_station_text),name), getString(R.string.add_station_button_1),getString(R.string.not_add_station_button_1)};
		}
		dialog.setTitle(strings[0]);
		dialog.setMessage(strings[1]);
		dialog.setNegativeButton(strings[2], new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {
				switch(type){
					case 1: addDefinition(url);break;
				}
			}
		});
		dialog.setNeutralButton(strings[3], new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				switch(type) {
					case 3:
					case 2: mHashMapOfAnchors.remove(url);
						mHashMapOfAnchors.remove(url);
						break;
				}
			}
		});
		dialog.show();

	}
	private void addDefinition(String url){
		MediaPlayerService.PlayingInfo definition = new MediaPlayerService.PlayingInfo(mLastAnchorName, url, FavoriteStationsFrame.PREF_TYPE_OTHER);
		mAddedStations.add(definition.serializeToStringArray());
		mExistedStations.put(url, mLastAnchorName);
		disableAddedButton(url);
		if(MainTabbedActivity.mBound)
			MainTabbedActivity.mService.startPlay(definition);
	}
	private void disableAddedButton(String url){
		LinearLayout stationsAnchorsGroup = (LinearLayout) mFragmentRootView.findViewById(R.id.stations_anchors_group), row;
		Button button;
		int length = stationsAnchorsGroup.getChildCount();
		for (int i = 0; length > i; i++) {
			row = (LinearLayout) stationsAnchorsGroup.getChildAt(i);
			button = (Button) row.getChildAt(0);
			if(button.getTag().toString().equals(url)){
				button.setEnabled(false);
				break;
			}
		}
	}

	public ArrayList<String[]> mAddedStations = new ArrayList<>();
	private HashMap<String, String> mExistedStations = new HashMap<>();

	private String downloadM3U(String myurl) throws IOException {
		InputStream is = null;
		// Only display the first 500 characters of the retrieved
		// web page content.
		int len = 500;

		try {
			URL url = new URL(myurl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the query
			conn.connect();
			int response = conn.getResponseCode();
			is = conn.getInputStream();

			// Convert the InputStream into a string
			String contentAsString = readIt(is, conn.getContentLength());
			return contentAsString;

			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	// Reads an InputStream and converts it to a String.
	public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		Reader reader = null;
		reader = new InputStreamReader(stream, "UTF-8");
		char[] buffer = new char[len];
		reader.read(buffer);
		int i;
		for (i = 0; i < buffer.length; i++)
			if (buffer[i] == '\n' || buffer[i] == '\r')
				break;
		return new String(buffer, 0, i);
	}
	//@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		//super.onRestoreInstanceState(savedInstanceState);
		restorePageContent(savedInstanceState);
	}
	private void restorePageContent(Bundle savedInstanceState) {
		mStationForRestore = deSerializeDefinitions(savedInstanceState.getStringArrayList(STATIONS_FOR_RESTORE));
		mNavigationsForRestore = deSerializeDefinitions(savedInstanceState.getStringArrayList(NAVIGATIONS_FOR_RESTORE));

	}
	@Override
	public void onResume(){
		super.onResume();
		triggerProgressIndicator();
		//IntentFilter intentFilter = new IntentFilter(MediaPlayerService.CUSTOM_EVENT);
		//LocalBroadcastManager.getInstance(mFragmentContainer.getContext()/*this*/).registerReceiver(mMessageReceiver, intentFilter);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mStationForRestore!=null)
			outState.putStringArrayList(STATIONS_FOR_RESTORE, serializeDefinitions(mStationForRestore));
		if(mNavigationsForRestore!=null)
			outState.putStringArrayList(NAVIGATIONS_FOR_RESTORE, serializeDefinitions(mNavigationsForRestore));
	}
	private ArrayList<String> serializeDefinitions(ArrayList<String[]> in){
		ArrayList<String> out = new ArrayList<String>();
		StringBuffer buf;
		for(String[] def : in){
			buf = new StringBuffer();
			int i;
			for(i=0;def.length -1>i;i++)
				buf.append(def[i]).append(delim0);
			buf.append(def[i]);
			out.add(buf.toString());
		}
		return out;
	}
	private ArrayList<String[]> deSerializeDefinitions(ArrayList<String> in){
		ArrayList<String[]> out = new ArrayList<>();
		if(in!= null)
			for(String def : in){
				out.add(def.split(delim0));
			}
		return out;
	}
	final static String STATIONS_FOR_RESTORE = "STATIONS_FOR_RESTORE";
	final static String NAVIGATIONS_FOR_RESTORE = "NAVIGATIONS_FOR_RESTORE";
	/*
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		//delayedHide(100);
	}
	*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			// This ID represents the Home or Up button.
			NavUtils.navigateUpFromSameTask(/*this*/getActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	/*
	private void toggle() {
		if (mVisible) {
			hide();
		} else {
			show();
		}
	}
	private void hide() {
		// Hide UI first
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
		mControlsView.setVisibility(View.GONE);
		mVisible = false;

		// Schedule a runnable to remove the status and navigation bar after a delay
		mHideHandler.removeCallbacks(mShowPart2Runnable);
		mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
	}
	*/
	/*
	@SuppressLint("InlinedApi")
	private void show() {
		// Show the system bar
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		mVisible = true;

		// Schedule a runnable to display UI elements after a delay
		mHideHandler.removeCallbacks(mHidePart2Runnable);
		mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
	}
	*/
	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	/*
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	*/
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
			Log.d("onServiceDisconnected", arg0.toString());
		}
	};
	*/
	@Override
	public void onDestroy(){
		//unBindMPService();
		super.onDestroy();
	}
	/*
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			int type = intent.getIntExtra(MediaPlayerService.CUSTOM_EVENT_CATEGORY, 0);
			String message = intent.getStringExtra(MediaPlayerService.CUSTOM_EVENT_MESSAGE);
			int pos = message.indexOf(' ');
			if(pos>0 && pos < message.length() -1){
				int code = 0;
				try{code= Integer.parseInt(message.substring(0, pos));}catch(Exception e){}
				String extra = message.substring(pos+1);
				if(extra.equals(MediaPlayerService.DO_NOT_ADD_STATION))
					askOnAddingStation(mService.getCurrentStationUrl(), mService.getCurrentStationName(),3);
			}
			switch(type){
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_ADD_RADIO_NAME:
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_ON_PREPARED_FINISHED:
					break;
				case MediaPlayerService.BROADCAST_MESSAGE_EXTRA_END_BY_NO_CONNECTION:
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
			}
		}
	};
*/
}
