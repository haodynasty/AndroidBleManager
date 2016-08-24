/*
 * Copyright (C) 2015 Joe Rogers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.v4.util.SimpleArrayMap;

import com.blakequ.bluetooth_manager_lib.scan.BluetoothScanManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class BluetoothLeScannerCompat {

    private static final String TAG = "BLeScannerCompat";

    @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
    public static void flushPendingScanResults(@NonNull BluetoothAdapter adapter, @NonNull ScanCallbackCompat callbackCompat) {
        IMPL.flushPendingScanResults(adapter, callbackCompat);
    }

    @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
    public static void startScan(@NonNull BluetoothAdapter adapter, @Nullable List<ScanFilterCompat> filters, @NonNull ScanSettingsCompat settings, @NonNull ScanCallbackCompat callbackCompat) {
        IMPL.startScan(adapter, filters, settings, callbackCompat);
    }

    @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
    public static void startScan(@NonNull BluetoothAdapter adapter, @NonNull ScanCallbackCompat callbackCompat) {
        IMPL.startScan(adapter, callbackCompat);
    }

    @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
    public static void stopScan(@NonNull BluetoothAdapter adapter, @NonNull ScanCallbackCompat callbackCompat) {
        IMPL.stopScan(adapter, callbackCompat);
    }

    static final BluetoothLeScannerCompatImpl IMPL;

    static {
        final int version = Build.VERSION.SDK_INT;
        if (BluetoothScanManager.isAPI21ScanningDisabled()){
            IMPL = new API18BluetoothLeScannerCompatImpl();
        }else{
            if (version >= Build.VERSION_CODES.LOLLIPOP) {
                IMPL = new API21BluetoothLeScannerCompatImpl();
            }else {
                IMPL = new API18BluetoothLeScannerCompatImpl();
            }
        }
    }

    interface BluetoothLeScannerCompatImpl {
        void flushPendingScanResults(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat);

        void startScan(BluetoothAdapter adapter, List<ScanFilterCompat> filters, ScanSettingsCompat settings, ScanCallbackCompat callbackCompat);

        void startScan(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat);

        void stopScan(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class API21BluetoothLeScannerCompatImpl implements BluetoothLeScannerCompatImpl {
        static final SimpleArrayMap<ScanCallbackCompat, API21ScanCallback> callbackMap = new SimpleArrayMap<>();

        @Override
        public void flushPendingScanResults(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat) {
            API21ScanCallback result = callbackMap.get(callbackCompat);
            if (result == null) {
                return;
            }
            adapter.getBluetoothLeScanner().flushPendingScanResults(result);
        }

        @Override
        public void startScan(BluetoothAdapter adapter, List<ScanFilterCompat> filters, ScanSettingsCompat settings, ScanCallbackCompat callbackCompat) {

            List<ScanFilter> scanFilters = null;
            if (filters != null) {
                scanFilters = new ArrayList<>(filters.size());

                for (ScanFilterCompat filter : filters) {
                    scanFilters.add(filter.toApi21());
                }
            }
            if (settings == null) {
                throw new IllegalStateException("Scan settings are null");
            }
            ScanSettings scanSettings = settings.toApi21();
            adapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings, registerCallback(callbackCompat));
        }

        @Override
        public void startScan(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat) {
            adapter.getBluetoothLeScanner().startScan(registerCallback(callbackCompat));
        }

        @Override
        public void stopScan(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat) {
            API21ScanCallback result = callbackMap.remove(callbackCompat);
            if (result == null) {
                return;
            }
            adapter.getBluetoothLeScanner().stopScan(result);
        }

        private API21ScanCallback registerCallback(ScanCallbackCompat callbackCompat) {
            API21ScanCallback result = callbackMap.get(callbackCompat);
            // Attempting to rescan, just let it fail deeper down.
            if (result != null) {
                return result;
            }
            result = new API21ScanCallback(callbackCompat);
            callbackMap.put(callbackCompat, result);
            return result;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    static class API18BluetoothLeScannerCompatImpl implements BluetoothLeScannerCompatImpl {
        static final SimpleArrayMap<ScanCallbackCompat, API18ScanCallback> callbackMap = new SimpleArrayMap<>();

        @Override
        public void flushPendingScanResults(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat) {
            // no matching api
            new IllegalArgumentException("this method can not found");
        }


        @SuppressWarnings("deprecation")
        @Override
        public void startScan(BluetoothAdapter adapter, List<ScanFilterCompat> filters, ScanSettingsCompat settings, ScanCallbackCompat callbackCompat) {
            adapter.startLeScan(registerCallback(filters, callbackCompat));
        }

        @SuppressWarnings("deprecation")
        @Override
        public void startScan(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat) {
            adapter.startLeScan(registerCallback(null, callbackCompat));
        }

        @SuppressWarnings("deprecation")
        @Override
        public void stopScan(BluetoothAdapter adapter, ScanCallbackCompat callbackCompat) {
            API18ScanCallback callback = callbackMap.remove(callbackCompat);
            if (callback == null) {
                return;
            }
            adapter.stopLeScan(callback);
        }

        private API18ScanCallback registerCallback(List<ScanFilterCompat> filters, ScanCallbackCompat callbackCompat) {
            API18ScanCallback result = callbackMap.get(callbackCompat);
            // Attempting to rescan, just let it fail deeper down.
            if (result != null) {
                return result;
            }
            result = new API18ScanCallback(filters, callbackCompat);
            callbackMap.put(callbackCompat, result);
            return result;
        }
    }

    static class API18ScanCallback implements BluetoothAdapter.LeScanCallback {

        private final List<ScanFilterCompat> filters;
        private final WeakReference<ScanCallbackCompat> callbackCompatRef;

        API18ScanCallback(List<ScanFilterCompat> filters, ScanCallbackCompat callbackCompat) {
            this.filters = filters;
            this.callbackCompatRef = new WeakReference<>(callbackCompat);
        }

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            ScanCallbackCompat callbackCompat = callbackCompatRef.get();
            if (callbackCompat == null) return;

            ScanResultCompat result = new ScanResultCompat(
                    device,
                    ScanRecordCompat.parseFromBytes(scanRecord),
                    rssi, System.currentTimeMillis());

            // No filters so return any result
            if (filters == null) {
                callbackCompat.onScanResult(ScanSettingsCompat.CALLBACK_TYPE_ALL_MATCHES, result);
                return;
            }

            // Filters specified, so see if there is a match.
            for (ScanFilterCompat filter : filters) {
                if (filter.matches(result)) {
                    callbackCompat.onScanResult(ScanSettingsCompat.CALLBACK_TYPE_ALL_MATCHES, result);
                    return;
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class API21ScanCallback extends ScanCallback {

        private final WeakReference<ScanCallbackCompat> callbackCompatRef;

        API21ScanCallback(ScanCallbackCompat callbackCompat) {
            this.callbackCompatRef = new WeakReference<>(callbackCompat);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanCallbackCompat callbackCompat = callbackCompatRef.get();
            if (callbackCompat != null) {
                callbackCompat.onScanResult(callbackType, new ScanResultCompat(result));
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            ScanCallbackCompat callbackCompat = callbackCompatRef.get();
            if (callbackCompat == null) return;
            List<ScanResultCompat> compatResults = new ArrayList<>(results.size());
            for (ScanResult result : results) {
                compatResults.add(new ScanResultCompat(result));
            }
            callbackCompat.onBatchScanResults(compatResults);
        }

        @Override
        public void onScanFailed(int errorCode) {
            ScanCallbackCompat callbackCompat = callbackCompatRef.get();
            if (callbackCompat != null) {
                callbackCompat.onScanFailed(errorCode);
            }
        }
    }
}
