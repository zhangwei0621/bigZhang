package com.study.googletranslatedemo.utils.adjust.base

abstract class BaseAdjust(
    val minValue: Int,
    val maxValue: Int,
    val defaultValue: Int,
    val rule: String,
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
     * 构建Cge方法块
     */
    open fun buildMethod(): String {
        return "$rule ${buildCgeValue()}"
    }

    /**
     * 构建Cge规则块
     */
    open fun buildRule(): String {
        return "@adjust ${buildMethod()}"
    }
}