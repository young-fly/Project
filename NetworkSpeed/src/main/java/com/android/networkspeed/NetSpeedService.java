package com.android.networkspeed;

import android.app.Service;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * @author young on 17-2-6.
 */
public class NetSpeedService extends Service {

    private static final String TAG = NetSpeedService.class.getSimpleName();
    private static final String TRAFFIC_MEGABYTE = "M/s";
    private static final String TRAFFIC_KILOBYTE = "K/s";
    private static final int MSG_SEND_NET_SPEED = 1;
    public static final String ACTION_SEND_TRAFFIC = "com.hct.action.SEND_TRAFFIC";
    public static final String EXTRA_TRAFFIC = "Traffic";

    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private final int count = 3; //Refresh once every 3 seconds
    private String mNetData = "";

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(mRunnable, count * 1000);
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_SEND_NET_SPEED;
            msg.arg1 = getNetSpeed();
            mHandler.sendMessage(msg);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread("NetSpeed");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MSG_SEND_NET_SPEED) {
                    String mTraffic = getTraffic(msg.arg1, msg.arg1 > 1024 * 1024 ? TRAFFIC_MEGABYTE : TRAFFIC_KILOBYTE);
                    Intent sendIntent = new Intent(ACTION_SEND_TRAFFIC);
                    sendIntent.putExtra(EXTRA_TRAFFIC, mTraffic);
                    getApplicationContext().sendBroadcast(sendIntent);
                }
            }
        };
    }

    private String getTraffic(int data, String type) {
        String traffic = "0";
        if (data > 1024 * 1024) {
            mNetData = String.valueOf(data / 1024f / 1024f);
        } else {
            mNetData = String.valueOf(data / 1024f);
        }
        Log.d(TAG, "mNet = " + mNetData);
        if (Float.valueOf(mNetData) > 0.01) {
            String[] split = mNetData.split("\\.");
            if (split[0].length() > 3 && type.equals(TRAFFIC_KILOBYTE)) {
                type = TRAFFIC_MEGABYTE;
                traffic = String.valueOf(Float.valueOf(split[0]) / 1024).substring(0, 4);
            } else if (split[0].length() == 3) {
                traffic = split[0];
            } else if (split[0].length() == 2) {
                traffic = split[0] + "." + split[1].substring(0, 1);
            } else {
                traffic = mNetData.substring(0, 4);
            }
            return traffic + type;
        } else {
            return traffic + type;
        }
    }

    private int getNetSpeed() {
        long nowTotalRxBytes = TrafficStats.getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));

        lastTotalRxBytes = nowTotalRxBytes;
        lastTimeStamp = nowTimeStamp;

        return (int) speed;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mHandler.post(mRunnable);
    }

    @Override
    public void onDestroy() {
        mHandlerThread.quitSafely();
        mHandler.removeCallbacks(mRunnable);
        mHandlerThread = null;
        mHandler = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
