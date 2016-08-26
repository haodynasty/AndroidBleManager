package com.blakequ.androidblemanager.adapter;

import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blakequ.androidblemanager.R;
import com.blakequ.bluetooth_manager_lib.device.resolvers.GattAttributeResolver;
import com.blakequ.bluetooth_manager_lib.util.ByteUtils;


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
 * date     : 2016/8/24 20:44 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class DescListAdapter extends BaseArrayListAdapter<BluetoothGattDescriptor>{

    public DescListAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.descUuid = (TextView) view.findViewById(R.id.tv_item2);
            viewHolder.descValue = (TextView) view.findViewById(R.id.tv_item1);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothGattDescriptor desc = (BluetoothGattDescriptor) getItem(position);
        viewHolder.descUuid.setText("uuid: "+desc.getUuid().toString().substring(4,8));
        if (desc.getValue() != null && desc.getValue().length > 0){
            viewHolder.descValue.setText("Name:"+ GattAttributeResolver.getAttributeName(desc.getUuid().toString(), mContext.getString(R.string.unknown))
                    +"\n"+"Value:"+ ByteUtils.byteArrayToHexString(desc.getValue()));
        }else{
            viewHolder.descValue.setText("Name:"+GattAttributeResolver.getAttributeName(desc.getUuid().toString(), mContext.getString(R.string.unknown)));
        }
        return view;
    }

    static class ViewHolder {
        TextView descUuid;
        TextView descValue;
    }
}
