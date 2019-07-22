package com.android.networkspeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * @author young on 18-11-15.
 */

public class NetSpeedView extends TextView {

    public static final String NETWORK_SPEED = "network_speed";
    private Context mContext;
    private Paint mPaint;
    private Rect mBound;
    private String mTraffic;

    private boolean isConnect;
    private SettingsObserver mObserver;
    private NetworkConnectReceiver mReceiver;


    public NetSpeedView(Context context) {
        this(context, null, 0);
    }

    public NetSpeedView(Context context, @Nullable AttributeSet attrs) {
        this(context, null, 0);
    }

    public NetSpeedView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(this.getCurrentTextColor());
        mPaint.setTextSize(getTextSize());

        mBound = new Rect();

        if (mObserver == null) {
            mObserver = new SettingsObserver();
            mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(NETWORK_SPEED), true, mObserver);
        }

        if (mReceiver == null) {
            mReceiver = new NetworkConnectReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(NetSpeedService.ACTION_SEND_TRAFFIC);
            mContext.registerReceiver(mReceiver, filter);
        }
    }

    /*@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mTraffic != null) {
            mPaint.getTextBounds(mTraffic, 0, mTraffic.length(), mBound);
            canvas.drawText(mTraffic, getWidth(), mBound.width() - 10, getHeight() / 2, mBound.height() / 2, mPaint);
        }
    }*/

    private void start() {
        mContext.startService(new Intent(mContext, NetSpeedService.class));
        setVisibility(VISIBLE);
    }

    private void stop() {
        mContext.stopService(new Intent(mContext, NetSpeedService.class));
        setVisibility(GONE);
    }

    class SettingsObserver extends ContentObserver {

        public SettingsObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (isConnect) {
                if (Settings.Global.getInt(mContext.getContentResolver(), NETWORK_SPEED, 0) == 0)
                    start();
                else
                    stop();
            }
        }
    }

    class NetworkConnectReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null && NetworkInfo.State.CONNECTED == info.getState()) {
                    isConnect = true;
                    if (Settings.Global.getInt(context.getContentResolver(), NETWORK_SPEED, 0) == 0)
                        start();
                    else
                        stop();
                } else {
                    isConnect = false;
                }
            } else if (action.equals(NetSpeedService.ACTION_SEND_TRAFFIC)) {
                mTraffic = intent.getStringExtra(NetSpeedService.EXTRA_TRAFFIC);
                if (mTraffic != null) {
                    setText(mTraffic);
                    //invalidate();
                }
            }
        }
    }
}
