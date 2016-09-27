package com.exlyo.camerarestarter;

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
		MainActivity.restartButtonAction(this);
		finish();
	}
}