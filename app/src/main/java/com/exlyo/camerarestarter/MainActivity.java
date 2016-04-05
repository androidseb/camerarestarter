package com.exlyo.camerarestarter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.restart_camera_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				restartButtonAction(MainActivity.this);
			}
		});
		findViewById(R.id.help_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				showMessageDialog(MainActivity.this, getString(R.string.help_message));
			}
		});
	}

	public static void restartButtonAction(final Context _context) {
		try {
			runRestartCameraShellCommand();
			showToastMessage(_context, _context.getString(R.string.camera_restared_successfully));
		} catch (Throwable t) {
			t.printStackTrace();
			showToastMessage(_context, _context.getString(R.string.camera_restart_failed, t.getMessage()));
		}
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

	private static void showMessageDialog(@NonNull final Activity _activity, final String _messageText) {
		_activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final AlertDialog.Builder builder = new AlertDialog.Builder(_activity);
				builder.setMessage(_messageText);
				builder.setPositiveButton(R.string.ok, null);
				final AlertDialog dialog = builder.create();
				dialog.show();
			}
		});
	}
}