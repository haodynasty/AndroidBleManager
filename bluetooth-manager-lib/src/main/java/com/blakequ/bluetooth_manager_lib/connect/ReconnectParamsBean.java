package com.blakequ.bluetooth_manager_lib.connect;

import android.os.SystemClock;

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
    private long startDisconnectTime;
    private static long BASE_SPACE_TIME = ConnectConfig.reconnectTime; //space time to reconnect

    public ReconnectParamsBean(String address) {
        this.address = address;
        this.startDisconnectTime = SystemClock.elapsedRealtime();
        this.number = 1;
    }

    public String getAddress() {
        return address;
    }

    public void updateAddress(String address){
        this.address = address;
        this.startDisconnectTime = SystemClock.elapsedRealtime();
        this.number = 1;
    }

    /**
     * 如果重连的次数超过4次，则下次重连时间呈指数增长
     * @return
     */
    public long getNextReconnectTime() {
        if (number <= ConnectConfig.reconnectedNum){
            nextReconnectTime = startDisconnectTime + BASE_SPACE_TIME*number;
        }else {
            nextReconnectTime = (long) (startDisconnectTime + BASE_SPACE_TIME* Math.pow(2, number));
        }
        return nextReconnectTime;
    }

    public int getNumber() {
        return number;
    }

    public void addNumber() {
        this.startDisconnectTime = SystemClock.elapsedRealtime();
        this.number++;
    }

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
