/*
 * rtl_tcp_andro is an Android port of the famous rtl_tcp driver for 
 * RTL2832U based USB DVB-T dongles. It does not require root.
 * Copyright (C) 2012 by Martin Marinov <martintzvetomirov@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package marto.rtl_tcp_andro;

import marto.rtl_tcp_andro.tools.BinaryRunnerService;
import marto.rtl_tcp_andro.tools.DialogManager;
import marto.rtl_tcp_andro.tools.BinaryRunnerService.OnServiceTalkCallback;
import marto.rtl_tcp_andro.tools.DialogManager.dialogs;
import marto.rtl_tcp_andro.tools.RtlTcpStartException.err_info;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class StreamActivity extends FragmentActivity implements OnServiceTalkCallback {
	
	public static final String PREFS_NAME = "rtl_tcp_androPREFS";
	public static final String DISABLE_JAVA_FIX_PREF = "disable.java.usb.fix";
	
	private TextView terminal;
	private ScrollView scroll;
	private EditText arguments;
	private ToggleButton onoff;
	private CheckBox forceroot;
	private SharedPreferences prefs;

	private static final int START_REQ_CODE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stream);
		prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		
		terminal = (TextView) findViewById(R.id.terminal);
		scroll = (ScrollView) findViewById(R.id.ScrollArea);
		arguments = (EditText) findViewById(R.id.commandline);
		
		((Button) findViewById(R.id.enable_gui)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				findViewById(R.id.statusmsg).setVisibility(View.GONE);
				findViewById(R.id.gui).setVisibility(View.VISIBLE);
			}
		});
		
		
		(forceroot = (CheckBox) findViewById(R.id.forceRoot)).setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				final SharedPreferences.Editor editor = prefs.edit();
				
				editor.putBoolean(DISABLE_JAVA_FIX_PREF, isChecked);
				
				editor.commit();
			}
		});
		forceroot.setChecked(prefs.getBoolean(DISABLE_JAVA_FIX_PREF, false));
		
		
		(onoff = (ToggleButton) findViewById(R.id.onoff)).setOnClickListener(new Button.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (BinaryRunnerService.lastservice == null) {
					terminal.setText("");
					startActivityForResult(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("iqsrc://"+arguments.getText().toString())), START_REQ_CODE);
					onoff.setChecked(true);
				} else {
					BinaryRunnerService.lastservice.stopSelf();
					onoff.setChecked(false);
				}
					
			}
		});
		
		((Button) findViewById(R.id.copybutton)).setOnClickListener(new Button.OnClickListener() {

			@SuppressWarnings("deprecation")
			@SuppressLint("NewApi")
			@Override
			public void onClick(View v) {
				final String textToClip = terminal.getText().toString();
				int sdk = android.os.Build.VERSION.SDK_INT;
				if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
					android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setText(textToClip);
				} else {
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
					android.content.ClipData clip = android.content.ClipData.newPlainText("text label",textToClip);
					clipboard.setPrimaryClip(clip);
				}
				Toast.makeText(getApplicationContext(), R.string.copied_to_clip, Toast.LENGTH_LONG).show();
			}
		});
		
		((Button) findViewById(R.id.clearbutton)).setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				terminal.setText("");
			}
		});
		
		((Button) findViewById(R.id.help)).setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(dialogs.DIAG_ABOUT);
			}
		});
		
		((Button) findViewById(R.id.license)).setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(dialogs.DIAG_LICENSE);
			}
		});
	}
	
	public void showDialog(final DialogManager.dialogs id, final String ... args) {

		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		final Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		final DialogFragment newFragment = DialogManager.invokeDialog(id, args);
		try {
			newFragment.show(ft, "dialog");
		} catch (Throwable t) {t.printStackTrace();};
	}
	

	
	@Override
	protected void onStart() {
		super.onStart();
		BinaryRunnerService.registerCallback(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		BinaryRunnerService.unregisterCallback(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("log", terminal.getText().toString());
		outState.putString("args", arguments.getText().toString());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		terminal.setText(savedInstanceState.getString("log"));
		arguments.setText(savedInstanceState.getString("args"));
		forceroot.setChecked(prefs.getBoolean(DISABLE_JAVA_FIX_PREF, false));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		onoff.setChecked(BinaryRunnerService.lastservice != null);
	}
	
	/**
	 * Thread safe append to terminal and auto scroll.
	 * @param text The text to be appended to the terminal
	 */
	private void addToLog(final String text) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				terminal.append(text+"\n");
				scroll.pageScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

	@Override
	public void OnProcessTalk(String line) {
		addToLog(line);
	}

	@Override
	public void OnClosed(int exitvalue) {
		addToLog("Exited with value "+exitvalue);
		
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				onoff.setChecked(BinaryRunnerService.lastservice != null);
			}
		});
	}
	
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (requestCode == START_REQ_CODE) {
					if (resultCode == RESULT_OK)
						addToLog("Starting was successful!");
					else {
						err_info einfo = err_info.unknown_error;
						try { einfo = err_info.values()[data.getIntExtra("marto.rtl_tcp_andro.RtlTcpExceptionId", err_info.unknown_error.ordinal())]; } catch (Throwable e) {};
						addToLog("ERROR STARTING! Reason: "+einfo);
					}
				}

				onoff.setChecked(BinaryRunnerService.lastservice != null);
			}
		});
	}

	@Override
	public void OnWholeLogDump(final String log) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				terminal.setText(log);
				scroll.pageScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

}
