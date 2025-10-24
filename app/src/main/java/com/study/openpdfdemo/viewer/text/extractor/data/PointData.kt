package com.study.openpdfdemo.viewer.text.extractor.data

/**
 * PDF坐标的一个点。
 * ([x],[y])的原点在左下角，([sx],[sy])的原点在左上角，它们是同一个点，只是参考的原点不同。
 */
data class PointData(
    val x: Float,
    val y: Float,
    val sx: Float,
    val sy: Float
) {
    /**
     * 生成一个新的、经过缩放的点数据
     */
    fun newScaled(scale: Float): PointData {
        return PointData(
            x * scale,
            y * scale,
            sx * scale,
            sy * scale
        )
    }
}