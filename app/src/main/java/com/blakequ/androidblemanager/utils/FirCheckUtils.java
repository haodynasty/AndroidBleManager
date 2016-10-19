package com.blakequ.androidblemanager.utils;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/10/18 15:31 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: fir.im版本检查更新
 */
public class FirCheckUtils {
    private Context mContext;
    private OnVersionDownloadListener onVersionDownloadListener;

    public FirCheckUtils(Context context){
        this.mContext = context;
    }

    /**
     * 启动fir版本检查，检查完毕会回调结果监听器
     * @param token fir的API token
     * @param onVersionDownloadListener
     */
    public void startCheckVersion(String token, OnVersionDownloadListener onVersionDownloadListener){
        if(Build.VERSION.SDK_INT >= 11) {
            (new CheckTask(mContext)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, token);
        } else {
            (new CheckTask(mContext)).execute(token);
        }
        this.onVersionDownloadListener = onVersionDownloadListener;
    }

    public static interface OnVersionDownloadListener{
        void onNewVersionGet(FirVersionBean versionBean);
    }

    private class CheckTask extends AsyncTask<String, Boolean, FirVersionBean>{
        private Context mContext;
        public CheckTask(Context context){
            this.mContext = context;
        }

        @Override
        protected FirVersionBean doInBackground(String... params) {
            return checkVersionFromFir(params[0]);
        }

        @Override
        protected void onPostExecute(FirVersionBean firVersionBean) {
            if (onVersionDownloadListener != null){
                onVersionDownloadListener.onNewVersionGet(firVersionBean);
            }
        }

        private FirVersionBean checkVersionFromFir(String token){
            String baseUrl = "http://fir.im/api/v2/app/version/%s?token=%s";
            String checkUpdateUrl = String.format(baseUrl, mContext.getPackageName(), token);
            Log.i("FirCheckUtils", "Request debug app update " + checkUpdateUrl);
            try {
                String firResponse = FirCheckUtils.get(checkUpdateUrl);
                Log.i("FirCheckUtils", "get request result "+firResponse);
                if (firResponse != null){
                    JSONObject versionJsonObj = new JSONObject(firResponse);

                    FirVersionBean version = new FirVersionBean();
                    version.setName(versionJsonObj.getString("name"));
                    version.setVersionName(versionJsonObj.getString("versionShort"));
                    version.setVersionCode(Integer.parseInt(versionJsonObj.getString("version")));
                    version.setChangeLog(versionJsonObj.getString("changelog"));
                    version.setInstallUrl(versionJsonObj.getString("install_url"));
                    version.setUpdateUrl(versionJsonObj.getString("update_url"));
                    version.setUpdateTime(versionJsonObj.getLong("updated_at"));
                    version.setIsUpdate(false);

                    //FIR上当前的versionCode
                    int firVersionCode = version.getVersionCode();
                    //FIR上当前的versionName
                    String firVersionName = version.getVersionName();
                    PackageManager pm = mContext.getPackageManager();
                    PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(),
                            PackageManager.GET_ACTIVITIES);
                    if (pi != null) {
                        int currentVersionCode = pi.versionCode;
                        String currentVersionName = pi.versionName;
                        if (firVersionCode > currentVersionCode) {
                            //需要更新
                            Log.i("FirCheckUtils", "version code upper, need update");
                            version.setIsUpdate(true);
                        } else if (firVersionCode == currentVersionCode) {
                            //如果本地app的versionCode与FIR上的app的versionCode一致，则需要判断versionName.
                            if (!currentVersionName.equals(firVersionName)) {
                                Log.i("FirCheckUtils", "version name different, need update");
                                version.setIsUpdate(true);
                            }
                        } else {
                            //不需要更新,当前版本高于FIR上的app版本.
                            Log.i("FirCheckUtils", " no need update");
                        }
                        Log.i("FirCheckUtils", "get parse result "+version);
                        return version;
                    }else{
                        Log.e("FirCheckUtils", "Fail to get package info");
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

    public static String post(String url, String content) {
        HttpURLConnection conn = null;
        try {
            // 创建一个URL对象
            URL mURL = new URL(url);
            // 调用URL的openConnection()方法,获取HttpURLConnection对象
            conn = (HttpURLConnection) mURL.openConnection();

            conn.setRequestMethod("POST");// 设置请求方法为post
            conn.setReadTimeout(5000);// 设置读取超时为5秒
            conn.setConnectTimeout(10000);// 设置连接网络超时为10秒
            conn.setDoOutput(true);// 设置此方法,允许向服务器输出内容

            // post请求的参数
            String data = content;
            // 获得一个输出流,向服务器写数据,默认情况下,系统不允许向服务器输出内容
            OutputStream out = conn.getOutputStream();// 获得一个输出流,向服务器写数据
            out.write(data.getBytes());
            out.flush();
            out.close();

            int responseCode = conn.getResponseCode();// 调用此方法就不必再使用conn.connect()方法
            if (responseCode == 200) {

                InputStream is = conn.getInputStream();
                String response = getStringFromInputStream(is);
                return response;
            } else {
                throw new NetworkErrorException("response status is "+responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();// 关闭连接
            }
        }

        return null;
    }

    public static String get(String url) {
        HttpURLConnection conn = null;
        try {
            // 利用string url构建URL对象
            URL mURL = new URL(url);
            conn = (HttpURLConnection) mURL.openConnection();

            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream is = conn.getInputStream();
                String response = getStringFromInputStream(is);
                return response;
            } else {
                throw new NetworkErrorException("response status is "+responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    private static String getStringFromInputStream(InputStream is)
            throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // 模板代码 必须熟练
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String state = os.toString();// 把流中的数据转换成字符串,采用的编码是utf-8(模拟器默认编码)
        os.close();
        return state;
    }

    public static class FirVersionBean{
        private String name;
        private int versionCode;
        private String versionName;
        private String changeLog;
        private long updateTime;
        private String updateUrl;
        private String installUrl;
        private boolean isUpdate;

        public String getChangeLog() {
            return changeLog;
        }

        public void setChangeLog(String changeLog) {
            this.changeLog = changeLog;
        }

        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime*1000;
        }

        public String getUpdateUrl() {
            return updateUrl;
        }

        public void setUpdateUrl(String updateUrl) {
            this.updateUrl = updateUrl;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public boolean isUpdate() {
            return isUpdate;
        }

        public void setIsUpdate(boolean isUpdate) {
            this.isUpdate = isUpdate;
        }

        @Override
        public String toString() {
            return "FirVersionBean{" +
                    "changeLog='" + changeLog + '\'' +
                    ", name='" + name + '\'' +
                    ", isUpdate='" + isUpdate + '\'' +
                    ", versionCode='" + versionCode + '\'' +
                    ", versionName='" + versionName + '\'' +
                    ", updateTime=" + updateTime +
                    ", updateUrl='" + updateUrl + '\'' +
                    ", installUrl='" + installUrl + '\'' +
                    '}';
        }
    }
}
