package com.exlyo.camerarestarter;

import android.app.IntentService;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationClickIntentService extends IntentService {
	private static boolean actionRunning = false;

	public NotificationClickIntentService() {
		super("Camera Restarter notification click action service");
	}

	@Override
	protected void onHandleIntent(final Intent _intent) {
		synchronized (NotificationClickIntentService.class) {
			if (actionRunning) {
				return;
			}
			actionRunning = true;
		}

		boolean actionSuccess = false;
		try {
			MainActivity.restartButtonActionImpl(this);
			actionSuccess = true;
		} catch (Throwable t) {
			t.printStackTrace();
		}

		try {
			final String lastRestartTimeString;
			if (actionSuccess) {
				lastRestartTimeString = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
			} else {
				lastRestartTimeString = "FAILED";
			}
			MainActivity.openClickableNotification(this, lastRestartTimeString);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		actionRunning = false;
	}
}
