package com.exlyo.camerarestarter;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.exlyo.camerarestarter.privatedata.AppPrivateData;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	private static final String PREFS_FILE_NAME = "camera_restarter_prefs";
	private static final String PREF_KEY_CAMERA_AUTO_LAUNCH = "camera_auto_launch";
	private static final String PREF_KEY_AUTO_CAMERA_ACTION = "auto_camera_action";
	private static final String PREF_KEY_SYSTEM_START_NOTIFICATION = "system_start_notification";

	private AdView mAdView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.restart_camera_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				MainActivity.logEvent(MainActivity.this, "RESTART_ACTION_BUTTON");
				restartButtonAction(MainActivity.this);
			}
		});
		final CheckBox autoLaunchCheckBox = (CheckBox) findViewById(R.id.auto_launch_camera_checkbox);
		autoLaunchCheckBox.setChecked(MainActivity.isAutoCameraLaunchEnabled(this));
		autoLaunchCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				MainActivity.setAutoCameraLaunchEnabled(MainActivity.this, autoLaunchCheckBox.isChecked());
			}
		});
		final CheckBox autoCameraActionCheckBox = (CheckBox) findViewById(R.id.auto_camera_action_checkbox);
		autoCameraActionCheckBox.setChecked(MainActivity.isAutoCameraActionEnabled(this));
		autoCameraActionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				MainActivity.setAutoCameraActionEnabled(MainActivity.this, autoCameraActionCheckBox.isChecked());
			}
		});

		final TextView openCloseNotificationButton = (TextView) findViewById(R.id.open_close_notification_button);
		updateOpenCloseNotificationButtonText(openCloseNotificationButton);
		openCloseNotificationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				openClickableNotification(MainActivity.this, null);
				updateOpenCloseNotificationButtonText(openCloseNotificationButton);
			}
		});

		final CheckBox openNotificationOnSystemStartCheckbox = (CheckBox) findViewById(R.id.open_notification_on_system_start_checkbox);
		openNotificationOnSystemStartCheckbox.setChecked(MainActivity.isSystemStartNotificationEnabled(this));
		openNotificationOnSystemStartCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				MainActivity.setSystemStartNotificationEnabled(MainActivity.this, openNotificationOnSystemStartCheckbox.isChecked());
			}
		});

		String helpText = "";
		try {
			final InputStream inputStream = getResources().openRawResource(R.raw.help);
			helpText = streamContentToString(inputStream);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		((TextView) findViewById(R.id.help_instructions_textview)).setText(helpText);

		if (AppPrivateData.hasFireBaseData) {
			final ViewGroup adContainer = (ViewGroup) findViewById(R.id.ad_container);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(final Void... _voids) {
					final List<String> allOwnedItemSkus = CRBillingManager.get().getAllOwnedItemSkus(MainActivity.this);
					if (allOwnedItemSkus != null && !allOwnedItemSkus.isEmpty()) {
						MainActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								adContainer.setVisibility(View.VISIBLE);
								final TextView thankYouTextView = new TextView(MainActivity.this);
								thankYouTextView.setGravity(Gravity.CENTER);
								thankYouTextView.setText("\n" + MainActivity.this.getString(R.string.thank_you_for_your_donation) + "\n");
								adContainer.addView(thankYouTextView,
									new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
							}
						});
						return null;
					}
					if (MainActivity.this.isFinishing()) {
						return null;
					}
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (MainActivity.this.isFinishing()) {
								return;
							}
							adContainer.setVisibility(View.VISIBLE);
							// Initialize the Mobile Ads SDK.
							MobileAds.initialize(MainActivity.this, AppPrivateData.adMobAppId);
							MobileAds.setAppMuted(true);
							mAdView = new AdView(MainActivity.this);
							mAdView.setAdSize(AdSize.SMART_BANNER);
							mAdView.setAdUnitId(AppPrivateData.adUnitId);
							adContainer.addView(mAdView,
								new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
							// Create an ad request. Check your logcat output for the hashed device ID to
							// get test ads on a physical device. e.g.
							// "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
							final AdRequest adRequest = new AdRequest.Builder().build();
							// Start loading the ad in the background.
							mAdView.loadAd(adRequest);
						}
					});
					return null;
				}
			}.execute();
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != R.id.menu_item_donate) {
			return super.onOptionsItemSelected(item);
		}
		CRBillingManager.actionDonate(MainActivity.this);
		return true;
	}

	private static void updateOpenCloseNotificationButtonText(final TextView _openCloseNotificationButton) {
		if (notificationOpen) {
			_openCloseNotificationButton.setText(R.string.close_clickable_notification);
		} else {
			_openCloseNotificationButton.setText(R.string.open_clickable_notification);
		}
	}

	private static boolean notificationOpen = false;

	public static void openClickableNotification(final Context _context, final String _lastRestartTimeString) {
		final NotificationCompat.Builder notificationBuilder;

		if (_lastRestartTimeString == null && notificationOpen) {
			notificationBuilder = null;
		} else {

			final String notificationText;
			if (_lastRestartTimeString == null) {
				notificationText = _context.getString(R.string.click_to_restart_camera);
			} else {
				notificationText = _context.getString(R.string.click_to_restart_camera_last_restart_at, _lastRestartTimeString);
			}
			notificationBuilder = new NotificationCompat.Builder(_context).setSmallIcon(R.drawable.ic_notification).setColor(0xFF48B7AC)
				.setContentTitle(_context.getString(R.string.app_name)).setContentText(notificationText).setOngoing(true)
				.setContentIntent(PendingIntent.getService(_context, 0, new Intent(_context, NotificationClickIntentService.class), 0));
		}
		notificationOpen = !notificationOpen;

		final NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationBuilder == null) {
			notificationManager.cancel(0);
		} else {
			notificationManager.notify(0, notificationBuilder.build());
		}
	}

	public static void restartButtonAction(final Context _context) {
		try {
			restartButtonActionImpl(_context);
			showToastMessage(_context, _context.getString(R.string.camera_restared_successfully));
		} catch (Throwable t) {
			t.printStackTrace();
			showToastMessage(_context, _context.getString(R.string.camera_restart_failed, t.getMessage()));
		}
	}

	public static void restartButtonActionImpl(final Context _context) throws Throwable {
		runRestartCameraShellCommand();
		if (MainActivity.isAutoCameraLaunchEnabled(_context)) {
            final Intent cameraStartIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraStartIntent.setAction(Intent.ACTION_MAIN);
            cameraStartIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            cameraStartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(cameraStartIntent);
		}
	}

	private synchronized static boolean isAutoCameraLaunchEnabled(final Context _context) {
		final SharedPreferences sharedPreferences = _context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		return sharedPreferences.getBoolean(PREF_KEY_CAMERA_AUTO_LAUNCH, true);
	}

	@SuppressLint("CommitPrefEdits")
	private synchronized static void setAutoCameraLaunchEnabled(final Context _context, final boolean _enabled) {
		final SharedPreferences sharedPreferences = _context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		final SharedPreferences.Editor edit = sharedPreferences.edit();
		edit.putBoolean(PREF_KEY_CAMERA_AUTO_LAUNCH, _enabled);
		edit.commit();
	}

	public synchronized static boolean isAutoCameraActionEnabled(final Context _context) {
		final SharedPreferences sharedPreferences = _context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		return sharedPreferences.getBoolean(PREF_KEY_AUTO_CAMERA_ACTION, false);
	}

	@SuppressLint("CommitPrefEdits")
	private synchronized static void setAutoCameraActionEnabled(final Context _context, final boolean _enabled) {
		final SharedPreferences sharedPreferences = _context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		final SharedPreferences.Editor edit = sharedPreferences.edit();
		edit.putBoolean(PREF_KEY_AUTO_CAMERA_ACTION, _enabled);
		edit.commit();
	}

	public synchronized static boolean isSystemStartNotificationEnabled(final Context _context) {
		final SharedPreferences sharedPreferences = _context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		return sharedPreferences.getBoolean(PREF_KEY_SYSTEM_START_NOTIFICATION, false);
	}

	@SuppressLint("CommitPrefEdits")
	private synchronized static void setSystemStartNotificationEnabled(final Context _context, final boolean _enabled) {
		final SharedPreferences sharedPreferences = _context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		final SharedPreferences.Editor edit = sharedPreferences.edit();
		edit.putBoolean(PREF_KEY_SYSTEM_START_NOTIFICATION, _enabled);
		edit.commit();
	}

	/**
	 * Uses a combination of the ps, grep, tr, cut and kill commands to kill a process from a specific user, using a specific file under /system/bin/.
	 * <p>
	 * An example of how this would work for <code>createKillCommandForSystemBin("media", "mediaserver")</code> could be as follows.
	 * <p>
	 * 1/ The "ps" command returns something like this:<br>
	 * <code>
	 * mediacodec 230   1     54248  8148  binder_thr b6c2e1d8 S media.codec<br>
	 * media     231   1     22560  7148  binder_thr b6b8c1d8 S /system/bin/mediadrmserver<br>
	 * mediaex   234   1     61740  7140  binder_thr b62051d8 S media.extractor<br>
	 * media     235   1     63852  8168  binder_thr b630f1d8 S /system/bin/mediaserver<br>
	 * media_rw  938   157   8392   2360  inotify_re b6cf129c S /system/bin/sdcard<br>
	 * u0_a11    23523 224   977172 42100 sys_epoll_ b5efe094 S android.process.media<br>
	 * </code>
	 * and the result is forwarded to the "grep" command<br>
	 * 2/ The "grep" command gets filters all lines containing ^(linestart) followed by "media" followed by an arbitrary number of characters followed by "/system/bin/mediaserver" followed by $(end of line) and for each line, the result is forwarded to the "tr -s" command<br>
	 * 3/ The "tr -s" command removes all duplicate spaces and the result is forwarded to the "cut -d ' ' -f2" command <br>
	 * 4/ The "cut -d ' ' -f2" command splits the line using the space character and extracts the second field which corresponds to the matching PID of the process listed in the "ps" command<br>
	 * 5/ The kill command uses the result to kill the PID, effectively peforming the "kill 235" command<br>
	 *
	 * @param _user     name of the user
	 * @param _fileName name of the file under /system/bin
	 * @return the command to perform for killing such a process
	 */
	private static String createKillCommandForSystemBin(final String _user, final String _fileName) {
		return "kill $(ps|grep ^" + _user + ".*[/]system[/]bin[/]" + _fileName + "$|tr -s ' '|cut -d ' ' -f2);";
	}

	private static void runRestartCameraShellCommand() throws Throwable {
		final Process p = Runtime.getRuntime().exec("su");
		DataOutputStream os = null;
		try {
			os = new DataOutputStream(p.getOutputStream());
			//Creating the command for killing the relevant camera processes
			final String command =
				//Command to kill the process using the file "/system/bin/mediaserver" under the "media" user name
				createKillCommandForSystemBin("media", "mediaserver")
					//Command to kill the process using the file "/system/bin/cameraserver" under the "camera" user name
					+ createKillCommandForSystemBin("camera", "cameraserver");
			os.writeBytes(command + "\n");
		} finally {
			if (os != null) {
				try {
					os.writeBytes("exit\n");
				} catch (Throwable t) {
					t.printStackTrace();
				}
				try {
					os.flush();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				try {
					os.close();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				try {
					p.waitFor();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				try {
					p.destroy();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	public static void showToastMessage(@NonNull final Context _context, final String _messageText) {
		final Runnable showToastRunnable = new Runnable() {
			@Override
			public void run() {
				Toast.makeText(_context, _messageText, Toast.LENGTH_SHORT).show();
			}
		};
		if (_context instanceof MainActivity) {
			((MainActivity) _context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showToastRunnable.run();
				}
			});
		} else {
			showToastRunnable.run();
		}
	}

	@Override
	protected void onPause() {
		if (mAdView != null) {
			mAdView.pause();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mAdView != null) {
			mAdView.resume();
		}
	}

	@Override
	protected void onActivityResult(final int _requestCode, final int _resultCode, final Intent _data) {
		CRBillingManager.get().onActivityResult(_requestCode, _resultCode, _data);
		super.onActivityResult(_requestCode, _resultCode, _data);
	}

	@Override
	protected void onDestroy() {
		if (mAdView != null) {
			mAdView.destroy();
		}
		super.onDestroy();
	}

	public static void logEvent(final Context _context, final String _eventName) {
		if (!AppPrivateData.hasFireBaseData) {
			return;
		}
		com.google.firebase.analytics.FirebaseAnalytics.getInstance(_context).logEvent(_eventName, new Bundle());
	}

	public static String streamContentToString(final InputStream _in) throws IOException {
		final StringBuilder sb = new StringBuilder();

		final BufferedReader br = new BufferedReader(new InputStreamReader(_in));
		try {
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				line = br.readLine();
				if (line != null) {
					sb.append("\n");
				}
			}
		} finally {
			br.close();
		}

		return sb.toString();
	}
}
