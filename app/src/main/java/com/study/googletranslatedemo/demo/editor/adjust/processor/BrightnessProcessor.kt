package com.study.googletranslatedemo.demo.editor.adjust.processor

import com.study.googletranslatedemo.demo.editor.adjust.processor.base.BaseProcessor

/**
 * 亮度
 */
class BrightnessProcessor(onValueChanged: (Int) -> Unit) : BaseProcessor(
    minValue = -100,
    maxValue = 100,
    defaultValue = 0,
    methodName = "adjust",
    paramName = "brightness",
    onValueChanged = onValueChanged
)