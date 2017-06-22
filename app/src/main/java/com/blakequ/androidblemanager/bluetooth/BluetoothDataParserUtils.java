package com.blakequ.androidblemanager.bluetooth;

import com.blakequ.androidblemanager.utils.ByteUtils;
import java.util.Calendar;

/**
 * 蓝牙数据解析
 * Created by PLUSUB on 2015/9/28.
 */
public class BluetoothDataParserUtils {

  private static final double TEMP_ARRAY[] =
      {47.7187, 47.4973, 47.2772, 47.0582, 46.8405, 46.6239, 46.4084, 46.1942, 45.9811, 45.7691,
          45.5583, 45.3485, 45.1399, 44.9324, 44.7260, 44.5207, 44.3164, 44.1133, 43.9111, 43.7100,
          43.5100, 43.3110, 43.1130, 42.9160, 42.7199, 42.5249, 42.3308, 42.1377, 41.9456, 41.7544,
          41.5641, 41.3747, 41.1863, 40.9987, 40.8120, 40.6263, 40.4413, 40.2572, 40.0740, 39.8916,
          39.7100, 39.5300, 39.3509, 39.1728, 38.9955, 38.8192, 38.6437, 38.4691, 38.2955, 38.1227,
          37.9508, 37.7797, 37.6095, 37.4402, 37.2718, 37.1042, 36.9374, 36.7715, 36.6064, 36.4422,
          36.2788, 36.1162, 35.9544, 35.7934, 35.6333, 35.4740, 35.3154, 35.1577, 35.0008, 34.8446,
          34.6892, 34.5347, 34.3808, 34.2278, 34.0755, 33.9240, 33.7733, 33.6233, 33.4740, 33.3255,
          33.1777, 33.0307, 32.8844, 32.7389, 32.5940, 32.4499, 32.3065, 32.1638, 32.0218, 31.8806,
          31.7400, 31.6001, 31.4609, 31.3225, 31.1846, 31.0475, 30.9111, 30.7753, 30.6402, 30.5058,
          30.3720, 30.2389, 30.1064, 29.9746, 29.8435, 29.7130, 29.5831, 29.4539, 29.3253, 29.1973,
          29.0700, 28.9433, 28.8172, 28.6917, 28.5669, 28.4426, 28.3190, 28.1959, 28.0735, 27.9516,
          27.8304, 27.7097, 27.5896, 27.4701, 27.3512, 27.2329, 27.1151, 26.9979, 26.8813, 26.7653,
          26.6498, 26.5348, 26.4204, 26.3066, 26.1933, 26.0806, 25.9684, 25.8567, 25.7456, 25.6350,
          25.5250,
          25.4157, 25.3069, 25.1986, 25.0908, 24.9835, 24.8768, 24.7705, 24.6647, 24.5595, 24.4547,
          24.3504,
          24.2467, 24.1434, 24.0406, 23.9382, 23.8364, 23.7350, 23.6342, 23.5338, 23.4338};

  /**
   * 获取目标温度
   */
  public static double getTemperatureObj(byte[] bytes) {
    byte[] result1 = new byte[4];
    result1[3] = bytes[0];
    result1[2] = (byte) ((0xF0 & bytes[4]) >> 4);
    result1[1] = 0x00;
    result1[0] = 0x00;
    int sensor = ByteUtils.byteArrayToInt(result1);

    byte[] result2 = new byte[4];
    result2[3] = bytes[1];
    result2[2] = (byte) (0x0F & bytes[4]);
    result2[1] = 0x00;
    result2[0] = 0x00;
    int power = ByteUtils.byteArrayToInt(result2);
    return voltageToTemperature(power, sensor);
  }

  /**
   * 获取环境温度
   */
  public static double getTemperatureEnv(byte[] bytes) {
    byte[] result1 = new byte[4];
    result1[3] = bytes[2];
    result1[2] = (byte) ((0xF0 & bytes[5]) >> 4);
    result1[1] = 0x00;
    result1[0] = 0x00;
    int sensor = ByteUtils.byteArrayToInt(result1);

    byte[] result2 = new byte[4];
    result2[3] = bytes[3];
    result2[2] = (byte) (0x0F & bytes[5]);
    result2[1] = 0x00;
    result2[0] = 0x00;
    int power = ByteUtils.byteArrayToInt(result2);
    return voltageToTemperature(power, sensor);
  }

  /**
   * 获取容量
   */
  public static double getCapacity(byte highByte, byte lowByte) {
    //获取容量
    byte[] capaTmp = new byte[4];
    capaTmp[3] = lowByte;
    capaTmp[2] = highByte;
    capaTmp[1] = 0x00;
    capaTmp[0] = 0x00;
    int tmp = ByteUtils.byteArrayToInt(capaTmp);

    //最高位是否为1
    boolean result = ((0x80 & highByte) == 0x80) ? true : false;

    double capacity = 0;
    if (result) {
      capacity = (double) ((tmp << 8) | 0xFF000000) / 524228.0;
    } else {
      capacity = (double) (tmp << 8) / 524228.0;
    }
    return capacity;
  }

  public static double getCapacity1(byte[] bytes){
    return getCapacity(bytes[6], bytes[7]);
  }

  public static double getCapacity2(byte[] bytes){
    return getCapacity(bytes[8], bytes[9]);
  }

  public static double getCapacity3(byte[] bytes){
    return getCapacity(bytes[10], bytes[11]);
  }

  public static long getTime(byte[] bytes) {
    if (bytes != null && bytes.length < 16) return System.currentTimeMillis();
    byte[] result = new byte[4];
    result[0] = bytes[15];
    result[1] = bytes[14];
    result[2] = bytes[13];
    result[3] = bytes[12];

    long timeMills = (ByteUtils.byteArrayToInt(result) & 0xFFFFFFFF);
    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, 0, 1, 0, 0, 0);
    timeMills = calendar.getTimeInMillis() + timeMills * 1000;
    calendar.setTimeInMillis(timeMills);
    return timeMills;
  }

  public static String toString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    sb.append(com.blakequ.bluetooth_manager_lib.util.ByteUtils.byteArrayToHexString(bytes));
    sb.append(" T1:" + String.format("%.2f", getTemperatureObj(bytes)));
    sb.append(" T2:" + String.format("%.2f", getTemperatureEnv(bytes)));
    sb.append(" C1:" + String.format("%.2f", getCapacity1(bytes)));
    sb.append(" C2:" + String.format("%.2f", getCapacity2(bytes)));
    sb.append(" C3:" + String.format("%.2f", getCapacity3(bytes)));
    return sb.toString();
  }

  /**
   * 电压转换为温度
   */
  private static double voltageToTemperature(int power, int sensor) {
    double rVal = 1.0 / sensor * 10.0 * Math.abs(power - sensor);
    //System.out.println("---rVal:"+rVal+" power:"+power+" sensor:"+sensor);
    int len = TEMP_ARRAY.length;
    double temperature = 0;
    if (rVal > TEMP_ARRAY[0]) {
      temperature = 26.0;
    } else if (rVal < TEMP_ARRAY[len - 1]) {
      temperature = 26.0 + (len - 1) * 0.1;
    } else {
      for (int i = 1; i < len; ++i) {
        if (rVal > TEMP_ARRAY[i] && rVal <= TEMP_ARRAY[i - 1]) {
          temperature = 26.0 + 0.1 * (i - 1) + 0.1 * (TEMP_ARRAY[i - 1] - rVal) / (TEMP_ARRAY[i - 1]
              - TEMP_ARRAY[i]);
          break;
        }
      }
    }
    return temperature;
  }

  private static void reverse(byte[] validData) {
    for (int i = 0; i < validData.length / 2; i++) {
      byte temp = validData[i];
      validData[i] = validData[validData.length - i - 1];
      validData[validData.length - i - 1] = temp;
    }
  }
}
