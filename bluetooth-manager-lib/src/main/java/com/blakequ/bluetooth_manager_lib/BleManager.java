package com.blakequ.bluetooth_manager_lib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectManager;
import com.blakequ.bluetooth_manager_lib.scan.BluetoothScanManager;
import com.blakequ.bluetooth_manager_lib.util.LogUtils;

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
 * date     : 2016/8/17 11:02 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: 能实现扫描管理和连接管理
 */
@TargetApi(18)
public final class BleManager {
    private BluetoothConnectManager singleConnectManager;
    private BluetoothScanManager scanManager;

    private static BleManager INSTANCE = null;
    private BleManager(){
    }

    public static BleManager getInstance(){
        if (INSTANCE == null){
            INSTANCE = new BleManager();
        }
        return INSTANCE;
    }

    public BluetoothConnectManager getConnectManager(Context context){
        return BluetoothConnectManager.getInstance(context);
    }

    public BluetoothScanManager getScanManager(Context context){
        return BluetoothScanManager.getInstance(context);
    }

    /**
     * Check if Bluetooth LE is supported by this Android device, and if so, make sure it is enabled.
     *
     * @return false if it is supported and not enabled
     * @throws BleNotAvailableException if Bluetooth LE is not supported.  (Note: The Android emulator will do this)
     */
    public static boolean checkAvailability(Context context) throws BleNotAvailableException {
        if (isSDKAvailable()) {
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                throw new BleNotAvailableException("Bluetooth LE not supported by this device");
            } else {
                if (((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSDKAvailable(){
        if (android.os.Build.VERSION.SDK_INT < 18) {
            throw new BleNotAvailableException("Bluetooth LE not supported by this device");
        }
        return true;
    }

    /**
     * is debug mode, you can set like setLogDebugMode(BuildConfig.DEBUG),and release version will close log,
     * and if you want close log then set setLogDebugMode(false)
     * @param isDebugMode
     */
    public void setLogDebugMode(boolean isDebugMode){
        LogUtils.setDebugLog(isDebugMode);
    }
}
