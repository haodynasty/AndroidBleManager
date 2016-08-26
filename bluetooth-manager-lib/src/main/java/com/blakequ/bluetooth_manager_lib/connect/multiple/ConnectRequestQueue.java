package com.blakequ.bluetooth_manager_lib.connect.multiple;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectInterface;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.connect.ReconnectParamsBean;
import com.blakequ.bluetooth_manager_lib.util.BluetoothUtils;
import com.blakequ.bluetooth_manager_lib.util.LogUtils;

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
 * date     : 2016/8/19 9:48 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:the queue using for manager connect request
 */
public abstract class ConnectRequestQueue extends BluetoothConnectInterface{
    private int queueLen;
    private Map<String, ReconnectParamsBean> reconnectMap; //reconnect device list and reconnect times number
    private Map<String, ConnectState> macMap;//<mac address, is connected>
    private Map<String, BluetoothGatt> gattMap;//notice:ArrayMap is not support concurrent, so can not use ArrayMap
    private Queue<String> deviceQueue;
    private final BluetoothUtils mBluetoothUtils;
    private ConnectStateListener connectStateListener;

    public ConnectRequestQueue(Context context, int maxLen){
        super(context);
        this.queueLen = maxLen;
        macMap = new ConcurrentHashMap<String, ConnectState>();//if not consider concurrent, should use ArrayMap
        gattMap = new ConcurrentHashMap<String, BluetoothGatt>();
        reconnectMap = new ConcurrentHashMap<String, ReconnectParamsBean>();
        deviceQueue = new ConcurrentLinkedQueue<>();
        mBluetoothUtils = BluetoothUtils.getInstance(context);
    }

    public void setConnectStateListener(ConnectStateListener listener){
        this.connectStateListener = listener;
    }

    @Override
    protected void onDeviceConnected(BluetoothGatt gatt) {
        if (gatt != null){
            updateConnectState(gatt.getDevice().getAddress(), ConnectState.CONNECTED);
        }
    }

    @Override
    protected void onDeviceDisconnect(BluetoothGatt gatt, int errorState) {
        LogUtils.e(TAG, "Disconnected from GATT server address:" + gatt.getDevice().getAddress());
        //可以不关闭，以便重用，因为在连接connect的时候可以快速连接
        if (!mBluetoothUtils.isBluetoothIsEnable()){
            //关闭所有的设备
            closeAll();
        }else {
            close(gatt.getDevice().getAddress());//防止出现status 133
        }
    }

    @Override
    protected void onDiscoverServicesFail(BluetoothGatt gatt) {
        if (gatt != null){
            updateConnectState(gatt.getDevice().getAddress(), ConnectState.NORMAL);
        }
    }

    @Override
    protected void onDiscoverServicesSuccess(BluetoothGatt gatt){
        if (gatt != null){
            updateConnectState(gatt.getDevice().getAddress(), ConnectState.CONNECTED);
        }
    }

    /**
     * start connect device one by one
     */
    protected void startConnect(){
        if (deviceQueue.size() > 0 && mBluetoothUtils.isBluetoothIsEnable()){
            triggerConnectNextDevice();
        }else {
            LogUtils.e(TAG, "Fail to start connect task! connect queue size " + deviceQueue.size() + " ble state:" + mBluetoothUtils.isBluetoothIsEnable());
        }
    }

    /**
     * connect bluetooth device one by one
     * @return the next connect device
     */
    private void triggerConnectNextDevice(){
        String mac = deviceQueue.peek();
        if (!isEmpty(mac)){
            LogUtils.i(TAG, "Start trigger connect device "+mac);
            connect(mac);
        }
    }

    private void updateConnectState(String address, ConnectState state) {
        macMap.put(address, state);
        updateConnectStateListener(address, state);
        switch (state){
            case NORMAL: //disconnect or close
                String mac = deviceQueue.peek();
                if (!isEmpty(mac) && address.equals(mac)){
                    deviceQueue.poll();
                    triggerConnectNextDevice();
                }
                triggerReconnect();
                break;
            case CONNECTED:
                reconnectMap.remove(address);
                String mac1 = deviceQueue.peek();
                if (!isEmpty(mac1) && address.equals(mac1)){
                    deviceQueue.poll();
                    triggerConnectNextDevice();
                }
                triggerReconnect();
                break;
            case CONNECTING:
                break;
        }
    }

    /**
     * release resource
     */
    protected void release(){
        macMap.clear();
        closeAll();
        gattMap.clear();
        reconnectMap.clear();
        deviceQueue.clear();
    }

    /**
     * get the length of queue
     * <p>Notice:this len maybe is not equal of maxLen(connect device num<=maxLen), is dynamic length by sensor physical truth</>
     */
    protected int getQueueSize(){
        return macMap.size();
    }

    /**
     * add device to connect queue, if the number out of range will discard.
     * @param macAddress
     */
    protected void addDeviceToQueue(String macAddress){
        if (!macMap.containsKey(macAddress)){
            if (macMap.size() >= queueLen){
                String address = deviceQueue.poll();
                if (isEmpty(address)){
                    address = getAllDevice().get(0);
                }
                removeDeviceFromQueue(address);
            }
            deviceQueue.add(macAddress);
            macMap.put(macAddress, ConnectState.NORMAL);
        }
    }

    protected void addDeviceToQueue(String[] devices){
        if (devices != null && devices.length > 0){
            for (int i=0; i<devices.length; i++){
                addDeviceToQueue(devices[i]);
            }
        }
    }

    /**
     * remove device from queue, disconnected device if in connection state
     * @param macAddress
     */
    protected void removeDeviceFromQueue(String macAddress){
        if (isEmpty(macAddress)) return;
        macMap.remove(macAddress);
        reconnectMap.remove(macAddress);
        if (gattMap.containsKey(macAddress)){
            close(macAddress);
        }
    }

    protected List<String> getAllDevice(){
        if (macMap.size() <= 0) return Collections.EMPTY_LIST;
        List<String> list = new ArrayList<>();
        for (String key:macMap.keySet()){
            list.add(key);
        }
        return list;
    }

    /**
     * get all connected device
     * @return
     */
    protected List<String> getAllConnectedDevice(){
        if (macMap.size() <= 0) return Collections.EMPTY_LIST;
        List<String> list = new ArrayList<>();
        for (String key:macMap.keySet()){
            if (macMap.get(key) == ConnectState.CONNECTED){
                list.add(key);
            }
        }
        return list;
    }

    /**
     * check device is connected state
     * @param address
     * @return
     */
    protected boolean isExistConnectedDevice(String address){
        if (macMap.containsKey(address)){
            return macMap.get(address) == ConnectState.CONNECTED;
        }
        return false;
    }

    /**
     * get bluetooth state of connect
     * @param address
     * @return
     */
    protected ConnectState getDeviceState(String address){
        return macMap.get(address);
    }

    protected List<String> getAllConnectingDevice(){
        if (macMap.size() <= 0) return Collections.EMPTY_LIST;
        List<String> list = new ArrayList<>();
        for (String key:macMap.keySet()){
            if (macMap.get(key) == ConnectState.CONNECTING){
                list.add(key);
            }
        }
        return list;
    }

    /**
     * has device is not connected
     * @return
     */
    protected boolean isDisconnectDevice(){
        for (ConnectState value:macMap.values()){
            if (value == ConnectState.NORMAL){
                return true;
            }
        }
        return false;
    }

    protected boolean isConnectingDevice(){
        for (ConnectState value:macMap.values()){
            if (value == ConnectState.CONNECTING){
                return true;
            }
        }
        return false;
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
     * trigger reconnect task
     */
    private void triggerReconnect(){
        //if deviceQueue is null, start reconnect
        if (deviceQueue.size() == 0){
            //将重连的设备全部放入重连队列
            for (String key:macMap.keySet()){
                if (macMap.get(key) == ConnectState.NORMAL){
                    ReconnectParamsBean bean;
                    if (!reconnectMap.containsKey(key)){
                        bean = new ReconnectParamsBean(key);
                        reconnectMap.put(key, bean);
                    }
                }
            }

            startReconnectTask();
        }
    }

    /**
     * can not reconnect all the time
     */
    private synchronized void startReconnectTask(){
        if (reconnectMap.size() <= 0) return;
        long nextTime = SystemClock.elapsedRealtime();
        String address = "";
        for (String addr:reconnectMap.keySet()){
            ReconnectParamsBean bean = reconnectMap.get(addr);
            if (bean.getNextReconnectTime() < nextTime){
                nextTime = bean.getNextReconnectTime();
                address = addr;
            }
        }

        //start reconnect task
        if (!isEmpty(address)){
            if (nextTime <= SystemClock.elapsedRealtime()){
                reconnectDevice(address);
            }else{
                getMainLooperHandler().removeCallbacks(reconnectTask);
                getMainLooperHandler().postDelayed(reconnectTask, nextTime - SystemClock.elapsedRealtime());
            }
        }
    }

    /**
     * reconnect runnable
     */
    private Runnable reconnectTask = new Runnable() {
        @Override
        public void run() {
            LogUtils.d(TAG, "Start reconnect task by handler");
            startReconnectTask();
        }
    };

    /**
     * reconnect device
     * @param address
     */
    private synchronized void reconnectDevice(final String address){
        if (macMap.containsKey(address)){
            ReconnectParamsBean bean = reconnectMap.get(address);
            if (mBluetoothUtils.isBluetoothIsEnable()) {
                if (bean == null){
                    reconnectMap.put(address, new ReconnectParamsBean(address));
                }else{
                    bean.addNumber();
                }

                //check is connected or connectting
                ConnectState state = macMap.get(address);
                if (state == ConnectState.NORMAL){
                    LogUtils.d(TAG, "Start reconnect device "+address+" reconnect number is "+bean.getNumber());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connect(address);
                        }
                    });
                }else{
                    LogUtils.w(TAG, "Fail to reconnect device! "+address+" state is "+state);
                }
            }else {
                LogUtils.w(TAG, "Fail to reconnect device! Bluetooth is not enable!");
            }
        }else{
            LogUtils.w(TAG, "Fail to reconnect device! "+address+" is remove from reconnectMap");
            reconnectMap.remove(address);
        }
    }


    protected boolean connect(final String address) {
        BluetoothAdapter mAdapter = mBluetoothUtils.getBluetoothAdapter();
        if (mAdapter == null || address == null) {
            LogUtils.e(TAG, "BluetoothAdapter not initialized or unspecified address "+address);
            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        if (!mBluetoothUtils.isBluetoothIsEnable()){
            LogUtils.e(TAG, "bluetooth is not enable.");
            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        if (isEmpty(getServiceUUID())){
            throw new IllegalArgumentException("Service uuid is null, you must invoke setServiceUUID(String) method to set service uuid");
        }

        // Previously connected device.  Try to reconnect.
        if (gattMap.containsKey(address)){
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            LogUtils.i(TAG, "Trying to use an existing gatt and reconnection device " + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            if (mBluetoothGatt.connect()) {
                updateConnectState(address, ConnectState.CONNECTING);
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
                LogUtils.i(TAG, "create a new connection address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
                gattMap.put(address, mBluetoothGatt);
                updateConnectState(address, ConnectState.CONNECTING);
                return true;
            }else{
                LogUtils.e(TAG, "Get Gatt fail!, address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            }
        }else{
            LogUtils.e(TAG, "Device not found, address=" + address);
        }
        return false;
    }

    /**
     * 关闭蓝牙连接,会释放BluetoothGatt持有的所有资源
     * @param address
     */
    protected boolean close(String address) {
        if (!isEmpty(address) && gattMap.containsKey(address)){
            LogUtils.w("ConnectRequestQueue", "close gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.close();
            gattMap.remove(address);
            updateConnectState(address, ConnectState.NORMAL);
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
            LogUtils.w("ConnectRequestQueue", "disconnect gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.disconnect();
            updateConnectState(address, ConnectState.NORMAL);
        }
    }

    private void updateConnectStateListener(String address, ConnectState state){
        if (connectStateListener != null){
            connectStateListener.onConnectStateChanged(address, state);
        }
    }

    protected void setQueueLen(int queueLen){
        this.queueLen = queueLen;
    }

    public boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
