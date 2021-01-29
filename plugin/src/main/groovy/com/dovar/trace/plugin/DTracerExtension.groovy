package com.dovar.trace.plugin

class DTracerExtension {

    /**
     * 是否启用
     */
    Boolean enable = true
    /**
     * 要处理的类，正则表达式
     */
    String processClassesRegex = null

    // 跳过方法
    Boolean skipConstructor = false
    Boolean skipStaticMethod = false
    Boolean skipSimpleMethod = true

    /**
     * 简单方法的TableLength设置
     *
     * 实际测试得出的常见方法的TableLength
     * <pre>
     * public class Test {*     public Test() { // len = 3
     *}*
     *     public Test(String s) { // len = 4
     *         System.out.print(s);
     *}*
     *     public void empty() { / = 1
     *}*
     *     public void setField(String field) { // len = 2
     *         this.fieldeld;
     *}*
     *     public String getField() { // len = 1
     *         retueld;
     *}*
     *     public void func(String s) { // len = 2
     *         System.out.(s);
     *}*
     *     public void func2(String s) { // len = 2
     *         System.out.prin s);
     *}*
     *     public void func3(String s) { // len = 3
     *         String s1 = s + s;
     *         System.out.ps1);
     *}*}* </pre>
     */
    Integer simpleMethodLength = 1

    // log输出
    Boolean enableClassLog = true
    Boolean enableMethodLog = false
    Boolean enableStackLog = true
}
