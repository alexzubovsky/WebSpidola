package com.example.android.webspidola;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by Sasha on 2/9/2016.
 */

public class RemoteControlReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("RemoteControlReceiver","RemoteControlReceiver.onReceive:" + intent.toString());
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()) {
				// Handle key press.
			}
		}
	}
}

