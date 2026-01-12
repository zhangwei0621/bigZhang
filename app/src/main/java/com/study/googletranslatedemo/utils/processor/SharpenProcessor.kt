package com.study.googletranslatedemo.utils.processor

import com.study.googletranslatedemo.utils.processor.base.BaseProcessor

/**
 * 锐化
 */
class SharpenProcessor(onValueChanged: (Int) -> Unit) : BaseProcessor(
    minValue = 0,
    maxValue = 100,
    defaultValue = 0,
    methodName = "adjust",
    paramName = "sharpen",
    onValueChanged = onValueChanged
) {
    override fun buildCgeValue(): Float {
        return getValue() / 10f
    }
}