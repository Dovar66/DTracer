package com.dovar.trace.impl;

import android.os.Looper;

public class MainThreadTracer extends ThreadTracer {

    public static final MainThreadTracer INSTANCE = new MainThreadTracer();

    public MainThreadTracer(int maxLevel) {
        super(Looper.getMainLooper(), maxLevel);
    }

    public MainThreadTracer() {
        super(Looper.getMainLooper());
    }
}