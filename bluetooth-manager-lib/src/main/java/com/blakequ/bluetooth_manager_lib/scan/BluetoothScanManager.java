package com.blakequ.bluetooth_manager_lib.scan;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresPermission;

import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanFilterCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanResultCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanSettingsCompat;
import com.blakequ.bluetooth_manager_lib.util.LogUtils;

import java.util.List;

/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p/>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/8/17 11:30 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: 扫描管理器，要实现的功能
 * 1. 扫描封装
 * 2. 扫描管理
 * 3. 支持ibeacon扫描解析
 * 4. 持续扫描省电管理（BackgroundPowerSaver)
 * 5. 循环扫描暂停与开始（当连接时可以让扫描暂停，一旦断开就重启扫描）
 * 6. 当前扫描状态
 * 注意：回调不在主线程中执行，需要自己在主线程中处理回调（{@link com.blakequ.bluetooth_manager_lib.scan.ScanOverListener} and {@link com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat}）,
 * 尤其是想在扫描结束之后直接执行连接蓝牙或断开蓝牙设备，都需要在主线程执行，否则在某些机型如三星会出现异常。
 */
@TargetApi(18)
public final class BluetoothScanManager {
    private static BluetoothScanManager INSTANCE = null;

    private static final String TAG = "BluetoothScanManager";
    private static boolean isAPI21ScanningDisabled = false;
    private Context mContext;
    //is background mode or not
    private boolean backgroundMode = false;
    private BackgroundPowerSaver mPowerSaver;
    private CycledLeScanner cycledLeScanner;
    private ScanCallbackCompat scanCallbackCompat;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private BluetoothScanManager(Context context){
        this.mContext = context;
        mPowerSaver = new BackgroundPowerSaver(context);
        cycledLeScanner = new CycledLeScanner(context,
                BackgroundPowerSaver.DEFAULT_BACKGROUND_SCAN_PERIOD,
                BackgroundPowerSaver.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD,
                backgroundMode,
                getScanCallback());
    }

    @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
    public static BluetoothScanManager getInstance(Context context){
        if (INSTANCE == null){
            LogUtils.d(TAG, "BluetoothScanManager instance creation");
            INSTANCE = new BluetoothScanManager(context);
        }
        return INSTANCE;
    }

    /**
     * Runs the specified action on the UI thread. If the current thread is the UI
     * thread, then the action is executed immediately. If the current thread is
     * not the UI thread, the action is posted to the event queue of the UI thread.
     *
     * @param action the action to run on the UI thread
     */
    public final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    /**
     * you can invoke this method and set scan time and waitting time between scan cycle,
     * @see BackgroundPowerSaver#setForegroundScanPeriod(long p)
     * @see BackgroundPowerSaver#setForegroundBetweenScanPeriod(long p)
     * @see BackgroundPowerSaver#setBackgroundScanPeriod(long p)
     * @see BackgroundPowerSaver#setBackgroundBetweenScanPeriod(long p)
     * @return
     */
    public BackgroundPowerSaver getPowerSaver(){
        return mPowerSaver;
    }

    /**
     * set scan device invoke
     * @param scanCallbackCompat
     */
    public void setScanCallbackCompat(ScanCallbackCompat scanCallbackCompat) {
        this.scanCallbackCompat = scanCallbackCompat;
    }

    public void setScanOverListener(ScanOverListener scanOverListener) {
        cycledLeScanner.setScanOverListener(scanOverListener);
    }

    /**
     * is scanning
     * @return
     */
    public boolean isScanning(){
      return cycledLeScanner.isScanning();
    }

    /**
     * stop cycle scan and will restart when invoke {@link #startCycleScan()}
     */
    public void stopCycleScan(){
        cycledLeScanner.setPauseScan(true);
    }

    /**
     * start scan device and will stop until invoke {@link #stopCycleScan()}
     */
    public void startCycleScan(){
        cycledLeScanner.startScan();
    }

    /**
     * Immediately start a scan(only one times)
     */
    public void startScanOnce(){
        cycledLeScanner.startOnceScan();
    }

    /**
     * add scan filter
     * @param scanFilter
     */
    public void addScanFilterCompats(ScanFilterCompat scanFilter){
        cycledLeScanner.addScanFilterCompats(scanFilter);
    }

    public void setScanSettings(ScanSettingsCompat scanSettings) {
        cycledLeScanner.setScanSettings(scanSettings);
    }

    /**
     * new scan is disabled(API >= 21)
     * @return
     */
    public static boolean isAPI21ScanningDisabled(){
        return isAPI21ScanningDisabled;
    }

    /**
     * set scan method(API>=21) disabled or not
     * @param disabled
     */
    public static void setAPI21ScanningDisabled(boolean disabled){
        isAPI21ScanningDisabled = disabled;
    }

    /**
     * This method notifies the beacon service that the application is either moving to background
     * mode or foreground mode.  When in background mode, BluetoothLE scans to look for beacons are
     * executed less frequently in order to save battery life. The specific scan rates for
     * background and foreground operation are set by the defaults below, but may be customized.
     * When ranging in the background, the time between updates will be much less frequent than in
     * the foreground.  Updates will come every time interval equal to the sum total of the
     * BackgroundScanPeriod and the BackgroundBetweenScanPeriod.
     *
     * @param backgroundMode true indicates the app is in the background
     * @see BackgroundPowerSaver#DEFAULT_FOREGROUND_SCAN_PERIOD
     * @see BackgroundPowerSaver#DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
     * @see BackgroundPowerSaver#DEFAULT_BACKGROUND_SCAN_PERIOD;
     * @see BackgroundPowerSaver#DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
     * @see BackgroundPowerSaver#setForegroundScanPeriod(long p)
     * @see BackgroundPowerSaver#setForegroundBetweenScanPeriod(long p)
     * @see BackgroundPowerSaver#setBackgroundScanPeriod(long p)
     * @see BackgroundPowerSaver#setBackgroundBetweenScanPeriod(long p)
     */
    public void setBackgroundMode(boolean backgroundMode) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogUtils.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
        }
        if (backgroundMode != this.backgroundMode) {
            this.backgroundMode = backgroundMode;
            cycledLeScanner.setBackgroundMode(mPowerSaver.getScanPeriod(), mPowerSaver.getBetweenScanPeriod(), backgroundMode);
        }
    }

    public boolean isBackgroundMode() {
        return backgroundMode;
    }

    private ScanCallbackCompat getScanCallback(){
        return new ScanCallbackCompat() {
            @Override
            public void onBatchScanResults(final List<ScanResultCompat> results) {
                if (scanCallbackCompat != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanCallbackCompat.onBatchScanResults(results);
                        }
                    });
                }
            }

            @Override
            public void onScanFailed(final int errorCode) {
                if (scanCallbackCompat != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanCallbackCompat.onScanFailed(errorCode);
                        }
                    });
                }
            }

            @Override
            public void onScanResult(final int callbackType, final ScanResultCompat result) {
                if (scanCallbackCompat != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanCallbackCompat.onScanResult(callbackType, result);
                        }
                    });
                }
            }
        };
    }
}
