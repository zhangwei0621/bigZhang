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

    /**
     * 纹理混合
     */
    const val EFFECT_BLEND_1 = "@krblend mix effect_1.jpg 75"
    const val EFFECT_BLEND_2 = "@krblend mix hehe.jpg 75"

    /**
     * lut滤镜
     */
    const val LUT_1 = "@adjust lut edgy_amber.png"
    const val LUT_2 = "@adjust lut filmstock.png"
    const val LUT_3 = "@adjust lut foggy_night.png"
    const val LUT_4 = "@adjust lut late_sunset.png"
    const val LUT_5 = "@adjust lut soft_warming.png"
    const val LUT_6 = "@adjust lut wildbird.png"
}