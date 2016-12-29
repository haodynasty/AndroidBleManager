package com.blakequ.bluetooth_manager_lib.scan;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;

import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.BluetoothLeScannerCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanFilterCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanSettingsCompat;
import com.blakequ.bluetooth_manager_lib.util.BluetoothUtils;
import com.orhanobut.logger.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * date     : 2016/8/18 11:54 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
@TargetApi(18)
public class CycledLeScanner {
    private boolean isPrintCycleTime = true;
    private final Context mContext;
    private long scanPeriod;
    private long betweenScanPeriod;
    private boolean mBackgroundFlag = false;
    private ScanCallbackCompat scanCallbackCompat;
    private final BluetoothUtils mBluetoothUtils;
    private ScanOverListener scanOverListener;
    private long nextScanStartTime = 0;
    private long scanStopTime = 0;
    private long lastScanEndTime = 0;
    private boolean mScanning = false;
    private boolean isPauseScan = false; //pause scan or restart
    private boolean isOnceScan = false; //scan only once
    private boolean isStartNow = false;
    private final Handler mHandler = new Handler();
    private boolean isSetScanSetting = false;
    private ScanSettingsCompat scanSettings;
    private final List<ScanFilterCompat> scanFilterCompats = new CopyOnWriteArrayList<>();

    public CycledLeScanner(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, ScanCallbackCompat callbackCompat){
        this.mContext = context;
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
        this.scanCallbackCompat = callbackCompat;
        this.mBackgroundFlag = backgroundFlag;
        this.mBluetoothUtils = BluetoothUtils.getInstance(context);
    }

    /**
     * invoke at the end of scan(every scan cycle over)
     * @param scanOverListener
     */
    public void setScanOverListener(ScanOverListener scanOverListener) {
        this.scanOverListener = scanOverListener;
    }

    /**
     * add scan filter
     * @param scanFilter
     */
    public void addScanFilterCompats(ScanFilterCompat scanFilter){
        scanFilterCompats.add(scanFilter);
    }

    public void setScanSettings(ScanSettingsCompat scanSettings) {
        isSetScanSetting = true;
        this.scanSettings = scanSettings;
    }

    public void startOnceScan() {
        isOnceScan = true;
        scanLeDevice(true);
    }

    /**
     * pause or restart scan device cycle
     * @param isPauseScan
     */
    public void setPauseScan(boolean isPauseScan) {
        this.isPauseScan = isPauseScan;
        if (!isPauseScan){
            scanLeDevice(true);
        }else {
            scanLeDevice(false);
        }
    }

    /**
     * start scan device
     */
    public void startScan(){
        isPauseScan = false;
        scanLeDevice(true);
    }

    /**
     * start scan device right now
     */
    public void startScanNow(){
        isStartNow = true;
        isPauseScan = false;
        scanLeDevice(true);
    }

    /**
     * Tells the cycler the scan rate and whether it is in operating in background mode.
     * Background mode flag  is used only with the Android 5.0 scanning implementations to switch
     * between LOW_POWER_MODE vs. LOW_LATENCY_MODE
     * @param backgroundFlag is running background
     */
    public void setBackgroundMode(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Logger.w("Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (backgroundFlag != mBackgroundFlag) {
            Logger.d("restart polling task scanPeriod:" + scanPeriod + " betweenScanPeriod:" + betweenScanPeriod + " backgroundFlag:" + backgroundFlag + " mode:" + mBackgroundFlag);
            mBackgroundFlag = backgroundFlag;
            this.scanPeriod = scanPeriod;
            this.betweenScanPeriod = betweenScanPeriod;
            long now = SystemClock.elapsedRealtime();

            //update next scan start time（在等待开始扫描时修正下一次开始时间,提前开始）
            if (nextScanStartTime > now){
                long proposedNextScanStartTime = lastScanEndTime + betweenScanPeriod;
                if (proposedNextScanStartTime < nextScanStartTime){
                    Logger.d("Waiting...Adjusted nextScanStartTime to be" + (proposedNextScanStartTime - now) + " old:" + (nextScanStartTime - now));
                    nextScanStartTime = proposedNextScanStartTime;
                }
            }

            //update current scan stop time(如果在扫描中则修正本次的结束时间,提前结束)
            if (scanStopTime > now){
                long proposedStopTime = nextScanStartTime + scanPeriod;
                if (proposedStopTime < scanStopTime){
                    Logger.d("Scanning...Adjusted scanStopTime to be " + (proposedStopTime - now) + " old:" + (scanStopTime - now));
                    scanStopTime = proposedStopTime;
                }
            }

            //set scan setting params
            if (!isSetScanSetting || scanSettings == null){
                if (mBackgroundFlag) {
                    Logger.d("starting filtered scan in SCAN_MODE_LOW_POWER");
                    scanSettings = (new ScanSettingsCompat.Builder().setScanMode(ScanSettingsCompat.SCAN_MODE_LOW_POWER)).build();
                } else {
                    Logger.d("starting non-filtered scan in SCAN_MODE_LOW_LATENCY");
                    scanSettings = (new ScanSettingsCompat.Builder().setScanMode(ScanSettingsCompat.SCAN_MODE_LOW_LATENCY)).build();
                }
            }
        }
    }

    public boolean isScanning() {
        return mScanning;
    }

    public boolean isPauseScan() {
        return isPauseScan;
    }

    /**
     * start or stop scan
     * @param enable true-start scan right now，false-stop scan
     */
    private void scanLeDevice(boolean enable) {
        BluetoothAdapter mAdapter = mBluetoothUtils.getBluetoothAdapter();
        if (mBluetoothUtils == null || !mBluetoothUtils.isBluetoothIsEnable()){
            Logger.e("ScanDevice: Scanning fail! BluetoothAdapter is null");
            return;
        }

        if (deferScanIfNeeded()){
            if (!isStartNow){
                return;
            }else{
                Logger.i("ScanDevice: Scan right now!");
                isStartNow = false;
            }
        }

        if (enable) {
            if (mScanning) {
                Logger.d("ScanDevice: Scanning is running now !");
                return;
            }
            Logger.d("ScanDevice: Starting Scanning scanPeriod:"+scanPeriod+", between:"+betweenScanPeriod);
            mScanning = true;
            if (!isPauseScan || isOnceScan){
                try {
                    if (android.os.Build.VERSION.SDK_INT < 23 || checkLocationPermission()) {
                        if (android.os.Build.VERSION.SDK_INT >= 23 && !isGpsProviderEnabled(mContext)){
                            Logger.e("If SDK>=23, current SDK=" + android.os.Build.VERSION.SDK_INT+", Location info not open and can not scan any device!");
                            scanCallbackCompat.onScanFailed(ScanCallbackCompat.SCAN_FAILED_LOCATION_CLOSE);
                        }else {
                            Logger.i("ScanDevice: Start scan...");
                            BluetoothLeScannerCompat.startScan(mAdapter, scanFilterCompats, getScanSettings(), scanCallbackCompat);
                        }
                    }else{
                        scanCallbackCompat.onScanFailed(ScanCallbackCompat.SCAN_FAILED_LOCATION_PERMISSION_FORBID);
                        Logger.e("If SDK>=23, current SDK="+android.os.Build.VERSION.SDK_INT+", Please check the location permission is enabled(ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION)");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.e("Internal Android exception scanning for beacons "+e.toString());
                }

                if (isOnceScan){
                    Logger.d("ScanDevice: Scanning once");
                    isOnceScan = false;
                }
            }else{
                Logger.d("ScanDevice: Pause Scanning");
            }
            scanStopTime = SystemClock.elapsedRealtime() + scanPeriod;
            nextScanStartTime = scanStopTime + betweenScanPeriod;
            scheduleScanStop();
        } else {
            Logger.d("ScanDevice: Stopping Scan");
            stopScan();
        }
    }

    private void scheduleScanStop(){
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = scanStopTime - SystemClock.elapsedRealtime();
        if (millisecondsUntilStop > 0) {
            if (isPrintCycleTime){
                Logger.d("Waiting to stop scan cycle for another " + millisecondsUntilStop + " milliseconds");
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scheduleScanStop();
                }
            }, millisecondsUntilStop > 1000 ? 1000 : millisecondsUntilStop);
        } else {
            Logger.d("Stop cycle scan");
            stopScan();
        }
    }

    private void stopScan(){
        if (scanOverListener != null){
            scanOverListener.onScanOver();
        }
        if (mScanning) {
            BluetoothAdapter mAdapter = mBluetoothUtils.getBluetoothAdapter();
            if (mAdapter != null && mBluetoothUtils.isBluetoothIsEnable()) {
                try {
                    BluetoothLeScannerCompat.stopScan(mAdapter, scanCallbackCompat);
                    lastScanEndTime = SystemClock.elapsedRealtime();
                    Logger.d("stopping bluetooth le scan "+lastScanEndTime);
                } catch (Exception e) {
                    Logger.w("Internal Android exception scanning for beacons "+e.toString());
                }
            } else {
                Logger.d("Bluetooth is disabled.  Cannot scan for beacons.");
            }
            nextScanStartTime = SystemClock.elapsedRealtime() + betweenScanPeriod;
            //start next scan cycle
            if (!isPauseScan){
                scanLeDevice(true);
            }
        }
        mScanning = false;
    }

    /**
     * check is defter scan
     * @return
     */
    private boolean deferScanIfNeeded(){
        long millisecondsUntilStart = nextScanStartTime - SystemClock.elapsedRealtime();
        if (millisecondsUntilStart > 0) {
            if (isPrintCycleTime){
                Logger.d("Waiting to start next Bluetooth scan for another "+millisecondsUntilStart+" milliseconds");
            }
            // Don't actually wait until the next scan time -- only wait up to 1 second.  This
            // allows us to start scanning sooner if a consumer enters the foreground and expects
            // results more quickly.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isPauseScan) {
                        scanLeDevice(true);
                    }
                }
            }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
            return true;
        }
        Logger.d("Start cycle scan");
        return false;
    }


    /**
     * get scan settings
     * @return
     */
    private ScanSettingsCompat getScanSettings() {
        if (scanSettings == null){
            if (mBackgroundFlag) {
                Logger.d("starting filtered scan in SCAN_MODE_LOW_POWER");
                scanSettings = (new ScanSettingsCompat.Builder().setScanMode(ScanSettingsCompat.SCAN_MODE_LOW_POWER)).build();
            } else {
                Logger.d("starting non-filtered scan in SCAN_MODE_LOW_LATENCY");
                scanSettings = (new ScanSettingsCompat.Builder().setScanMode(ScanSettingsCompat.SCAN_MODE_LOW_LATENCY)).build();
            }
        }
        return scanSettings;
    }

    /**
     * is open GPS
     * @param context
     * @return
     */
    public static boolean isGpsProviderEnabled(Context context){
        LocationManager service = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        return service.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * when API>=23, if the location disabled, can not scan any devices
     * @return
     */
    private boolean checkLocationPermission() {
        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean checkPermission(final String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
