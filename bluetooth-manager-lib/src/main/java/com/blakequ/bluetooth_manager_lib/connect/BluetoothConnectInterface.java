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
import android.os.Message;

import com.blakequ.bluetooth_manager_lib.util.LogUtils;

import java.lang.reflect.Field;
import java.util.List;
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
public abstract class BluetoothConnectInterface extends BluetoothGattCallback {
    protected static final String TAG = "BluetoothConnectInterface";
    protected Context context;
    private BluetoothOperatorQueue mOpratorQueue;

    public BluetoothConnectInterface(Context context){
        this.context = context;
        mOpratorQueue = new BluetoothOperatorQueue();
    }

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
    protected abstract List<BluetoothSubScribeData> getSubscribeDataList();

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

    public abstract boolean close(String address);

    public abstract void disconnect(String address);

    protected abstract void reconnectDevice(String address);

    /**
     * hand message in main thread, send msg by {@link #sendMessage(int, Object)}
     * @param msg
     */
    protected abstract void handleMessageInMainThread(Message msg);

    /**
     * send msg to main looper, and you can invoke {@link #handleMessageInMainThread(Message)} to operator
     * @param msgId
     * @param obj
     */
    protected void sendMessage(int msgId, Object obj){
        Message msg = new Message();
        msg.what = msgId;
        msg.obj = obj;
        msg.arg1 = 0;
        mHandler.sendMessage(msg);
    }

    /**
     * send inner message
     * @param msgId
     * @param obj
     */
    private void sendInnerMessage(int msgId, Object obj){
        Message msg = new Message();
        msg.what = msgId;
        msg.obj = obj;
        msg.arg1 = 1;
        mHandler.sendMessage(msg);
    }

    protected Handler getMainLooperHandler(){
        return mHandler;
    }

    /**
     * in main looper do operator about connect/disconnect/close/discover
     */
    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.arg1 == 0){
                handleMessageInMainThread(msg);
                return true;
            }
            switch (msg.what){
                case 1://discover service
                    BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                    if (gatt != null && !gatt.discoverServices()){
                        LogUtils.e(TAG, "onConnectionStateChange start service discovery fail!");
                    }
                    break;
                case 2://subscribe service
                    BluetoothGatt gatt2 = (BluetoothGatt) msg.obj;
                    if (gatt2 != null){
                        subscribe(gatt2.getDevice().getAddress());
                        mOpratorQueue.start(gatt2);
                    }
                    break;
            }
            return true;
        }
    });

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        LogUtils.i(TAG, "onCharacteristicRead data status:" + GattError.parseConnectionError(status) + " " + characteristic.getUuid().toString());
        mOpratorQueue.nextOperator();
        if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        LogUtils.i(TAG, "onCharacteristicWrite write status:" + GattError.parseConnectionError(status));
        mOpratorQueue.nextOperator();
        if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        //status=133是GATT_ERROR错误http://stackoverflow.com/questions/25330938/android-bluetoothgatt-status-133-register-callback
        //http://www.loverobots.cn/android-ble-connection-solution-bluetoothgatt-status-133.html
        LogUtils.i(TAG, "onConnectionStateChange gattStatus=" + GattError.parseConnectionError(status) + " newStatus="
                + (newState == BluetoothProfile.STATE_CONNECTED ? "CONNECTED" : "DISCONNECTED"));

        //不同的手机当蓝牙关闭，设备断开（重启，远离）返回的状态不一样，newState都一样是DISCONNECTED，设备切换不会产生影响
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {//调用connect会调用
                LogUtils.i(TAG, "Connected to GATT server");
                // Attempts to discover services after successful connection.
                sendInnerMessage(1, gatt);
                onDeviceConnected(gatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//调用disconnect会调用，设备断开或蓝牙关闭会进入
                onDeviceDisconnect(gatt, newState);
            }
        } else{ //调用connect和disconnect出错后会进入,设备断开或蓝牙关闭会进入
            onDeviceDisconnect(gatt, newState);
        }
        if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onConnectionStateChange(gatt, status, newState);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        LogUtils.i(TAG, "onDescriptorRead status=" + GattError.parseConnectionError(status));
        mOpratorQueue.nextOperator();
        if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        LogUtils.i(TAG, "onDescriptorWrite status=" + GattError.parseConnectionError(status));
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
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        LogUtils.i(TAG, "onServicesDiscovered status=" + GattError.parseConnectionError(status));
        if (status == BluetoothGatt.GATT_SUCCESS) {
            //start subscribe data
            sendInnerMessage(3, gatt);
        }else {
            onDiscoverServicesFail(gatt);
            LogUtils.e(TAG, "onServicesDiscovered fail!");
        }
        if (getBluetoothGattCallback() != null) getBluetoothGattCallback().onServicesDiscovered(gatt, status);
    }

    /**
     * 订阅蓝牙设备通知及读写数据
     * @return
     */
    protected void subscribe(String address){
        BluetoothGatt mBluetoothGatt = getBluetoothGatt(address);
        if (mBluetoothGatt == null){
            LogUtils.e(TAG, "can not subscribe to ble device info "+address);
            return;
        }
        mOpratorQueue.clean();

        if (isEmpty(getServiceUUID())){
            LogUtils.e(TAG, "Service UUID is null");
            return;
        }

        //check subscribe list
        if (isEmpty(getSubscribeDataList())){
            LogUtils.e(TAG, "subscribe operator list is null");
            return;
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(getServiceUUID()));
        if (gattService != null){
            for (BluetoothSubScribeData data:getSubscribeDataList()){
                final BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(data.getCharacteristicUUID());
                if (characteristic != null){
                    switch (data.getOperatorType()){
                        case CHAR_WIRTE:
                            characteristic.setValue(data.getCharacteristicValue());
                            mOpratorQueue.addOperator(characteristic, true);
                            break;
                        case CHAR_READ:
                            //bug fix:samsung phone bug, can not read value
                            if (checkIsSamsung()){
                                setProperty(characteristic);
                            }
                            mOpratorQueue.addOperator(characteristic, false);
                            break;
                        case DESC_READ:
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(data.getDescriptorUUID());
                            if (descriptor != null){
                                mOpratorQueue.addOperator(descriptor, false);
                            }else {
                                LogUtils.e(TAG, "Fail to get descriptor read uuid:"+data.getDescriptorUUID());
                            }
                            break;
                        case DESC_WRITE:
                            BluetoothGattDescriptor descriptor2 = characteristic.getDescriptor(data.getDescriptorUUID());
                            if (descriptor2 != null){
                                descriptor2.setValue(data.getDescriptorValue());
                                mOpratorQueue.addOperator(descriptor2, true);
                            }else {
                                LogUtils.e(TAG, "Fail to get descriptor write uuid:"+data.getDescriptorUUID());
                            }
                            break;
                        case NOTIFY:
                            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor descriptor3 = characteristic.getDescriptor(data.getDescriptorUUID());
                            if (descriptor3 != null){
                                descriptor3.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mOpratorQueue.addOperator(descriptor3, true);
                            }else {
                                LogUtils.e(TAG, "Fail to get notify descriptor uuid:"+data.getDescriptorUUID());
                            }
                            break;
                    }
                }else {
                    LogUtils.e(TAG, "Fail to get characteristic service uuid:"+data.getCharacteristicUUID());
                }
            }
        }else {
            LogUtils.e(TAG, "Can not get gatt service uuid:"+getServiceUUID());
        }
    }

    /**
     * 设置属性
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
