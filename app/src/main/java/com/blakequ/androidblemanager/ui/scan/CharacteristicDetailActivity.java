package com.blakequ.androidblemanager.ui.scan;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.adapter.DescListAdapter;
import com.blakequ.androidblemanager.adapter.NotifyListAdapter;
import com.blakequ.androidblemanager.event.UpdateEvent;
import com.blakequ.androidblemanager.ui.ToolbarActivity;
import com.blakequ.androidblemanager.utils.CustomTextWatcher;
import com.blakequ.androidblemanager.widget.ListViewForScrollView;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectManager;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothSubScribeData;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;
import com.blakequ.bluetooth_manager_lib.device.resolvers.GattAttributeResolver;
import com.blakequ.bluetooth_manager_lib.util.BluetoothUtils;
import com.blakequ.bluetooth_manager_lib.util.ByteUtils;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;

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
 * date     : 2016/8/25 14:29 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class CharacteristicDetailActivity extends ToolbarActivity implements View.OnClickListener{
    public static final String EXTRA_DEVICE = "extra_device";
    public static final String EXTRA_UUID = "extra_uuid";
    @Bind(R.id.char_device_name)
    protected TextView mTvName;
    @Bind(R.id.char_name)
    protected TextView mTvCharName;
    @Bind(R.id.char_device_uuid)
    protected TextView mTvCharUuid;
    @Bind(R.id.char_device_state)
    protected TextView mTvState;
    @Bind(R.id.char_write)
    protected TextView mTvWrite;
    @Bind(R.id.char_write_result)
    protected TextView mTvWriteValue;
    @Bind(R.id.char_read_value)
    protected TextView mTvReadValue;
    @Bind(R.id.char_read)
    protected TextView mTvRead;
    @Bind(R.id.char_notify)
    protected TextView mTvNotify;
    @Bind(R.id.char_notify_list)
    protected ListViewForScrollView mNotifyList;
    @Bind(R.id.char_descriptor_list)
    protected ListViewForScrollView mTvDescriptorList;
    @Bind(R.id.char_properties)
    protected TextView mTvProperties;
    @Bind(R.id.char_read_notify)
    protected TextView mTvNotifyAndRead;
    @Bind(R.id.read_view)
    protected View readView;
    @Bind(R.id.write_view)
    protected View writeView;
    @Bind(R.id.notify_view)
    protected View notifyView;
    protected String writeValue;

    private BluetoothGattCharacteristic characteristic;
    private BluetoothLeDevice mDevice;
    private NotifyListAdapter notifyListAdapter;
    private BluetoothConnectManager connectManager;
    private BluetoothGatt gatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        setTitle("Characteristic Detail");
        notifyListAdapter = new NotifyListAdapter(this);
        connectManager = BluetoothConnectManager.getInstance(this);
        connectManager.addConnectStateListener(listener);
        mNotifyList.setAdapter(notifyListAdapter);
        Intent intent = getIntent();
        mDevice = (BluetoothLeDevice) intent.getParcelableExtra(EXTRA_DEVICE);
        String uuid = intent.getStringExtra(EXTRA_UUID);
        UUID serverUUid = null;
        gatt = connectManager.getBluetoothGatt(mDevice.getAddress());
        if (gatt != null){
            List<BluetoothGattService> list = gatt.getServices();
            if (list != null){
                for (BluetoothGattService service:list){
                    for (BluetoothGattCharacteristic characteristics : service.getCharacteristics()){
                        if (characteristics.getUuid().toString().equals(uuid)){
                            characteristic = characteristics;
                            serverUUid = service.getUuid();
                            break;
                        }
                    }
                }
            }
        }else {
            Logger.e("gatt is null");
        }
        if (characteristic != null){
            initView();
            final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
            mTvCharName.setText(GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
            mTvName.setText(mDevice.getAdRecordStore().getLocalNameComplete());
            mTvCharUuid.setText("uuid: "+uuid);
            mTvProperties.setText(getPropertyString(characteristic.getProperties()));
            DescListAdapter mAdapter = new DescListAdapter(this);
            mTvDescriptorList.setAdapter(mAdapter);
            mTvDescriptorList.setVisibility(View.VISIBLE);
            checkProperty(characteristic.getProperties());
            for (BluetoothGattDescriptor gattDescriptor:characteristic.getDescriptors()){
                mAdapter.add(gattDescriptor);
                Logger.i("desc:" + gattDescriptor.getUuid());
            }

            //start subscribe auto
            //1.set service uuid
            connectManager.setServiceUUID(serverUUid.toString());
            //2.clean history descriptor data
            connectManager.cleanSubscribeData();
            //3.add subscribe params
            if (BluetoothUtils.isCharacteristicRead(characteristic.getProperties())){
                connectManager.addBluetoothSubscribeData(
                        new BluetoothSubScribeData.Builder().setCharacteristicRead(characteristic.getUuid()).build());
            }
            if (BluetoothUtils.isCharacteristicNotify(characteristic.getProperties())){
                connectManager.addBluetoothSubscribeData(
                        new BluetoothSubScribeData.Builder().setCharacteristicNotify(characteristic.getUuid()).build()
                );
            }
            //start descriptor
            boolean isSuccess = connectManager.startSubscribe(gatt);
        }else{
            setOperatorEnable(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        connectManager.closeAll();
        connectManager.removeConnectStateListener(listener);
    }

    private void initView(){
        mTvWrite.setOnClickListener(this);
        mTvRead.setOnClickListener(this);
        mTvNotify.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.char_read:
//                gatt.readCharacteristic(characteristic);
                //or
                connectManager.cleanSubscribeData();
                connectManager.addBluetoothSubscribeData(
                        new BluetoothSubScribeData.Builder().setCharacteristicRead(characteristic.getUuid()).build());
                boolean isSuccess = connectManager.startSubscribe(gatt);
                break;
            case R.id.char_write:
                displayDataDialog();
                break;
            case R.id.char_notify:
//                setCharacteristicNotification(characteristic, true);
                //or
                connectManager.cleanSubscribeData();
                connectManager.addBluetoothSubscribeData(
                        new BluetoothSubScribeData.Builder().setCharacteristicNotify(characteristic.getUuid()).build()
                );
                boolean isSuccess2 = connectManager.startSubscribe(gatt);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return true;
    }

    @Override
    public int provideContentViewId() {
        return R.layout.activity_character_detail;
    }

    private void displayDataDialog() {
        final EditText textView2 = new EditText(this);
        textView2.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
        textView2.addTextChangedListener(new CustomTextWatcher(textView2));
        textView2.setHint("input hex value(e.g. 01,10, 11AB)");

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_filter)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        writeValue = textView2.getText().toString().trim();
                        writeValue = writeValue.replaceAll(" ", "");
                        int len = writeValue.length();
                        if (len > 0 && len%2 == 0) {
                            byte[] bytes = invertStringToBytes(writeValue);
                            if (bytes != null){
                                characteristic.setValue(bytes);
                                gatt.writeCharacteristic(characteristic);
                                mTvWriteValue.setText("0x"+writeValue);
                            }else{
                                Logger.e("write value fail");
                            }
                        }else {
                            Toast.makeText(CharacteristicDetailActivity.this, "Input value is invalid, you should input like(hex value): 01, 1101, 0A11", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setView(textView2)
                .show();
    }

    private byte[] invertStringToBytes(String value){
        int len = value.length()/2;
        if (len > 0){
            byte[] bytes = new byte[len];
            for (int i=0; i<len; i++){
                Integer val = Integer.valueOf(value.substring(i * 2, i * 2 + 2), 16);
                bytes[i] = val.byteValue();
            }
            return bytes;
        }
        return null;
    }

    private String getPropertyString(int property){
        StringBuilder sb = new StringBuilder();
        // 可读
        if ((property & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            sb.append("Read ");
        }
        // 可写，注：要 & 其可写的两个属性
        if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0
                || (property & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            sb.append("Write ");
        }
        // 可通知，可指示
        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            sb.append("Notity ");
        }
        if ((property & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            sb.append("ndicate ");
        }
        // 广播
        if ((property & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0){
            sb.append("Broadcast ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void checkProperty(int property){
        writeView.setVisibility(View.GONE);
        readView.setVisibility(View.GONE);
        notifyView.setVisibility(View.GONE);
        // 可读
        if ((property & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            readView.setVisibility(View.VISIBLE);
        }
        // 可写，注：要 & 其可写的两个属性
        if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0
                || (property & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            writeView.setVisibility(View.VISIBLE);
        }
        // 可通知，可指示
        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
                || (property & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            readView.setVisibility(View.VISIBLE);
            notifyView.setVisibility(View.VISIBLE);
            mNotifyList.setVisibility(View.VISIBLE);
            if ((property & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                mTvNotifyAndRead.setText("READ/NOTIFY VALUES");
            }
        }
        // 广播
        if ((property & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0){
        }
    }

    private ConnectStateListener listener = new ConnectStateListener() {
        @Override
        public void onConnectStateChanged(String address, ConnectState state) {
            switch (state){
                case CONNECTED:
                    setOperatorEnable(true);
                    break;
                case CONNECTING:
                    break;
                case NORMAL:
                    setOperatorEnable(false);
                    break;
            }
        }
    };

    private void setOperatorEnable(boolean enable){
        if (enable){
            mTvState.setVisibility(View.GONE);
            mTvRead.setEnabled(true);
            mTvWrite.setEnabled(true);
            mTvNotify.setEnabled(true);
        }else{
            mTvState.setVisibility(View.VISIBLE);
            mTvRead.setEnabled(false);
            mTvWrite.setEnabled(false);
            mTvNotify.setEnabled(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventReceiveNotify(UpdateEvent event){
        if (event != null && event.getType() == UpdateEvent.Type.BLE_DATA){
            BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) event.getObj();
            final byte[] dataArr = characteristic.getValue();
            final String flag = event.getMsg();
            if (dataArr != null && dataArr.length > 0){
                if (flag.equals("read")){
                    mTvReadValue.setText("byte:"+ ByteUtils.byteArrayToHexString(dataArr)+" ,string:"+ByteUtils.byteArrayToHexString(dataArr));
                }else if(flag.equals("write")){
                    mTvWriteValue.setText("byte:"+ByteUtils.byteArrayToHexString(dataArr)+" ,string:"+ ByteUtils.byteArrayToHexString(dataArr));
                }else if(flag.equals("notify")){
                    notifyListAdapter.addHead(ByteUtils.byteArrayToHexString(dataArr));
                }else{
                    Toast.makeText(CharacteristicDetailActivity.this, "Fail to operator info", Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(CharacteristicDetailActivity.this, "Chara data is null", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        if (gatt == null) {
            Logger.w("CharacteristicDetailActivity", "BluetoothAdapter not initialized");
            return;
        }
        gatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled){
            for (BluetoothGattDescriptor descriptor:characteristic.getDescriptors()){
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

    }
}
