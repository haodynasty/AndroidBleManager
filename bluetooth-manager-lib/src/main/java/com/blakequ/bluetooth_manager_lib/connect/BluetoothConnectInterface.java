package com.blakequ.bluetooth_manager_lib.connect;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.blakequ.bluetooth_manager_lib.util.BluetoothUtils;
import com.orhanobut.logger.Logger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

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
 * date     : 2016/8/19 11:16 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:bluetooth connect interface
 */
public abstract class BluetoothConnectInterface {
    protected static final String TAG = "BluetoothConnectInterface";
    protected Context context;
    private BluetoothOperatorQueue mOpratorQueue;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public BluetoothConnectInterface(Context context){
        this.context = context;
        mOpratorQueue = new BluetoothOperatorQueue();
    }

    /**
     * release resource
     */
    public abstract void release();

    /**
     * get gatt connect callback
     * @return
     */
    protected abstract BluetoothGattCallback getBluetoothGattCallback();

    protected abstract String getServiceUUID();

    /**
     * get device gatt service, if not will return null
     * @param address
     * @return null if not find gatt service
     */
    public abstract BluetoothGatt getBluetoothGatt(String address);

    /**
     * get the list of subscribe
     * @return
     */
    protected abstract Queue<BluetoothSubScribeData> getSubscribeDataQueue();

    /**
     * invoke when bluetooth disconnect
     * @param gatt
     */
    protected abstract void onDeviceDisconnect(BluetoothGatt gatt, int errorState);

    /**
     * invoke when bluetooth connected
     * @param gatt
     */
    protected abstract void onDeviceConnected(BluetoothGatt gatt);

    /**
     * invoke when fail to discover service
     * @param gatt
     */
    protected abstract void onDiscoverServicesFail(BluetoothGatt gatt);

    /**
     * invoke when success to discover service
     * @param gatt
     */
    protected abstract void onDiscoverServicesSuccess(BluetoothGatt gatt);

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

    protected Handler getMainLooperHandler(){
        return mHandler;
    }

    protected BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Logger.i("onCharacteristicRead data status:" + GattError.parseConnectionError(status) + " " + characteristic.getUuid().toString());
            mOpratorQueue.nextOperator();
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Logger.i("onCharacteristicWrite write status:" + GattError.parseConnectionError(status));
            mOpratorQueue.nextOperator();
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            //status=133是GATT_ERROR错误http://stackoverflow.com/questions/25330938/android-bluetoothgatt-status-133-register-callback
            //http://www.loverobots.cn/android-ble-connection-solution-bluetoothgatt-status-133.html
            Logger.i("onConnectionStateChange gattStatus=" + GattError.parseConnectionError(status) + " newStatus="
                    + (newState == BluetoothProfile.STATE_CONNECTED ? "CONNECTED" : "DISCONNECTED"));

            //不同的手机当蓝牙关闭，设备断开（重启，远离）返回的状态不一样，newState都一样是DISCONNECTED，设备切换不会产生影响
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {//调用connect会调用
                    Logger.i("Connected to GATT server");
                    // Attempts to discover services after successful connection.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onDeviceConnected(gatt);
                            if (gatt != null && !gatt.discoverServices()) {
                                Logger.e("onConnectionStateChange start service discovery fail! Thread:" + Thread.currentThread());
                            }
                        }
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//调用disconnect会调用，设备断开或蓝牙关闭会进入
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onDeviceDisconnect(gatt, newState);
                        }
                    });
                }
            } else{ //调用connect和disconnect出错后会进入,设备断开或蓝牙关闭会进入
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDeviceDisconnect(gatt, newState);
                    }
                });
            }
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Logger.i("onDescriptorRead status=" + GattError.parseConnectionError(status));
            mOpratorQueue.nextOperator();
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Logger.i("onDescriptorWrite status=" + GattError.parseConnectionError(status));
            mOpratorQueue.nextOperator();
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onDescriptorWrite(gatt, descriptor, status);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            Logger.i("onServicesDiscovered status=" + GattError.parseConnectionError(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //start subscribe data
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDiscoverServicesSuccess(gatt);
                        if (gatt != null){
                            startSubscribe(gatt);
                        }
                    }
                });
            }else {
                Logger.e("onServicesDiscovered fail!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDiscoverServicesFail(gatt);
                    }
                });
            }
            if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onServicesDiscovered(gatt, status);
        }
    };

    /**
     * start subscribe data, add data to subscribe list before invoke this method
     * <p>You should invoke this method after onServicesDiscovered, otherwise can not find service<p/>
     * @param bluetoothGatt
     * @return boolean is success start read or write character
     */
    public boolean startSubscribe(BluetoothGatt bluetoothGatt){
        if (bluetoothGatt == null){
            Logger.e("Fail to subscribe, BluetoothGatt is null");
            return false;
        }
        boolean isSuccess = subscribe(bluetoothGatt.getDevice().getAddress());
        mOpratorQueue.start(bluetoothGatt);
        return isSuccess;
    }

    /**
     * 订阅蓝牙设备通知及读写数据
     * @return
     */
    protected boolean subscribe(String address){
        BluetoothGatt mBluetoothGatt = getBluetoothGatt(address);
        if (mBluetoothGatt == null){
            Logger.e("can not subscribe to ble device info "+address);
            return false;
        }
        mOpratorQueue.clean();

        if (isEmpty(getServiceUUID())){
            Logger.e("Service UUID is null");
            return false;
        }

        //check subscribe list
        if (getSubscribeDataQueue() == null && getSubscribeDataQueue().size() > 0){
            Logger.e("Subscribe BLE data is null, you must invoke addBluetoothSubscribeData to add data");
            return false;
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(getServiceUUID()));
        if (gattService != null){
            for (BluetoothSubScribeData data:getSubscribeDataQueue()){
                final BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(data.getCharacteristicUUID());
                if (characteristic != null){
                    switch (data.getOperatorType()){
                        case CHAR_WIRTE:
                            if (BluetoothUtils.isCharacteristicWrite(characteristic.getProperties())){
                                characteristic.setValue(data.getCharacteristicValue());
                                mOpratorQueue.addOperator(characteristic, true);
                            }else{
                                Logger.e("Fail to write characteristic, not have write property , uuid:"+characteristic.getUuid()+" ,property:"+characteristic.getProperties());
                            }
                            break;
                        case CHAR_READ:
                            //bug fix:samsung phone bug, can not read value
                            if (checkIsSamsung()){
                                setProperty(characteristic);
                            }
                            if(BluetoothUtils.isCharacteristicRead(characteristic.getProperties())){
                                mOpratorQueue.addOperator(characteristic, false);
                            }else{
                                Logger.e("Fail to read characteristic, not have read property , uuid:" + characteristic.getUuid() + " ,property:" + characteristic.getProperties());
                            }
                            break;
                        case DESC_READ:
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(data.getDescriptorUUID());
                            if (descriptor != null){
                                mOpratorQueue.addOperator(descriptor, false);
                            }else {
                                Logger.e("Fail to get descriptor read uuid:"+data.getDescriptorUUID());
                            }
                            break;
                        case DESC_WRITE:
                            BluetoothGattDescriptor descriptor2 = characteristic.getDescriptor(data.getDescriptorUUID());
                            if (descriptor2 != null){
                                descriptor2.setValue(data.getDescriptorValue());
                                mOpratorQueue.addOperator(descriptor2, true);
                            }else {
                                Logger.e("Fail to get descriptor write uuid:"+data.getDescriptorUUID());
                            }
                            break;
                        case NOTIFY:
                            if(BluetoothUtils.isCharacteristicNotify(characteristic.getProperties())){
                                mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                                BluetoothGattDescriptor descriptor3 = characteristic.getDescriptor(data.getDescriptorUUID());
                                if (descriptor3 != null){
                                    descriptor3.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    mOpratorQueue.addOperator(descriptor3, true);
                                }else {
                                    Logger.e("Fail to get notify descriptor uuid:"+data.getDescriptorUUID());
                                }
                            }else{
                                Logger.e("Fail to notify characteristic, not have notify property , uuid:" + characteristic.getUuid() + " ,property:" + characteristic.getProperties());
                            }
                            break;
                    }
                }else {
                    Logger.e("Fail to get characteristic service uuid:"+data.getCharacteristicUUID());
                }
            }
        }else {
            Logger.e("Can not get gatt service uuid:"+getServiceUUID());
            return false;
        }
        return true;
    }

    /**
     * 设置属性,设置读权限
     * @param flagReadChar
     */
    private void setProperty(BluetoothGattCharacteristic flagReadChar){
        Field properField = null;
        try {
            properField = flagReadChar.getClass().getDeclaredField("mProperties");
            properField.setAccessible(true);
            properField.set(flagReadChar, 10);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断手机类型
     * @return
     */
    protected boolean checkIsSamsung() {
        String brand = android.os.Build.BRAND;
        if (brand.toLowerCase().equals("samsung")) {
            return true;
        }
        return false;
    }

    public boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public <V> boolean isEmpty(List<V> sourceList) {
        return sourceList == null || sourceList.size() == 0;
    }

}
