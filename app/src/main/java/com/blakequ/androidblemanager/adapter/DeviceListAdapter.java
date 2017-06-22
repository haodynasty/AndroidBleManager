package com.blakequ.androidblemanager.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.bluetooth.BluetoothDataParserUtils;
import com.blakequ.androidblemanager.utils.Constants;
import com.blakequ.bluetooth_manager_lib.device.BeaconType;
import com.blakequ.bluetooth_manager_lib.device.BeaconUtils;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;
import com.blakequ.bluetooth_manager_lib.device.ibeacon.IBeaconDevice;
import java.util.Arrays;

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
public class DeviceListAdapter extends BaseArrayListAdapter<BluetoothLeDevice>{

    public DeviceListAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
            viewHolder.deviceIcon = (ImageView) view.findViewById(R.id.device_icon);
            viewHolder.deviceLastUpdated = (TextView) view.findViewById(R.id.device_last_update);
            viewHolder.ibeaconMajor = (TextView) view.findViewById(R.id.ibeacon_major);
            viewHolder.ibeaconMinor = (TextView) view.findViewById(R.id.ibeacon_minor);
            viewHolder.ibeaconDistance = (TextView) view.findViewById(R.id.ibeacon_distance);
            viewHolder.ibeaconUUID = (TextView) view.findViewById(R.id.ibeacon_uuid);
            viewHolder.ibeaconTxPower = (TextView) view.findViewById(R.id.ibeacon_tx_power);
            viewHolder.ibeaconSection = view.findViewById(R.id.ibeacon_section);
            viewHolder.ibeaconDistanceDescriptor = (TextView) view.findViewById(R.id.ibeacon_distance_descriptor);
            viewHolder.deviceRecord = (TextView) view.findViewById(R.id.ibeacon_scan_record);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        final BluetoothLeDevice device = (BluetoothLeDevice) getItem(position);
        final String deviceName = device.getName();
        final double rssi = device.getRssi();

        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText(R.string.unknown_device);
        }

        if (BeaconUtils.getBeaconType(device) == BeaconType.IBEACON) {
            final IBeaconDevice iBeacon = new IBeaconDevice(device);
            final String accuracy = Constants.DOUBLE_TWO_DIGIT_ACCURACY.format(iBeacon.getAccuracy());

            viewHolder.deviceIcon.setImageResource(R.mipmap.ic_device_ibeacon);
            viewHolder.ibeaconSection.setVisibility(View.VISIBLE);
            viewHolder.ibeaconMajor.setText(String.valueOf(iBeacon.getMajor()));
            viewHolder.ibeaconMinor.setText(String.valueOf(iBeacon.getMinor()));
            viewHolder.ibeaconTxPower.setText(String.valueOf(iBeacon.getCalibratedTxPower()));
            viewHolder.ibeaconUUID.setText(iBeacon.getUUID());
            viewHolder.ibeaconDistance.setText(
                    mContext.getString(R.string.formatter_meters, accuracy));
            viewHolder.ibeaconDistanceDescriptor.setText(iBeacon.getDistanceDescriptor().toString());
        } else {
            viewHolder.deviceIcon.setImageResource(R.mipmap.ic_bluetooth);
            viewHolder.ibeaconSection.setVisibility(View.GONE);
        }

        final String rssiString =
                mContext.getString(R.string.formatter_db, String.valueOf(rssi));
        final String runningAverageRssiString =
                mContext.getString(R.string.formatter_db, String.valueOf(device.getRunningAverageRssi()));

        viewHolder.deviceLastUpdated.setText(
                android.text.format.DateFormat.format(
                        Constants.TIME_FORMAT, new java.util.Date(device.getTimestamp())));
        viewHolder.deviceAddress.setText(device.getAddress());
        viewHolder.deviceRssi.setText(rssiString + " / " + runningAverageRssiString);

        //add for test temp
        byte[] scan = device.getScanRecord();
        if (scan != null && scan.length > 0 && scan.length >30){
            byte[] data = Arrays.copyOfRange(scan, 9, 25);
            viewHolder.deviceRecord.setVisibility(View.VISIBLE);
            String val = BluetoothDataParserUtils.toString(data) + " Distance:" + String.format("%.2f", calculateAccuracy(-59, device.getRssi()))+"m";
            viewHolder.deviceRecord.setText(val);
        }
        return view;
    }

    public double calculateAccuracy(final int txPower, final double rssi) {
        if (rssi == 0) {
            return 0; // if we cannot determine accuracy, return -1.
        }

        final double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        TextView ibeaconUUID;
        TextView ibeaconMajor;
        TextView ibeaconMinor;
        TextView ibeaconTxPower;
        TextView ibeaconDistance;
        TextView ibeaconDistanceDescriptor;
        TextView deviceLastUpdated;
        View ibeaconSection;
        ImageView deviceIcon;
        TextView deviceRecord;
    }
}
