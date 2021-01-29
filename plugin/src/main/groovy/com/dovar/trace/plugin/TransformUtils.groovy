package com.dovar.trace.plugin


import com.android.build.api.transform.*
import javassist.ClassPool
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtMethod
import javassist.bytecode.CodeAttribute
import javassist.bytecode.LineNumberAttribute
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class TransformUtils {

    /**
     * 将所有class直接复制到目标文件夹，不做任何操作
     */
    static void copy(Collection<TransformInput> inputs, TransformOutputProvider outputProvider) {
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                // input = xxx/build/intermediates/classes/release
                // output = xxx/build/intermediates/transforms/TransformBuildConfig/release/folders/1/1/81a690789a26ea8c4e3e8a94e133cfa9c224f932
                String outputFileName = dirInput.name + '-' + dirInput.file.path.hashCode()
                File output = outputProvider.getContentLocation(outputFileName, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(dirInput.file, output)
//                LogUtils.debug("input = '${dirInput}', output = '${output}'")
            }

            input.jarInputs.each { JarInput jarInput ->
                String outputFileName = jarInput.name.replace(".jar", "") + '-' + jarInput.file.path.hashCode()
                File output = outputProvider.getContentLocation(outputFileName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, output)
//                LogUtils.debug("input = '${jarInput}', output = '${output}'")
            }
        }
    }

    /**
     * 将安卓和inputs加入ClassPool的ClassPath
     */
    static void appendClassPathToPool(ClassPool pool, Project project, Collection<TransformInput> inputs) {
        project.android.bootClasspath.each {
            pool.appendClassPath((String) it.absolutePath)
        }

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                pool.appendClassPath(dirInput.file.absolutePath)
            }
            input.jarInputs.each { JarInput jarInput ->
                pool.insertClassPath(jarInput.file.absolutePath)
            }
        }
    }

    /**
     * 是否为安卓生成的class
     */
    static boolean isAndroidGeneratedClasses(CtClass ctClass) {
        // com.package.demo.R
        // com.package.demo.R$layout
        // com.package.demo.R$id
        // com.package.demo.BuildConfig
        def cls = ctClass.simpleName
        return cls == 'R' || cls.startsWith('R$') || cls == 'BuildConfig'
    }

    public static final int EMPTY_METHOD_LENGTH = 2
    public static final int EMPTY_CONSTRUCTOR_LENGTH = 4

    static int getBehaviourLength(CtBehavior behavior) {
        CodeAttribute ca = behavior.getMethodInfo().getCodeAttribute()
        if (ca == null) return -1
        LineNumberAttribute info = (LineNumberAttribute) ca.getAttribute(LineNumberAttribute.tag)
        if (info == null) return -1
        return info.tableLength()
    }

    static int getConstructorLength(CtConstructor constructor) {
        return getBehaviourLength(constructor)
    }

    static int getMethodLength(CtMethod method) {
        return getBehaviourLength(method)
    }

    static boolean isEmptyMethod(CtMethod method) {
        return getMethodLength(method) < EMPTY_METHOD_LENGTH
    }

    static boolean isEmptyConstructor(CtConstructor constructor) {
        return getConstructorLength(constructor) < EMPTY_CONSTRUCTOR_LENGTH
    }
}
