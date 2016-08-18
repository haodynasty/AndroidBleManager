package com.blakequ.bluetooth_manager_lib.scan;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.blakequ.bluetooth_manager_lib.util.LogUtils;


/**
 *
 * Simply creating an instance of this class and holding a reference to it in your Application can
 * improve battery life by 60% by slowing down scans when your app is in the background.
 *
 */
@TargetApi(18)
public class BackgroundPowerSaver implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "BackgroundPowerSaver";
    private BluetoothScanManager scanManager;
    private int activeActivityCount = 0;
    /**
     * The default duration in milliseconds of the Bluetooth scan cycle
     */
    public static final long DEFAULT_FOREGROUND_SCAN_PERIOD = 10000;
    /**
     * The default duration in milliseconds spent not scanning between each Bluetooth scan cycle
     */
    public static final long DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD = 5*1000;
    /**
     * The default duration in milliseconds of the Bluetooth scan cycle when no ranging/monitoring clients are in the foreground
     */
    public static final long DEFAULT_BACKGROUND_SCAN_PERIOD = 10000;
    /**
     * The default duration in milliseconds spent not scanning between each Bluetooth scan cycle when no ranging/monitoring clients are in the foreground
     */
    public static final long DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD = 5 * 60 * 1000;

    private long foregroundScanPeriod = DEFAULT_FOREGROUND_SCAN_PERIOD;
    private long foregroundBetweenScanPeriod = DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
    private long backgroundScanPeriod = DEFAULT_BACKGROUND_SCAN_PERIOD;
    private long backgroundBetweenScanPeriod = DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;

    /**
     *
     * Constructs a new BackgroundPowerSaver
     *
     * @param context
     * @deprecated the countActiveActivityStrategy flag is no longer used.
     *
     */
    public BackgroundPowerSaver(Context context, boolean countActiveActivityStrategy) {
        this(context);
    }

    /**
     *
     * Constructs a new BackgroundPowerSaver using the default background determination strategy
     *
     * @param context
     */
    public BackgroundPowerSaver(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogUtils.w(TAG, "BackgroundPowerSaver requires API 18 or higher.");
            return;
        }
        ((Application)context.getApplicationContext()).registerActivityLifecycleCallbacks(this);
        scanManager = BluetoothScanManager.getInstance(context);
    }

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link BluetoothScanManager#setBackgroundMode}
     *
     * @param p
     */
    public void setForegroundScanPeriod(long p) {
        foregroundScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds between each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link BluetoothScanManager#setBackgroundMode}
     *
     * @param p
     */
    public void setForegroundBetweenScanPeriod(long p) {
        foregroundBetweenScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link BluetoothScanManager#setBackgroundMode}
     *
     * @param p
     */
    public void setBackgroundScanPeriod(long p) {
        backgroundScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds spent not scanning between each Bluetooth LE scan cycle when no ranging/monitoring clients are in the foreground
     *
     * @param p
     */
    public void setBackgroundBetweenScanPeriod(long p) {
        backgroundBetweenScanPeriod = p;
    }

    public long getScanPeriod() {
        if (scanManager.isBackgroundMode()) {
            return backgroundScanPeriod;
        } else {
            return foregroundScanPeriod;
        }
    }

    public long getBetweenScanPeriod() {
        if (scanManager.isBackgroundMode()) {
            return backgroundBetweenScanPeriod;
        } else {
            return foregroundBetweenScanPeriod;
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        activeActivityCount++;
        if (activeActivityCount < 1) {
            LogUtils.d(TAG, "reset active activity count on resume.  It was " + activeActivityCount);
            activeActivityCount = 1;
        }
        scanManager.setBackgroundMode(false);
        LogUtils.d(TAG, "activity resumed: "+activity+" active activities: "+activeActivityCount);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        activeActivityCount--;
        LogUtils.d(TAG, "activity paused: "+activity+" active activities: "+activeActivityCount);
        if (activeActivityCount < 1) {
            LogUtils.d(TAG, "setting background mode");
            scanManager.setBackgroundMode(true);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
