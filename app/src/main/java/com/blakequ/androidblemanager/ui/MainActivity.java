package com.blakequ.androidblemanager.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.blakequ.androidblemanager.BuildConfig;
import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.adapter.FragmentPageAdapter;
import com.blakequ.androidblemanager.containers.BluetoothLeDeviceStore;
import com.blakequ.androidblemanager.event.UpdateEvent;
import com.blakequ.androidblemanager.ui.connect.ConnectManyFragment;
import com.blakequ.androidblemanager.ui.connect.ConnectOneFragment;
import com.blakequ.androidblemanager.ui.scan.ScanFragment;
import com.blakequ.androidblemanager.utils.BluetoothUtils;
import com.blakequ.androidblemanager.utils.Constants;
import com.blakequ.androidblemanager.utils.PreferencesUtils;
import com.blakequ.androidblemanager.widget.ScrollViewPager;
import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothConnectManager;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.multiple.MultiConnectManager;
import com.blakequ.bluetooth_manager_lib.scan.BluetoothScanManager;
import com.blakequ.bluetooth_manager_lib.scan.ScanOverListener;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanResultCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        BleManager.getInstance().setLogDebugMode(BuildConfig.DEBUG);

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

    private void initScan(){
        mBluetoothUtils = new BluetoothUtils(this);
        mDeviceStore = new BluetoothLeDeviceStore();
        scanManager = BluetoothScanManager.getInstance(this);
//        scanManager.addScanFilterCompats(new ScanFilterCompat.Builder().setDeviceName("").build());
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
                if (filterSwitch) {
                    if (filterRssi <= result.getRssi()) {
                        if (filterName == null || filterName.equals("")) {
                            mDeviceStore.addDevice(result.getLeDevice());
                        } else if (filterName.equals(result.getScanRecord().getDeviceName())) {
                            mDeviceStore.addDevice(result.getLeDevice());
                        }
                    }
                } else {
                    mDeviceStore.addDevice(result.getLeDevice());
                }
                EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.SCAN_UPDATE));
            }
        });
    }

    public void startScan(){
        if (mBluetoothUtils.isBluetoothLeSupported()){
            if (!mBluetoothUtils.isBluetoothOn()){
                mBluetoothUtils.askUserToEnableBluetoothIfNeeded();
            }else {
                mDeviceStore.clear();
                EventBus.getDefault().post(new UpdateEvent(UpdateEvent.Type.SCAN_UPDATE));
                scanManager.startCycleScan();
                invalidateOptionsMenu();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            startScan();
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

}
