package com.dovar.trace.impl;

import android.text.TextUtils;
import android.util.Log;

import com.dovar.trace.ITracer;

import java.util.Arrays;

public abstract class BaseTracer implements ITracer {

    public final String TAG = getClass().getSimpleName();
    /**
     * 调用时间。直接用数组实现栈，减小对方法调用耗时的影响。
     */
    protected final long[] mTimes;
    /**
     * 方法名。直接用数组实现栈，减小对方法调用耗时的影响。
     */
    protected final String[] mNames;
    /**
     * 最大调用嵌套层级，也就是栈的容量。调用嵌套深度不能超过此数值，否则可能导致统计出错
     */
    private final int mMaxLevel;
    /**
     * 当前Level，相当于栈的指针
     */
    protected int mLevel = 0;
    /**
     * 方法执行超过多少ms输出Log
     */
    protected int mThreshold = 5;
    /**
     * 是否启动
     */
    protected boolean mEnable = false;

    /**
     * 要不要输出error
     */
    private boolean logError = true;

    private boolean spaceLevel = true;

    public BaseTracer(int maxLevel) {
        mMaxLevel = maxLevel;
        mTimes = new long[mMaxLevel];
        mNames = new String[mMaxLevel];
    }

    public BaseTracer() {
        this(40);
    }

    protected abstract boolean checkMatchStart(String method);

    protected abstract boolean checkMatchEnd(String method);

    /**
     * 获取时间戳
     */
    protected abstract long timestamp();

    /**
     * 方法执行超过多少ms输出Log
     */
    public void setThreshold(int threshold) {
        mThreshold = threshold;
    }

    @Override
    public void traceStart() {
        reset();
        mEnable = true;
    }

    @Override
    public void traceEnd() {
        mEnable = false;
    }

    public void reset() {
        mLevel = 0;
    }

    @Override
    public void methodStart(String method) {
        if (!mEnable || !checkMatchStart(method)) {
            return;
        }
        if (mLevel >= mMaxLevel) {
            err("max level[" + mMaxLevel + "] exceeded: " + method);
            return;
        }
        mTimes[mLevel] = timestamp(); // push
        mNames[mLevel] = method;
        ++mLevel;
    }

    @Override
    public void methodEnd(String method) {
        if (!mEnable || mLevel <= 0 || !checkMatchEnd(method)) {
            return;
        }
        if (!checkContains(method)) return;
        do {
            --mLevel;
            if (mLevel < 0) {
                mLevel = 0;
                return;
            }
        } while (!methodEquals(mNames[mLevel], method));//处理发生异常被捕获的场景
        long time = timestamp() - mTimes[mLevel]; // pop
        output(method, mLevel, time);
    }

    private boolean checkContains(String method) {
        if (TextUtils.isEmpty(method)) return false;
        for (String mName : mNames) {
            if (method.equals(mName)) return true;
        }
        return false;
    }

    /**
     * 判断两个方法是否相同。因为前面的包名往往都是一样的，从后往前匹配，能更早的检测到是否匹配失败。
     */
    private boolean methodEquals(String m1, String m2) {
        if (m1 == null || m2 == null) {
            return false;
        }
        final int length = m1.length();
        if (length != m2.length()) {
            return false;
        }
        for (int i = length - 1; i >= 0; i--) {
            if (m1.charAt(i) != m2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 方法耗时输出，可根据需要过滤。
     */
    protected void output(String method, int level, long time) {
        if (time > mThreshold) {
            log("%s%s: %d ms", space(level), method, time);
        }
    }

    protected void log(String msg, Object... args) {
        Log.w(TAG, String.format(msg, args));
    }

    protected void err(String msg, Object... args) {
        if (!logError) return;
        Log.e(TAG, String.format(msg, args));
    }

    private String space(int level) {
        if (!spaceLevel) return "level[" + level + "]\t";
        if (level <= 0) {
            return "";
        }
        char[] chars = new char[level];
        Arrays.fill(chars, '\t');
        return new String(chars);
    }

    public void setLogError(boolean enable) {
        logError = enable;
    }

    public void setSpaceLevel(boolean space) {
        spaceLevel = space;
    }
}