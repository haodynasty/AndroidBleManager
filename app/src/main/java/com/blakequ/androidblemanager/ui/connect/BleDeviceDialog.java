package com.blakequ.androidblemanager.ui.connect;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.adapter.SelectListAdapter;
import com.blakequ.androidblemanager.utils.DensityUtils;
import com.blakequ.bluetooth_manager_lib.device.BluetoothLeDevice;

import java.util.List;

/**
 * Copyright (C) quhao All Rights Reserved <blakequ@gmail.com>
 * <p/>
 * Licensed under the Plusub License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * author  : quhao <blakequ@gmail.com>
 * date     : 2016/4/11 17:47
 * last modify author :
 * version : 1.0
 * description:
 */
public class BleDeviceDialog extends PopupWindow implements View.OnClickListener{
    private Activity context;
    private TextView mTvCancel;
    private TextView mTvScan;
    private ListView mListDevice;
    private SelectListAdapter mDeviceAdapter;
    private OnClickListener mOnClickListener;

    public BleDeviceDialog(Activity context){
        super(context);
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.include_pop_ble_device, null);
        mTvCancel = (TextView) view.findViewById(R.id.main_ble_cancle);
        mTvCancel.setOnClickListener(this);
        mTvScan = (TextView) view.findViewById(R.id.main_ble_confirm);
        mTvScan.setOnClickListener(this);
        mListDevice = (ListView) view.findViewById(R.id.common_listview);
        mDeviceAdapter = new SelectListAdapter(context);
        mListDevice.setAdapter(mDeviceAdapter);
        mListDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothLeDevice device = (BluetoothLeDevice) mDeviceAdapter.getItem(position);
                mDeviceAdapter.updateChecked(device.getAddress());
            }
        });
        setContentView(view);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(DensityUtils.getScreenH(context) / 2);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(0xe0000000));
        setFocusable(true);
        setAnimationStyle(R.style.popwin_anim_style);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss() {
                // TODO Auto-generated method stub
//				mCoverBoard.setVisibility(View.GONE);
                backgroundAlpha(1f);
            }

        });
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        // TODO Auto-generated method stub
        backgroundAlpha(0.5f);
        super.showAtLocation(parent, gravity, x, y);
    }

    public void addDeviceList(List<BluetoothLeDevice> mDevices){
        if (mDevices != null && mDevices.size() > 0){
            mDeviceAdapter.refreshData(mDevices);
        }
    }

    public void setSingleSelect(boolean isSingleSelect){
        mDeviceAdapter.setSingleSelect(isSingleSelect);
    }

    /**
     * 设置添加屏幕的背景透明度
     * @param bgAlpha 0为不可见，1为透明
     */
    private void backgroundAlpha(float bgAlpha)
    {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        context.getWindow().setAttributes(lp);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.main_ble_cancle:
                dismiss();
                break;
            case R.id.main_ble_confirm:
                dismiss();
                if (mOnClickListener != null){
                    mOnClickListener.onSelectDevice(mDeviceAdapter.getSelectDevice());
                }
                break;
        }
    }

    public void setOnClickListener(OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    public interface OnClickListener{
        void onSelectDevice(List<BluetoothLeDevice> address);
    }
}
