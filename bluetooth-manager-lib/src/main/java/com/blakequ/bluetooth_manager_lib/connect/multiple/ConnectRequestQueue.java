package com.blakequ.bluetooth_manager_lib.connect.multiple;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.support.v4.util.ArrayMap;

import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectInterface;
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
 * date     : 2016/8/19 9:48 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:the queue using for manager connect request
 */
public abstract class ConnectRequestQueue extends BluetoothConnectInterface{
    protected int queueLen;
    protected Map<String, Integer> reconnectMap; //reconnect device list and reconnect times number
    protected Map<String, ConnectState> macMap;//<mac address, is connected>
    protected Map<String, BluetoothGatt> gattMap;//notice:ArrayMap is not support concurrent, so can not use ArrayMap

    public ConnectRequestQueue(Context context, int maxLen){
        super(context);
        this.queueLen = maxLen;
        macMap = new ConcurrentHashMap<String, ConnectState>();//if not consider concurrent, should use ArrayMap
        gattMap = new ConcurrentHashMap<String, BluetoothGatt>();
        reconnectMap = new ArrayMap<String, Integer>();
    }

    @Override
    protected void onDeviceConnected(BluetoothGatt gatt) {
        if (gatt != null){
            updateConnectState(gatt.getDevice().getAddress(), ConnectState.CONNECTED);
        }
    }

    @Override
    protected void onDeviceDisconnect(BluetoothGatt gatt, int errorState) {
        if (gatt != null){
            updateConnectState(gatt.getDevice().getAddress(), ConnectState.NORMAL);
            triggerReconnect();
        }
    }

    @Override
    protected void onDiscoverServicesFail(BluetoothGatt gatt) {
        if (gatt != null){
            updateConnectState(gatt.getDevice().getAddress(), ConnectState.NORMAL);
            triggerReconnect();
        }
    }

    /**
     * trigger reconnect task
     */
    protected void triggerReconnect(){

    }

    /**
     * connect bluetooth device one by one
     * @return the next connect device
     */
    protected String nextConnectDevice(){
        //find not connect device
        return null;
    }

    protected void updateConnectState(String address, ConnectState state) {
        macMap.put(address, state);
    }

    /**
     * release resource
     */
    protected void release(){
        macMap.clear();
        closeAll();
        gattMap.clear();
        reconnectMap.clear();
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
        if (macMap.size() < queueLen){
            if (!macMap.containsKey(macAddress)){
                macMap.put(macAddress, ConnectState.NORMAL);
            }
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
        macMap.remove(macAddress);
        if (gattMap.containsKey(macAddress)){
            close(macAddress);
        }
    }

    public List<String> getAllDevice(){
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
        List<String> list = new ArrayList<>();
        for (String key:macMap.keySet()){
            if (macMap.get(key) == ConnectState.CONNECTED){
                list.add(key);
            }
        }
        return list;
    }

    protected List<String> getAllConnectingDevice(){
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
     * 关闭蓝牙连接,会释放BluetoothGatt持有的所有资源
     * @param address
     */
    @Override
    public boolean close(String address) {
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
    @Override
    public void disconnect(String address){
        if (!isEmpty(address) && gattMap.containsKey(address)){
            LogUtils.w("ConnectRequestQueue", "disconnect gatt server " + address);
            BluetoothGatt mBluetoothGatt = gattMap.get(address);
            mBluetoothGatt.disconnect();
            updateConnectState(address, ConnectState.NORMAL);
        }
    }

    public boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
