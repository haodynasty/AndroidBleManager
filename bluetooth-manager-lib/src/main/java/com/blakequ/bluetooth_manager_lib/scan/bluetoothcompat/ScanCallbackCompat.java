/*
 * Copyright (C) 2014 The Android Open Source Project
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

import java.util.List;

/**
 * Bluetooth LE scan callbacks. Scan results are reported using these callbacks.
 *
 * @see BluetoothLeScannerCompat#startScan
 */

public abstract class ScanCallbackCompat {
    /**
     * Fails to start scan as BLE scan with the same settings is already started by the app.
     */
    public static final int SCAN_FAILED_ALREADY_STARTED = 1;

    /**
     * Fails to start scan as app cannot be registered.
     */
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;

    /**
     * Fails to start scan due an internal error
     */
    public static final int SCAN_FAILED_INTERNAL_ERROR = 3;

    /**
     * Fails to start power optimized scan as this feature is not supported.
     */
    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 4;

    /**
     * new error code, Fails to scan as location permission is forbid()
     */
    public static final int SCAN_FAILED_LOCATION_PERMISSION_FORBID = 5;

    /**
     * use SDK>=23, The location is not open(if sdk>=23, will not scan any device)
     * http://stackoverflow.com/questions/33043582/bluetooth-low-energy-startscan-on-android-6-0-does-not-find-devices/33045489#33045489
     */
    public static final int SCAN_FAILED_LOCATION_CLOSE = 6;

    /**
     * Callback when a BLE advertisement has been found.
     *
     * @param callbackType Determines how this callback was triggered. Currently could only be
     *                     {@link android.bluetooth.le.ScanSettings#CALLBACK_TYPE_ALL_MATCHES}.
     * @param result       A Bluetooth LE scan result.
     */
    @SuppressWarnings("EmptyMethod")
    public void onScanResult(int callbackType, ScanResultCompat result) {
        // no implementation
    }

    /**
     * Callback when batch results are delivered.
     *
     * @param results List of scan results that are previously scanned.
     */
    @SuppressWarnings("EmptyMethod")
    public void onBatchScanResults(List<ScanResultCompat> results) {
    }

    /**
     * Callback when scan could not be started.
     *
     * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
     */
    @SuppressWarnings("EmptyMethod")
    public void onScanFailed(int errorCode) {
    }
}
