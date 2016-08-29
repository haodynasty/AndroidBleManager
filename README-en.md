# AndroidBleManager
android BLE device scan and connect manager, this library

# Functions

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
* Can not scan any device(SDK >= 21), [link](http://stackoverflow.com/questions/33043582/bluetooth-low-energy-startscan-on-android-6-0-does-not-find-devices/33045489#33045489)
```
//
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
* open or close log: BleManager.getInstance().setLogDebugMode(BuildConfig.DEBUG);

# Links
- [Bluetooth-LE-Library](https://github.com/alt236/Bluetooth-LE-Library---Android)
- [BluetoothCompat](https://github.com/joerogers/BluetoothCompat)

# Apk
- [demo address](http://fir.im/pxfn)
- 
