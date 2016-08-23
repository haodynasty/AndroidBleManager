package com.blakequ.bluetooth_manager_lib.connect;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.RequiresPermission;

import com.blakequ.bluetooth_manager_lib.util.BluetoothUtils;
import com.blakequ.bluetooth_manager_lib.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * description: 用于当前蓝牙的连接管理，负责连接的建立断开，使用时需要设置回调{@link #setBluetoothGattCallback(BluetoothGattCallback)},
 * 连接{@link #connect(String)},断开连接{@link #disconnect(String)}, 关闭连接{@link #close(String)}.
 * 注意：<br>
 * 1.在进行蓝牙断开和连接调用的时候，需要在主线程执行，否则在三星手机会出现许多异常错误或无法连接的情况<br>
 * 2.该连接管理只能连接一个设备，不支持同时连接多个设备<br>
 * 3.可以自定义断开后重连次数和重连的间隔时间<br>
 * 4.必须设置设备的Service UUID {@link #setServiceUUID(String)}， 否则不能自动的进行通知和char和desc的读写操作（还需要{@link #addBluetoothSubscribeData(BluetoothSubScribeData)}）
 */
@TargetApi(18)
public final class BluetoothConnectManager extends BluetoothConnectInterface{
    private static final String TAG = "BluetoothConnectManager";

    private static BluetoothConnectManager INSTANCE = null;
    private final BluetoothUtils mBluetoothUtils;
    private BluetoothGattCallback mBluetoothGattCallback;
    private BluetoothManager bluetoothManager;
    private final Map<String, BluetoothGatt> gattMap; //保存连接过的gatt
    private final List<BluetoothSubScribeData> subscribeList;
    private static String serviceUUID;
    private ReconnectParamsBean reconnectParamsBean;
    private ConnectStateListener connectStateListener;
    private static Object obj = new Object();

    public BluetoothConnectManager(Context context) {
        super(context);
        subscribeList = new ArrayList<BluetoothSubScribeData>();
        mBluetoothUtils = BluetoothUtils.getInstance(context);
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        gattMap = new ConcurrentHashMap<String, BluetoothGatt>(); //会有并发的断开和连接，故而必须使用并发ConcurrentHashMap才行，否则会有ConcurrentModificationException
    }


    @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
    public static BluetoothConnectManager getInstance(Context context){
        if (INSTANCE == null){
            synchronized (obj){
                if (INSTANCE == null){
                    INSTANCE = new BluetoothConnectManager(context);
                }
            }
        }
        return INSTANCE;
    }


    public void setBluetoothGattCallback(BluetoothGattCallback callback){
        this.mBluetoothGattCallback = callback;
    }

    public void setConnectStateListener(ConnectStateListener listener){
        this.connectStateListener = listener;
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

    @Override
    public BluetoothGatt getBluetoothGatt(String address){
        if (!isEmpty(address) && gattMap.containsKey(address)){
            return gattMap.get(address);
        }
        return null;
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
    protected void onDeviceDisconnect(final BluetoothGatt gatt, int errorState) {
        //is bluetooth enable
        //可以不关闭，以便重用，因为在连接connect的时候可以快速连接
        if (!checkIsSamsung() || !mBluetoothUtils.isBluetoothIsEnable()){//三星手机断开后直接连接
            LogUtils.e(TAG, "Disconnected from GATT server address:"+gatt.getDevice().getAddress());
            close(gatt.getDevice().getAddress()); //防止出现status 133
        }else {
            updateConnectStateListener(gatt.getDevice().getAddress(), ConnectState.NORMAL);
        }

        //if disconnect by hand, so not run reconnect device
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reconnectDevice(gatt.getDevice().getAddress()); //如果设备断开则指定时间后尝试重新连接,重新连接
            }
        });
    }

    @Override
    protected void onDeviceConnected(BluetoothGatt gatt) {
        updateConnectStateListener(gatt.getDevice().getAddress(), ConnectState.CONNECTED);
        reconnectParamsBean = null;
    }

    @Override
    protected void onDiscoverServicesFail(final BluetoothGatt gatt) {
        if (!checkIsSamsung() || !mBluetoothUtils.isBluetoothIsEnable()){//三星手机断开后直接连接
            LogUtils.e(TAG, "Disconnected from GATT server address:"+gatt.getDevice().getAddress());
            close(gatt.getDevice().getAddress()); //防止出现status 133
        }else {
            updateConnectStateListener(gatt.getDevice().getAddress(), ConnectState.NORMAL);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reconnectDevice(gatt.getDevice().getAddress());
            }
        });
    }

    @Override
    protected void onDiscoverServicesSuccess(BluetoothGatt gatt) {
    }

    @Override
    protected List<BluetoothSubScribeData> getSubscribeDataList() {
        return subscribeList;
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
    protected boolean connect(final String address) {
        BluetoothAdapter mAdapter = mBluetoothUtils.getBluetoothAdapter();
        if (mAdapter == null || address == null) {
            LogUtils.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        if (!mBluetoothUtils.isBluetoothIsEnable()){
            LogUtils.e(TAG, "bluetooth is not enable.");
            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        if (isEmpty(serviceUUID)){
            throw new IllegalArgumentException("Service uuid is null, you must invoke setServiceUUID(String) method to set service uuid");
        }

        // Previously connected device.  Try to reconnect.
        if (gattMap.containsKey(address)){
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            LogUtils.i(TAG, "Trying to use an existing gatt and reconnection device " + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            if (mBluetoothGatt.connect()) {
                closeOtherDevice(address);
                updateConnectStateListener(address, ConnectState.CONNECTING);
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
            BluetoothGatt mBluetoothGatt = device.connectGatt(context, false, this);
            if (mBluetoothGatt != null){
                LogUtils.d(TAG, "create a new connection address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
                gattMap.put(address, mBluetoothGatt);
                closeOtherDevice(address);
                updateConnectStateListener(address, ConnectState.CONNECTING);
                return true;
            }else{
                LogUtils.e(TAG, "Get Gatt fail!, address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            }
        }else{
            LogUtils.e(TAG, "Device not found, address=" + address);
        }
        updateConnectStateListener(address, ConnectState.NORMAL);
        return false;
    }


    /**
     * 关闭蓝牙连接,会释放BluetoothGatt持有的所有资源
     * @param address
     */
    protected boolean close(String address) {
        if (!isEmpty(address) && gattMap.containsKey(address)){
            LogUtils.w(TAG, "close gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.close();
            gattMap.remove(address);
            updateConnectStateListener(address, ConnectState.NORMAL);
            return true;
        }
        return false;
    }

    /**
     * 关闭所有蓝牙设备
     */
    protected void closeAll(){
        for (String address:gattMap.keySet()) {
            close(address);
        }
    }

    /**
     * 断开蓝牙连接，不会释放BluetoothGatt持有的所有资源，可以调用mBluetoothGatt.connect()很快重新连接上
     * 如果不及时释放资源，可能出现133错误，http://www.loverobots.cn/android-ble-connection-solution-bluetoothgatt-status-133.html
     * @param address
     */
    protected void disconnect(String address){
        if (!isEmpty(address) && gattMap.containsKey(address)){
            reconnectParamsBean = new ReconnectParamsBean(address);
            reconnectParamsBean.setNumber(1000);
            LogUtils.w(TAG, "disconnect gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.disconnect();
            updateConnectStateListener(address, ConnectState.NORMAL);
        }
    }

    /**
     * 重新连接断开的设备
     * @param address
     */
    private void reconnectDevice(final String address){
        if (reconnectParamsBean != null){
            if (!reconnectParamsBean.getAddress().equals(address)){
                reconnectParamsBean.updateAddress(address);
            }else {
                if (reconnectParamsBean.getNumber() == 1){//same device
                    reconnectParamsBean.updateAddress(address);
                }else if(reconnectParamsBean.getNumber() == 1000){//disconnect by hand
                    reconnectParamsBean.setNumber(1);
                    return;
                }
            }
        }else{
            reconnectParamsBean = new ReconnectParamsBean(address);
        }

        getMainLooperHandler().postDelayed(new Runnable() {
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
                    if (!isEmpty(getConnectedDevice())) {
                        isReconncted = false;
                        reconnectParamsBean = null;
                    }

                    if (isReconncted && getConnectedDevice().size() == 0) {
                        LogUtils.d(TAG, "reconnecting! will reconnect " + address);
                        if (reconnectParamsBean != null){
                            reconnectParamsBean.addNumber();
                            //重连必须在主线程运行
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connect(address);
                                }
                            });
                        }else {
                            LogUtils.w(TAG, "Fail to reconnect, ReconnectParams is null");
                        }
                    } else {
                        LogUtils.w(TAG, "Fail to reconnect, refuse! " + address + " flag:" + isReconncted);
                    }
                }else{
                    LogUtils.w(TAG, "Fail to reconnect, the bluetooth is disable!");
                }
            }
        }, reconnectParamsBean.getNextReconnectTime());
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

    private void updateConnectStateListener(String address, ConnectState state){
        if (connectStateListener != null){
            connectStateListener.onConnectStateChanged(address, state);
        }
    }
}
