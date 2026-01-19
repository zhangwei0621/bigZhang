package com.study.googletranslatedemo.demo.editor.adjust.processor

import com.study.googletranslatedemo.demo.editor.adjust.processor.base.BaseProcessor

/**
 * 模糊。由于adjust方法的模糊强度有限(拉满了也没多模糊)，这里采用专门的模糊方法
 */
class BlurProcessor(onValueChanged: (Int) -> Unit) : BaseProcessor(
    minValue = 0,
    maxValue = 100,
    defaultValue = 0,
    methodName = "blur",
    paramName = "lerp",
    onValueChanged = onValueChanged
) {
    override fun buildParam(): String {
        // lerp 强度[0,1] 基数[0.6,2.0]
        return "$paramName ${buildCgeValue()} 1"
    }
}