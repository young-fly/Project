package com.android.networkspeed;

import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


	Button button;
	NetSpeedView netSpeedView;
	static final String TAG = "MainActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		button = (Button) findViewById(R.id.netspeed);
		netSpeedView = (NetSpeedView) findViewById(R.id.bytes_text);
		button.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.netspeed){
			Log.d(TAG,"netspeed");
			//Settings.Global.putInt(getContentResolver(), NetSpeedView.NETWORK_SPEED, 1);
		}
	}
}
