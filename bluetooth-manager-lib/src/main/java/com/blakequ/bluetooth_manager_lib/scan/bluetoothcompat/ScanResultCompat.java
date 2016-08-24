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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;

/**
 * Backwards compatible version of a ScanResult
 */
public class ScanResultCompat implements Parcelable {
    // Remote bluetooth device.
    private BluetoothLeDevice mDevice;

    // Scan record, including advertising data and scan response data.
    @Nullable
    private ScanRecordCompat mScanRecord;

    // Received signal strength.
    private int mRssi;

    // Device timestamp when the result was last seen.
    private long mTimestampNanos;

    /**
     * Constructor of scan result.
     *
     * @param device         Remote bluetooth device that is found.
     * @param scanRecord     Scan record including both advertising data and scan response data.
     * @param rssi           Received signal strength.
     * @param timestampNanos Device timestamp when the scan result was observed.
     */
    public ScanResultCompat(BluetoothDevice device, @Nullable ScanRecordCompat scanRecord, int rssi,
                            long timestampNanos) {
        mDevice = new BluetoothLeDevice(device, rssi, scanRecord.getBytes(), timestampNanos);
        mScanRecord = scanRecord;
        mRssi = rssi;
        mTimestampNanos = timestampNanos;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    ScanResultCompat(ScanResult result) {
        mDevice = new BluetoothLeDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes(), System.currentTimeMillis());//result.getTimestampNanos()
        mScanRecord = new ScanRecordCompat(result.getScanRecord());
        mRssi = result.getRssi();
        mTimestampNanos = System.currentTimeMillis();//result.getTimestampNanos();
    }

    private ScanResultCompat(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mDevice != null) {
            dest.writeInt(1);
            mDevice.writeToParcel(dest, flags);
        }
        else {
            dest.writeInt(0);
        }
        if (mScanRecord != null) {
            dest.writeInt(1);
            dest.writeByteArray(mScanRecord.getBytes());
        }
        else {
            dest.writeInt(0);
        }
        dest.writeInt(mRssi);
        dest.writeLong(mTimestampNanos);
    }

    private void readFromParcel(Parcel in) {
        if (in.readInt() == 1) {
            mDevice = BluetoothLeDevice.CREATOR.createFromParcel(in);
        }
        if (in.readInt() == 1) {
            mScanRecord = ScanRecordCompat.parseFromBytes(in.createByteArray());
        }
        mRssi = in.readInt();
        mTimestampNanos = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns the remote bluetooth device identified by the bluetooth device address.
     */
    public BluetoothLeDevice getLeDevice() {
        return mDevice;
    }

    /**
     * Returns the remote bluetooth device identified by the bluetooth device address.
     * @return
     */
    public BluetoothDevice getDevice() {
        if (mDevice != null){
            return mDevice.getDevice();
        }
        return null;
    }

    /**
     * Returns the scan record, which is a combination of advertisement and scan response.
     */
    @Nullable
    public ScanRecordCompat getScanRecord() {
        return mScanRecord;
    }

    /**
     * Returns the received signal strength in dBm. The valid range is [-127, 127].
     */
    public int getRssi() {
        return mRssi;
    }

    /**
     * Returns timestamp since boot when the scan record was observed.
     */
    public long getTimestampNanos() {
        return mTimestampNanos;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mDevice, mRssi, mScanRecord, mTimestampNanos);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ScanResultCompat other = (ScanResultCompat) obj;
        return ObjectsCompat.equals(mDevice, other.mDevice) && (mRssi == other.mRssi) &&
                ObjectsCompat.equals(mScanRecord, other.mScanRecord)
                && (mTimestampNanos == other.mTimestampNanos);
    }

    @Override
    public String toString() {
        return "ScanResult{" + "mDevice=" + mDevice + ", mScanRecord="
                + ObjectsCompat.toString(mScanRecord) + ", mRssi=" + mRssi + ", mTimestampNanos="
                + mTimestampNanos + '}';
    }

    public static final Creator<ScanResultCompat> CREATOR = new Creator<ScanResultCompat>() {
        @Override
        public ScanResultCompat createFromParcel(Parcel source) {
            return new ScanResultCompat(source);
        }

        @Override
        public ScanResultCompat[] newArray(int size) {
            return new ScanResultCompat[size];
        }
    };

}
