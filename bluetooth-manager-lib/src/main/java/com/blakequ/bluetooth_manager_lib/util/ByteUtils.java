package com.blakequ.bluetooth_manager_lib.util;

import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class ByteUtils {

    /**
     * The Constant HEXES.
     */
    private static final String HEXES = "0123456789ABCDEF";

    private ByteUtils(){
        // TO AVOID INSTANTIATION
    }

    /**
     * Gets a pretty representation of a Byte Array as a HEX String.
     * <p>
     * Sample output: [01, 30, FF, AA]
     *
     * @param array the array
     * @return the string
     */
    public static String byteArrayToHexString(final byte[] array) {
        final StringBuilder sb = new StringBuilder();
        boolean firstEntry = true;
        sb.append('[');

        for (final byte b : array) {
            if (!firstEntry) {
                sb.append(", ");
            }
            sb.append(HEXES.charAt((b & 0xF0) >> 4));
            sb.append(HEXES.charAt((b & 0x0F)));
            firstEntry = false;
        }

        sb.append(']');
        return sb.toString();
    }

    /**
     * Checks to see if a byte array starts with another byte array.
     *
     * @param array  the array
     * @param prefix the prefix
     * @return true, if successful
     */
    public static boolean doesArrayBeginWith(final byte[] array, final byte[] prefix) {
        if (array.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Converts a byte array with a length of 2 into an int
     *
     * @param input the input
     * @return the int from the array
     */
    public static int getIntFrom2ByteArray(final byte[] input) {
        final byte[] result = new byte[4];

        result[0] = 0;
        result[1] = 0;
        result[2] = input[0];
        result[3] = input[1];

        return ByteUtils.getIntFromByteArray(result);
    }

    /**
     * Converts a byte to an int, preserving the sign.
     * <p>
     * For example, FF will be converted to 255 and not -1.
     *
     * @param bite the byte
     * @return the int from byte
     */
    public static int getIntFromByte(final byte bite) {
        return bite & 0xFF;
    }

    /**
     * Converts a byte array to an int.
     *
     * @param bytes the bytes
     * @return the int from byte array
     */
    public static int getIntFromByteArray(final byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * Converts a byte array to a long.
     *
     * @param bytes the bytes
     * @return the long from byte array
     */
    public static long getLongFromByteArray(final byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }


    /**
     * Inverts an byte array in place.
     *
     * @param array the array
     */
    public static void invertArray(final byte[] array) {
        final int size = array.length;
        byte temp;

        for (int i = 0; i < size / 2; i++) {
            temp = array[i];
            array[i] = array[size - 1 - i];
            array[size - 1 - i] = temp;
        }
    }

    /**
     * Returns a string composed from a {@link SparseArray}.
     */
    public static String toString(SparseArray<byte[]> array) {
        if (array == null) {
            return "null";
        }
        if (array.size() == 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        for (int i = 0; i < array.size(); ++i) {
            buffer.append(array.keyAt(i)).append("=").append(array.valueAt(i));
        }
        buffer.append('}');
        return buffer.toString();
    }


    /**
     * Returns a string composed from a {@link Map}.
     */
    public static <T> String toString(Map<T, byte[]> map) {
        if (map == null) {
            return "null";
        }
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        Iterator<Map.Entry<T, byte[]>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<T, byte[]> entry = it.next();
            Object key = entry.getKey();
            buffer.append(key).append("=").append(Arrays.toString(map.get(key)));
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }
}
