package com.exlyo.camerarestarter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.exlyo.camerarestarter.privatedata.AppPrivateData;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
	private static final String PREFS_FILE_NAME = "camera_restarter_prefs";
	private static final String PREF_KEY_CAMERA_AUTO_LAUNCH = "camera_auto_launch";
	private static final String PREF_KEY_AUTO_CAMERA_ACTION = "auto_camera_action";

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

		if (AppPrivateData.hasFireBaseData) {
			final ViewGroup adContainer = (ViewGroup) findViewById(R.id.ad_container);
			adContainer.setVisibility(View.VISIBLE);
			// Initialize the Mobile Ads SDK.
			MobileAds.initialize(this, AppPrivateData.adMobAppId);
			MobileAds.setAppMuted(true);
			mAdView = new AdView(this);
			mAdView.setAdSize(AdSize.SMART_BANNER);
			mAdView.setAdUnitId(AppPrivateData.adUnitId);
			adContainer
				.addView(mAdView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			// Create an ad request. Check your logcat output for the hashed device ID to
			// get test ads on a physical device. e.g.
			// "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
			final AdRequest adRequest = new AdRequest.Builder().build();
			// Start loading the ad in the background.
			mAdView.loadAd(adRequest);
		}
	}

	public static void restartButtonAction(final Context _context) {
		try {
			runRestartCameraShellCommand();
			if (MainActivity.isAutoCameraLaunchEnabled(_context)) {
				final Intent cameraStartIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				cameraStartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				_context.startActivity(cameraStartIntent);
			}
			showToastMessage(_context, _context.getString(R.string.camera_restared_successfully));
		} catch (Throwable t) {
			t.printStackTrace();
			showToastMessage(_context, _context.getString(R.string.camera_restart_failed, t.getMessage()));
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

	private static void runRestartCameraShellCommand() throws Throwable {
		final Process p = Runtime.getRuntime().exec("su");
		DataOutputStream os = null;
		BufferedReader br = null;
		try {
			os = new DataOutputStream(p.getOutputStream());
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			os.writeBytes("line=$(ps|grep media.*[/]system[/]bin[/]mediaserver)" + "\n");
			os.writeBytes("echo ${line}" + "\n");
			String line = br.readLine();
			if (line.startsWith("media") && line.endsWith("/system/bin/mediaserver")) {
				line = line.replaceAll("media[ ]*", "");
				final int pid = Integer.parseInt(line.substring(0, line.indexOf(" ")));
				os.writeBytes("kill " + pid + "\n");
			}
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
			}

			if (br != null) {
				try {
					br.close();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	private static void showToastMessage(@NonNull final Context _context, final String _messageText) {
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
}
