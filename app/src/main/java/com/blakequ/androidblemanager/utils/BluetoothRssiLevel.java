package com.blakequ.androidblemanager.utils;

import android.content.Context;

/**
 * Created by PLUSUB on 2015/10/27.
 */
public class BluetoothRssiLevel {

    public static final int MIN_RSSI =  -64;

    /**
     * 通过rssi获取信号强度
     * @param rssi
     * @return 信号强度范围0~5
     */
    public static int getRssiLevel(int rssi){
        int level = 0;
        if (rssi >= -45){
            level = 5;
        }else if (rssi > -54 && rssi <= -45){
            level = 4;
        }else if (rssi > -65 && rssi <= -54){
            level = 3;
        }else if (rssi > -80 && rssi <= -65){
            level = 2;
        }else if (rssi > -90 && rssi <= -80){
            level = 1;
        }else{
            level = 0;
        }
        return level;
    }

    /**
     * 获取信号资源视图
     * @param context
     * @param rssi
     * @return
     */
    public static int getRssiResLevel(Context context, int rssi){
        int level = getRssiLevel(rssi);
        int resId = context.getResources().getIdentifier("ic_signal_cellular_" + level+"_bar",
                "mipmap", context.getPackageName());
        return resId;
    }
}
