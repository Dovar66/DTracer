package com.dovar.demo;

import android.app.Application;

import com.dovar.trace.DTracer;
import com.dovar.trace.impl.MainThreadTracer;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MainThreadTracer tracer = MainThreadTracer.INSTANCE;
        tracer.setThreshold(5);
        DTracer.setTracer(tracer);
        DTracer.startTracing();
    }
}
