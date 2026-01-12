package com.study.googletranslatedemo.utils.filter

/**
 * 测试用的几个CGE滤镜规则
 * 语法请参考：https://github.com/wysaid/android-gpuimage-plus/wiki/Parsing-String-Rule-(ZH)
 */
object FilterRule {
    /**
     * 黑白
     */
    const val BLACK = "@adjust hsl 0 -1 0"

    /**
     * 素描
     */
    const val SKETCH = "@style sketch 1"

    /**
     * 模糊
     */
    const val BLUR = "@blur lerp 0.5 0"

    /**
     * 组合滤镜:黑白+模糊
     */
    const val BLACK_AND_BLUR = "$BLACK $BLUR"
}