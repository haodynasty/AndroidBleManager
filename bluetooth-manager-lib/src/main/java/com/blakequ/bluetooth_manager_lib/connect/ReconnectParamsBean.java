package com.blakequ.bluetooth_manager_lib.connect;

import android.os.SystemClock;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.orhanobut.logger.Logger;

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
 * date     : 2016/8/19 18:23 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class ReconnectParamsBean {
    private String address;
    private int number;//reconnect times number
    private long nextReconnectTime;//next reconnect time
    private long startDisconnectTime; //the bluetooth disconnected time
    private boolean isReconnectNow = false;

    public ReconnectParamsBean(String address) {
        this.address = address;
        this.startDisconnectTime = SystemClock.elapsedRealtime();
        this.number = 0;
    }

    public String getAddress() {
        return address;
    }

    public void updateAddress(String address){
        this.address = address;
        this.startDisconnectTime = SystemClock.elapsedRealtime();
        this.number = 0;
    }

    /**
     * get next reconnect time
     * @return
     */
    public long getNextReconnectTime() {
        BleParamsOptions options = BleManager.getBleParamsOptions();
        switch (options.getReconnectStrategy()){
            case ConnectConfig.RECONNECT_EXPONENT:
                nextReconnectTime = (long) (startDisconnectTime + options.getReconnectBaseSpaceTime() * Math.pow(2, number));
                break;
            case ConnectConfig.RECONNECT_LINE_EXPONENT:
                if (number <= options.getReconnectedLineToExponentTimes()){
                    nextReconnectTime = startDisconnectTime + options.getReconnectBaseSpaceTime()*number;
                }else {
                    nextReconnectTime = (long) (startDisconnectTime + options.getReconnectBaseSpaceTime() * Math.pow(2, number));
                }
                break;
            case ConnectConfig.RECONNECT_LINEAR:
                nextReconnectTime = startDisconnectTime + options.getReconnectBaseSpaceTime()*number;
                break;
            case ConnectConfig.RECONNECT_FIXED_TIME:
                nextReconnectTime = startDisconnectTime + options.getReconnectBaseSpaceTime();
                break;
        }

        if (isReconnectNow){
            nextReconnectTime = SystemClock.elapsedRealtime();
        }

        //max reconnect times, not reconnect
        if (number >= options.getReconnectMaxTimes()){
            Logger.d("reconnect number="+number+" more than max times "+options.getReconnectMaxTimes());
            //将时间设置非常大
            nextReconnectTime = SystemClock.elapsedRealtime() + 10*24*60*60*1000;
        }
        return nextReconnectTime;
    }

    public boolean isReconnectNow() {
        return isReconnectNow;
    }

    public void setReconnectNow(boolean reconnectNow) {
        number = 0;
        startDisconnectTime = SystemClock.elapsedRealtime();
        isReconnectNow = reconnectNow;
    }

    public int getNumber() {
        return number;
    }

    /**
     * invoke after bluetooth disconnected
     */
    public void addNumber() {
        isReconnectNow = false;
        this.startDisconnectTime = SystemClock.elapsedRealtime();
        this.number++;
    }

    /**
     * if you can not want to reconnect by auto, you can set a max value
     * @param num
     */
    public void setNumber(int num){
        this.number = num;
    }

    @Override
    public String toString() {
        return "ReconnectParamsBean{" +
                "address='" + address + '\'' +
                ", number=" + number +
                ", next reconnect after " + (getNextReconnectTime()-startDisconnectTime)/1000 + "seconds"+
                ", startDisconnectTime=" + startDisconnectTime +
                '}';
    }
}
