/*
 * FileName: IActivity.java
 * Copyright (C) 2014 Plusub Tech. Co. Ltd. All Rights Reserved <admin@plusub.com>
 * 
 * Licensed under the Plusub License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * author  : service@plusub.com
 * date     : 2014-12-1 上午11:16:49
 * last modify author :
 * version : 1.0
 */
package com.blakequ.androidblemanager.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * 
 * @ClassName: DensityUtils
 * @Description: TODO 系统屏幕的一些操作
 * @author qh@plusub.com
 * @date 2014-12-1 下午10:03:05
 * @version v1.0
 */
public final class DensityUtils {

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 sp
     */
    public static int px2sp(Context context, float pxValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 sp 的单位 转成为 px
     */
    public static int sp2px(Context context, float spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 获取dialog宽度(单位像素)
     */
    public static int getDialogW(Activity aty) {
        DisplayMetrics dm = new DisplayMetrics();
        aty.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int w = dm.widthPixels - 100;
        return w;
    }

    /**
     * 获取屏幕宽度(单位像素)
     */
    public static int getScreenW(Activity aty) {
        DisplayMetrics dm = new DisplayMetrics();
        aty.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int w = dm.widthPixels;
        return w;
    }

    /**
     * 获取屏幕高度(单位像素)
     */
    public static int getScreenH(Activity aty) {
        DisplayMetrics dm = new DisplayMetrics();
        aty.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int h = dm.heightPixels;
        return h;
    }
    
    /**
     * 获取屏幕属性
     * <p>Title: getScreenD
     * <p>Description: 
     * @param aty
     * @return
     */
    public static DisplayMetrics getScreenD(Activity aty){
    	DisplayMetrics dm = new DisplayMetrics();
    	aty.getWindowManager().getDefaultDisplay().getMetrics(dm);
    	return dm;
    }
}