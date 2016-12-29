package com.blakequ.bluetooth_manager_lib;


import android.support.annotation.IntRange;

import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;

import static com.blakequ.bluetooth_manager_lib.scan.BackgroundPowerSaver.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
import static com.blakequ.bluetooth_manager_lib.scan.BackgroundPowerSaver.DEFAULT_BACKGROUND_SCAN_PERIOD;
import static com.blakequ.bluetooth_manager_lib.scan.BackgroundPowerSaver.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
import static com.blakequ.bluetooth_manager_lib.scan.BackgroundPowerSaver.DEFAULT_FOREGROUND_SCAN_PERIOD;

/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/12/1 14:28 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: config of ble connect and scan
 */

public class BleParamsOptions {

    private final boolean isDebugMode;

    //using for scan
    private final long foregroundScanPeriod;
    private final long foregroundBetweenScanPeriod;
    private final long backgroundScanPeriod;
    private final long backgroundBetweenScanPeriod;

    //using for connect
    private final int maxConnectDeviceNum;//一次最大连接设备个数
    private final int reconnectStrategy; //重连策略
    private final int reconnectMaxTimes; //最大重连次数
    private final long reconnectBaseSpaceTime; //重连基础时间间隔ms
    private final int reconnectedLineToExponentTimes; //快速重连的次数(线性到指数)
    private final int connectTimeOutTimes; //连接超时时间

    public boolean isDebugMode() {
        return isDebugMode;
    }

    public long getForegroundScanPeriod() {
        return foregroundScanPeriod;
    }

    public long getForegroundBetweenScanPeriod() {
        return foregroundBetweenScanPeriod;
    }

    public long getBackgroundScanPeriod() {
        return backgroundScanPeriod;
    }

    public long getBackgroundBetweenScanPeriod() {
        return backgroundBetweenScanPeriod;
    }

    public int getMaxConnectDeviceNum() {
        return maxConnectDeviceNum;
    }

    public int getReconnectStrategy() {
        return reconnectStrategy;
    }

    public int getReconnectMaxTimes() {
        return reconnectMaxTimes;
    }

    public long getReconnectBaseSpaceTime() {
        return reconnectBaseSpaceTime;
    }

    public int getReconnectedLineToExponentTimes() {
        return reconnectedLineToExponentTimes;
    }

    public int getConnectTimeOutTimes() {
        return connectTimeOutTimes;
    }

    private BleParamsOptions(Builder builder){
        this.isDebugMode = builder.isDebugMode;
        this.foregroundScanPeriod = builder.foregroundScanPeriod;
        this.foregroundBetweenScanPeriod = builder.foregroundBetweenScanPeriod;
        this.backgroundScanPeriod = builder.backgroundScanPeriod;
        this.backgroundBetweenScanPeriod = builder.backgroundBetweenScanPeriod;
        this.maxConnectDeviceNum = builder.maxConnectDeviceNum;
        this.reconnectStrategy = builder.reconnectStrategy;
        this.reconnectMaxTimes = builder.reconnectMaxTimes;
        this.reconnectBaseSpaceTime = builder.reconnectBaseSpaceTime;
        this.reconnectedLineToExponentTimes = builder.reconnectedLineToExponentTimes;
        this.connectTimeOutTimes = builder.connectTimeOutTimes;
    }

    public static class Builder {

        private boolean isDebugMode = true;
        private long foregroundScanPeriod = DEFAULT_FOREGROUND_SCAN_PERIOD;
        private long foregroundBetweenScanPeriod = DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
        private long backgroundScanPeriod = DEFAULT_BACKGROUND_SCAN_PERIOD;
        private long backgroundBetweenScanPeriod = DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
        private int maxConnectDeviceNum = 5;//一次最大连接设备个数
        private int reconnectStrategy = ConnectConfig.RECONNECT_LINE_EXPONENT; //重连策略
        private int reconnectMaxTimes = Integer.MAX_VALUE; //最大重连次数
        private long reconnectBaseSpaceTime = 8000; //重连基础时间间隔ms
        private int reconnectedLineToExponentTimes = 5; //快速重连的次数(线性到指数)
        private int connectTimeOutTimes = 15000; //连接超时时间15s

        /**
         * setting is debug mode, if false then the log will disable
         * @param isDebugMode you can set by BuildConfig.DEBUG, default is true
         * @return
         */
        public Builder setDebugMode(boolean isDebugMode){
            this.isDebugMode = isDebugMode;
            return this;
        }

        /**
         * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
         * This function is used to setup the period when switching
         * between background/foreground. To have it effect on an already running scan (when the next
         * cycle starts)
         *
         * @param foregroundScanPeriod defalut is 10 seconds, you should using milliseconds
         * @return
         */
        public Builder setForegroundScanPeriod(@IntRange(from = 0) long foregroundScanPeriod) {
            if (foregroundScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+foregroundScanPeriod);
            }
            this.foregroundScanPeriod = foregroundScanPeriod;
            return this;
        }

        /**
         * Sets the duration in milliseconds between each Bluetooth LE scan cycle to look for beacons.
         * This function is used to setup the period when switching
         * between background/foreground. To have it effect on an already running scan (when the next
         * cycle starts)
         * @param foregroundBetweenScanPeriod defalut is 5 seconds, you should using milliseconds
         * @return
         */
        public Builder setForegroundBetweenScanPeriod(@IntRange(from = 0) long foregroundBetweenScanPeriod) {
            if (foregroundBetweenScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+foregroundBetweenScanPeriod);
            }
            this.foregroundBetweenScanPeriod = foregroundBetweenScanPeriod;
            return this;
        }

        /**
         * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
         * This function is used to setup the period when switching
         * between background/foreground. To have it effect on an already running scan (when the next
         * cycle starts)
         * @param backgroundScanPeriod default is 10 seconds, you should using milliseconds
         * @return
         */
        public Builder setBackgroundScanPeriod(@IntRange(from = 0) long backgroundScanPeriod) {
            if (backgroundScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+backgroundScanPeriod);
            }
            this.backgroundScanPeriod = backgroundScanPeriod;
            return this;
        }

        /**
         * Sets the duration in milliseconds spent not scanning between each Bluetooth LE scan cycle when no ranging/monitoring clients are in the foreground
         * @param backgroundBetweenScanPeriod default is 5 minutes, you should using milliseconds
         * @return
         */
        public Builder setBackgroundBetweenScanPeriod(@IntRange(from = 0) long backgroundBetweenScanPeriod) {
            if (backgroundBetweenScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+backgroundBetweenScanPeriod);
            }
            this.backgroundBetweenScanPeriod = backgroundBetweenScanPeriod;
            return this;
        }

        /**
         * max number of connect ble device
         * @param maxConnectDeviceNum default is 5
         * @return
         */
        public Builder setMaxConnectDeviceNum(@IntRange(from = 1) int maxConnectDeviceNum) {
            if (maxConnectDeviceNum < 1){
                throw new IllegalArgumentException("Connect device number must > 1, now is "+maxConnectDeviceNum);
            }
            this.maxConnectDeviceNum = maxConnectDeviceNum;
            return this;
        }

        /**
         * how to reconnect, you have choose reconnectStrategy:
         * <ol>
         * <li>1. ConnectConfig.RECONNECT_LINEAR, each reconnection interval time is the same</li>
         * <li>2. ConnectConfig.RECONNECT_EXPONENT, the reconnect time interval is exponential growth</li>
         * <li>3. ConnectConfig.RECONNECT_LINE_EXPONENT, Start time interval is the same, after reconnectedLineToExponentTimes times then use exponential growth<li/>
         * </ol>
         * @param reconnectStrategy
         * @return
         */
        public Builder setReconnectStrategy(@IntRange(from = 1, to = 4) int reconnectStrategy) {
            if (reconnectStrategy < 1 || reconnectStrategy > 4){
                throw new IllegalArgumentException("reconnectStrategy range is 1 to 4");
            }
            this.reconnectStrategy = reconnectStrategy;
            return this;
        }

        /**
         * usable only the reconnect strategy is ConnectConfig.RECONNECT_LINE_EXPONENT
         * @param reconnectedLineToExponentTimes the times from linear to exponential, default is 5 times
         * @return
         */
        public Builder setReconnectedLineToExponentTimes(@IntRange(from = 1) int reconnectedLineToExponentTimes) {
            if (reconnectedLineToExponentTimes < 1){
                throw new IllegalArgumentException("reconnectedLineToExponentTimes value must >= 1, now is "+reconnectedLineToExponentTimes);
            }
            this.reconnectedLineToExponentTimes = reconnectedLineToExponentTimes;
            return this;
        }

        /**
         * max reconnect times, if you set 0 will not reconnect
         * @param reconnectMaxTimes default is Integer.MAX_VALUE
         * @return
         */
        public Builder setReconnectMaxTimes(@IntRange(from = 0) int reconnectMaxTimes) {
            if (reconnectMaxTimes < 0){
                throw new IllegalArgumentException("Reconnect max times must > 0, now is "+reconnectMaxTimes);
            }
            this.reconnectMaxTimes = reconnectMaxTimes;
            return this;
        }

        /**
         * reconnect interval time(>=1000ms)
         * @param reconnectBaseSpaceTime default is 8 seconds
         * @return
         */
        public Builder setReconnectBaseSpaceTime(@IntRange(from = 1000) long reconnectBaseSpaceTime) {
            if (reconnectBaseSpaceTime < 1000){
                throw new IllegalArgumentException("reconnectBaseSpaceTime must >= 1000ms, now is "+reconnectBaseSpaceTime);
            }
            this.reconnectBaseSpaceTime = reconnectBaseSpaceTime;
            return this;
        }

        /**
         * time out of connect device(after this time will check bluetooth state, if bluetooth not available will close all connect)
         * @param connectTimeOutTimes default is 15 seconds(must > 1 seconds)
         * @return
         */
        public Builder setConnectTimeOutTimes(@IntRange(from = 1000) int connectTimeOutTimes){
            if (connectTimeOutTimes < 1000){
                throw new IllegalArgumentException("connectTimeOutTimes must >= 1000ms, now is "+connectTimeOutTimes);
            }
            this.connectTimeOutTimes = connectTimeOutTimes;
            return this;
        }

        /** Builds configured {@link BleParamsOptions} object */
        public BleParamsOptions build() {
            return new BleParamsOptions(this);
        }
    }

    public static BleParamsOptions createDefault() {
        return new Builder().build();
    }

}
