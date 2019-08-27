package com.android.assisttouch;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * @author  by Young on 2017/11/16.
 */

public class TouchBallService extends Service {


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle data = intent.getExtras();
            if (data != null) {
                int type = data.getInt("type");
                if (type == AccessibilityUtil.TYPE_ADD) {
                    TouchWindowManager.addBallView(this);
                } else {
                    TouchWindowManager.removeBallView(this);
                }
            }
        }
        return START_STICKY;
    }
}
