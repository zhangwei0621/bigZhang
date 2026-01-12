package com.study.googletranslatedemo.utils.adjust

import com.study.googletranslatedemo.utils.adjust.base.BaseAdjust

/**
 * 锐化
 */
class SharpenAdjust(onValueChanged: (Int) -> Unit) : BaseAdjust(
    minValue = 0,
    maxValue = 100,
    defaultValue = 0,
    rule = "sharpen",
    onValueChanged = onValueChanged
) {
    override fun buildCgeValue(): Float {
        return getValue() / 10f
    }
}