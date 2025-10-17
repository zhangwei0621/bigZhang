package com.study.openpdfdemo.viewer.data

data class PDFColorWrap(
    val r: Float, val g: Float, val b: Float
) {
    fun asFloatArray() = floatArrayOf(r, g, b)

    fun asInt(): Int {
        return -0x1000000 or
                ((r * 255.0f + 0.5f).toInt() shl 16) or
                ((g * 255.0f + 0.5f).toInt() shl 8) or (b * 255.0f + 0.5f).toInt()
    }
}