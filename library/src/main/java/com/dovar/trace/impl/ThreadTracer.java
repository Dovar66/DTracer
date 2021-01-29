package com.dovar.trace.impl;

import android.os.Looper;
import android.os.SystemClock;

public class ThreadTracer extends BaseTracer {

    private final Looper mLooper;

    public ThreadTracer(Looper looper, int maxLevel) {
        super(maxLevel);
        mLooper = looper;
    }

    public ThreadTracer(Looper looper) {
        super();
        mLooper = looper;
    }

    @Override
    protected boolean checkMatchStart(String method) {
        return Looper.myLooper() == mLooper;
    }

    @Override
    protected boolean checkMatchEnd(String method) {
        return Looper.myLooper() == mLooper;
    }

    protected long timestamp() {
        return SystemClock.currentThreadTimeMillis();
    }
}