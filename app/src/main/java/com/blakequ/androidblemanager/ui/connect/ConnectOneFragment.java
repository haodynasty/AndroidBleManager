package com.blakequ.androidblemanager.ui.connect;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.blakequ.androidblemanager.ui.scan.DeviceControlActivity;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectManager;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
public class ConnectOneFragment extends Fragment{

    private View rootView;
    private BleDeviceDialog dialog;
    @Bind(R.id.tvBluetoothLe)
    protected TextView mTvDevice;
    @Bind(R.id.tvBluetoothStatus)
    protected TextView mTvState;
    @Bind(R.id.common_listview)
    protected ListView mListView;
    private BluetoothConnectManager connectManager;
    private DeviceStateAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectManager = BluetoothConnectManager.getInstance(getActivity());
        connectManager.addConnectStateListener(new ConnectStateListener() {
            @Override
            public void onConnectStateChanged(String address, ConnectState state) {
                mTvDevice.setText(address);
                mTvState.setText(state.toString());
                mAdapter.updateState(address, state);
                switch (state){
                    case CONNECTING:
                        break;
                    case CONNECTED:
                        break;
                    case NORMAL:
                        mTvState.setText("Disconnect");
                        break;
                }
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.invalidateOptionsMenu();
                }
            }
        });
        mAdapter = new DeviceStateAdapter(getActivity());
        EventBus.getDefault().register(this);
        dialog = new BleDeviceDialog(getActivity());
        dialog.setOnClickListener(new BleDeviceDialog.OnClickListener() {
            @Override
            public void onSelectDevice(List<BluetoothLeDevice> list) {
                if (list != null && list.size() > 0){
                    mAdapter.addAll(list);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        connectManager.closeAll();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.frament_connect_one, null);
        ButterKnife.bind(this, rootView);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothLeDevice device = (BluetoothLeDevice) mAdapter.getItem(position);
                if (device != null){
//                    connectManager.connect(device.getAddress());
                    final Intent intent = new Intent(getActivity(), DeviceControlActivity.class);
                    intent.putExtra(DeviceControlActivity.EXTRA_DEVICE, device);
                    startActivity(intent);
                }
            }
        });
        return rootView;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventRefresh(UpdateEvent event){
        switch (event.getType()){
            case POP_SHOW:
                if (event.getArg1() == 1){
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        BluetoothLeDeviceStore store = activity.getDeviceStore();
                        if (store != null) {
                            dialog.addDeviceList(store.getDeviceList());
                            dialog.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
                        }
                    }
                }
                break;
        }
    }
}
