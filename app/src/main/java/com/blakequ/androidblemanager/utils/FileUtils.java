package com.blakequ.androidblemanager.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文件工具类
 * @author blakequ@gmail.com
 *
 */
public class FileUtils {
	
	private static String TAG = "FileUtil";
	
	/**
	 * Gets the Android external storage directory
	 */
	public static String getSDCardPath(){
		if(isSDCardAvailable()){
			return Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		return "";
	}

	/**
	 * get the inner file dir, the path is:/data/data/package_name/files
	 * @param context
	 * @return
	 */
	public static File getInnerFileDir(Context context){
		return context.getFilesDir();
	}

	/**
	 * get the inner cache dir, the path is:/data/data/package_name/cache
	 * <br>notice:the cache will be delete if internal storage space is not enough,
	 * 不要把重要的文件放在cache文件里面，
	 * 可以放置在files里面，因为这个文件只有在APP被卸载的时候才会被删除。还有要注意的一点是，如果应用程序是更新操作，内部存储不会被删除，区别于被用户手动卸载
	 * @param context
	 * @return
	 */
	public static File getInnerCacheDir(Context context){
		return context.getCacheDir();
	}

	/**
	 * 获取外部sdcard上私有存储缓存位置(Android文件夹是隐藏文件夹，用户无法操作)，path:/sdcard/Android/date/package_name/cache
	 * <br>notice:如果我们想缓存图片等比较耗空间的文件，推荐放在getExternalCacheDir()所在的文件下面，这个文件和getCacheDir()很像，
	 * 都可以放缓存文件，在APP被卸载的时候，都会被系统删除，而且缓存的内容对其他APP是相对私有的,**应用卸载的时候会被删除**
	 * @param context
	 * @return if sdcard is not available, will return null
	 */
	public static File getOutCacheDir(Context context){
		if(isSDCardAvailable()){
			return context.getExternalCacheDir();
		}
		return null;
	}

	/**
	 * 获取外部sdcard上私有存储图片路径(Android文件夹是隐藏文件夹，用户无法操作)，path:/sdcard/Android/date/package_name/files/Pictures
	 * **应用卸载的时候会被删除**
	 * @param context
	 * @return if sdcard is not available, will return null
	 */
	public static File getOutFilePicDir(Context context){
		if(isSDCardAvailable()){
			return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		}
		return null;
	}

	/**
	 * check the sdcard is mounted and available
	 * @return
	 */
	public static boolean isSDCardAvailable(){
//		boolean canRead = Environment.getExternalStorageDirectory().canRead();
//		boolean onlyRead = Environment.getExternalStorageState().equals(
//				Environment.MEDIA_MOUNTED_READ_ONLY);
		boolean mounted = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);

		return mounted;
	}
	
	/**
	 * create public picture file if not exists
	 * @return if the file exists return true, otherwise, return false and create folder
	 */
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static boolean createExternalStoragePublicPicture(){
		if (!isSDCardAvailable()) {
			return false;
		}
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		return path.mkdirs();
	}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static String getExternalStoragePublicDownload(){
		if (!isSDCardAvailable()) {
			return "";
		}
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		if (!path.exists()) {
			path.mkdirs();
		}
		return path.getAbsolutePath();
	}
	
	/**
	 * 创建文件
	 * @param file
	 * @return
	 */
	public static File createNewFile(File file) {

		try {

			if (file.exists()) {
				return file;
			}

			File dir = file.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			if (!file.exists()) {
				file.createNewFile();
			}
		} catch (IOException e) {
			Log.e(TAG, "", e);
			return null;
		}
		return file;
	}

	/**
	 * 创建文件
	 * @param path
	 */
	public static File createNewFile(String path) {
		File file = new File(path);
		return createNewFile(file);
	}// end method createText()

	/**
	 * 删除文件
	 * 
	 * @param path
	 */
	public static void deleteFile(String path) {
		File file = new File(path);
		deleteFile(file);
	}

	/**
	 * 删除文件
	 * @param file
	 */
	public static void deleteFile(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isFile()) {
			file.delete();
		} else if (file.isDirectory()) {
			File files[] = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				deleteFile(files[i]);
			}
		}
		file.delete();
	}

	/**
	 * 删除文件或目录
	 * @param file
	 * @param deleteThisPath 是否删除文件夹路径
	 */
	public static void deleteFile(File file, boolean deleteThisPath) {
		if (file != null && file.exists()) {
			if (file.isDirectory()) {// 处理目录
				File files[] = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					deleteFile(files[i], true);
				}
			}
			if (deleteThisPath) {
				if (!file.isDirectory()) {// 如果是文件，删除
					file.delete();
				} else {// 目录
					if (file.listFiles().length == 0) {// 目录下没有文件或者目录，删除
						file.delete();
					}
				}
			}
		}
	}

	/**
	 * 向Text文件中写入内容
	 * @param path
	 * @param content
	 * @return
	 */
	public static boolean write(String path, String content) {
		return write(path, content, false);
	}

	/**
	 * 将content写入文件file，append是否已追加方式添加（true：不清空原文件内容）
	 * @param path
	 * @param content
	 * @param append
	 * @return
	 */
	public static boolean write(String path, String content, boolean append) {
		return write(new File(path), content, append);
	}

	/**
	 * 将content写入文件file,默认以覆盖方式写入，如果采用往后添加，则使用{@link #write(File, String, boolean)}
	 * @param file
	 * @param content
	 * @return
	 */
	public static boolean write(File file, String content) {
		return write(file, content, false);
	}

	/**
	 * 将content写入文件file，append是否已追加方式添加（true：不清空原文件内容）
	 * @param file
	 * @param content
	 * @param append
	 * @return
	 */
	public static boolean write(File file, String content, boolean append) {
		if (file == null || TextUtils.isEmpty(content)) {
			return false;
		}
		if (!file.exists()) {
			file = createNewFile(file);
		}
		FileOutputStream ops = null;
		try {
			ops = new FileOutputStream(file, append);
			ops.write(content.getBytes());
			ops.flush();
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return false;
		} finally {
			try {
				if (ops != null) ops.close();
			} catch (IOException e) {
				Log.e(TAG, "", e);
			}
			ops = null;
		}

		return true;
	}
	
	/**
	 * 获得文件名
	 * 
	 * @param path
	 * @return
	 */
	public static String getFileName(String path) {
		if (TextUtils.isEmpty(path)) {
			return null;
		}
		File f = new File(path);
		String name = f.getName();
		f = null;
		return name;
	}

	/**
	 * 读取文件内容，从第startLine行开始，读取lineCount行
	 * 
	 * @param file
	 * @param startLine
	 * @param lineCount
	 * @return 读到文字的list,如果list.size小于lineCount则说明读到文件末尾了
	 */
	public static List<String> readFile(File file, int startLine, int lineCount) {
		if (file == null || startLine < 1 || lineCount < 1) {
			return null;
		}
		if (!file.exists()) {
			return null;
		}
		FileReader fileReader = null;
		List<String> list = null;
		try {
			list = new ArrayList<String>();
			fileReader = new FileReader(file);
			LineNumberReader lnr = new LineNumberReader(fileReader);
			boolean end = false;
			for (int i = 1; i < startLine; i++) {
				if (lnr.readLine() == null) {
					end = true;
					break;
				}
			}
			if (end == false) {
				for (int i = startLine; i < startLine + lineCount; i++) {
					String line = lnr.readLine();
					if (line == null) {
						break;
					}
					list.add(line);

				}
			}
		} catch (Exception e) {
			Log.e(TAG, "read log error!", e);
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}

	/**
	 * 创建文件夹
	 * @param dir
	 * @return
	 */
	public static boolean createDir(File dir) {
		try {
			if (!dir.exists()) {
				dir.mkdirs();
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "create dir error", e);
			return false;
		}
	}


	/**
	 * 判断SD卡上的文件是否存在
	 * <p>Title: isFileExist
	 * <p>Description: 
	 * @param fileName
	 * @return
	 */
	public static boolean isFileExist(String fileName) {
		File file = new File(fileName);
		return file.exists();
	}


	/**
	 * 将一个InputStream里面的数据写入到SD卡中
	 * <p>Title: write2SDFromInput
	 * <p>Description: 
	 * @param path
	 * @param fileName
	 * @param input
	 * @return
	 */
	public static File write2SDFromInput(String path, String fileName,
			InputStream input) {
		File file = null;
		OutputStream output = null;
		try {
			createFolder(path);
			file = createNewFile(path + "/" + fileName);
			output = new FileOutputStream(file);
			byte buffer[] = new byte[1024];
			int len = -1;
			while ((len = input.read(buffer)) != -1) {
				output.write(buffer, 0, len);
			}
			output.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return file;
	}

	/**
	 * 从文件中一行一行的读取文件
	 * <p>Title: readFile
	 * <p>Description: 
	 * @param file
	 * @return
	 */
	public static String readFile(File file) {
		Reader read = null;
		String content = "";
		String string = "";
		BufferedReader br = null;
		try {
			read = new FileReader(file);
			br = new BufferedReader(read);
			while ((content = br.readLine().toString().trim()) != null) {
				string += content + "\r\n";
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				read.close();
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("string=" + string);
		return string.toString();
	}
	
	/**
	 * check the directory is available , if had existed return true, otherwise created 
	 * @param path the path of file
	 * @return
	 */
	public static boolean isDirectoryAvailable(File path){
		if(path.exists()){
			return true;
		}
		return path.mkdirs();
	}
	
	/**
	 * check the directory is available , if had existed return true, otherwise created 
	 * @param path the path of file
	 * @return
	 */
	public static boolean isDirectoryAvailable(String path){
		File file = new File(path);
		return isDirectoryAvailable(file);
	}
	
	/**
	 * check the file is exists or not
	 * @param directory the file directory
	 * @param fileName the file name
	 * @return if exists return true
	 */
	public static boolean isFileExists(File directory, String fileName){
		File file = new File(directory, fileName);
		return file.exists();
	}
	
	/**
	 * create a file , if not exist and create it
	 * @param directory the directory of the file exists
	 * @param fileName the name of file
	 * @return if success return true, otherwise create fail or file exist
	 */
	public static boolean createFile(File directory, String fileName){
		if(!isFileExists(directory, fileName)){
			File file = new File(directory, fileName);
			try {
				return file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * create a folder 
	 * @param pathName the directory of folder, for example: <p><b>mnt\sdcard\app_name\folder_name</b></p>
	 *               you should input {@link #createFolder(String...)}, if not exist will create it,
	 *               otherwise return exist path
	 * @return if success return true
	 */
	public static boolean createFolder(String... pathName){
		File folder = new File(buildDirectory(pathName));
		if(!folder.exists()){
			return folder.mkdirs();
		}
		return true;
	}
	
	/**
	 * 判断文件的类型
	 * @param file 文件
	 * @param postfixName 文件后缀集合
	 * @return 如果是postfixName后缀文件，则返回true
	 */
	public static boolean checkPostfixOfFile(File file, String... postfixName){
		String fName=file.getName();
		int dotIndex = fName.lastIndexOf(".");//获取后缀名前的分隔符"."在fName中的位置。
		if(dotIndex < 0){
	        return false;
	    }
	    String end = fName.substring(dotIndex+1,fName.length()).toLowerCase();//获取文件的后缀名 
		for(String postfix : postfixName){
			if(end.equals(postfix)) return true;
		}
		return false;
	}
	
	/**
	 * create folder and return path, if exist not create 
	 * <p>Title: createAppTempFolder
	 * <p>Description: 
	 * @return the path of directory
	 */
	public static String getFolderByPath(String... pathName){
		String file = buildDirectory(pathName);
		File folder = new File(file);
		if(!folder.exists()){
			folder.mkdirs();
		}
		return file;
	}
	
	/**
	 * 拷贝文件
	 * 
	 * @param fromFile
	 * @param toFile
	 * @throws IOException
	 */
	public static void copyFile(File fromFile,File toFile) throws IOException {
		if(!fromFile.exists() || fromFile.length() == 0) return;
		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[1024];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1)
				to.write(buffer, 0, bytesRead); // write
		} finally {
			if (from != null)
				try {
					from.close();
				} catch (IOException e) {
					Log.e(TAG, "", e);
				}
			if (to != null)
				try {
					to.close();
				} catch (IOException e) {
					Log.e(TAG, "", e);
				}
		}
	}
	
	public static void copyFile(File fromFile, String toFile) throws IOException{
		copyFile(fromFile,new File(toFile));
	}
	
	/**
	 * generate the path by the dirname
	 * @param DirName the hierarchy of the file directory
	 * @return the full path of the application
	 */
	private static String buildDirectory(String... DirName){
		StringBuilder builder = new StringBuilder();
		builder.append(Environment.getExternalStorageDirectory());
		builder.append(File.separatorChar);
		for(String s : DirName){
			builder.append(File.separatorChar);
			builder.append(sanitizeName(s));
		}
		return builder.toString();
	}
	
	/**
     * A set of characters that are prohibited from being in file names.
     */
    private static final Pattern PROHIBITED_CHAR_PATTERN =
        Pattern.compile("[^ A-Za-z0-9_.()]+");
    
    /**
     * The maximum length of a filename, as per the FAT32 specification.
     */
    private static final int MAX_FILENAME_LENGTH = 50;
    
    /**
     * Normalizes the input string and make sure it is a valid fat32 file name.
     *
     * @param name the name to normalize
     * @return the sanitized name
     */
    private static String sanitizeName(String name) {
      String cleaned = PROHIBITED_CHAR_PATTERN.matcher(name).replaceAll("");
      return (cleaned.length() > MAX_FILENAME_LENGTH)
          ? cleaned.substring(0, MAX_FILENAME_LENGTH)
          : cleaned.toString();
    }
    
    /**
     * 获取Uri里的真实路径
     * <p>Title: getRealPath
     * <p>Description: 
     * @param mContext
     * @param fileUrl
     * @return
     */
    private String getRealPath(Context mContext, Uri fileUrl) {
		String filePath = null;
		Uri filePathUri = fileUrl;
		if (fileUrl != null) {
			if (fileUrl.getScheme().toString().compareTo("content") == 0) // content://开头的uri
			{
				Cursor cursor = mContext.getContentResolver().query(fileUrl,
						null, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					int column_index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
					filePath = cursor.getString(column_index); // 取出文件路径
					cursor.close();
				}
			} else if (fileUrl.getScheme().compareTo("file") == 0) // file:///开头的uri
			{
				filePath = filePathUri.toString();
				filePath = filePathUri.toString().replace("file://", "");
			}
		}
		//解决中午路径的FileNotFoundException
		if (!FileUtils.isFileExist(filePath)) {
			try {
				filePath = URLDecoder.decode(filePath,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		return filePath;
	}

	/**
	 * 获取文件夹大小
	 * @param file File实例
	 * @return long 单位字节
	 * @throws Exception
	 */
	public static long getFolderSize(File file)throws Exception{
		long size = 0;
		if (file != null && file.exists()){
			File[] fileList = file.listFiles();
			for (int i = 0; i < fileList.length; i++){
				if (fileList[i].isDirectory()){
					size = size + getFolderSize(fileList[i]);
				} else{
					size = size + fileList[i].length();
				}
			}
		}
		return size;
	}
}
