package com.blakequ.androidblemanager.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blakequ.androidblemanager.R;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * date     : 2016/8/26 16:39 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class DeviceStateAdapter extends BaseArrayListAdapter<BluetoothLeDevice>{

    private Holder mHolder;
    private Map<String, ConnectState> map;
    public DeviceStateAdapter(Context context) {
        super(context);
        map = new HashMap<>();
    }


    /**
     * 更新为选择状态
     * @param address
     */
    public void updateState(String address, ConnectState state){
        map.put(address, state);
        notifyDataSetChanged();
    }

    public void removeDevice(String address){
        int pos = 0;
        for (int i=0; i<getCount(); i++){
            BluetoothLeDevice entity = (BluetoothLeDevice) getItem(i);
            if (entity.getAddress().equals(address)) pos = i;
        }
        delete(pos);
    }

    @Override
    public void add(BluetoothLeDevice data){
        if (!map.containsKey(data.getAddress())){
            getAllData().add(data);
            map.put(data.getAddress(), ConnectState.NORMAL);
            Collections.sort(getAllData(), comparator);
            this.notifyDataSetChanged();
        }
    }

    @Override
    public void refreshData(List<BluetoothLeDevice> datas) {
        getAllData().clear();
        addAll(datas);
    }

    @Override
    public void addAll(List<BluetoothLeDevice> datas) {
        if(datas != null && datas.size() > 0) {
            for (BluetoothLeDevice entity : datas){
                if (!map.containsKey(entity.getAddress())){
                    map.put(entity.getAddress(), ConnectState.NORMAL);
                    getAllData().add(entity);
                }
            }
            Collections.sort(getAllData(), comparator);
            this.notifyDataSetChanged();
        }
    }

    private Comparator<BluetoothLeDevice> comparator = new Comparator<BluetoothLeDevice>() {
        @Override
        public int compare(BluetoothLeDevice lhs, BluetoothLeDevice rhs) {
            return lhs.getAddress().compareTo(rhs.getAddress());
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.listitem_device_state, null);
            mHolder = new Holder(convertView);
            convertView.setTag(mHolder);
        }else{
            mHolder = (Holder) convertView.getTag();
        }

        setContent(position, mHolder);
        return convertView;
    }

    private void setContent(int position, Holder mHolder) {
        BluetoothLeDevice entity = (BluetoothLeDevice) getItem(position);
        if (entity != null){
            String name = entity.getAdRecordStore().getLocalNameComplete();
            if (name == null || name.length() == 0) name="Unknow Name";
            mHolder.mTvName.setText(name);
            mHolder.mTvAddress.setText(entity.getAddress());
            ConnectState state = map.get(entity.getAddress());
            mHolder.mTvState.setTextColor(mContext.getColor(R.color.text_content));
            if (state == ConnectState.NORMAL){
                mHolder.mTvState.setText("Disconnect");
            }else if(state == ConnectState.CONNECTING){
                mHolder.mTvState.setText("Connecting");
            }else{
                mHolder.mTvState.setText("Connected");
                mHolder.mTvState.setTextColor(mContext.getColor(R.color.red));
            }
        }
    }

    class Holder{
        public Holder(View view){
            ButterKnife.bind(this, view);
        }
        @Bind(R.id.device_tv_text)
        TextView mTvName;
        @Bind(R.id.device_tv_mac)
        TextView mTvAddress;
        @Bind(R.id.device_tv_state)
        TextView mTvState;

    }
}
