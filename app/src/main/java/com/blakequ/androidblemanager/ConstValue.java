/*
******************************* Copyright (c)*********************************\
**
**                 (c) Copyright 2016-2017 All Rights Reserved
**
**                           By(成都凡米科技有限公司)
**
**-----------------------------------版本信息------------------------------------
** 版    本: V0.1
** 时    间：2017-06-28 10:05 PLUSUB
**------------------------------------------------------------------------------
********************************End of Head************************************\
*/

package com.blakequ.androidblemanager;

import android.content.Context;
import com.blakequ.androidblemanager.utils.Constants;
import com.blakequ.androidblemanager.utils.PreferencesUtils;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;

public final class ConstValue   {

  private static BleParamsOptions.Builder options = new BleParamsOptions.Builder()
      .setBackgroundBetweenScanPeriod(5 * 60 * 1000)
                .setBackgroundScanPeriod(10000)
                .setForegroundBetweenScanPeriod(5000)
                .setForegroundScanPeriod(15000)
                .setDebugMode(BuildConfig.DEBUG)
                .setMaxConnectDeviceNum(5)
                .setReconnectBaseSpaceTime(8000)
                .setReconnectMaxTimes(4)
                .setReconnectStrategy(ConnectConfig.RECONNECT_FIXED_TIME)
                .setReconnectedLineToExponentTimes(5);

  public static BleParamsOptions getBleOptions(Context context){
    int scanPeriod = PreferencesUtils.getInt(context, Constants.SCAN_PERIOD, 10*1000);
    int pausePeriod = PreferencesUtils.getInt(context, Constants.PAUSE_PERIOD, 5*1000);
    return options.setForegroundScanPeriod(scanPeriod)
        .setForegroundBetweenScanPeriod(pausePeriod)
        .build();
  }

}
