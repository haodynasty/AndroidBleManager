package com.blakequ.androidblemanager.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.utils.BluetoothRssiLevel;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;

import java.util.ArrayList;
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
 * author  : Blake <blakequ@gmail.com> <br>
 * date     : 2016/6/27 15:25 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class SelectListAdapter extends BaseArrayListAdapter<BluetoothLeDevice> {

    private Holder mHolder;
    private Map<String, Boolean> map;
    private boolean isSingleSelect = true;
    public SelectListAdapter(Context context) {
        super(context);
        map = new HashMap<>();
    }

    public void setSingleSelect(boolean isSingleSelect){
        this.isSingleSelect = isSingleSelect;
    }

    public List<BluetoothLeDevice> getSelectDevice(){
        List<BluetoothLeDevice> list = new ArrayList<>();
        for (String mac:map.keySet()){
            if (map.get(mac)){
                BluetoothLeDevice device = getDevice(mac);
                if (device != null){
                    list.add(device);
                }
            }
        }
        return list;
    }

    private BluetoothLeDevice getDevice(String address){
        for (BluetoothLeDevice entity : getAllData()){
            if (entity.getAddress().equals(address)){
                return entity;
            }
        }
        return null;
    }

    public void cleanSelect(){
        for (BluetoothLeDevice entity : getAllData()){
            map.put(entity.getAddress(), false);
        }
        notifyDataSetChanged();
    }

    /**
     * 更新为选择状态
     * @param address
     */
    public void updateChecked(String address){
        for (BluetoothLeDevice entity : getAllData()){
            if (isSingleSelect){
                map.put(entity.getAddress(), false);
            }
            if (entity.getAddress().equals(address)) {
                map.put(entity.getAddress(), true);
            }
        }
        notifyDataSetChanged();
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
                    map.put(entity.getAddress(), false);
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
            convertView = mInflater.inflate(R.layout.listitem_select, null);
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
            mHolder.mIvFlag.setVisibility(View.INVISIBLE);
            if (map.get(entity.getAddress())){
                mHolder.mIvFlag.setVisibility(View.VISIBLE);
            }
            mHolder.mTvAddress.setText(entity.getAddress());
            mHolder.mIvSingle.setImageResource(BluetoothRssiLevel.getRssiResLevel(mContext, entity.getRssi()));
        }
    }

    class Holder{
        public Holder(View view){
            ButterKnife.bind(this, view);
        }
        @Bind(R.id.device_iv_check)
        ImageView mIvFlag;
        @Bind(R.id.device_tv_text)
        TextView mTvName;
        @Bind(R.id.device_tv_mac)
        TextView mTvAddress;
        @Bind(R.id.device_tv_device_signal)
        ImageView mIvSingle;

    }
}
