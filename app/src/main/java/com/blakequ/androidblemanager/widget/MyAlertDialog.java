package com.blakequ.androidblemanager.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.blakequ.androidblemanager.R;
import com.blakequ.androidblemanager.utils.IntentUtils;

import permissions.dispatcher.PermissionRequest;

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
 * date     : 2016/5/12 15:54 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class MyAlertDialog {

    /**
     * 打开设置界面
     */
    public static AlertDialog showOpenSettingDialog(final Context context, @StringRes int messageResId){
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.open_setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        IntentUtils.startAppSettings(context);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .setMessage(context.getString(messageResId))
                .show();
        return dialog;
    }


    /**
     * 弹出请求提示框之前的提示框
     * @param messageResId
     * @param request
     */
    public static AlertDialog showRationaleDialog(final Context context, @StringRes int messageResId, final PermissionRequest request) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(messageResId)
                .show();
        return dialog;
    }

    /**
     * 弹出对话框,两个按钮,显示需要调用show()
     * @param context
     * @param messageResId
     * @param postBtResId
     * @param negaBtResId
     * @param postListener
     * @param negaListener
     */
    public static AlertDialog getDialog(final Context context, @StringRes int messageResId, @StringRes int postBtResId, @StringRes int negaBtResId
        ,DialogInterface.OnClickListener postListener, DialogInterface.OnClickListener negaListener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(postBtResId, postListener)
                .setNegativeButton(negaBtResId, negaListener)
                .setCancelable(true)
                .setMessage(messageResId)
                .create();
        return dialog;
    }

    /**
     * 三个按钮的dialog
     * @param context
     * @param messageResId
     * @param postBtResId 最右边按钮文字
     * @param neutralBtResId 中间按钮文字
     * @param negaBtResId 最左边按钮文字
     * @param postListener 右边按钮监听器
     * @param neutralListener 中间按钮监听器
     * @param negaListener 左边按钮监听器
     * @return
     */
    public static AlertDialog getDialog(final Context context, @StringRes int messageResId, @StringRes int postBtResId, @StringRes int neutralBtResId, @StringRes int negaBtResId
            ,DialogInterface.OnClickListener postListener, DialogInterface.OnClickListener neutralListener, DialogInterface.OnClickListener negaListener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(postBtResId, postListener)
                .setNeutralButton(neutralBtResId, negaListener)
                .setNegativeButton(negaBtResId, negaListener)
                .setCancelable(true)
                .setMessage(messageResId)
                .create();
        return dialog;
    }

    /**
     * 单个按钮,显示需要调用show()
     * @param context
     * @param messageResId
     * @param postBtResId
     * @param postListener
     */
    public static AlertDialog getDialog(final Context context, @StringRes int messageResId, @StringRes int postBtResId
            , DialogInterface.OnClickListener postListener){
        return getDialog(context, messageResId, postBtResId, postListener, true);
    }

    /**
     * 单个按钮,显示需要调用show()
     * @param context
     * @param messageResId
     * @param postBtResId
     * @param postListener
     */
    public static AlertDialog getDialog(final Context context, @StringRes int messageResId, @StringRes int postBtResId
            , DialogInterface.OnClickListener postListener, boolean cancelable) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(postBtResId, postListener)
                .setCancelable(cancelable)
                .setMessage(messageResId)
                .create();
        return dialog;
    }

    /**
     * 自定义dialog
     * @param context
     * @param view
     * @param postBtResId
     * @param negaBtResId
     * @param postListener
     * @param negaListener
     * @param isCancel 是否点击外部可消失
     * @return
     */
    public static AlertDialog getViewDialog(final Context context, View view, @StringRes int postBtResId, @StringRes int negaBtResId
            , DialogInterface.OnClickListener postListener, DialogInterface.OnClickListener negaListener, boolean isCancel){
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(isCancel)
                .setPositiveButton(postBtResId, postListener)
                .setNegativeButton(negaBtResId, negaListener)
                .setView(view, 0, 0, 0, 0)
                .create();
        return dialog;
    }
}
