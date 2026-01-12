package com.study.googletranslatedemo.utils.adjust

import com.study.googletranslatedemo.utils.adjust.base.BaseAdjust

/**
 * 对比度
 */
class ContrastAdjust(onValueChanged: (Int) -> Unit) : BaseAdjust(
    minValue = 25,
    maxValue = 200,
    defaultValue = 100,
    rule = "contrast",
    onValueChanged = onValueChanged
)