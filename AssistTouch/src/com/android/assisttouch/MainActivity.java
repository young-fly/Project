package com.android.assisttouch;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button mBtnStart;
	private Button mBtnQuit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		if (Build.VERSION.SDK_INT >= 23) {
			if (!Settings.canDrawOverlays(this)) {
				Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivityForResult(intent, 1);
				Toast.makeText(this, "请先允许AssistTouch出现在顶部", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void initView() {
		mBtnStart = (Button) findViewById(R.id.btn_start);
		mBtnQuit = (Button) findViewById(R.id.btn_quit);
		mBtnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, TouchBallService.class);
				Bundle data = new Bundle();
				data.putInt("type", AccessibilityUtil.TYPE_ADD);
				intent.putExtras(data);
				startService(intent);
			}
		});
		mBtnQuit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, TouchBallService.class);
				Bundle data = new Bundle();
				data.putInt("type", AccessibilityUtil.TYPE_DEL);
				intent.putExtras(data);
				startService(intent);
			}
		});
	}
}