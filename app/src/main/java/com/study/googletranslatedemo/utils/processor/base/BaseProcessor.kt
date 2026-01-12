package com.study.googletranslatedemo.utils.processor.base

/**
 * 语法："@method arg1 arg2.."，可以参考滤镜的写法。
 * 如果默认实现无法满足，可以尝试重写[buildProcessor]等方法
 */
abstract class BaseProcessor(
    val minValue: Int,
    val maxValue: Int,
    val defaultValue: Int,
    val methodName: String,
    val paramName: String,
    private val onValueChanged: (Int) -> Unit
) {
    protected var current = defaultValue

    fun setValue(value: Int) {
        current = value.coerceIn(minValue, maxValue)
        onValueChanged(getValue())
    }

    fun getValue(): Int {
        return current
    }

    fun reset() {
        setValue(defaultValue)
    }

    /**
     * 构建符合Cge语法的值，请综合界面步长要求和语法要求的取值范围进行计算
     */
    open fun buildCgeValue(): Float {
        return getValue() / 100f
    }

    /**
     * 构建参数
     */
    open fun buildParam(): String {
        return "$paramName ${buildCgeValue()}"
    }

    /**
     * 构建处理器
     */
    open fun buildProcessor(): String {
        return "@$methodName ${buildParam()}}"
    }
}