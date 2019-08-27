package com.android.assisttouch;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;

import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;


import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.WindowManager;
import android.content.ComponentName;
import android.view.WindowManagerGlobal;

import com.android.internal.widget.LockPatternUtils;

/**
 * @author by Young on 2017/11/16.
 * <p>
 * 单击：返回上一级
 * 双击：锁定屏幕，打开多任务，打开通知栏
 * 长按：移动位置
 * 上滑：锁定屏幕，打开多任务，返回桌面
 * 下滑：锁定屏幕，打开多任务，打开通知栏
 * 左右滑：多任务切换
 */

public class AccessibilityUtil {

    static final String TAG = "AccessibilityUtil";

    private static int mRecentsTaskSize;
    private static int mRecentsTaskIndex;
    private static List<RecentsTaskInfo> taskInfos;

    public static final int TYPE_ADD = 0;
    public static final int TYPE_DEL = 1;

    public static final int TYPE_GESTURE_LEFT = 0;
    public static final int TYPE_GESTURE_RIGHT = 1;

    public static final int FEATURE_BACK = 1;
    public static final int FEATURE_HOME = 2;
    public static final int FEATURE_RECENTS = 3;
    public static final int FEATURE_NOTIFICATION = 4;
    public static final int FEATURE_QUICK_SETTINGS = 5;
    public static final int FEATURE_LOCK_SCREEN = 6;
    public static final int FEATURE_RECENTS_TRANSFORM = 7;
    public static final int FEATURE_TACK_SCREENSHOT = 8;

    /**
     * 可以使用 SharedPreferences 或者数据库还保存启动的数据
     */
    public static final String ASSIST_TOUCH_CLICK = "click";
    public static final String ASSIST_TOUCH_DOUBLE_CLICK = "double_click";
    public static final String ASSIST_TOUCH_LONG_PRESS = "long_press";
    public static final String ASSIST_TOUCH_DOWN_SLIDE = "down_slide";
    public static final String ASSIST_TOUCH_UP_SLIDE = "up_slide";
    public static final String ASSIST_TOUCH_LEFT_RIGHT_SLIDE = "left_right_slide";

    /**
     * 单击返回功能
     */
    public static void doBack() {
        Log.d(TAG, "doBack");
        inputKeyEvent(KeyEvent.KEYCODE_BACK);
    }

    /**
     * 返回桌面
     */
    public static void doHome() {
        Log.d(TAG, "doHome");
        inputKeyEvent(KeyEvent.KEYCODE_HOME);
    }

    /**
     * 打开多任务
     *
     * @param context
     */
    public static void doRecents(Context context) {
        if (!isInPackageAndClass("com.android.systemui", ".recents.RecentsActivity", context)) {
            Log.d(TAG, "doRecents");
            inputKeyEvent(KeyEvent.KEYCODE_APP_SWITCH);
        }
    }

    /**
     * 打开通知栏
     * * @param context
     */
    public static void doNotification(Context context) {
        Log.d(TAG, "doNotification");
        StatusBarManager statusBarManager = (StatusBarManager) context.getSystemService(
                Service.STATUS_BAR_SERVICE);
        statusBarManager.expandNotificationsPanel();
    }

    /**
     * 打开快捷设置
     * * @param context
     */
    public static void doQuickSettings(Context context) {
        Log.d(TAG, "doQuickSettings");
        StatusBarManager statusBarManager = (StatusBarManager) context.getSystemService(
                Service.STATUS_BAR_SERVICE);
        statusBarManager.expandSettingsPanel();
    }

    /**
     * 锁定屏幕
     *
     * @param context
     */
    public static void doLockScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
        new LockPatternUtils(context).requireCredentialEntry(UserHandle.USER_ALL);
        try {
            WindowManagerGlobal.getWindowManagerService().lockNow(null);
            Log.d(TAG, "doLockScreen");
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to lock device.", e);
        }
    }

    /**
     * 多任务切换
     *
     * @param context
     */
    public static void doRecentsTransform(Context context, int type) {
        Log.d(TAG, "doRecentsTransform");
        int currentSize = getProcessInfo(context).size();
        if (currentSize > 0) {
            if (mRecentsTaskSize != currentSize) {
                mRecentsTaskSize = currentSize;
                mRecentsTaskIndex = 0;
                Log.d(TAG, "doRecentsTransform.mRecentsTaskIndex = 0, mRecentsTaskSize = " + mRecentsTaskSize);
            } else {
                if (mRecentsTaskIndex >= mRecentsTaskSize - 1 && type == TYPE_GESTURE_RIGHT) {
                    mRecentsTaskIndex = mRecentsTaskSize - 1;
                } else if (mRecentsTaskIndex <= 0 && type == TYPE_GESTURE_LEFT) {
                    mRecentsTaskIndex = 0;
                } else {
                    if (type == TYPE_GESTURE_RIGHT) {
                        mRecentsTaskIndex = mRecentsTaskIndex + 1;
                    } else {
                        mRecentsTaskIndex = mRecentsTaskIndex - 1;
                    }
                    Log.d(TAG, "doRecentsTransform.mRecentsTaskIndex1 = " + mRecentsTaskIndex);
                }
            }
            Log.d(TAG, "doRecentsTransform.mRecentsTaskIndex2 = " + mRecentsTaskIndex);
            if (!isInPackage(taskInfos.get(mRecentsTaskIndex).getPackageName(), context)) {
                Intent intent = taskInfos.get(mRecentsTaskIndex).getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                context.startActivity(intent);
            } else if (mRecentsTaskIndex >= mRecentsTaskSize - 1 && type == TYPE_GESTURE_RIGHT) {
                mRecentsTaskIndex = mRecentsTaskSize - 1;
            } else if (mRecentsTaskIndex <= 0 && type == TYPE_GESTURE_LEFT) {
                mRecentsTaskIndex = 0;
            } else {
                if (type == TYPE_GESTURE_RIGHT) {
                    mRecentsTaskIndex = mRecentsTaskIndex + 1;
                } else {
                    mRecentsTaskIndex = mRecentsTaskIndex - 1;
                }
                Log.d(TAG, "doRecentsTransform.mRecentsTaskIndex3 = " + mRecentsTaskIndex);
                Intent intent = taskInfos.get(mRecentsTaskIndex).getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                context.startActivity(intent);
            }
        }
    }

    public static List<RecentsTaskInfo> getProcessInfo(Context context) {
        if (taskInfos == null) {
            taskInfos = new ArrayList<>();
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = context.getPackageManager();

        List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(
                ActivityManager.getMaxRecentTasksStatic(), ActivityManager.RECENT_IGNORE_UNAVAILABLE);

        int numTasks = recentTasks.size();
        Log.d(TAG, "getProcessInfo.numTasks = " + numTasks);
        if (numTasks != taskInfos.size()) {
            taskInfos.clear();
            // 这个activity的信息是我们的launcher
            ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(
                    Intent.CATEGORY_HOME).resolveActivityInfo(pm, 0);

            for (int i = 0; i < numTasks && (i < ActivityManager.getMaxRecentTasksStatic()); i++) {
                final ActivityManager.RecentTaskInfo info = recentTasks.get(i);
                RecentsTaskInfo taskInfo = new RecentsTaskInfo();
                Intent intent = new Intent(info.baseIntent);

                if (info.origActivity != null) {
                    intent.setComponent(info.origActivity);
                } else { /*continue;*/ }

                //如果找到是launcher，直接continue，后面的taskInfos.add操作就不会发生了
                if (homeInfo != null) {
                    if (homeInfo.packageName.equals(intent.getComponent().getPackageName())
                            && homeInfo.name.equals(intent.getComponent().getClassName())) {
                        continue;
                    }
                }
                // 设置intent的启动方式为 创建新task()【并不一定会创建】
                intent.setFlags((intent.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                // 获取指定应用程序activity的信息(按我的理解是：某一个应用程序的最后一个在前台出现过的activity。)
                final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);

                if (resolveInfo != null) {
                    final ActivityInfo activityInfo = resolveInfo.activityInfo;
                    final String title = activityInfo.loadLabel(pm).toString();
                    Drawable icon = activityInfo.loadIcon(pm);
                    String packageName = activityInfo.packageName;

                    String appFilter = "com.android.systemui";

                    if (title != null && title.length() > 0 && icon != null && !appFilter.contains(packageName)) {
                        taskInfo.setIntent(intent);
                        taskInfo.setTitle(title);
                        taskInfo.setIcon(icon);
                        taskInfo.setPackageName(packageName);
                        taskInfos.add(taskInfo);
                        Log.d(TAG, "packageName =" + packageName);
                    }
                }
            }
        } else {
            for (int i = 0; i < taskInfos.size() - 1; i++) {
                if (isInPackage(taskInfos.get(i).getPackageName(), context)) {
                    mRecentsTaskIndex = i;
                }
            }
        }
        return taskInfos;
    }

    private static void inputKeyEvent(int keyCode) {
        sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        sendKeyEvent(KeyEvent.ACTION_UP, keyCode);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static final void sendKeyEvent(int action, int code) {
        final KeyEvent keyEvent = new KeyEvent(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), action, code, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        InputManager.getInstance().injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public static boolean isInPackageAndClass(String packageName, String className, Context context) {
        boolean result = false;
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);

        if (rti != null && rti.size() > 0) {
            String pn = rti.get(0).topActivity.getPackageName();
            String cn = rti.get(0).topActivity.getShortClassName();
            Log.i(TAG, "pn = " + pn + ", cn = " + cn);
            if (pn.equals(packageName) && cn.equals(className))
                result = true;
        }
        return result;
    }

    public static boolean isInPackage(String packageName, Context context) {
        boolean result = false;
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);

        if (rti != null && rti.size() > 0) {
            String pn = rti.get(0).topActivity.getPackageName();
            Log.i(TAG, "pn = " + pn);
            if (pn.equals(packageName))
                result = true;
        }
        return result;
    }

    private static Context mContext;
    private static Handler mHandler = new Handler();
    static final Object mScreenshotLock = new Object();
    static ServiceConnection mScreenshotConnection = null;
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_SERVICE =
            "com.android.systemui.screenshot.TakeScreenshotService";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER =
            "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";

    static final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                    notifyScreenshotError();
                }
            }
        }
    };

    /**
     * 截图
     * {@link WindowManager#TAKE_SCREENSHOT_FULLSCREEN} 全屏模式
     */
    public static void doTakeScreenshot(Context context) {
        mContext = context;
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            final ComponentName serviceComponent = new ComponentName(SYSUI_PACKAGE,
                    SYSUI_SCREENSHOT_SERVICE);
            final Intent serviceIntent = new Intent();
            serviceIntent.setComponent(serviceComponent);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, WindowManager.TAKE_SCREENSHOT_FULLSCREEN);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        mHandler.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        //msg.arg1 = msg.arg2 = 0;
                        //if (mStatusBar != null && mStatusBar.isVisibleLw())
                        msg.arg1 = 1;
                        //if (mNavigationBar != null && mNavigationBar.isVisibleLw())
                        msg.arg2 = 1;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != null) {
                            mContext.unbindService(mScreenshotConnection);
                            mScreenshotConnection = null;
                            mHandler.removeCallbacks(mScreenshotTimeout);
                            notifyScreenshotError();
                        }
                    }
                }
            };
            if (mContext.bindService(serviceIntent, conn,
                    Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE)) {
                mScreenshotConnection = conn;
                mHandler.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    private static void notifyScreenshotError() {
        // If the service process is killed, then ask it to clean up after itself
        final ComponentName errorComponent = new ComponentName(SYSUI_PACKAGE,
                SYSUI_SCREENSHOT_ERROR_RECEIVER);
        Intent errorIntent = new Intent(Intent.ACTION_USER_PRESENT);
        errorIntent.setComponent(errorComponent);
        errorIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT |
                Intent.FLAG_RECEIVER_FOREGROUND);
        mContext.sendBroadcast(errorIntent);
    }

}
