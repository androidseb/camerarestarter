package com.exlyo.camerarestarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context _context, final Intent _intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(_intent.getAction())) {
			if (MainActivity.isSystemStartNotificationEnabled(_context)) {
				MainActivity.openClickableNotification(_context, null);
			}
		}
	}
}
