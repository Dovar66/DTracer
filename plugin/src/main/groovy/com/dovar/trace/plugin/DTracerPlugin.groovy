package com.dovar.trace.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class DTracerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('DTracer', DTracerExtension)
        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new DTracerTransform(project))
        LogUtils.info("DTracerPlugin Applied")
    }
}