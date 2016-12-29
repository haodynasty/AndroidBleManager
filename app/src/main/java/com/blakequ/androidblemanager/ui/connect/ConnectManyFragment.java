package com.blakequ.androidblemanager.ui.connect;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.adapter.DeviceStateAdapter;
import com.blakequ.androidblemanager.containers.BluetoothLeDeviceStore;
import com.blakequ.androidblemanager.event.UpdateEvent;
import com.blakequ.androidblemanager.ui.MainActivity;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.connect.multiple.MultiConnectManager;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

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
 * date     : 2016/8/23 19:20 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class ConnectManyFragment extends Fragment{

    private View rootView;
    private BleDeviceDialog dialog;
    @Bind(R.id.tvBluetoothMaxNumber)
    protected TextView mTvMax;
    @Bind(R.id.tvBluetoothCurrentNum)
    protected TextView mTvCurrentNum;
    @Bind(R.id.common_listview)
    protected ListView mListView;
    private MultiConnectManager multiConnectManager;
    private DeviceStateAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.frament_connect_two, null);
        ButterKnife.bind(this, rootView);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothLeDevice device = (BluetoothLeDevice) mAdapter.getItem(position);
                if (device != null){
                    multiConnectManager.startConnect(device.getAddress());
                }
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                final BluetoothLeDevice device = (BluetoothLeDevice) mAdapter.getItem(position);
                if (device != null){
                    Snackbar.make(rootView, "Would you want remove device "+device.getAddress()+" ？", Snackbar.LENGTH_LONG)
                            .setAction("Remove", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Logger.d("remove device form queue "+device.getAddress());
                                    mAdapter.removeDevice(device.getAddress());
                                    multiConnectManager.removeDeviceFromQueue(device.getAddress());
                                }
                            }).show();
                }
                return true;
            }
        });
        mTvMax.setText(multiConnectManager.getMaxLen() + "");
        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new DeviceStateAdapter(getActivity());
        multiConnectManager = MultiConnectManager.getInstance(getActivity());
        multiConnectManager.addConnectStateListener(new ConnectStateListener() {
            @Override
            public void onConnectStateChanged(String address, ConnectState state) {
                mAdapter.updateState(address, state);
                mTvCurrentNum.setText(multiConnectManager.getConnectedDevice().size() + "");
                switch (state) {
                    case CONNECTING:
                        break;
                    case CONNECTED:
                    case NORMAL:
                        MainActivity activity = (MainActivity) getActivity();
                        if (activity != null) {
                            activity.invalidateOptionsMenu();
                        }
                        break;
                }
            }
        });
        dialog = new BleDeviceDialog(getActivity());
        dialog.setSingleSelect(false);
        dialog.setOnClickListener(new BleDeviceDialog.OnClickListener() {
            @Override
            public void onSelectDevice(List<BluetoothLeDevice> list) {
                if (list != null && list.size() > 0) {
                    mAdapter.addAll(list);
                    multiConnectManager.addDeviceToQueue(getDeviceList(mAdapter.getAllData()));
                    updateDeviceList();
                }
            }
        });
        EventBus.getDefault().register(this);
    }

    private void updateDeviceList(){
        List<BluetoothLeDevice> adapterlist = mAdapter.getAllData();
        List<String> tlist = multiConnectManager.getAllDevice();
        List<BluetoothLeDevice> newList = new ArrayList<>();
        newList.addAll(adapterlist);

        for (BluetoothLeDevice device:newList){
            String tmp = null;
            for (String mac:tlist){
                if (device.getAddress().equals(mac)){
                    tmp = mac;
                }
            }
            if (tmp == null){
                mAdapter.removeDevice(device.getAddress());
            }
        }
    }

    private String[] getDeviceList(List<BluetoothLeDevice> list){
        String[] array = new String[list.size()];
        int i =0;
        for (BluetoothLeDevice device:list){
            array[i++] = device.getAddress();
        }
        return array;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        multiConnectManager.release();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventRefresh(UpdateEvent event) {
        switch (event.getType()) {
            case POP_SHOW:
                if (event.getArg1() == 2) {
                    final MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        BluetoothLeDeviceStore store = activity.getDeviceStore();
                        if (store != null && store.size() <= 0) {
                            Snackbar.make(rootView, "Not bluetooth device, please scan device first", Snackbar.LENGTH_LONG)
                                    .setAction("Scan Now", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            activity.setCurrentIndex(0);
                                            activity.startScan();
                                        }
                                    }).show();
                        }else {
                            dialog.addDeviceList(store.getDeviceList());
                            dialog.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
                        }
                    }
                }
                break;
            case TAB_SWITCH:
                int tab = event.getArg1();
                if (tab != 2){
                    multiConnectManager.release();
                    mAdapter.clear();
                }
                break;
        }
    }
}
