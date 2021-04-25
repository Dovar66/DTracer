package com.dovar.trace.plugin;

interface Constants {

    String VAR_CLASS_NAME = "<class-name>";
    String VAR_SIMPLE_CLASS_NAME = "<simple-class-name>";
    String VAR_METHOD_NAME = "<method-name>";

    String TRACER_PACKAGE = "com.dovar.trace";
    String DEFAULT_METHOD_START = "com.dovar.trace.DTracer.methodStart(\"<class-name>.<method-name>\");";
    String DEFAULT_METHOD_END = "com.dovar.trace.DTracer.methodEnd(\"<class-name>.<method-name>\");";
    String TRACER_CLASS = "com/dovar/trace/DTracer";

}
