package com.study.googletranslatedemo.utils.filter

import android.graphics.Bitmap
import org.wysaid.nativePort.CGENativeLibrary

object FilterImage {
    /**
     * 生成滤镜后bitmap。注意源图别整太大，不然会失败
     */
    // TODO: 预览推荐实现CGEImageHandler+GLSurfaceView
    fun applyFilter(src: Bitmap?, filter: String?, intensity: Float = 1f): Bitmap? {
        if (src == null) return null
        if (filter.isNullOrBlank()) return src
        val filterBitmap =
            CGENativeLibrary.filterImage_MultipleEffects(src, filter, intensity).apply {
                // 确保density一致
                density = src.density
            }
        return filterBitmap
    }
}