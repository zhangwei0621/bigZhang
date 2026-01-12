package com.study.googletranslatedemo.utils.adjust

import com.study.googletranslatedemo.utils.adjust.base.BaseAdjust

/**
 * 模糊
 */
class BlurAdjust(onValueChanged: (Int) -> Unit) : BaseAdjust(
    minValue = 0,
    maxValue = 100,
    defaultValue = 0,
    rule = "blur",
    onValueChanged = onValueChanged
)