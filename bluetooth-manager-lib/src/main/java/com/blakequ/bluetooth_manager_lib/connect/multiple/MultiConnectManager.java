package com.blakequ.bluetooth_manager_lib.connect.multiple;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothSubScribeData;
import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
 * date     : 2016/8/19 9:49 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:manager multiple bluetooth device connect
 * <p>
 *     1.must set uuid for subscribe bluetooth device data, {@link #setServiceUUID(String)}, {@link #addBluetoothSubscribeData(BluetoothSubScribeData)}<br>
 *     2.register callback of bluetooth notify by {@link #setBluetoothGattCallback(BluetoothGattCallback)}<br>
 *     3.add device to connect queue, {@link #addDeviceToQueue(String)} or {@link #addDeviceToQueue(String[])}, {@link #removeDeviceFromQueue(String)}<br>
 *     4.start auto connect one by one, {@link #startConnect()}<br>
 *     5.close all connect, {@link #close(String)}, {@link #closeAll()}<br>
 *     <p/>
 */
@TargetApi(18)
public final class MultiConnectManager extends ConnectRequestQueue {
    private static final String TAG = "MultiConnectManager";
    private static MultiConnectManager INSTANCE;
    private BluetoothManager bluetoothManager;
    private static String serviceUUID;
    private BluetoothGattCallback mBluetoothGattCallback;
    private final Queue<BluetoothSubScribeData> subscribeQueue;
    private static Object obj = new Object();

    private MultiConnectManager(Context context){
        super(context);
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        subscribeQueue = new ConcurrentLinkedQueue<BluetoothSubScribeData>();
        BleManager.getBleParamsOptions();
    }

    public static MultiConnectManager getInstance(Context context){
        //双重锁
        if (INSTANCE == null){
            synchronized (obj){
                if (INSTANCE == null){
                    INSTANCE = new MultiConnectManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public List<BluetoothDevice> getConnectedDevice() {
        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (!isEmpty(devices)){
            List<BluetoothDevice> newDevices = new ArrayList<BluetoothDevice>();
            for (BluetoothDevice device: devices){
                if (getDeviceState(device.getAddress()) == ConnectState.CONNECTED) {
                    newDevices.add(device);
                }else {
                    Logger.i("Not exist connected device in queue "+device.getAddress());
                }
            }
            return newDevices;
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * register callback of bluetooth notify
     * @param callback
     */
    public void setBluetoothGattCallback(BluetoothGattCallback callback){
        this.mBluetoothGattCallback = callback;
    }

    /**
     * add subscribe data while read or write characteristic(or descriptor) after discover service.
     * if each device using different config, you should invoke {@link #cleanSubscribeData()} to clean queue before using {@link #startSubscribe(BluetoothGatt)}
     * @param data
     * @see #cleanSubscribeData()
     * @see #startSubscribe(BluetoothGatt)
     * @see #setServiceUUID(String)
     */
    public void addBluetoothSubscribeData(BluetoothSubScribeData data){
        subscribeQueue.add(data);
    }

    /**
     * clean subscribe list
     * @see #addBluetoothSubscribeData(BluetoothSubScribeData)
     */
    public void cleanSubscribeData(){
        subscribeQueue.clear();
    }

    /**
     * set bluetooth service uuid, can not be null
     * @see #addBluetoothSubscribeData(BluetoothSubScribeData)
     * @param serviceUUID
     */
    public void setServiceUUID(String serviceUUID){
        this.serviceUUID = serviceUUID;
    }

    @Override
    protected BluetoothGattCallback getBluetoothGattCallback() {
        return mBluetoothGattCallback;
    }

    @Override
    protected String getServiceUUID() {
        return serviceUUID;
    }

    @Override
    protected Queue<BluetoothSubScribeData> getSubscribeDataQueue() {
        return subscribeQueue;
    }

    @Deprecated
    public void setMaxConnectDeviceNum(int number){
        ConnectConfig.maxConnectDeviceNum = number;
    }
}
