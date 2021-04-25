package com.dovar.demo;

import android.app.Application;

import com.dovar.trace.DTracer;
import com.dovar.trace.impl.MainThreadTracer;

import kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInsResourceLoader;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        BuiltInsResourceLoader mLoader=null;
        MainThreadTracer tracer = MainThreadTracer.INSTANCE;
        tracer.setThreshold(-1);
        DTracer.setTracer(tracer);
        DTracer.startTracing();
    }
}
