package com.study.googletranslatedemo.demo.editor.adjust.processor

import com.study.googletranslatedemo.demo.editor.adjust.processor.base.BaseProcessor

/**
 * 对比度
 */
class ContrastProcessor(onValueChanged: (Int) -> Unit) : BaseProcessor(
    minValue = 25,
    maxValue = 200,
    defaultValue = 100,
    methodName = "adjust",
    paramName = "contrast",
    onValueChanged = onValueChanged
)