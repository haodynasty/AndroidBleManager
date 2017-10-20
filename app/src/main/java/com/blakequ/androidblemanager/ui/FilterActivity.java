package com.blakequ.androidblemanager.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.blakequ.androidblemanager.ConstValue;
import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.adapter.SpinnerAdapter;
import com.blakequ.androidblemanager.event.UpdateEvent;
import com.blakequ.androidblemanager.utils.Constants;
import com.blakequ.androidblemanager.utils.PreferencesUtils;
import com.blakequ.bluetooth_manager_lib.BleManager;
import org.greenrobot.eventbus.EventBus;

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
 * date     : 2016/8/24 11:12 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class FilterActivity extends ToolbarActivity {

    @Bind(R.id.filter_switch)
    protected Switch mSwitch;
    @Bind(R.id.filter_et_name)
    protected EditText mEtName;
    @Bind(R.id.filter_et_rssi)
    protected SeekBar mEtRssi;
    @Bind(R.id.filter_tv_rssi)
    protected TextView mTvRssi;
    @Bind(R.id.diableFlag)
    protected ImageView mIvFlag;
    private int rssi;
    private int scanPeriod;
    private int pausePeriod;
    private int scordKey;
    private SpinnerAdapter adapter;

    @Bind(R.id.filter_tv_scan)
    protected TextView mTvScan;
    @Bind(R.id.filter_tv_pause)
    protected TextView mTvPause;
    @Bind(R.id.filter_et_scan_period)
    protected SeekBar mSeekBarScan;
    @Bind(R.id.filter_et_pause_period)
    protected SeekBar mSeekBarPause;
    @Bind(R.id.filter_spinner)
    protected Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        setTitle("Filter");

        boolean isOpen = PreferencesUtils.getBoolean(this, Constants.FILTER_SWITCH, false);
        setCheck(isOpen);
        mSwitch.setChecked(isOpen);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setCheck(isChecked);
            }
        });
        mEtRssi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rssi = -progress;
                mTvRssi.setText("Rssi Value: " + String.valueOf(rssi));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mEtName.setText(PreferencesUtils.getString(this, Constants.FILTER_NAME, ""));
        rssi = PreferencesUtils.getInt(this, Constants.FILTER_RSSI, -100);
        mTvRssi.setText(String.valueOf(rssi));
        mEtRssi.setProgress(Math.abs(rssi));
        mIvFlag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        scanPeriod = PreferencesUtils.getInt(this, Constants.SCAN_PERIOD, 10*1000)/1000;
        pausePeriod = PreferencesUtils.getInt(this, Constants.PAUSE_PERIOD, 5*1000)/1000;
        mTvScan.setText("Scan Period: " + String.valueOf(scanPeriod)+" seconds");
        mTvPause.setText("Pause Period: " + String.valueOf(pausePeriod)+" seconds");
        mSeekBarScan.setProgress(scanPeriod);
        mSeekBarPause.setProgress(pausePeriod);
        mSeekBarScan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTvScan.setText("Scan Period: " + String.valueOf(progress)+" seconds");
                scanPeriod = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekBarPause.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTvPause.setText("Pause Period: " + String.valueOf(progress)+" seconds");
                pausePeriod = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        scordKey = PreferencesUtils.getInt(this, Constants.SHOW_SPINNER, -1);
        adapter = new SpinnerAdapter(this);
        mSpinner.setAdapter(adapter);
        mSpinner.setSelection(adapter.getPositionByValue(scordKey));
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SpinnerAdapter.Record record = (SpinnerAdapter.Record) adapter.getItem(i);
                scordKey = record.id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void setCheck(boolean isChecked){
        if (!isChecked){
            mIvFlag.setVisibility(View.VISIBLE);
        }else{
            mIvFlag.setVisibility(View.GONE);
            mEtName.clearFocus();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        PreferencesUtils.putBoolean(this, Constants.FILTER_SWITCH, mSwitch.isChecked());
        if (mSwitch.isChecked()){
            PreferencesUtils.putString(this, Constants.FILTER_NAME, mEtName.getText().toString().trim());
            PreferencesUtils.putInt(this, Constants.FILTER_RSSI, rssi);
            if (scanPeriod == 0) scanPeriod = 1;
            if (pausePeriod == 0) pausePeriod = 1;
            PreferencesUtils.putInt(this, Constants.SCAN_PERIOD, scanPeriod*1000);
            PreferencesUtils.putInt(this, Constants.PAUSE_PERIOD, pausePeriod*1000);
            BleManager.setBleParamsOptions(ConstValue.getBleOptions(this));
            PreferencesUtils.putInt(getApplicationContext(), Constants.SHOW_SPINNER, scordKey);
            EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.CONFIG_CHANGE));
        }
    }

    @Override
    public int provideContentViewId() {
        return R.layout.activity_filter;
    }
}
