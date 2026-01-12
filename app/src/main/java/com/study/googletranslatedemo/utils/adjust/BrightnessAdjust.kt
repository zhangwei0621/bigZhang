package com.study.googletranslatedemo.utils.adjust

import com.study.googletranslatedemo.utils.adjust.base.BaseAdjust

/**
 * 亮度
 */
class BrightnessAdjust(onValueChanged: (Int) -> Unit) : BaseAdjust(
    minValue = -100,
    maxValue = 100,
    defaultValue = 0,
    rule = "brightness",
    onValueChanged = onValueChanged
)