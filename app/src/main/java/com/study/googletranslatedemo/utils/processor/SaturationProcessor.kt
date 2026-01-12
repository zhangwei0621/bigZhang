package com.study.googletranslatedemo.utils.processor

import com.study.googletranslatedemo.utils.processor.base.BaseProcessor

/**
 * 饱和度
 */
class SaturationProcessor(onValueChanged: (Int) -> Unit) : BaseProcessor(
    minValue = 0,
    maxValue = 200,
    defaultValue = 100,
    methodName = "adjust",
    paramName = "saturation",
    onValueChanged = onValueChanged
)