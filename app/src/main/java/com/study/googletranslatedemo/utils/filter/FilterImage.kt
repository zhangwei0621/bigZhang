package com.study.googletranslatedemo.utils.filter

import android.graphics.Bitmap
import org.wysaid.nativePort.CGENativeLibrary

object FilterImage {
    // TODO: 预览推荐实现CGEImageHandler+GLSurfaceView
    fun applyFilter(src: Bitmap?, filter: String?, intensity: Float = 1f): Bitmap? {
        if (src == null) return null
        if (filter.isNullOrBlank()) return src
        return CGENativeLibrary.filterImage_MultipleEffects(src, filter, intensity)
    }
}