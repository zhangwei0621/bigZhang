package com.study.googletranslatedemo.utils.adjust

import com.study.googletranslatedemo.utils.adjust.base.BaseAdjust

/**
 * 饱和度
 */
class SaturationAdjust(onValueChanged: (Int) -> Unit) : BaseAdjust(
    minValue = 0,
    maxValue = 200,
    defaultValue = 100,
    rule = "saturation",
    onValueChanged = onValueChanged
)