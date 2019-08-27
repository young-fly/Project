package com.android.assisttouch;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Field;

/**
 * @author by Young on 2017/11/16.
 */

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class TouchBallView extends LinearLayout {


    static final String TAG = AccessibilityUtil.TAG;

    private ImageView mImgBall;
    private ImageView mImgBigBall;
    private ImageView mImgBg;

    private WindowManager mWindowManager;

    private WindowManager.LayoutParams mLayoutParams;

    private long mLastDownTime;
    private float mLastDownX;
    private float mLastDownY;

    private boolean mIsLongTouch;//是否长按

    private boolean mIsTouching;//是否触摸


    private float mTouchSlop; //短暂触摸滑动
    private final static long LONG_CLICK_TIMEOUT = 500;
    private final static long CLICK_TIMEOUT = 200;

    private final static int SINGLE_CLICK_TIMEOUT = 1;
    private final static int DOUBLE_CLICK_TIMEOUT = 2;

    private TouchBallService mService;
    private int mCurrentMode; //模式

    private final static int MODE_NONE = 0x000;
    private final static int MODE_DOWN = 0x001;
    private final static int MODE_UP = 0x002;
    private final static int MODE_LEFT = 0x003;
    private final static int MODE_RIGHT = 0x004;
    private final static int MODE_MOVE = 0x005;
    private final static int MODE_GONE = 0x006;
    private final static int MODE_CLICK = 0x007;
    private final static int MODE_DOUBLE_CLICK = 0x008;


    private final static int OFFSET = 10;

    private float mBigBallX;
    private float mBigBallY;

    private int mOffsetToParent;
    private int mOffsetToParentY;
    private Vibrator mVibrator;
    private long[] mPattern = {0, 100};

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case SINGLE_CLICK_TIMEOUT:
                    mHandler.removeMessages(SINGLE_CLICK_TIMEOUT);
                    doAssistTouch(MODE_CLICK);
                    //AccessibilityUtil.doBack();
                    //Log.d(TAG, "SINGLE_CLICK_TIMEOUT");
                    break;
                case DOUBLE_CLICK_TIMEOUT:
                    mHandler.removeMessages(DOUBLE_CLICK_TIMEOUT);
                    doAssistTouch(MODE_DOUBLE_CLICK);
                    //AccessibilityUtil.doLockScreen(mService);
                    //Log.d(TAG, "DOUBCLE_CLICK_TIMEOUT");
                    break;
                default:
                    break;
            }
            return true;
        }
    });


    public TouchBallView(Context context, WindowManager windowManager) {
        super(context);
        mService = (TouchBallService) context;
        mWindowManager = windowManager;
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.layout_ball, this);
        mImgBall = (ImageView) findViewById(R.id.img_ball);
        mImgBigBall = (ImageView) findViewById(R.id.img_big_ball);
        mImgBg = (ImageView) findViewById(R.id.img_bg);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mCurrentMode = MODE_NONE;

        mOffsetToParent = dip2px(25);
        mOffsetToParentY = getStatusBarHeight() + mOffsetToParent;

        mImgBigBall.post(new Runnable() {
            @Override
            public void run() {
                mBigBallX = mImgBigBall.getX();
                mBigBallY = mImgBigBall.getY();
            }
        });

        mImgBg.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mIsTouching = true;
                        mImgBall.setVisibility(INVISIBLE);
                        mImgBigBall.setVisibility(VISIBLE);
                        mLastDownTime = System.currentTimeMillis();
                        mLastDownX = event.getX();
                        mLastDownY = event.getY();

                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isLongTouch()) {
                                    mIsLongTouch = true;
                                    mVibrator.vibrate(mPattern, -1);
                                }
                            }
                        }, LONG_CLICK_TIMEOUT);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!mIsLongTouch && isTouchSlop(event)) {
                            return true;
                        }
                        if (mIsLongTouch && (mCurrentMode == MODE_NONE || mCurrentMode == MODE_MOVE)) {
                            mLayoutParams.x = (int) (event.getRawX() - mOffsetToParent);
                            mLayoutParams.y = (int) (event.getRawY() - mOffsetToParentY);
                            mWindowManager.updateViewLayout(TouchBallView.this, mLayoutParams);
                            mBigBallX = mImgBigBall.getX();
                            mBigBallY = mImgBigBall.getY();
                            mCurrentMode = MODE_MOVE;
                        } else {
                            doGesture(event);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        mIsTouching = false;
                        if (mIsLongTouch) {
                            mIsLongTouch = false;
                        } else if (isClick(event)) {
                            if (mHandler.hasMessages(SINGLE_CLICK_TIMEOUT)) {
                                mHandler.removeMessages(SINGLE_CLICK_TIMEOUT);
                                mHandler.sendEmptyMessage(DOUBLE_CLICK_TIMEOUT);
                            } else {
                                mHandler.sendEmptyMessageDelayed(SINGLE_CLICK_TIMEOUT, 200);
                            }
                        } else {
                            doAssistTouch(mCurrentMode);
                        }
                        mImgBall.setVisibility(VISIBLE);
                        mImgBigBall.setVisibility(INVISIBLE);
                        mCurrentMode = MODE_NONE;
                        break;
                }
                return true;
            }
        });
    }

    private boolean isLongTouch() {
        long time = System.currentTimeMillis();
        if (mIsTouching && mCurrentMode == MODE_NONE && (time - mLastDownTime >= LONG_CLICK_TIMEOUT)) {
            return true;
        }
        return false;
    }


    /**
     * 判断是否是轻微滑动
     *
     * @param event
     * @return
     */
    private boolean isTouchSlop(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (Math.abs(x - mLastDownX) < mTouchSlop && Math.abs(y - mLastDownY) < mTouchSlop) {
            return true;
        }
        return false;
    }

    /**
     * 判断手势（左右滑动、上拉下拉)）
     *
     * @param event
     */
    private void doGesture(MotionEvent event) {
        float offsetX = event.getX() - mLastDownX;
        float offsetY = event.getY() - mLastDownY;

        if (Math.abs(offsetX) < mTouchSlop && Math.abs(offsetY) < mTouchSlop) {
            return;
        }
        if (Math.abs(offsetX) > Math.abs(offsetY)) {
            if (offsetX > 0) {
                if (mCurrentMode == MODE_RIGHT) {
                    return;
                }
                mCurrentMode = MODE_RIGHT;
                mImgBigBall.setX(mBigBallX + OFFSET);
                mImgBigBall.setY(mBigBallY);
            } else {
                if (mCurrentMode == MODE_LEFT) {
                    return;
                }
                mCurrentMode = MODE_LEFT;
                mImgBigBall.setX(mBigBallX - OFFSET);
                mImgBigBall.setY(mBigBallY);
            }
        } else {
            if (offsetY > 0) {
                if (mCurrentMode == MODE_DOWN || mCurrentMode == MODE_GONE) {
                    return;
                }
                mCurrentMode = MODE_DOWN;
                mImgBigBall.setX(mBigBallX);
                mImgBigBall.setY(mBigBallY + OFFSET);
            } else {
                if (mCurrentMode == MODE_UP) {
                    return;
                }
                mCurrentMode = MODE_UP;
                mImgBigBall.setX(mBigBallX);
                mImgBigBall.setY(mBigBallY - OFFSET);
            }
        }
    }

    /**
     * 手指抬起后，根据当前模式触发对应功能
     */
    private void doAssistTouch(int currentMode) {
        switch (currentMode) {
            case MODE_CLICK:
                AccessibilityUtil.doBack();
                break;
            case MODE_DOUBLE_CLICK:
                doSettingType(AccessibilityUtil.FEATURE_LOCK_SCREEN);
                break;
            case MODE_LEFT:
                AccessibilityUtil.doRecentsTransform(mService, AccessibilityUtil.TYPE_GESTURE_LEFT);
                break;
            case MODE_RIGHT:
                AccessibilityUtil.doRecentsTransform(mService, AccessibilityUtil.TYPE_GESTURE_RIGHT);
                break;
            case MODE_DOWN:
                doSettingType(AccessibilityUtil.FEATURE_NOTIFICATION);
                break;
            case MODE_UP:
                doSettingType(AccessibilityUtil.FEATURE_RECENTS);
                break;

        }
        mImgBigBall.setX(mBigBallX);
        mImgBigBall.setY(mBigBallY);
    }

    /**
     * 可以通过 SharedPreferences 设置的类型调用相应的功能
     * @param type
     */
    private void doSettingType(int type) {
        switch (type) {
            case AccessibilityUtil.FEATURE_HOME:
                AccessibilityUtil.doHome();
                break;
            case AccessibilityUtil.FEATURE_RECENTS:
                AccessibilityUtil.doRecents(mService);
                break;
            case AccessibilityUtil.FEATURE_NOTIFICATION:
                AccessibilityUtil.doNotification(mService);
                break;
            case AccessibilityUtil.FEATURE_QUICK_SETTINGS:
                AccessibilityUtil.doQuickSettings(mService);
                break;
            case AccessibilityUtil.FEATURE_LOCK_SCREEN:
                AccessibilityUtil.doLockScreen(mService);
                break;
            case AccessibilityUtil.FEATURE_TACK_SCREENSHOT:
                AccessibilityUtil.doTakeScreenshot(mService);
                break;
        }
    }

    public void setLayoutParams(WindowManager.LayoutParams params) {
        mLayoutParams = params;
    }

    /**
     * 判断是否是点击
     *
     * @param event
     * @return
     */
    private boolean isClick(MotionEvent event) {
        float offsetX = Math.abs(event.getX() - mLastDownX);
        float offsetY = Math.abs(event.getY() - mLastDownY);
        //long time = System.currentTimeMillis() - mLastDownTime;

        if (offsetX < mTouchSlop * 2 && offsetY < mTouchSlop * 2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取通知栏高度
     *
     * @return
     */
    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object o = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = (Integer) field.get(o);
            statusBarHeight = getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    public int dip2px(float dip) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dip, getContext().getResources().getDisplayMetrics()
        );
    }

}
