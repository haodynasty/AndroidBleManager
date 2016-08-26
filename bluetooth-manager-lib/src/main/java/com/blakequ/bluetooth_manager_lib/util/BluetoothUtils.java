package com.blakequ.bluetooth_manager_lib.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * the utils of bluetooth
 *
 * <li>
 * <li>1.check phone is supports BLE features{@link #isBluetoothLeSupported(Context)}
 * <li>2.the bluetooth is open or enable{@link #isBluetoothIsEnable()}
 * <li>3.if the bluetooth is closed you can using this method open system setting to open bluetooth{@link #askUserToEnableBluetoothIfNeeded(Activity)}??{@link #openBlueToothSetting(Activity)}
 */
public final class BluetoothUtils {
    private static String TAG = "BluetoothUtils";
    public final static int REQUEST_ENABLE_BT = 2001;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothManager mBluetoothManager = null;
    private static BluetoothUtils mBluetoothUtils;
    private Context mContext;

    public static synchronized BluetoothUtils getInstance(Context mContext){
        if (mBluetoothUtils == null){
            mBluetoothUtils = new BluetoothUtils(mContext);
        }
        return mBluetoothUtils;
    }

    private BluetoothUtils(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * get adapter
     * @return
     */
    public BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            if (mBluetoothManager == null){
                mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            }
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Failed to construct a BluetoothAdapter");
            }
        }
        return mBluetoothAdapter;
    }

    /**
     * bluetooth is enable
     */
    public boolean isBluetoothIsEnable(){
        if (getBluetoothAdapter() == null || !isBluetoothLeSupported(mContext)) {
            return false;
        }

        return getBluetoothAdapter().isEnabled();
    }

    /**
     * open system setting to open bluetooth
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be {@link android.app.Activity#RESULT_OK} if Bluetooth has been
     * turned on or {@link android.app.Activity#RESULT_CANCELED} if the user
     * has rejected the request or an error has occurred.
     */
    public void askUserToEnableBluetoothIfNeeded(Activity activity) {
        if (isBluetoothLeSupported(activity) && (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * you phone is support bluetooth feature
     * @return
     */
    public static boolean isBluetoothLeSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * open system setting to open bluetooth
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be {@link android.app.Activity#RESULT_OK} if Bluetooth has been
     * turned on or {@link android.app.Activity#RESULT_CANCELED} if the user
     * has rejected the request or an error has occurred.
     * @param mActivity
     */
    public static void openBlueToothSetting(Activity mActivity){
        final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public static boolean isCharacteristicRead(int property){
        if ((property & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            return true;
        }
        return false;
    }

    public static boolean isCharacteristicWrite(int property){
        if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0
                || (property & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            return true;
        }
        return false;
    }

    public static boolean isCharacteristicNotify(int property){
        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
                || (property & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            return true;
        }
        return false;
    }

    public static boolean isCharacteristicBroadcast(int property){
        if ((property & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0){
            return true;
        }
        return false;
    }

}
