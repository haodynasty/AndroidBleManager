package com.blakequ.bluetooth_manager_lib.connect;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.orhanobut.logger.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * bluetooth write and read queue oprator
 * Created by PLUSUB on 2015/11/16.
 */
public class BluetoothOperatorQueue {

    private String uuid;
    private final Queue<SimpleEntity> sWriteQueue = new ConcurrentLinkedQueue<SimpleEntity>();
    private boolean sIsWriting = false;
    private BluetoothGatt mBluetoothGatt;

    public BluetoothOperatorQueue(){
    }

    public synchronized void clean(){
        sIsWriting = false;
        uuid = null;
        sWriteQueue.clear();
    }

    /**
     * start operator to write or read BluetoothGatt Service
     * @param mBluetoothGatt
     * @exception IllegalArgumentException if mBluetoothGatt is null
     */
    public void start(BluetoothGatt mBluetoothGatt){
        if (mBluetoothGatt == null){
            throw new IllegalArgumentException("BluetoothGatt is null, can not write or read BluetoothGatt Service");
        }
        this.mBluetoothGatt = mBluetoothGatt;
        nextOperator();
    }

    /**
     * add write or read characteristic operator by order,In order execution
     * @param gattCharacteristic
     * @param isWrite true is write operator, false is read
     */
    public synchronized void addOperator(BluetoothGattCharacteristic gattCharacteristic, boolean isWrite) {
        SimpleEntity entity = new SimpleEntity(isWrite, gattCharacteristic);
        sWriteQueue.add(entity);
    }

    /**
     * add write or read descriptor operator by order,In order execution
     * @param gattDescriptor
     * @param isWrite true is write operator, false is read
     */
    public synchronized void addOperator(BluetoothGattDescriptor gattDescriptor, boolean isWrite) {
        SimpleEntity entity = new SimpleEntity(isWrite, gattDescriptor);
        sWriteQueue.add(entity);
    }

    /**
     * next operator, should invoke by hand
     */
    public synchronized void nextOperator() {
        sIsWriting = false;
        if (!sWriteQueue.isEmpty() && !sIsWriting) {
            doOperator(sWriteQueue.poll());
        }
    }

    /**
     * do operator of read or write
     * @param entity
     */
    private synchronized boolean doOperator(SimpleEntity entity) {
        if (mBluetoothGatt == null){
            Logger.e("do operator fail, bluetoothgatt is null");
            return false;
        }
        boolean result = true;
        if (entity.obj instanceof BluetoothGattCharacteristic) {
            sIsWriting = true;
            BluetoothGattCharacteristic character = (BluetoothGattCharacteristic) entity.obj;
            uuid = character.getUuid().toString();
            if (entity.isWrite){
                result = mBluetoothGatt.writeCharacteristic(character);
            }else{
//                test(character);
                result = mBluetoothGatt.readCharacteristic(character);
            }
        } else if (entity.obj instanceof BluetoothGattDescriptor) {
            sIsWriting = true;
            BluetoothGattDescriptor desc = (BluetoothGattDescriptor) entity.obj;
            uuid = desc.getUuid().toString();
            if (entity.isWrite){
                result = mBluetoothGatt.writeDescriptor(desc);
            }else {
                result = mBluetoothGatt.readDescriptor(desc);
            }
        } else {
            Logger.d("do operator next");
            nextOperator();
        }
        Logger.d("do operator result:"+result+" "+uuid);
        return result;
    }

//    private void test(BluetoothGattCharacteristic characteristic){
//        boolean result = (characteristic.getProperties() &
//                BluetoothGattCharacteristic.PROPERTY_READ) == 0;
//        System.out.println("result:"+result+ " "+characteristic.getProperties()+" "+BluetoothGattCharacteristic.PROPERTY_READ);
//    }

    private static class SimpleEntity {
        public boolean isWrite;
        public Object obj;
        public String info;

        public SimpleEntity(boolean isWrite, Object obj) {
            this.isWrite = isWrite;
            this.obj = obj;
        }

        public SimpleEntity(boolean isWrite, Object obj, String info) {
            this.isWrite = isWrite;
            this.obj = obj;
            this.info = info;
        }
    }
}
