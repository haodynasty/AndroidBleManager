package com.blakequ.androidblemanager.utils;

import android.accounts.NetworkErrorException;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * 下载工具类
 * @author blakequ Blakequ@gmail.com
 *
 */
public class DownloadUtils {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int DATA_TIMEOUT = 40000;
    private final static int DATA_BUFFER = 8192;

    public interface DownloadListener {
    	/**
    	 * 实时更新下载进度
    	 * <p>Title: downloading
    	 * <p>Description: 
    	 * @param progress
    	 */
        public void downloading(int progress);
        
        /**
         * 下载完成
         * <p>Title: downloaded
         * <p>Description:
         */
        public void downloaded();
    }

    public static long download(String urlStr, File dest, boolean append, DownloadListener downloadListener) throws Exception {
        int downloadProgress = 0;
        long remoteSize = 0;
        int currentSize = 0;
        long totalSize = -1;

        if(!append && dest.exists() && dest.isFile()) {
            dest.delete();
        }

        if(append && dest.exists() && dest.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(dest);
                currentSize = fis.available();
            } catch(IOException e) {
                throw e;
            } finally {
                if(fis != null) {
                    fis.close();
                }
            }
        }

        HttpURLConnection conn = null;
        try {
            // 利用string url构建URL对象
            URL mURL = new URL(urlStr);
            conn = (HttpURLConnection) mURL.openConnection();

            if(currentSize > 0) {
                conn.setRequestProperty("RANGE", "bytes=" + currentSize + "-");
            }
            conn.setRequestMethod("GET");
            conn.setReadTimeout(DATA_TIMEOUT);
            conn.setConnectTimeout(CONNECT_TIMEOUT);


            InputStream is = null;
            FileOutputStream os = null;
            try {
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    is = conn.getInputStream();
                    remoteSize = conn.getContentLength();
                    String value = conn.getHeaderField("Content-Encoding");
                    Log.i("DownloadUtils", "Get header Content-Encoding:" + value);
                    if(value != null && value.equalsIgnoreCase("gzip")) {
                        is = new GZIPInputStream(is);
                    }

                    os = new FileOutputStream(dest, append);
                    byte buffer[] = new byte[DATA_BUFFER];
                    int readSize = 0;
                    while((readSize = is.read(buffer)) > 0){
                        os.write(buffer, 0, readSize);
                        os.flush();
                        totalSize += readSize;
                        if(downloadListener!= null){
                            downloadProgress = (int) (totalSize*100/remoteSize);
                            downloadListener.downloading(downloadProgress);
                        }
                    }
                    if(totalSize < 0) {
                        totalSize = 0;
                    }
                } else {
                    throw new NetworkErrorException("response status is "+responseCode);
                }
            } finally {
                if(os != null) {
                    os.close();
                }
                if(is != null) {
                    is.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        if(totalSize < 0) {
            throw new Exception("Download file fail: " + urlStr);
        }

        if(downloadListener!= null){
            downloadListener.downloaded();
        }

        return totalSize;
    }
}
