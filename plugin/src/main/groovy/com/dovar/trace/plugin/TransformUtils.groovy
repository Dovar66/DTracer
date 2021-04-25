package com.dovar.trace.plugin

import com.android.build.api.transform.*
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class TransformUtils {

    static byte[] modifyClasses(String className, byte[] srcByteCode) {
        byte[] classBytesCode = null
        try {
            LogUtils.debug("modifyClass: $className")

            classBytesCode = modifyClass(srcByteCode)
            //调试模式下再遍历一遍看修改的方法情况
//            if (Logger.isDebug()) {
//                seeModifyMethod(classBytesCode)
//            }
            return classBytesCode
        } catch (Exception e) {
            e.printStackTrace()
        }
        if (classBytesCode == null) {
            classBytesCode = srcByteCode
        }
        return classBytesCode
    }
    /**
     * 真正修改类中方法字节码
     */
    private static byte[] modifyClass(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor adapter = new TraceClassVisitor(classWriter)
        ClassReader cr = new ClassReader(srcClass)
        cr.accept(adapter, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }
    /**
     * 查看修改字节码后的方法
     */
    private static void seeModifyMethod(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor visitor = new TraceClassVisitor(classWriter)
        visitor.seeModifyMethod = true
        ClassReader cr = new ClassReader(srcClass)
        cr.accept(visitor, ClassReader.EXPAND_FRAMES)
    }

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
//    static void appendClassPathToPool(ClassPool pool, Project project, Collection<TransformInput> inputs) {
//        project.android.bootClasspath.each {
//            pool.appendClassPath((String) it.absolutePath)
//        }
//
//        inputs.each { TransformInput input ->
//            input.directoryInputs.each { DirectoryInput dirInput ->
//                pool.appendClassPath(dirInput.file.absolutePath)
//            }
//            input.jarInputs.each { JarInput jarInput ->
//                pool.insertClassPath(jarInput.file.absolutePath)
//            }
//        }
//    }

    /**
     * 是否为安卓生成的class
     */
//    static boolean isAndroidGeneratedClasses(CtClass ctClass) {
//        // com.package.demo.R
//        // com.package.demo.R$layout
//        // com.package.demo.R$id
//        // com.package.demo.BuildConfig
//        def cls = ctClass.simpleName
//        return cls == 'R' || cls.startsWith('R$') || cls == 'BuildConfig'
//    }

    public static final int EMPTY_METHOD_LENGTH = 2
    public static final int EMPTY_CONSTRUCTOR_LENGTH = 4

//    static int getBehaviourLength(CtBehavior behavior) {
//        CodeAttribute ca = behavior.getMethodInfo().getCodeAttribute()
//        if (ca == null) return -1
//        LineNumberAttribute info = (LineNumberAttribute) ca.getAttribute(LineNumberAttribute.tag)
//        if (info == null) return -1
//        return info.tableLength()
//    }
//
//    static int getConstructorLength(CtConstructor constructor) {
//        return getBehaviourLength(constructor)
//    }
//
//    static int getMethodLength(CtMethod method) {
//        return getBehaviourLength(method)
//    }
//
//    static boolean isEmptyMethod(CtMethod method) {
//        return getMethodLength(method) < EMPTY_METHOD_LENGTH
//    }
//
//    static boolean isEmptyConstructor(CtConstructor constructor) {
//        return getConstructorLength(constructor) < EMPTY_CONSTRUCTOR_LENGTH
//    }

    /**
     * 是否对扫描类进行修改
     *
     * @param className 扫描到的类名
     * @param exclude 过滤掉的类
     */
    static boolean isShouldModifyClass(String className, DTracerExtension ext) {
        if (className.contains('R$') ||
                className.contains('R2$') ||
                className.endsWith('R') ||
                className.endsWith('R2') ||
                className.endsWith('BuildConfig')) {
            return false
        }

        // 不处理Tracer自身所在的包。否则会导致App无法运行。
        if (className.startsWith(Constants.TRACER_PACKAGE)) {
            return false
        }

        if (ext.processPackages != null) {
            boolean res = false
            ext.processPackages.each { pkg ->
                if (className.startsWith(pkg)) {
                    res = true
                    return //打断闭包
                }
            }
            return res
        }

        if (ext.processClassesRegex != null) {
            return className.matches(ext.processClassesRegex)
        }
        return true
    }

    static String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }

    public static HashMap<Integer, String> accCodeMap = new HashMap<>()

    static String accCode2String(int access) {
        def builder = new StringBuilder()
        def map = getAccCodeMap()
        map.entrySet().each {
            entry ->
                if ((entry.getKey().intValue() & access) > 0) {
                    //此处如果使用|作为分隔符会导致编译报错 因此改用斜杠
                    builder.append('\\' + entry.getValue() + '/ ')
                }
        }
        return builder.toString()
    }

    static Map<Integer, String> getAccCodeMap() {
        if (accCodeMap.size() == 0) {
            HashMap<String, Integer> map = new HashMap<>()
            map.put("ACC_PUBLIC", 1)
            map.put("ACC_PRIVATE", 2)
            map.put("ACC_PROTECTED", 4)
            map.put("ACC_STATIC", 8)
            map.put("ACC_FINAL", 16)
            map.put("ACC_SUPER", 32)
            map.put("ACC_SYNCHRONIZED", 32)
            map.put("ACC_VOLATILE", 64)
            map.put("ACC_BRIDGE", 64)
            map.put("ACC_VARARGS", 128)
            map.put("ACC_TRANSIENT", 128)
            map.put("ACC_NATIVE", 256)
            map.put("ACC_INTERFACE", 512)
            map.put("ACC_ABSTRACT", 1024)
            map.put("ACC_STRICT", 2048)
            map.put("ACC_SYNTHETIC", 4096)
            map.put("ACC_ANNOTATION", 8192)
            map.put("ACC_ENUM", 16384)
            map.put("ACC_DEPRECATED", 131072)
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                accCodeMap.put(entry.getValue(), entry.getKey())
            }
        }
        return accCodeMap
    }
}
