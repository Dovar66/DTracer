package com.dovar.trace.plugin

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class TraceMethodVisitor extends MethodVisitor {

    String methodName
    String className

    public TraceMethodVisitor(int api, MethodVisitor methodVisitor, String name, String className) {
        super(api, methodVisitor)
        methodName = name
        this.className = className
    }


    /**方法的开始,即刚进入方法里面*/
    @Override
    public void visitCode() {
        mv.visitLdcInsn(className + ":" + methodName)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Constants.TRACER_CLASS, "methodStart", "(Ljava/lang/String;)V", false);
        super.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) {
            mv.visitLdcInsn(className + ":" + methodName)
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Constants.TRACER_CLASS, "methodEnd", "(Ljava/lang/String;)V", false);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitEnd() {
        mv.visitMaxs(6, 6);
        super.visitEnd();
    }
}