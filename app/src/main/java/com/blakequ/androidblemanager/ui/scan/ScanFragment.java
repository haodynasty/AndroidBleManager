package com.blakequ.androidblemanager.ui.scan;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.adapter.DeviceListAdapter;
import com.blakequ.androidblemanager.containers.BluetoothLeDeviceStore;
import com.blakequ.androidblemanager.event.UpdateEvent;
import com.blakequ.androidblemanager.ui.MainActivity;
import com.blakequ.androidblemanager.utils.BluetoothUtils;
import com.blakequ.androidblemanager.utils.Constants;
import com.blakequ.androidblemanager.utils.PreferencesUtils;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
public class ScanFragment extends Fragment implements AdapterView.OnItemClickListener{

    @Bind(R.id.tvBluetoothLe)
    protected TextView mTvBluetoothLeStatus;
    @Bind(R.id.tvBluetoothStatus)
    protected TextView mTvBluetoothStatus;
    @Bind(R.id.tvBluetoothFilter)
    protected TextView mTvBluetoothFilter;
    @Bind(R.id.tvItemCount)
    protected TextView mTvItemCount;
    @Bind(android.R.id.list)
    protected ListView mList;
    @Bind(android.R.id.empty)
    protected View mEmpty;
    private View rootView;

    private DeviceListAdapter mLeDeviceListAdapter;
    private BluetoothUtils mBluetoothUtils;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        mBluetoothUtils = new BluetoothUtils(getActivity());
        mLeDeviceListAdapter = new DeviceListAdapter(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        final boolean mIsBluetoothOn = mBluetoothUtils.isBluetoothOn();
        final boolean mIsBluetoothLePresent = mBluetoothUtils.isBluetoothLeSupported();

        if (mIsBluetoothOn) {
            mTvBluetoothStatus.setText(R.string.on);
        } else {
            mTvBluetoothStatus.setText(R.string.off);
        }

        if (mIsBluetoothLePresent) {
            mTvBluetoothLeStatus.setText(R.string.supported);
        } else {
            mTvBluetoothLeStatus.setText(R.string.not_supported);
        }

        String filterName = PreferencesUtils.getString(getContext(), Constants.FILTER_NAME, "");
        int filterRssi = PreferencesUtils.getInt(getContext(), Constants.FILTER_RSSI, -100);
        boolean filterSwitch = PreferencesUtils.getBoolean(getContext(), Constants.FILTER_SWITCH, false);
        if (filterSwitch){
            if (filterName != null && filterName.length() > 0){
                mTvBluetoothFilter.setText("FilterName:"+filterName+" ,FilterRssi:"+filterRssi);
            }else {
                mTvBluetoothFilter.setText("FilterRssi:"+filterRssi);
            }
        }else {
            mTvBluetoothFilter.setText(R.string.off);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventRefresh(UpdateEvent event){
        switch (event.getType()){
            case SCAN_UPDATE:
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null){
                    BluetoothLeDeviceStore store = activity.getDeviceStore();
                    if (store != null){
                        mLeDeviceListAdapter.refreshData(store.getDeviceList());
                        updateItemCount(mLeDeviceListAdapter.getCount());
                    }
                }
                break;
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.frament_scan, null);
        ButterKnife.bind(this, rootView);
        mList.setEmptyView(mEmpty);
        mList.setOnItemClickListener(this);
        mList.setAdapter(mLeDeviceListAdapter);
        updateItemCount(0);
        return rootView;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BluetoothLeDevice device = (BluetoothLeDevice) mLeDeviceListAdapter.getItem(position);
        if (device == null) return;

        final Intent intent = new Intent(getActivity(), DeviceDetailsActivity.class);
        intent.putExtra(DeviceDetailsActivity.EXTRA_DEVICE, device);

        startActivity(intent);
    }

    private void updateItemCount(final int count) {
        mTvItemCount.setText(getString(R.string.formatter_item_count, String.valueOf(count)));
    }
}
