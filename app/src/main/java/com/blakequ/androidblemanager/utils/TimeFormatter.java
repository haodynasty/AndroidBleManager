package com.blakequ.androidblemanager.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormatter {
    private final static String FORMAT = "yyyy-MM-dd HH:mm:ss zzz";
    private final static SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT, Locale.CHINA);

    public static String getIsoDateTime(final Date date) {
        return FORMATTER.format(date);
    }

    public static String getIsoDateTime(final long millis) {
        return getIsoDateTime(new Date(millis));
    }
}
