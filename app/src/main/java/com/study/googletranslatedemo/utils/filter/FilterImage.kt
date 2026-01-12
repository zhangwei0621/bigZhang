package com.study.googletranslatedemo.utils.filter

import android.graphics.Bitmap
import org.wysaid.nativePort.CGENativeLibrary

class FilterImage {
    // 原图
    private var srcBitmap: Bitmap? = null

    // 滤镜处理后图像
    private var dstBitmap: Bitmap? = null

    /**
     * 设置原图像
     */
    fun setSrcBitmap(bitmap: Bitmap) {
        srcBitmap?.recycle()
        srcBitmap = bitmap
    }

    /**
     * 应用Cge处理器
     * @return 参考[getDstBitmap]
     */
    fun applyProcessor(rule: String?): Bitmap? {
        srcBitmap?.let { bmp ->
            dstBitmap?.recycle()
            dstBitmap = if (!rule.isNullOrBlank()) {
                CGENativeLibrary.filterImage_MultipleEffects(bmp, rule, 1f)
            } else {
                null
            }
        }
        return getDstBitmap()
    }

    /**
     * 获取处理后的图像，如果没有则是原图
     */
    fun getDstBitmap() = dstBitmap ?: srcBitmap

    /**
     * 销毁数据
     */
    fun clear() {
        srcBitmap?.recycle()
        srcBitmap = null
        dstBitmap?.recycle()
        dstBitmap = null
    }
}