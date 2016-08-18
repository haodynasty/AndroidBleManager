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

import android.annotation.TargetApi;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Compatible version of ScanSettings
 */
public class ScanSettingsCompat implements Parcelable {
    /**
     * Perform Bluetooth LE scan in low power mode. This is the default scan mode as it consumes the
     * least power.
     */
    public static final int SCAN_MODE_LOW_POWER = 0;

    /**
     * Perform Bluetooth LE scan in balanced power mode. Scan results are returned at a rate that
     * provides a good trade-off between scan frequency and power consumption.
     */
    public static final int SCAN_MODE_BALANCED = 1;

    /**
     * Scan using highest duty cycle. It's recommended to only use this mode when the application is
     * running in the foreground.
     */
    public static final int SCAN_MODE_LOW_LATENCY = 2;

    /**
     * Trigger a callback for every Bluetooth advertisement found that matches the filter criteria.
     * If no filter is active, all advertisement packets are reported.
     */
    public static final int CALLBACK_TYPE_ALL_MATCHES = 1;

    // Bluetooth LE scan mode.
    private final int mScanMode;

    // Bluetooth LE scan callback type
    private final int mCallbackType;

    // Time of delay for reporting the scan result
    private final long mReportDelayMillis;

    public int getScanMode() {
        return mScanMode;
    }

    public int getCallbackType() {
        return mCallbackType;
    }

    /**
     * Returns report delay timestamp based on the device clock.
     */
    public long getReportDelayMillis() {
        return mReportDelayMillis;
    }

    private ScanSettingsCompat(int scanMode, int callbackType,
                         long reportDelayMillis) {
        mScanMode = scanMode;
        mCallbackType = callbackType;
        mReportDelayMillis = reportDelayMillis;
    }

    private ScanSettingsCompat(Parcel in) {
        mScanMode = in.readInt();
        mCallbackType = in.readInt();
        mReportDelayMillis = in.readLong();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    ScanSettings toApi21() {
        return new ScanSettings.Builder()
                .setReportDelay(getReportDelayMillis())
                .setScanMode(getScanMode())
                .build();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mScanMode);
        dest.writeInt(mCallbackType);
        dest.writeLong(mReportDelayMillis);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ScanSettingsCompat>
            CREATOR = new Creator<ScanSettingsCompat>() {
        @Override
        public ScanSettingsCompat[] newArray(int size) {
            return new ScanSettingsCompat[size];
        }

        @Override
        public ScanSettingsCompat createFromParcel(Parcel in) {
            return new ScanSettingsCompat(in);
        }
    };

    /**
     * Builder for {@link ScanSettingsCompat}.
     */
    public static final class Builder {
        private int mScanMode = SCAN_MODE_LOW_POWER;
        private final int mCallbackType = CALLBACK_TYPE_ALL_MATCHES;
        private long mReportDelayMillis = 0;

        /**
         * Set scan mode for Bluetooth LE scan.
         *
         * @param scanMode The scan mode can be one of {@link ScanSettingsCompat#SCAN_MODE_LOW_POWER},
         *            {@link ScanSettingsCompat#SCAN_MODE_BALANCED} or
         *            {@link ScanSettingsCompat#SCAN_MODE_LOW_LATENCY}.
         * @throws IllegalArgumentException If the {@code scanMode} is invalid.
         */
        public Builder setScanMode(int scanMode) {
            if (scanMode < SCAN_MODE_LOW_POWER || scanMode > SCAN_MODE_LOW_LATENCY) {
                throw new IllegalArgumentException("invalid scan mode " + scanMode);
            }
            mScanMode = scanMode;
            return this;
        }

        // Returns true if the callbackType is valid.
        private boolean isValidCallbackType(int callbackType) {
            return callbackType == CALLBACK_TYPE_ALL_MATCHES;
        }

        /**
         * Set report delay timestamp for Bluetooth LE scan.
         *
         * @param reportDelayMillis Delay of report in milliseconds. Set to 0 to be notified of
         *            results immediately. Values &gt; 0 causes the scan results to be queued up and
         *            delivered after the requested delay or when the internal buffers fill up.
         * @throws IllegalArgumentException If {@code reportDelayMillis} &lt; 0.
         */
        public Builder setReportDelay(long reportDelayMillis) {
            if (reportDelayMillis < 0) {
                throw new IllegalArgumentException("reportDelay must be > 0");
            }
            mReportDelayMillis = reportDelayMillis;
            return this;
        }

        /**
         * Build {@link ScanSettingsCompat}.
         */
        public ScanSettingsCompat build() {
            return new ScanSettingsCompat(mScanMode, mCallbackType, mReportDelayMillis);
        }
    }
}
