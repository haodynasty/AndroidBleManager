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
import android.os.SystemClock;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.blakequ.bluetooth_manager_lib.util.BluetoothUtils;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
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
 * date     : 2016/8/18 11:29 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: 用于当前蓝牙的连接管理，负责连接的建立断开，使用时需要设置回调{@link #setBluetoothGattCallback(BluetoothGattCallback)},
 * 连接{@link #connect(String)},断开连接{@link #disconnect(String)}, 关闭连接{@link #close(String)}.
 * 注意：<br>
 * 1.在进行蓝牙断开和连接调用的时候，需要在主线程执行，否则在三星手机会出现许多异常错误或无法连接的情况<br>
 * 2.该连接管理只能连接一个设备，不支持同时连接多个设备<br>
 * 3.可以自定义断开后重连次数和重连的间隔时间<br>
 * 4.如果要订阅服务数据（read,write,notify）Service UUID {@link #setServiceUUID(String)}， 否则不能自动的进行通知和char和desc的读写操作（还需要{@link #addBluetoothSubscribeData(BluetoothSubScribeData)}）<br>
 * 5.单独订阅数据，需要调用{@link #cleanSubscribeData()}清除订阅历史列表, {@link #addBluetoothSubscribeData(BluetoothSubScribeData)}添加参数, {@link #startSubscribe(BluetoothGatt)}启动订阅，会自动回调{@link #setBluetoothGattCallback(BluetoothGattCallback)}订阅结果
 */
@TargetApi(18)
public final class BluetoothConnectManager extends BluetoothConnectInterface{
    private static final String TAG = "BluetoothConnectManager";

    private static BluetoothConnectManager INSTANCE = null;
    private final BluetoothUtils mBluetoothUtils;
    private BluetoothGattCallback mBluetoothGattCallback;
    private BluetoothManager bluetoothManager;
    private final Map<String, BluetoothGatt> gattMap; //保存连接过的gatt
    private final Queue<BluetoothSubScribeData> subscribeQueue;
    private static String serviceUUID;
    private ReconnectParamsBean reconnectParamsBean;
    private List<ConnectStateListener> connectStateListeners;
    private ConnectState currentState = ConnectState.NORMAL;
    private static Object obj = new Object();

    public BluetoothConnectManager(Context context) {
        super(context);
        subscribeQueue = new ConcurrentLinkedQueue<BluetoothSubScribeData>();
        mBluetoothUtils = BluetoothUtils.getInstance(context);
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        gattMap = new ConcurrentHashMap<String, BluetoothGatt>(); //会有并发的断开和连接，故而必须使用并发ConcurrentHashMap才行，否则会有ConcurrentModificationException
        connectStateListeners = new ArrayList<>();
        BleManager.getBleParamsOptions();
    }

    @Override
    public void release() {
        closeAll();
        gattMap.clear();
        reconnectParamsBean = null;
    }


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


    /**
     * add callback of gatt connect, notice:<br>
     * 1. can not do any task which need a lot of time<br>
     * 2. you should update UI in the main thread, in callback method use {@link #runOnUiThread(Runnable)}
     * @param callback
     * @see #runOnUiThread(Runnable)
     */
    public void setBluetoothGattCallback(BluetoothGattCallback callback){
        this.mBluetoothGattCallback = callback;
    }

    /**
     * add listener of connect state
     * @param listener
     * @see #removeConnectStateListener(ConnectStateListener)
     */
    public void addConnectStateListener(ConnectStateListener listener){
        synchronized(connectStateListeners){
            connectStateListeners.add(listener);
        }
    }

    /**
     * remove listener
     * @param listener
     * @see #addBluetoothSubscribeData(BluetoothSubScribeData)
     */
    public void removeConnectStateListener(ConnectStateListener listener){
        synchronized(connectStateListeners){
            connectStateListeners.remove(listener);
        }
    }

    /**
     * add subscribe data while auto read or write characteristic(or descriptor) after discover service, you can clean subscribe list by {@link #cleanSubscribeData()}
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
            Logger.e( "Disconnected from GATT server address:"+gatt.getDevice().getAddress());
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
            Logger.e( "Disconnected from GATT server address:"+gatt.getDevice().getAddress());
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

    public void updateReconnectParams(){
        reconnectParamsBean = null;
    }

    @Override
    protected void onDiscoverServicesSuccess(BluetoothGatt gatt) {
    }

    @Override
    protected Queue<BluetoothSubScribeData> getSubscribeDataQueue() {
        return subscribeQueue;
    }

    /**
     * has device is connected or connecting
     * @return
     */
    public boolean isConnectDevice(){
        if (gattMap.size() == 0) return false;
        return true;
    }

    /**
     * 获取已经连接的设备,注意：返回的设备不一定全部是当前APP所连接的设备，需要通过UUID或设备名字等区分
     * @return
     */
    public List<BluetoothDevice> getConnectedDevice(){
        if (isConnectDevice()){
            return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        }
        return Collections.EMPTY_LIST;
    }

    public ConnectState getCurrentState(){
        return currentState;
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
            Logger.w( "BluetoothAdapter not initialized or unspecified address.");
            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        if (!mBluetoothUtils.isBluetoothIsEnable()){
            Logger.e( "bluetooth is not enable.");
            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (gattMap.containsKey(address)){
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            Logger.i( "Trying to use an existing gatt and reconnection device " + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
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
            BluetoothGatt mBluetoothGatt = device.connectGatt(context, false, gattCallback);
            if (mBluetoothGatt != null){
                Logger.d( "create a new connection address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
                gattMap.put(address, mBluetoothGatt);
                closeOtherDevice(address);
                updateConnectStateListener(address, ConnectState.CONNECTING);
                return true;
            }else{
                Logger.e( "Get Gatt fail!, address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            }
        }else{
            Logger.e( "Device not found, address=" + address);
        }
        updateConnectStateListener(address, ConnectState.NORMAL);
        return false;
    }


    /**
     * close bluetooth, release resource
     * @param address
     */
    public boolean close(String address) {
        if (!isEmpty(address) && gattMap.containsKey(address)){
            Logger.w("close gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.close();
            gattMap.remove(address);
            updateConnectStateListener(address, ConnectState.NORMAL);
            return true;
        }
        return false;
    }

    /**
     * close all bluetooth connect,  release all resource
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
            reconnectParamsBean = new ReconnectParamsBean(address);
            reconnectParamsBean.setNumber(1000);
            Logger.w("disconnect gatt server " + address);
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
                if (reconnectParamsBean.getNumber() == 0){//same device
                    reconnectParamsBean.updateAddress(address);
                }else if(reconnectParamsBean.getNumber() == 1000){//disconnect by hand
                    Logger.i("reconnect fail! disconnect by hand");
                    reconnectParamsBean.setNumber(0);
                    return;
                }
            }
            reconnectParamsBean.addNumber();
        }else{
            reconnectParamsBean = new ReconnectParamsBean(address);
        }

        //计算下一次重连的时间
        long nextReconnectTime = reconnectParamsBean.getNextReconnectTime() - SystemClock.elapsedRealtime();
        if (nextReconnectTime < 0){
            nextReconnectTime = 0;
        }
        Logger.i("next reconnect time " + reconnectParamsBean.toString()+" after:"+nextReconnectTime/1000+"seconds");

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
                        Logger.d("reconnecting! will reconnect " + address);
                        if (reconnectParamsBean != null){
                            //重连必须在主线程运行
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connect(address);
                                }
                            });
                        }else {
                            Logger.w("Fail to reconnect, ReconnectParams is null");
                        }
                    } else {
                        Logger.w("Fail to reconnect, refuse! " + address + " flag:" + isReconncted);
                    }
                }else{
                    Logger.w("Fail to reconnect, the bluetooth is disable!");
                }
            }
        }, nextReconnectTime);
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
        synchronized (connectStateListeners){
            currentState = state;
            if (state == ConnectState.CONNECTING){
                //start check time out connect
                BleParamsOptions options = BleManager.getBleParamsOptions();
                getMainLooperHandler().postDelayed(timeOutTask, options.getConnectTimeOutTimes());
            }
            for (ConnectStateListener listener:connectStateListeners){
                if (listener != null) listener.onConnectStateChanged(address, state);
            }
        }
    }

    /**
     * connect time out task
     */
    private Runnable timeOutTask = new Runnable() {
        @Override
        public void run() {
            if (!mBluetoothUtils.isBluetoothIsEnable()){
                Logger.w("Fail to connect device! Bluetooth is not enable!");
                closeAll();
            }
        }
    };
}
