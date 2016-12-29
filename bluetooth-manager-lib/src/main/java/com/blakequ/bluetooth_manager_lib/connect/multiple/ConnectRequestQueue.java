package com.blakequ.bluetooth_manager_lib.connect.multiple;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectInterface;
import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.connect.ReconnectParamsBean;
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
 * date     : 2016/8/19 9:48 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:the queue using for manager connect request
 */
public abstract class ConnectRequestQueue extends BluetoothConnectInterface{
    private static final String TAG = "ConnectRequestQueue";
    private Map<String, ReconnectParamsBean> reconnectMap; //reconnect device list and reconnect times number
    private Map<String, ConnectState> macMap;//<mac address, is connected>
    private Map<String, BluetoothGatt> gattMap;//notice:ArrayMap is not support concurrent, so can not use ArrayMap
    private Queue<String> deviceQueue;
    private final BluetoothUtils mBluetoothUtils;
    private List<ConnectStateListener> connectStateListeners;

    public ConnectRequestQueue(Context context){
        super(context);
        macMap = new ConcurrentHashMap<String, ConnectState>();//if not consider concurrent, should use ArrayMap
        gattMap = new ConcurrentHashMap<String, BluetoothGatt>();
        reconnectMap = new ConcurrentHashMap<String, ReconnectParamsBean>();
        deviceQueue = new ConcurrentLinkedQueue<>();
        mBluetoothUtils = BluetoothUtils.getInstance(context);
        connectStateListeners = new ArrayList<>();
    }

    public void addConnectStateListener(ConnectStateListener listener){
        synchronized (connectStateListeners){
            connectStateListeners.add(listener);
        }
    }

    public void removeConnectStateListener(ConnectStateListener listener){
        synchronized (connectStateListeners){
            connectStateListeners.remove(listener);
        }
    }

    @Override
    protected void onDeviceConnected(BluetoothGatt gatt) {
        if (gatt != null){
            updateConnectState(gatt.getDevice().getAddress(), ConnectState.CONNECTED);
        }
    }

    @Override
    protected void onDeviceDisconnect(BluetoothGatt gatt, int errorState) {
        Logger.e( "Disconnected from GATT server address:" + gatt.getDevice().getAddress());
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
    public void startConnect(){
        if (deviceQueue.size() > 0 && mBluetoothUtils.isBluetoothIsEnable()){
            triggerConnectNextDevice();
        }else {
            triggerReconnect("");
            Logger.e( "startConnect--Fail to from connect queue, and start reconnect task. ble state:" + mBluetoothUtils.isBluetoothIsEnable());
        }
    }

    /**
     * start connect device(will trigger reconnect)
     * @param macAddress
     */
    public void startConnect(String macAddress){
        if (macAddress != null && macAddress.length() > 0){
            if (macMap.containsKey(macAddress)){
                ConnectState state = macMap.get(macAddress);
                //如果是未连接状态，则开启重连，重置重连次数，并立即连接
                if (macMap.get(macAddress) == ConnectState.NORMAL){
                    ReconnectParamsBean bean;
                    if (!reconnectMap.containsKey(macAddress)){
                        bean = new ReconnectParamsBean(macAddress);
                        reconnectMap.put(macAddress, bean);
                    }else{
                        bean = reconnectMap.get(macAddress);
                    }
                    bean.setReconnectNow(true);
                    startReconnectTask();
                }else{
                    Logger.i( "Device is " + state + " state");
                }
            }else{
                Logger.e( "Fail to connect device, device can not found in queue, you must invoke addDeviceToQueue(Stirng)");
            }
        }else{
            Logger.e( "Fail to connect device, mac address is null");
        }
    }

    /**
     * connect bluetooth device one by one
     * @return the next connect device
     */
    private void triggerConnectNextDevice(){
        String mac = deviceQueue.peek();
        if (!isEmpty(mac)){
            Logger.i( "Start trigger connect device "+mac);
            connect(mac);
        }
    }

    private void updateConnectState(String address, ConnectState state) {
        //bug:Can not remove device from queue, this position just update connect state
        if (macMap.containsKey(address)) {
            macMap.put(address, state);
            updateConnectStateListener(address, state);
        }
        switch (state){
            case NORMAL: //disconnect or close
                String mac = deviceQueue.peek();
                if (!isEmpty(mac)){
                    if (address.equals(mac)){
                        deviceQueue.poll();
                    }
                    triggerConnectNextDevice();
                }
                triggerReconnect(address);
                break;
            case CONNECTED:
                reconnectMap.remove(address);
                String mac1 = deviceQueue.peek();
                if (!isEmpty(mac1)){
                    if (address.equals(mac1)){
                        deviceQueue.poll();
                    }
                    triggerConnectNextDevice();
                }
                triggerReconnect(address);
                break;
            case CONNECTING:
                //start check time out connect
                BleParamsOptions options = BleManager.getBleParamsOptions();
                getMainLooperHandler().postDelayed(timeOutTask, options.getConnectTimeOutTimes());
                break;
        }
    }

    /**
     * connect time out task
     */
    private Runnable timeOutTask = new Runnable() {
        @Override
        public void run() {
            if (!mBluetoothUtils.isBluetoothIsEnable()){
                Logger.w( "Fail to connect device! Bluetooth is not enable!");
                closeAll();
            }
        }
    };

    /**
     * release resource
     */
    @Override
    public void release(){
        macMap.clear();
        closeAll();
        gattMap.clear();
        reconnectMap.clear();
        deviceQueue.clear();
        getMainLooperHandler().removeCallbacks(reconnectTask);
    }

    /**
     * get the size of current queue
     * <p>Notice:this len maybe is not equal of maxLen(connect device num<=maxLen), is dynamic length by sensor physical truth</>
     * @see #getMaxLen()
     */
    public int getQueueSize(){
        return macMap.size();
    }

    /**
     * add device to connect queue, if the number out of range will discard.
     * @param macAddress
     * @see #startConnect()
     */
    public void addDeviceToQueue(String macAddress){
        if (!macMap.containsKey(macAddress)){
            if (macMap.size() >= getMaxLen()){
                String address = deviceQueue.poll();
                if (isEmpty(address)){
                    address = getFirstDevice();
                }
                removeDeviceFromQueue(address);
            }
            deviceQueue.add(macAddress);
            macMap.put(macAddress, ConnectState.NORMAL);
        }
    }

    /**
     * add device to connect queue
     * @param devices
     * @see #startConnect()
     */
    public void addDeviceToQueue(String[] devices){
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
    public void removeDeviceFromQueue(String macAddress){
        if (isEmpty(macAddress)) return;
        macMap.remove(macAddress);
        deviceQueue.remove(macAddress);
        reconnectMap.remove(macAddress);
        if (gattMap.containsKey(macAddress)){
            close(macAddress);
        }
    }

    public List<String> getAllDevice(){
        if (macMap.size() <= 0) return Collections.EMPTY_LIST;
        List<String> list = new ArrayList<>();
        for (String key:macMap.keySet()){
            list.add(key);
        }
        return list;
    }

    private String getFirstDevice(){
        if (macMap.size() <= 0) return null;
        for (String key:macMap.keySet()){
            return key;
        }
        return null;
    }

    /**
     * get all connected device
     * @return
     */
    public List<String> getAllConnectedDevice(){
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
     * is contain device
     * @param address
     * @return
     */
    public boolean containsDevice(String address){
        return macMap.containsKey(address);
    }


    /**
     * get bluetooth state of connect
     * @param address
     * @return
     */
    public ConnectState getDeviceState(String address){
        return macMap.get(address);
    }

    public List<String> getAllConnectingDevice(){
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
    public boolean isDisconnectDevice(){
        for (ConnectState value:macMap.values()) {
            if (value == ConnectState.NORMAL){
                return true;
            }
        }
        return false;
    }

    /**
     * is have device is connecting
     * @return
     */
    public boolean isConnectingDevice(){
        for (ConnectState value:macMap.values()){
            if (value == ConnectState.CONNECTING){
                return true;
            }
        }
        return false;
    }

    /**
     * is have device is connected
     * @return
     */
    public boolean isConnectedDevice(){
        for (ConnectState value:macMap.values()){
            if (value == ConnectState.CONNECTED){
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
    private void triggerReconnect(String mac){
        //if deviceQueue is null, start reconnect
        if (deviceQueue.size() == 0){
            //将重连的设备全部放入重连队列
            for (String key:macMap.keySet()){
                if (macMap.get(key) == ConnectState.NORMAL){
                    ReconnectParamsBean bean;
                    if (!reconnectMap.containsKey(key)){
                        bean = new ReconnectParamsBean(key);
                        reconnectMap.put(key, bean);
                    }else if(key.equals(mac)){
                        bean = reconnectMap.get(key);
                        bean.addNumber();
                        Logger.d( "trigger reconnect, reconnect after "+(bean.getNextReconnectTime() - SystemClock.elapsedRealtime())/1000+" seconds");
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
        long nextTime = SystemClock.elapsedRealtime()*2;
        String address = "";
        //select minimum time of list
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
                Logger.d( "start reconnect device:"+address);
                reconnectDevice(address);
            }else{
                Logger.d( "start reconnect device "+address+" after "+(nextTime - SystemClock.elapsedRealtime())/1000+" seconds");
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
            Logger.d( "Start reconnect task by handler");
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
                }

                //check is connected or connectting
                ConnectState state = macMap.get(address);
                if (state == ConnectState.NORMAL){
                    Logger.d( "Start reconnect device "+address+" reconnect number is "+bean.getNumber());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connect(address);
                        }
                    });
                }else{
                    Logger.w( "Fail to reconnect device! "+address+" state is "+state);
                }
            }else {
                closeAll();
                Logger.w( "Fail to reconnect device! Bluetooth is not enable!");
            }
        }else{
            Logger.w("Fail to reconnect device! "+address+" is remove from reconnectMap");
            reconnectMap.remove(address);
        }
    }


    /**
     * You should invoke {@link #startConnect()} to begin to connect device. Not recommended for direct use this method
     * @see #startConnect()
     * @param address
     * @return
     */
    protected boolean connect(final String address) {
        BluetoothAdapter mAdapter = mBluetoothUtils.getBluetoothAdapter();
        if (mAdapter == null || address == null) {
            Logger.e("BluetoothAdapter not initialized or unspecified address "+address);
            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        if (!mBluetoothUtils.isBluetoothIsEnable()){
            Logger.e("bluetooth is not enable.");
            closeAll();
//            updateConnectStateListener(address, ConnectState.NORMAL);
            return false;
        }

        if (isEmpty(getServiceUUID())){
            Logger.w("Service uuid is null");
        }

        // Previously connected device.  Try to reconnect.
        if (gattMap.containsKey(address)){
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            Logger.i("Trying to use an existing gatt and reconnection device " + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
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
                Logger.i("create a new connection address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
                gattMap.put(address, mBluetoothGatt);
                updateConnectState(address, ConnectState.CONNECTING);
                return true;
            } else {
                Logger.e("Get Gatt fail!, address=" + address + " thread:" + (Thread.currentThread() == Looper.getMainLooper().getThread()));
            }
        } else {
            Logger.e("Device not found, address=" + address);
        }
        return false;
    }

    /**
     * 关闭蓝牙连接,会释放BluetoothGatt持有的所有资源
     * @param address
     */
    public boolean close(String address) {
        if (!isEmpty(address) && gattMap.containsKey(address)){
            Logger.w( "close gatt server " + address);
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
            Logger.w("disconnect gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.disconnect();
            updateConnectState(address, ConnectState.NORMAL);
        }
    }

    private void updateConnectStateListener(String address, ConnectState state){
        synchronized (connectStateListeners){
            for (ConnectStateListener listener:connectStateListeners){
                if (listener != null) listener.onConnectStateChanged(address, state);
            }
        }
    }

    /**
     * max connected number of bluetooth queue
     * @return
     */
    public int getMaxLen(){
        return ConnectConfig.maxConnectDeviceNum;
    }

    public boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
