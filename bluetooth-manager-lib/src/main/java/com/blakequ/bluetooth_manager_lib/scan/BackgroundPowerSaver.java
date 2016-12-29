package com.blakequ.bluetooth_manager_lib.scan;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.orhanobut.logger.Logger;


/**
 *
 * Simply creating an instance of this class and holding a reference to it in your Application can
 * improve battery life by 60% by slowing down scans when your app is in the background.
 *
 */
@TargetApi(18)
public class BackgroundPowerSaver implements Application.ActivityLifecycleCallbacks {
    private int activeActivityCount = 0;
    private Context mContext;
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
            Logger.w("BackgroundPowerSaver requires API 18 or higher.");
            return;
        }
        ((Application)context.getApplicationContext()).registerActivityLifecycleCallbacks(this);
        this.mContext = context;
    }

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link BluetoothScanManager#setBackgroundMode}
     *
     * @param p
     */
    @Deprecated
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
    @Deprecated
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
    @Deprecated
    public void setBackgroundScanPeriod(long p) {
        backgroundScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds spent not scanning between each Bluetooth LE scan cycle when no ranging/monitoring clients are in the foreground
     *
     * @param p
     */
    @Deprecated
    public void setBackgroundBetweenScanPeriod(long p) {
        backgroundBetweenScanPeriod = p;
    }

    public long getScanPeriod() {
        BleParamsOptions options = BleManager.getBleParamsOptions();
        if (BluetoothScanManager.getInstance(mContext).isBackgroundMode()) {
            if (options != null){
                return options.getBackgroundScanPeriod();
            }else {
                return backgroundScanPeriod;
            }
        } else {
            if (options != null){
                return options.getForegroundScanPeriod();
            }else {
                return foregroundScanPeriod;
            }
        }
    }

    public long getBetweenScanPeriod() {
        BleParamsOptions options = BleManager.getBleParamsOptions();
        if (BluetoothScanManager.getInstance(mContext).isBackgroundMode()) {
            if (options != null){
                return options.getBackgroundBetweenScanPeriod();
            }else {
                return backgroundBetweenScanPeriod;
            }
        } else {
            if (options != null){
                return options.getForegroundBetweenScanPeriod();
            }else {
                return foregroundBetweenScanPeriod;
            }
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
            Logger.d("reset active activity count on resume.  It was " + activeActivityCount);
            activeActivityCount = 1;
        }
        BluetoothScanManager.getInstance(mContext).setBackgroundMode(false);
        Logger.d("activity resumed: "+activity+" active activities: "+activeActivityCount);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        activeActivityCount--;
        Logger.d("activity paused: "+activity+" active activities: "+activeActivityCount);
        if (activeActivityCount < 1) {
            Logger.d("setting background mode");
            BluetoothScanManager.getInstance(mContext).setBackgroundMode(true);
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
