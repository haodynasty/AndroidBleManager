package com.blakequ.bluetooth_manager_lib.util;

import java.util.LinkedHashMap;

public class LimitedLinkHashMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -5375660288461724925L;

    private final int mMaxSize;

    public LimitedLinkHashMap(final int maxSize) {
        super(maxSize + 1, 1, false);
        mMaxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(final Entry<K, V> eldest) {
        return this.size() > mMaxSize;
    }
}
