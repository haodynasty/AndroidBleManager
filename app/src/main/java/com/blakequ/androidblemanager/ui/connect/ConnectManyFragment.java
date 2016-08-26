package com.blakequ.androidblemanager.ui.connect;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
                mTvCurrentNum.setText(multiConnectManager.getConnectedDevice().size()+"");
                switch (state){
                    case CONNECTING:
                        break;
                    case CONNECTED:
                        MainActivity activity = (MainActivity) getActivity();
                        if (activity != null) {
                            activity.invalidateOptionsMenu();
                        }
                        break;
                    case NORMAL:
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
                    int maxLen = multiConnectManager.getMaxLen();
                    if (list.size() > maxLen) {
                        mAdapter.addAll(list.subList(0, maxLen));
                    } else {
                        mAdapter.addAll(list);
                    }
                    multiConnectManager.addDeviceToQueue(getDeviceList(mAdapter.getAllData()));
                }
            }
        });
        EventBus.getDefault().register(this);
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
