package com.blakequ.androidblemanager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;

import java.util.List;

/**
 * http://blog.csdn.net/qq_25835645/article/details/46732189
 * Created by PLUSUB on 2015/11/13.
 */
public class IntentUtils {

    /**
     * 是否有某个权限（某权限是否开启）
     * <p><b>注: </b>结果不一定准确，对于第三方厂商会更改<p/>
     * @param context
     * @param permissionString 权限字符串，例如android.permission.RECORD_AUDIO
     * @return
     */
    public static boolean isHavePermission(Context context, String permissionString){
        PackageManager pm = context.getPackageManager();
        boolean isHave = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission(permissionString, context.getPackageName()));
        return isHave;
    }

    /**
     * 获取应用申请的权限列表
     * @param context
     * @return 如果没有权限则返回空
     */
    public static String[] getPermissionList(Context context){
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pack = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            return pack.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * http://www.bkjia.com/Androidjc/1038751.html
     * @param context
     * @return
     */
    public static boolean isMIUI(Context context) {
        boolean result = false;
        Intent i = new Intent("miui.intent.action.APP_PERM_EDITOR");
        i.setClassName("com.android.settings",
                "com.miui.securitycenter.permission.AppPermissionsEditor");
        if (isIntentAvailable(context, i)) {
            result = true;
        }
        return result;
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.GET_ACTIVITIES);
        return list.size() > 0;
    }


    public static void startPermissionActivity(Context context){
        PackageManager pm = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Intent i = new Intent("miui.intent.action.APP_PERM_EDITOR");
        i.setClassName("com.android.settings", "com.miui.securitycenter.permission.AppPermissionsEditor");
        i.putExtra("extra_package_uid", info.applicationInfo.uid);
        try {
            context.startActivity(i);
        } catch (Exception e) {
            //只有xiaomi才能启动
            e.printStackTrace();
        }
    }

    public static void startAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    /**
     * 或要去开启结果，必须实现startActivityForResult
     * @param context
     * @param requestCode
     */
    public static void startLocationSettings(Activity context, int requestCode){
        Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivityForResult(enableLocationIntent, requestCode);
    }
}
