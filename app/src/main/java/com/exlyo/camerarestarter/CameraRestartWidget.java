package com.exlyo.camerarestarter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

//Thank you George Mathew for your code I copy pasted in onUpdate() :-)
//http://wptrafficanalyzer.in/blog/android-home-screen-app-widget-with-onclick-event/
public class CameraRestartWidget extends AppWidgetProvider {
	public static String WIDGET_ACTION = "com.exlyo.camerarestarter.widget";

	@Override
	public void onUpdate(final Context _context, final AppWidgetManager _appWidgetManager, final int[] _appWidgetIds) {
		// Perform this loop procedure for each App Widget that belongs to this provider
		for (final int appWidgetId : _appWidgetIds) {
			// Create an Intent to launch MainActivity
			final Intent intent = new Intent(_context, MainActivity.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			// Creating a pending intent, which will be invoked when the user
			// clicks on the widget
			final PendingIntent pendingIntent = getPendingSelfIntent(_context, WIDGET_ACTION);

			// Get the layout for the App Widget
			final RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.restart_camera_widget);

			//  Attach an on-click listener to the clock
			views.setOnClickPendingIntent(R.id.camera_restart_widget_button, pendingIntent);

			// Tell the AppWidgetManager to perform an update on the current app widget
			_appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	private static PendingIntent getPendingSelfIntent(Context context, String action) {
		final Intent intent = new Intent(context, CameraRestartWidget.class);
		intent.setAction(action);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	@Override
	public void onReceive(final Context _context, final Intent _intent) {
		super.onReceive(_context, _intent);
		if (WIDGET_ACTION.equals(_intent.getAction())) {
			MainActivity.restartButtonAction(_context);
		}
	}
}