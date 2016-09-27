package com.exlyo.camerarestarter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class RunCameraRestartActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (MainActivity.isAutoCameraActionEnabled(this)) {
			MainActivity.logEvent(RunCameraRestartActivity.this, "RESTART_ACTION_ACTIVITY");
			MainActivity.restartButtonAction(this);
		} else {
			MainActivity.showToastMessage(this, getString(R.string.auto_restart_camera_action_disabled_toast));
			startActivity(new Intent(this, MainActivity.class));
		}
		finish();
	}
}
