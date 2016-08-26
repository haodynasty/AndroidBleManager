/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blakequ.androidblemanager.ui.scan;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.event.UpdateEvent;
import com.blakequ.androidblemanager.ui.ToolbarActivity;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectManager;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.connect.GattError;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;
import com.blakequ.bluetooth_manager_lib.device.resolvers.GattAttributeResolver;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends ToolbarActivity {
    public static final String EXTRA_DEVICE = "extra_device";
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private static final String LIST_NAME = "NAME";
    private static final String LIST_UUID = "UUID";
    @Bind(R.id.gatt_services_list)
    protected ExpandableListView mGattServicesList;
    @Bind(R.id.connection_state)
    protected TextView mConnectionState;
    @Bind(R.id.uuid)
    protected TextView mGattUUID;
    @Bind(R.id.description)
    protected TextView mGattUUIDDesc;
    @Bind(R.id.data_as_string)
    protected TextView mDataAsString;
    @Bind(R.id.data_as_array)
    protected TextView mDataAsArray;
    private BluetoothConnectManager connectManager;
    private int connectState = 0;
    private List<List<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private String mDeviceAddress;
    private String mDeviceName;
    private boolean mConnected = false;
    private String mExportString;
    private BluetoothLeDevice device;

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition, final int childPosition, final long id) {
            if (mGattCharacteristics != null) {
                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                Intent intent = new Intent(DeviceControlActivity.this, CharacteristicDetailActivity.class);
                intent.putExtra(CharacteristicDetailActivity.EXTRA_DEVICE, device);
                intent.putExtra(CharacteristicDetailActivity.EXTRA_UUID, characteristic.getUuid().toString());
                startActivity(intent);
                return true;
            }
            return false;
        }
    };

    private ConnectStateListener stateListener = new ConnectStateListener() {
        @Override
        public void onConnectStateChanged(String address, ConnectState state) {
            switch (state) {
                case CONNECTED:
                    connectState = 1;
                    mConnected = true;
                    updateConnectionState(R.string.connected);
                    break;
                case CONNECTING:
                    mConnected = false;
                    break;
                case NORMAL:
                    connectState = 2;
                    mConnected = false;
                    updateConnectionState(R.string.disconnected);
                    break;
            }
            invalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Device Control");
        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        mDeviceName = device.getName();
        mDeviceAddress = device.getAddress();
        ButterKnife.bind(this);
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);

        connectManager = BluetoothConnectManager.getInstance(this);
        connectManager.addConnectStateListener(stateListener);
        connectManager.setBluetoothGattCallback(new BluetoothGattCallback() {

            @Override
            public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.BLE_DATA, characteristic, "read"));
                }else{
                    EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.BLE_DATA, characteristic, "fail"));
                    Log.e(TAG, "fail to read characteristic");
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.BLE_DATA, characteristic, "write"));
                }else{
                    EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.BLE_DATA, characteristic, "fail"));
                    Log.e(TAG, "fail to write characteristic");
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.BLE_DATA, characteristic, "notify"));
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, final int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DeviceControlActivity.this, "Disconnect! error："+ GattError.parseConnectionError(status), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayGattServices(gatt.getServices());
                        }
                    });
                }
            }
        });
        connectManager.connect(mDeviceAddress);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            if (connectState != 2){
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
            }
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }

        if (mExportString == null) {
            menu.findItem(R.id.menu_share).setVisible(false);
        } else {
            menu.findItem(R.id.menu_share).setVisible(true);
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectManager.closeAll();
        connectManager.removeConnectStateListener(stateListener);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                connectManager.connect(mDeviceAddress);
                connectState = 0;
                invalidateOptionsMenu();
                return true;
            case R.id.menu_disconnect:
                connectManager.disconnect(mDeviceAddress);
                return true;
            case R.id.menu_share:
                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                final String subject = getString(R.string.exporter_email_device_services_subject, mDeviceName, mDeviceAddress);

                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(android.content.Intent.EXTRA_TEXT, mExportString);

                startActivity(Intent.createChooser(
                        intent,
                        getString(R.string.exporter_email_device_list_picker_text)));

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public int provideContentViewId() {
        return R.layout.activity_gatt_services;
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mGattUUID.setText(R.string.no_data);
        mGattUUIDDesc.setText(R.string.no_data);
        mDataAsArray.setText(R.string.no_data);
        mDataAsString.setText(R.string.no_data);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        generateExportString(gattServices);

        String uuid = null;
        final String unknownServiceString = getResources().getString(R.string.unknown_service);
        final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        final List<Map<String, String>> gattServiceData = new ArrayList<>();
        final List<List<Map<String, String>>> gattCharacteristicData = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (final BluetoothGattService gattService : gattServices) {
            final Map<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid.substring(4,8));
            System.out.println("---service name:"+currentServiceData.get(LIST_NAME));
            System.out.println("---service uuid:" + uuid);
            gattServiceData.add(currentServiceData);

            final List<Map<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            final List<BluetoothGattCharacteristic> charas = new ArrayList<>();

            // Loops through available Characteristics.
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                final Map<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                String property = getPropertyString(gattCharacteristic.getProperties());
                currentCharaData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid.substring(4,8)+" "+property);
                System.out.println("-----char name:" + currentCharaData.get(LIST_NAME));
                System.out.println("-----chat uuid:"+ uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                for (BluetoothGattDescriptor gattDescriptor:gattCharacteristic.getDescriptors()){
                    System.out.println("--------des name:" + gattDescriptor.getUuid());
                    System.out.println("--------des uuid:" + gattDescriptor.getValue()+" "+gattDescriptor.getPermissions());
                }
            }

            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        final SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        mGattServicesList.setAdapter(gattServiceAdapter);
        invalidateOptionsMenu();
    }

    /**
     * get property,http://blog.csdn.net/chenxh515/article/details/45723299
     * @param property
     * @return
     */
    private String getPropertyString(int property){
        StringBuilder sb = new StringBuilder("(");
        //Read
        if ((property & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            sb.append("Read ");
        }
        //Write
        if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0
                || (property & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            sb.append("Write ");
        }
        //Notify
        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
                || (property & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            sb.append("Notity Indicate ");
        }
        //Broadcast
        if ((property & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0){
            sb.append("Broadcast ");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }

    private void generateExportString(final List<BluetoothGattService> gattServices) {
        final String unknownServiceString = getResources().getString(R.string.unknown_service);
        final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        final StringBuilder exportBuilder = new StringBuilder();

        exportBuilder.append("Device Name: ");
        exportBuilder.append(mDeviceName);
        exportBuilder.append('\n');
        exportBuilder.append("Device Address: ");
        exportBuilder.append(mDeviceAddress);
        exportBuilder.append('\n');
        exportBuilder.append('\n');

        exportBuilder.append("Services:");
        exportBuilder.append("--------------------------");
        exportBuilder.append('\n');

        String uuid = null;
        for (final BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();

            exportBuilder.append(GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
            exportBuilder.append(" (");
            exportBuilder.append(uuid);
            exportBuilder.append(')');
            exportBuilder.append('\n');

            final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();

                exportBuilder.append('\t');
                exportBuilder.append(GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
                exportBuilder.append(" (");
                exportBuilder.append(uuid);
                exportBuilder.append(')');
                exportBuilder.append('\n');
            }

            exportBuilder.append('\n');
            exportBuilder.append('\n');
        }

        exportBuilder.append("--------------------------");
        exportBuilder.append('\n');

        mExportString = exportBuilder.toString();
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int colourId;

                switch (resourceId) {
                    case R.string.connected:
                        colourId = android.R.color.holo_green_dark;
                        break;
                    case R.string.disconnected:
                        colourId = android.R.color.holo_red_dark;
                        break;
                    default:
                        colourId = android.R.color.black;
                        break;
                }

                mConnectionState.setText(resourceId);
                mConnectionState.setTextColor(getResources().getColor(colourId));
            }
        });
    }

    private static String tryString(final String string, final String fallback) {
        if (string == null) {
            return fallback;
        } else {
            return string;
        }
    }
}