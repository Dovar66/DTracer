package com.dovar.trace.plugin

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class DTracerTransform extends Transform {

    Project project
    DTracerExtension ext = null

    DTracerTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "DTracer"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        long ts = System.currentTimeMillis()
        if (ext == null) {
            ext = project.extensions.DTracer
        }
        if (ext == null || !ext.enable) {
            LogUtils.info("plugin disabled")
            TransformUtils.copy(transformInvocation.inputs, transformInvocation.outputProvider)
            return
        }

        transformInvocation.inputs.each { TransformInput input ->
            //jar
            input.jarInputs.each { JarInput jarInput ->
                String jarName = jarInput.name
                String filePath = jarInput.file.getAbsolutePath()
                String outputFileName = jarName.replace(".jar", "") + '-' + jarInput.file.path.hashCode()
                LogUtils.debug("jarInput: ${jarInput.name}  path: ${jarInput.file.path}")
                File output = transformInvocation.outputProvider.getContentLocation(outputFileName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (filePath.endsWith(".jar")
                        && !jarName.startsWith("com.android.support") && !jarName.startsWith("android.") && !jarName.startsWith("androidx.")//android相关
                        && !jarName.startsWith("org.jetbrains.kotlin")//kotlin相关
//                        && !jarName.startsWith("jetified")//被jetifier修改的第三方库
                ) {
                    handleJar(filePath, output)
                } else {
                    //将jar复制到output目录
                    FileUtils.copyFile(jarInput.file, output)
                }
            }
            //module的java代码
            input.directoryInputs.each { DirectoryInput dirInput ->
                final int start = dirInput.file.absolutePath.length() + 1
                final int end = SdkConstants.DOT_CLASS.length()
                //获取output目录
                def dest = transformInvocation.outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                LogUtils.debug("directoryInput: ${dirInput.file.absolutePath}  dest: ${dest.absolutePath}")
                File dir = dirInput.file
                if (dir) {
                    HashMap<String, File> modifyMap = new HashMap<>()
                    dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                        File classFile ->
                            File modified = modifyClassFile(dir, classFile, transformInvocation.context.getTemporaryDir())
                            if (modified != null) {
                                //key为相对路径
                                modifyMap.put(classFile.absolutePath.replace(dir.absolutePath, ""), modified)
                            }
                    }
                    FileUtils.copyDirectory(dirInput.file, dest)
                    modifyMap.entrySet().each {
                        Map.Entry<String, File> en ->
                            File target = new File(dest.absolutePath + en.getKey())
                            if (target.exists()) {
                                target.delete()
                            }
                            FileUtils.copyFile(en.getValue(), target)
                            en.getValue().delete()
                    }
                }
            }
        }
        LogUtils.info("total time: ${System.currentTimeMillis() - ts}")
    }

    /**
     * 目录文件中修改对应字节码
     */
    private File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        FileOutputStream outputStream = null
        try {
            String className = TransformUtils.path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            if (TransformUtils.isShouldModifyClass(className, ext)) {
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
                byte[] modifiedClassBytes = TransformUtils.modifyClasses(className, sourceClassBytes)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, className.replace('.', '') + '.class')
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    outputStream = new FileOutputStream(modified)
                    outputStream.write(modifiedClassBytes)
                }
            } else {
                return classFile
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close()
                }
            } catch (Exception e) {
            }
        }
        return modified

    }

//    private void processClass(ClassPool pool, String className, String outDir) {
//        CtClass c
//        try {
//            c = pool.getCtClass(className)
//        } catch (NotFoundException e) {
//            LogUtils.error("can not find class '${className}'!\n${e.getMessage()}")
//            return
//        }
//        if (c == null || !acceptCtClass(c)) return
//        if (ext.enableClassLog) {
//            LogUtils.info("process class '${c.name}'")
//        }
//        try {
//            if (c.isFrozen()) {
//                c.defrost()
//            }
//            // 所有方法和构造函数
//            c.declaredMethods.findAll { CtBehavior behavior ->
//                return acceptCtBehavior(behavior)
//            }.each { CtBehavior method ->
//                if (ext.enableClassLog && ext.enableMethodLog) {
//                    LogUtils.info("\tprocess method '${method.name}'")
//                }
//                String before = replaceVar(Constants.DEFAULT_METHOD_START, c, method)
//                String after = replaceVar(Constants.DEFAULT_METHOD_END, c, method)
//                if (!isEmpty(before)) {
//                    method.insertBefore(before)
//                }
//                if (!isEmpty(after)) {
//                    method.insertAfter(after)
//                }
//            }
//            c.writeFile(outDir)
//        } catch (CannotCompileException e) {
//            LogUtils.error("can not compile code ! \n${e.getMessage()}")
//        } catch (Exception e) {
//            LogUtils.error("process class '${className}' failed!")
//            if (ext.enableStackLog) {
//                e.printStackTrace()
//            }
//        } finally {
//            c.detach()
//        }
//    }

//    private boolean acceptCtClass(CtClass ctClass) {
//        if (ctClass==null||ctClass.isInterface()) {
//            return false
//        }
//        // 不处理Android生成的类
//        if (TransformUtils.isAndroidGeneratedClasses(ctClass)) {
//            return false
//        }
//        // 不处理Tracer自身所在的包。否则会导致App无法运行。
//        if (ctClass.name.startsWith(Constants.TRACER_PACKAGE)) {
//            return false
//        }
//        if (ctClass.name.contains("databinding")) return false
//        if (ext.processClassesRegex != null) {
//            return ctClass.name.matches(ext.processClassesRegex)
//        }
//        // 默认：处理所有类
//        return true
//    }

//    private boolean acceptCtBehavior(CtBehavior it) {
//        if (it.getMethodInfo().isStaticInitializer()) {
//            return false
//        }
//        if (it.isEmpty()) {
//            return false
//        }
//        // 跳过synthetic方法，例如AsyncTask会生成同名synthetic方法
//        if ((it.getModifiers() & AccessFlag.SYNTHETIC) != 0) {
//            return false
//        }
//        if ((it.getModifiers() & AccessFlag.ABSTRACT) != 0) {
//            return false
//        }
//        if ((it.getModifiers() & AccessFlag.NATIVE) != 0) {
//            return false
//        }
//        if ((it.getModifiers() & AccessFlag.INTERFACE) != 0) {
//            return false
//        }
//        if (ext.skipConstructor && it.methodInfo.isConstructor()) { // 跳过构造函数
//            return false
//        }
//        if (it instanceof CtConstructor && TransformUtils.isEmptyConstructor((CtConstructor) it)) {
//            // 跳过空构造函数
//            return false
//        }
//        if (ext.skipStaticMethod && (it.getModifiers() & AccessFlag.STATIC) != 0) { // 跳过静态方法
//            return false
//        }
//        if (ext.skipSimpleMethod && it instanceof CtMethod && isSimpleMethod((CtMethod) it)) {
//            // 跳过简单方法
//            return false
//        }
//        return true
//    }

//    private static String replaceVar(String s, CtClass c, CtBehavior m) {
//        if (s == null || s.length() == 0) {
//            return null
//        }
//        return s.replace(Constants.VAR_CLASS_NAME, c.name)
//                .replace(Constants.VAR_SIMPLE_CLASS_NAME, c.simpleName)
//                .replace(Constants.VAR_METHOD_NAME, m.name)
//    }
//
//    private boolean isSimpleMethod(CtMethod method) {
//        return TransformUtils.getMethodLength(method) < Math.max(ext.simpleMethodLength, TransformUtils.EMPTY_METHOD_LENGTH)
//    }

    private static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0
    }

    /**
     * 解压jar->处理class->压缩jar
     *
     * Q:为什么要一个个class解压出来立即处理而不是完全解压后再处理？
     * A:因为在windows/mac上默认文件名不区分大小写，当jar中存在 xxx/xx/A.class 与 xxx/xxx/a.class 时，完全解压整个jar会导致A.class或a.class文件被覆盖。
     */
    private File handleJar(String jarPath, File output) {
        JarFile inputJarFile = new JarFile(jarPath)
        Enumeration<JarEntry> jarEntries = inputJarFile.entries()
        JarOutputStream zipOutputStream = new JarOutputStream(new FileOutputStream(output.absolutePath))

        String tempClassDir = output.parentFile.absolutePath + File.separator + "temp_dtrace"
        File tempDir = new File(tempClassDir)
        tempDir.mkdirs()
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement()
            String entryName = jarEntry.getName()
            if (!entryName.endsWith('.class')) {
                continue
            }

            //解压出单个class文件
            String outFileName = tempClassDir + File.separator + entryName
            File outFile = new File(outFileName)
            outFile.getParentFile().mkdirs()
            InputStream inputStream = inputJarFile.getInputStream(jarEntry)
            FileOutputStream fileOutputStream = new FileOutputStream(outFile)

            //处理class
            String className = entryName.replace(File.separatorChar, '.'.charAt(0))
            byte[] modifiedClassBytes = null
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
            className = className.substring(0, className.lastIndexOf('.class'))
            if (TransformUtils.isShouldModifyClass(className, ext)) {
                modifiedClassBytes = TransformUtils.modifyClasses(className, sourceClassBytes)
            }
            if (modifiedClassBytes == null) {
                fileOutputStream.write(sourceClassBytes)
            } else {
                fileOutputStream.write(modifiedClassBytes)
            }
            fileOutputStream.close()
            inputStream.close()

            //压缩单个处理过的class文件到dest
            zipOutputStream.putNextEntry(new ZipEntry(entryName))
            if (!outFile.directory) {
                InputStream zipInputStream = new FileInputStream(outFile)
                zipOutputStream << zipInputStream
                zipInputStream.close()
            }

            //删掉已处理的文件
            FileUtils.deleteQuietly(outFile)
        }
        inputJarFile.close()
        zipOutputStream.close()
        FileUtils.deleteDirectory(tempDir)
        return null
    }
}