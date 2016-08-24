package com.blakequ.androidblemanager.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.utils.Constants;
import com.blakequ.androidblemanager.utils.PreferencesUtils;

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
        }
    }

    @Override
    public int provideContentViewId() {
        return R.layout.activity_filter;
    }
}
