# AndroidBleManager
android BLE device scan and connect manager, this library

# Functions
- 低版本扫描兼容
- 低功耗扫描，启动后台扫描时节约60%的电量
- 单个蓝牙连接管理
- 多蓝牙连接管理
- 扫描高低版本兼容性封装
- 扫描的步骤
- 单设备连接的步骤
- 多设备连接的步骤

# Permission Explanation
You will need the following permissions to access the Bluetooth Hardware

* `android.permission.BLUETOOTH`
* `android.permission.BLUETOOTH_ADMIN`

if SDK >= 23, add permission

* `android.permission.ACCESS_COARSE_LOCATION`
* `android.permission.ACCESS_FINE_LOCATION`

## TODO

* Tidy up Javadoc. There is quite a lot of it that is template
* Add parsers for common Ad Records.
* 如果无法扫描到任何设备，请检查当前APP运行SDK是否>=23, 如果SDK>=23的手机必须申请位置权限并且打开位置信息，否则无法扫描到设备（是23的最新限制，当然如果知道mac地址可直接连接）,检查可通过如下代码
```
//http://stackoverflow.com/questions/33043582/bluetooth-low-energy-startscan-on-android-6-0-does-not-find-devices/33045489#33045489
private boolean checkLocationPermission() {
        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean checkPermission(final String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    public static boolean isGpsProviderEnabled(Context context){
            LocationManager service = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
            return service.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
```
* 动态打开关闭日志BleManager.getInstance().setLogDebugMode(BuildConfig.DEBUG);

# Links
- [Bluetooth-LE-Library](https://github.com/alt236/Bluetooth-LE-Library---Android)
- [BluetoothCompat](https://github.com/joerogers/BluetoothCompat)

# Apk
- [demo address](http://fir.im/pxfn)
- 
