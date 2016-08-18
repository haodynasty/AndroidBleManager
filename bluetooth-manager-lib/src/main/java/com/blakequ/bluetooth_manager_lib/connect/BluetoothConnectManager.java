package com.blakequ.bluetooth_manager_lib.connect;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.blakequ.bluetooth_manager_lib.util.BluetoothUtils;
import com.blakequ.bluetooth_manager_lib.util.LogUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 * date     : 2016/8/18 11:29 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: 用于当前蓝牙的连接管理，负责连接的建立断开，使用时需要设置回调{@link #setBluetoothGattCallback(BluetoothConnectCallback)},
 * 连接{@link #connect(String)},断开连接{@link #disconnect(String)}, 关闭连接{@link #close(String)}.
 * 注意：<p>
 * 1.在进行蓝牙断开和连接调用的时候，需要在主线程执行，否则在三星手机会出现许多异常错误或无法连接的情况
 * 2.该连接管理只能连接一个设备，不支持同时连接多个设备
 * 3.可以自定义断开后重连次数和重连的间隔时间
 * 4.必须设置设备的Service UUID {@link #setServiceUUID(String)}， 否则不能自动的进行通知和char和desc的读写操作（还需要{@link #addBluetoothSubscribeData(BluetoothSubScribeData)}）
 */
@TargetApi(18)
public final class BluetoothConnectManager {
    private static final String TAG = "BluetoothConnectManager";
    private static long reconnectTime = 4000; //断开后等待尝试重新连接的时间
    private static int reconnectedNum = 2; //断开后重新连接的次数（不会立即重连--考虑到可能是切换连接设备）

    private Context context;
    private static BluetoothConnectManager INSTANCE = null;
    private final BluetoothUtils mBluetoothUtils;
    private BluetoothConnectCallback mBluetoothGattCallback;
    private BluetoothOperatorQueue mOpratorQueue;
    private BluetoothManager bluetoothManager;
    private final Map<String, BluetoothGatt> gattMap; //保存连接过的gatt
    private final Map<String, Integer> deviceReconnectMap; //记录当前设备重新连接次数
    private boolean isDisconnectByHand = false;
    private final List<BluetoothSubScribeData> subscribeList = new ArrayList<BluetoothSubScribeData>();
    private static String serviceUUID;

    /**
     * 在主线程执行连接断开蓝牙等操作
     */
    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case 0://reconnect
                    connect((String) msg.obj);
                    break;
                case 1://discover service
                    BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                    if (!gatt.discoverServices()){
                        LogUtils.e(TAG, "onConnectionStateChange start service discovery fail!");
                    }
                    break;
                case 2://disconnect and reconnect device
                    BluetoothGatt gatt1 = (BluetoothGatt) msg.obj;
                    //可以不关闭，以便重用，因为在连接connect的时候可以快速连接
                    if (!checkIsSamsung() || !mBluetoothUtils.isBluetoothIsEnable()){//三星手机断开后直接连接
                        LogUtils.e(TAG, "Disconnected from GATT server address:"+msg.obj);
                        close(gatt1.getDevice().getAddress()); //防止出现status 133
                    }
                    reconnectDevice(gatt1.getDevice().getAddress()); //如果设备断开则指定时间后尝试重新连接,重新连接2次，不行则关闭
                    break;
                case 3://subscribe service
                    BluetoothGatt gatt2 = (BluetoothGatt) msg.obj;
                    subscribe(gatt2.getDevice().getAddress());
                    mOpratorQueue.start(gatt2);
                    break;
            }
            return true;
        }
    });

    private BluetoothConnectManager(Context context){
        this.context = context;
        mBluetoothUtils = BluetoothUtils.getInstance(context);
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mOpratorQueue = new BluetoothOperatorQueue();
        deviceReconnectMap = new ConcurrentHashMap<String, Integer>();
        gattMap = new ConcurrentHashMap<String, BluetoothGatt>(); //会有并发的断开和连接，故而必须使用并发ConcurrentHashMap才行，否则会有ConcurrentModificationException
    }

    @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
    public static BluetoothConnectManager getInstance(Context context){
        if (INSTANCE == null){
            INSTANCE = new BluetoothConnectManager(context);
        }
        return INSTANCE;
    }

    public void setBluetoothGattCallback(BluetoothConnectCallback callback){
        this.mBluetoothGattCallback = callback;
    }

    /**
     * add subscribe data while read or write characteristic(or descriptor) after discover service
     * @param data
     */
    public void addBluetoothSubscribeData(BluetoothSubScribeData data){
        subscribeList.add(data);
    }

    /**
     * set bluetooth service uuid, can not be null
     * @see #addBluetoothSubscribeData(BluetoothSubScribeData)
     * @param serviceUUID
     */
    public void setServiceUUID(String serviceUUID){
        this.serviceUUID = serviceUUID;
    }

    /**
     * 连接断开后重新连接的次数，默认2次（不会立即重连--考虑到可能是切换连接设备）,等待时间{@link #reconnectTime}
     * @param num
     */
    public void setReconnectNumber(int num){
        this.reconnectedNum = num;
    }

    /**
     * set reconnect space time by milliseconds
     * @param reconnectTime
     */
    public void setReconnectTime(long reconnectTime){
        this.reconnectTime = reconnectTime;
    }

    /**
     * 获取连接设备的BluetoothGatt对象，如果没有返回null
     * @param address
     * @return
     */
    public BluetoothGatt getBluetoothGatt(String address){
        if (!isEmpty(address) && gattMap.containsKey(address)){
            return gattMap.get(address);
        }
        return null;
    }

    /**
     * has device is connecting
     * @return
     */
    public boolean isConnectingDevice(){
        if (gattMap.size() == 0) return false;
        return true;
    }

    /**
     * 获取已经连接的设备,注意：返回的设备不一定全部是当前APP所连接的设备，需要通过UUID或设备名字等区分
     * @return
     */
    public List<BluetoothDevice> getConnectedDevice(){
        return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device. 为保证只有一个连接，当连接创建或初始化成功则会强制关闭其他连接
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        BluetoothAdapter mAdapter = mBluetoothUtils.getBluetoothAdapter();
        if (mAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (!mBluetoothUtils.isBluetoothIsEnable()){
            Log.e(TAG, "bluetooth is not enable.");
            return false;
        }

        if (isEmpty(serviceUUID)){
            throw new IllegalArgumentException("Service uuid is null, you must invoke setServiceUUID(String) method to set service uuid");
        }

        // Previously connected device.  Try to reconnect.
        if (gattMap.containsKey(address)){
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            Log.i(TAG, "Trying to use an existing gatt and reconnection device " + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            if (mBluetoothGatt.connect()) {
                closeOtherDevice(address);
                return true;
            } else {
                close(address);
                return false;
            }
        }

        BluetoothDevice device = mAdapter.getRemoteDevice(address);
        if (device != null){
             /*if We want to directly connect to the device, we can setting the autoConnect
             parameter to false.*/
            BluetoothGatt mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
            if (mBluetoothGatt != null){
                Log.d(TAG, "create a new connection address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
                gattMap.put(address, mBluetoothGatt);
                closeOtherDevice(address);
                return true;
            }else{
                Log.e(TAG, "Get Gatt fail!, address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            }
        }else{
            Log.e(TAG, "Device not found, address=" + address);
        }
        return false;
    }


    /**
     * 关闭蓝牙连接,会释放BluetoothGatt持有的所有资源
     * @param address
     */
    public boolean close(String address) {
        if (!isEmpty(address) && gattMap.containsKey(address)){
            LogUtils.w(TAG, "close gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.close();
            gattMap.remove(address);
            return true;
        }
        return false;
    }

    /**
     * 关闭所有蓝牙设备
     */
    public void closeAll(){
        for (String address:gattMap.keySet()) {
            close(address);
        }
    }

    /**
     * 断开蓝牙连接，不会释放BluetoothGatt持有的所有资源，可以调用mBluetoothGatt.connect()很快重新连接上
     * 如果不及时释放资源，可能出现133错误，http://www.loverobots.cn/android-ble-connection-solution-bluetoothgatt-status-133.html
     * @param address
     */
    public void disconnect(String address){
        if (!isEmpty(address) && gattMap.containsKey(address)){
            isDisconnectByHand = true;
            LogUtils.w(TAG, "disconnect gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * 重新连接断开的设备
     * @param address
     */
    private void reconnectDevice(final String address){
        int times = 1;
        isDisconnectByHand = false;
        if (deviceReconnectMap.containsKey(address)){
            times = deviceReconnectMap.get(address);
        }
        deviceReconnectMap.put(address, times + 1);
        if (times <= reconnectedNum){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //重新连接要求没有已经连接的设备，没有正在连接的，蓝牙可用
                    if (mBluetoothUtils.isBluetoothIsEnable()) {
                        boolean isReconncted = false;
                        if (gattMap.containsKey(address)) {
                            if (gattMap.size() == 1) isReconncted = true;
                        } else if (gattMap.size() == 0) {
                            isReconncted = true;
                        }

                        //如果已经连接上，也不重连
                        if (!isEmpty(getConnectedDevice())){
                            isReconncted = false;
                        }

                        if (isReconncted && getConnectedDevice().size() == 0) {
                            LogUtils.d(TAG, "reconnecting! will reconnect " + address);
                            //重连必须在主线程运行
                            Message msg = new Message();
                            msg.what = 0;
                            msg.obj = address;
                            mHandler.sendMessage(msg);
                        } else {
                            LogUtils.w(TAG, "reconnecting refuse! " + address + " flag:" + isReconncted);
                        }
                    }
                }
            }, reconnectTime * times);
        } else {
            //如果尝试reconnectedTimes次没有连接上
            LogUtils.d(TAG, "reconnecting fail! " + address);
            deviceReconnectMap.clear();
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onReconnectFail(address);
        }
    }

    /**
     * 关闭除了当前地址的设备外的其他连接
     * @param address
     */
    private void closeOtherDevice(String address){
        if (!isEmpty(address)){
            //关闭正在已经连接的设备
            List<BluetoothDevice> list = getConnectedDevice();
            for (BluetoothDevice device:list) {
                if (!device.getAddress().equals(address)){
                    close(device.getAddress());
                }
            }

            //关闭其他已经断开的设备
            for (String ads:gattMap.keySet()) {
                BluetoothGatt mBluetoothGatt = gattMap.get(ads);
                if (!ads.equals(address)){
                    close(ads);
                }
            }
        }
    }

    /**
     * 请求回调
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            LogUtils.i(TAG, "onCharacteristicRead data status:" + GattError.parseConnectionError(status) + " " + characteristic.getUuid().toString());
            mOpratorQueue.nextOperator();
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            LogUtils.i(TAG, "onCharacteristicWrite write status:" + GattError.parseConnectionError(status));
            mOpratorQueue.nextOperator();
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onCharacteristicWrite(gatt, characteristic, status);
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
                    if (gatt != null){
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = gatt;
                        mHandler.sendMessage(msg);
                    }

                    deviceReconnectMap.clear();
                    deviceReconnectMap.put(gatt.getDevice().getAddress(), 1);
                    isDisconnectByHand = false;
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//调用disconnect会调用，设备断开或蓝牙关闭会进入
                    if (gatt != null && !isDisconnectByHand){
                        isDisconnectByHand = false;
                        Message msg = new Message();
                        msg.what = 2;
                        msg.obj = gatt;
                        mHandler.sendMessage(msg);
                    }
                }
            } else{ //调用connect和disconnect出错后会进入,设备断开或蓝牙关闭会进入
                if (gatt != null){
                    Message msg = new Message();
                    msg.what = 2;
                    msg.obj = gatt;
                    mHandler.sendMessage(msg);
                }
            }
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
            LogUtils.i(TAG, "onDescriptorRead status=" + GattError.parseConnectionError(status));
            mOpratorQueue.nextOperator();
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
            LogUtils.i(TAG, "onDescriptorWrite status=" + GattError.parseConnectionError(status));
            mOpratorQueue.nextOperator();
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onDescriptorWrite(gatt, descriptor, status);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LogUtils.i(TAG, "onServicesDiscovered status=" + GattError.parseConnectionError(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //服务可用时，订阅数据,必须在主线程
                if (gatt != null){
                    Message msg = new Message();
                    msg.what = 3;
                    msg.obj = gatt;
                    mHandler.sendMessage(msg);
                }
            }else {
                Log.e(TAG, "onServicesDiscovered fail!");
            }
            if (mBluetoothGattCallback != null) mBluetoothGattCallback.onServicesDiscovered(gatt, status);
        }
    };

    /**
     * 订阅蓝牙设备通知及读写数据
     * @return
     */
    private void subscribe(String address){
        BluetoothGatt mBluetoothGatt = getBluetoothGatt(address);
        if (mBluetoothGatt == null){
            LogUtils.e(TAG, "can not subscribe to ble device info "+address);
            return;
        }
        mOpratorQueue.clean();

        if (isEmpty(serviceUUID)){
            LogUtils.e(TAG, "Service UUID is null");
            return;
        }

        //check subscribe list
        if (isEmpty(subscribeList)){
            LogUtils.e(TAG, "subscribe operator list is null");
            return;
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceUUID));
        if (gattService != null){
            for (BluetoothSubScribeData data:subscribeList){
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
            LogUtils.e(TAG, "Can not get gatt service uuid:"+serviceUUID);
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
    private boolean checkIsSamsung() {
        String brand = android.os.Build.BRAND;
        if (brand.toLowerCase().equals("samsung")) {
            return true;
        }
        return false;
    }

    private boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    private <V> boolean isEmpty(List<V> sourceList) {
        return sourceList == null || sourceList.size() == 0;
    }
}
