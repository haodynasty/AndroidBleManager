package com.blakequ.androidblemanager.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.blakequ.androidblemanager.BuildConfig;
import com.blakequ.androidblemanager.ConstValue;
import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.adapter.FragmentPageAdapter;
import com.blakequ.androidblemanager.containers.BluetoothLeDeviceStore;
import com.blakequ.androidblemanager.event.UpdateEvent;
import com.blakequ.androidblemanager.service.AppUpgradeService;
import com.blakequ.androidblemanager.ui.connect.ConnectManyFragment;
import com.blakequ.androidblemanager.ui.connect.ConnectOneFragment;
import com.blakequ.androidblemanager.ui.scan.ScanFragment;
import com.blakequ.androidblemanager.utils.BluetoothUtils;
import com.blakequ.androidblemanager.utils.Constants;
import com.blakequ.androidblemanager.utils.FileUtils;
import com.blakequ.androidblemanager.utils.FirCheckUtils;
import com.blakequ.androidblemanager.utils.IntentUtils;
import com.blakequ.androidblemanager.utils.LocationUtils;
import com.blakequ.androidblemanager.utils.PreferencesUtils;
import com.blakequ.androidblemanager.widget.MyAlertDialog;
import com.blakequ.androidblemanager.widget.ScrollViewPager;
import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectManager;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.multiple.MultiConnectManager;
import com.blakequ.bluetooth_manager_lib.scan.BluetoothScanManager;
import com.blakequ.bluetooth_manager_lib.scan.ScanOverListener;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanResultCompat;
import com.orhanobut.logger.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends ToolbarActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Bind(R.id.common_viewpager)
    protected ScrollViewPager mViewPager;
    @Bind(R.id.fab)
    protected FloatingActionButton fab;

    private List<Fragment> fragments;
    private FragmentPageAdapter mAdapter;
    private BluetoothLeDeviceStore mDeviceStore;
    private BluetoothUtils mBluetoothUtils;
    private BluetoothScanManager scanManager;
    private String filterName;
    private int filterRssi;
    private boolean filterSwitch;
    private int currentTab = 0;
    private String[] permissionList = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private StringBuilder mStringBuilder;
    private File saveFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        BleManager.setBleParamsOptions(ConstValue.getBleOptions(this));

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.POP_SHOW, currentTab));
            }
        });
        fab.setVisibility(View.GONE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //这里是获取NavigationView里面view的方法
        View headerLayout = navigationView.getHeaderView(0);
        ((TextView)headerLayout.findViewById(R.id.tv_my_version)).setText(BuildConfig.VERSION_NAME);

        fragments = new ArrayList<Fragment>();
        fragments.add(new ScanFragment());
        fragments.add(new ConnectOneFragment());
        fragments.add(new ConnectManyFragment());
        mAdapter = new FragmentPageAdapter(getSupportFragmentManager(), fragments);
//        mViewPager.setOffscreenPageLimit(fragments.size());
        mViewPager.setAdapter(mAdapter);
        mViewPager.setLocked(true);
        mViewPager.addOnPageChangeListener(listener);

        initScan();

        updateFirAppUpdate();
        String tmp = FileUtils.getOutCacheDir(getApplicationContext()).getPath()+"/result_data.txt";
        saveFile = new File(tmp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterName = PreferencesUtils.getString(this, Constants.FILTER_NAME, "");
        filterRssi = PreferencesUtils.getInt(this, Constants.FILTER_RSSI, -100);
        filterSwitch = PreferencesUtils.getBoolean(this, Constants.FILTER_SWITCH, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scanManager.isScanning()){
            scanManager.stopCycleScan();
        }
    }

    public BluetoothLeDeviceStore getDeviceStore(){
        return mDeviceStore;
    }

    private void updateFirAppUpdate(){
        new FirCheckUtils(this).startCheckVersion(BuildConfig.FIR_ID, BuildConfig.FIR_TOKEN, new FirCheckUtils.OnVersionDownloadListener() {
            @Override
            public void onNewVersionGet(final FirCheckUtils.FirVersionBean versionBean) {
                if (versionBean != null && versionBean.isUpdate()) {
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    if (checkPermission()){
                                        Intent intent = new Intent(getApplicationContext(), AppUpgradeService.class);
                                        intent.putExtra(AppUpgradeService.EXTRA_DOWLOAD_URL, versionBean.getInstallUrl());
                                        startService(intent);
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setCancelable(true)
                            .setTitle("软件更新")
                            .setMessage("检测到测试版有更新:" + versionBean.getChangeLog() + "，是否立即更新？")
                            .create();
                    dialog.show();
                }
            }
        });
    }



    private void initScan(){
        mBluetoothUtils = new BluetoothUtils(this);
        mDeviceStore = new BluetoothLeDeviceStore();
        scanManager = BluetoothScanManager.getInstance(this);
        scanManager.setScanOverListener(new ScanOverListener() {
            @Override
            public void onScanOver() {
                if (scanManager.isPauseScanning()){
                    invalidateOptionsMenu();
                }
            }
        });
        scanManager.setScanCallbackCompat(new ScanCallbackCompat() {
            @Override
            public void onBatchScanResults(List<ScanResultCompat> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(final int errorCode) {
                super.onScanFailed(errorCode);
                Snackbar.make(fab, "Fail to scan device! error code:" + errorCode, Snackbar.LENGTH_LONG)
                        .setAction("Detail", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (errorCode == SCAN_FAILED_LOCATION_CLOSE){
                                    Toast.makeText(MainActivity.this, "Location is closed, you should open first", Toast.LENGTH_LONG).show();
                                }else if(errorCode == SCAN_FAILED_LOCATION_PERMISSION_FORBID){
                                    Toast.makeText(MainActivity.this, "You have not permission of location", Toast.LENGTH_LONG).show();
                                }else{
                                    Toast.makeText(MainActivity.this, "Other exception", Toast.LENGTH_LONG).show();
                                }
                            }
                        }).show();
            }

            @Override
            public void onScanResult(int callbackType, ScanResultCompat result) {
                super.onScanResult(callbackType, result);
                String deviceName = result.getScanRecord().getDeviceName();
                Logger.i("scan device "+result.getLeDevice().getAddress()+" "+deviceName);
                if (deviceName != null) deviceName = deviceName.toLowerCase();
                if (filterSwitch) {
                    if (filterRssi <= result.getRssi()) {
                        if (filterName == null || filterName.equals("")) {
                            mDeviceStore.addDevice(result.getLeDevice());
                        } else if (filterName.toLowerCase().equals(deviceName)) {
                          mDeviceStore.addDevice(result.getLeDevice());
                          //saveFileLog(result.getDevice().getAddress(), result.getScanRecord().getBytes(), result.getLeDevice().getTimestamp());
                        }
                    }
                } else {
                    mDeviceStore.addDevice(result.getLeDevice());
                }
                EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.SCAN_UPDATE));
            }
        });
    }

  //private void saveFileLog(String mac, byte[] record, long time){
  //  if (record != null && record.length > 0 && record.length >30){
  //    byte[] data = Arrays.copyOfRange(record, 9, 25);
  //    String str = BluetoothDataParserUtils.toString(data);
  //
  //    if (mStringBuilder == null) mStringBuilder = new StringBuilder();
  //    mStringBuilder.append(android.text.format.DateFormat.format(
  //        Constants.TIME_FORMAT, new java.util.Date(time)));
  //    mStringBuilder.append(' '+mac+' ');
  //    mStringBuilder.append(str + "\r\n");
  //    if (mStringBuilder.toString().length() >= 1024){
  //      FileUtils.write(saveFile, mStringBuilder.toString(), true);
  //      mStringBuilder.delete(0, mStringBuilder.length());
  //    }
  //  }
  //}

    public void startScan(){
        if (checkPermission()){
            if (checkIsBleState()){
                mDeviceStore.clear();
                EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.SCAN_UPDATE));
//                scanManager.startCycleScan();
                scanManager.startScanNow();
                invalidateOptionsMenu();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 11 || requestCode == 12) {//请求位置信息
            if (LocationUtils.isGpsProviderEnabled(this)){
                Toast.makeText(this, R.string.ble_location_is_open, Toast.LENGTH_LONG).show();
            }else{
                if (requestCode == 11){
                    showReOpenLocationDialog();
                }else{
                    Toast.makeText(this, R.string.ble_location_not_open_notice, Toast.LENGTH_LONG).show();
                }
            }
        }else{
            if (resultCode != Activity.RESULT_OK){
//                checkIsBleState();
            }else{
                startScan();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUpdate(UpdateEvent event){

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (currentTab == 0){
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            if (!scanManager.isScanning()) {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_filter).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_filter).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
            }
        }else if(currentTab == 1){
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_filter).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            int size = BluetoothConnectManager.getInstance(this).getConnectedDevice().size();
            if (size > 0){
                if(BluetoothConnectManager.getInstance(this).getCurrentState() == ConnectState.CONNECTING){
                    menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
                }else{
                    menu.findItem(R.id.menu_disconnect).setVisible(true);
                }
            }
        }else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_filter).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            if (MultiConnectManager.getInstance(this).isConnectingDevice()){
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
                menu.findItem(R.id.menu_disconnect).setVisible(true);
            }else{
                menu.findItem(R.id.menu_connect).setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menu_scan:
                startScan();
                break;
            case R.id.menu_stop:
                scanManager.stopCycleScan();
                break;
            case R.id.menu_share:
                mDeviceStore.shareDataAsEmail(this);
                break;
            case R.id.menu_filter:
                startActivity(new Intent(this, FilterActivity.class));
                break;
            case R.id.menu_connect:
                if (mDeviceStore.size() == 0){
                    Snackbar.make(fab, "Not bluetooth device, please scan device first", Snackbar.LENGTH_LONG)
                            .setAction("Scan Now", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mViewPager.setCurrentItem(0, false);
                                    startScan();
                                }
                            }).show();
                }else {
                    MultiConnectManager.getInstance(this).startConnect();
                }
                break;
            case R.id.menu_disconnect:
                BluetoothConnectManager.getInstance(this).closeAll();
                MultiConnectManager.getInstance(this).closeAll();
                break;
        }
        return true;
    }

    @Override
    public int provideContentViewId() {
        return R.layout.activity_main;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
            mViewPager.setCurrentItem(0, false);
        } else if (id == R.id.nav_gallery) {
            scanManager.stopCycleScan();
            mViewPager.setCurrentItem(1, false);
        } else if (id == R.id.nav_slideshow) {
            scanManager.stopCycleScan();
            mViewPager.setCurrentItem(2, false);
        } else if (id == R.id.nav_manage) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/haodynasty/AndroidBleManager"));
            startActivity(intent);
        } else if (id == R.id.nav_share) {
            mDeviceStore.shareDataAsEmail(this);
        } else if (id == R.id.nav_send) {
            Intent data=new Intent(Intent.ACTION_SENDTO);
            data.setData(Uri.parse("mailto:blakequ@gmail.com"));
            data.putExtra(Intent.EXTRA_SUBJECT, "AndroidBleManger Feedback");
            data.putExtra(Intent.EXTRA_TEXT, "Please input you question and advise:\n");
            startActivity(data);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void setCurrentIndex(int index){
        if (index >= 0 && index < fragments.size()){
            mViewPager.setCurrentItem(index, false);
        }
    }

    private ViewPager.OnPageChangeListener listener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            currentTab = position;
            EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.TAB_SWITCH, position));
            if (position == 0){
                fab.setVisibility(View.GONE);
            }else {
                fab.setVisibility(View.VISIBLE);
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private boolean checkIsBleState(){
        if (!mBluetoothUtils.isBluetoothLeSupported()){
            showNotSupportDialog();
        }else if(!mBluetoothUtils.isBluetoothOn()){
            showOpenBleDialog();
        }else{
            return true;
        }
        return false;
    }

    private void showNotSupportDialog(){
        MyAlertDialog.getDialog(this, R.string.ble_not_support, R.string.ble_exit_app,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).show();
    }

    private void showExitDialog(){
        MyAlertDialog.getDialog(this, R.string.exit_app, R.string.ble_exit_app, R.string.cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                },
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * 检查无法扫描到的情况dialog
     */
    private void showCheckBleNotScanDialog() {
        if (Build.VERSION.SDK_INT >= 23){
            MyAlertDialog.getDialog(this, R.string.ble_not_scan, R.string.ble_not_scan_bt1, R.string.cancel,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!LocationUtils.isGpsProviderEnabled(MainActivity.this)){
                                showOpenLocationSettingDialog();
                            }else{
                                Toast.makeText(MainActivity.this, R.string.ble_location_has_open, Toast.LENGTH_LONG).show();
                            }
                        }
                    },
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }else{
            MyAlertDialog.getDialog(this, R.string.ble_not_scan1, R.string.cancel,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    /**
     * 是否打开ble
     */
    private void showOpenBleDialog() {
        MyAlertDialog.getDialog(this, R.string.ble_not_open, R.string.ble_open, R.string.cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBluetoothUtils.askUserToEnableBluetoothIfNeeded();
                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * 重新检查位置信息是否开启
     */
    private void showReOpenLocationDialog() {
        MyAlertDialog.getDialog(this, R.string.ble_location_not_open, R.string.ble_location_open, R.string.cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        IntentUtils.startLocationSettings(MainActivity.this, 12);
                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * 打开位置信息
     */
    private void showOpenLocationSettingDialog(){
        View view = LayoutInflater.from(this).inflate(R.layout.include_location_dialog, null);
        MyAlertDialog.getViewDialog(this, view, R.string.ble_location_open, R.string.cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        IntentUtils.startLocationSettings(MainActivity.this, 11);
                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showReOpenLocationDialog();
                        dialog.dismiss();
                    }
                }, false).show();
    }


    /**
     * 检查权限
     * @return
     */
    public boolean checkPermission(){
        if (Build.VERSION.SDK_INT >= 23){
            boolean hasPermission = PermissionUtils.hasSelfPermissions(this, permissionList);
            MainActivityPermissionsDispatcher.showCheckPermissionStateWithCheck(this);
            if (!LocationUtils.isGpsProviderEnabled(this)){
                return false;
            }
            return hasPermission;
        }
        return true;
    }


    //请求权限
    /**
     * 这个方法中写正常的逻辑（假设有该权限应该做的事）
     */
    @NeedsPermission({Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showCheckPermissionState(){
        //检查是否开启位置信息（如果没有开启，则无法扫描到任何蓝牙设备在6.0）
        if (!LocationUtils.isGpsProviderEnabled(this)){
            showOpenLocationSettingDialog();
        }
    }

    /**
     * 弹出权限同意窗口之前调用的提示窗口
     * @param request
     */
    @OnShowRationale({Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showRationaleForPermissionState(PermissionRequest request) {
        // NOTE: Show a rationale to explain why the permission is needed, e.g. with a dialog.
        // Call proceed() or cancel() on the provided PermissionRequest to continue or abort
        MyAlertDialog.showRationaleDialog(this, R.string.permission_rationale, request);
    }

    /**
     * 提示窗口和权限同意窗口--被拒绝时调用
     */
    @OnPermissionDenied({Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onPermissionStateDenied() {
        // NOTE: Deal with a denied permission, e.g. by showing specific UI
        // or disabling certain functionality
        Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
    }

    /**
     * 当完全拒绝了权限打开之后调用
     */
    @OnNeverAskAgain({Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onPermissionNeverAskAgain() {
        MyAlertDialog.showOpenSettingDialog(this, R.string.open_setting_permission);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}
