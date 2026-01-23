package com.study.googletranslatedemo.utils.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import androidx.core.graphics.scale
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.tryRecycle
import com.study.googletranslatedemo.utils.canvas.CanvasUtils.drawBitmapToCenter

/**
 * 图像宽高适配工具(按最小边比例计算)
 */
class BitmapFitDrawHelper {
    // 容器宽度
    private var _containerWidth: Int = 0

    // 容器高度
    private var _containerHeight: Int = 0

    // 源图像
    private var _srcBitmap: Bitmap? = null

    // 经过宽高适配后的图像
    private var _fitBitmap: Bitmap? = null

    /**
     * 设置源图像
     */
    fun setSrcBitmap(
        bitmap: Bitmap,
        containerWidth: Int = _containerWidth,
        containerHeight: Int = _containerHeight
    ): Boolean {
        _srcBitmap.tryRecycle()
        _srcBitmap = bitmap
        _containerWidth = containerWidth
        _containerHeight = containerHeight
        // 源图像变动需要强制适配
        return checkFitState(true)
    }

    /**
     * 设置容器大小
     */
    fun setContainerSize(
        containerWidth: Int = _containerWidth,
        containerHeight: Int = _containerHeight
    ): Boolean {
        _containerWidth = containerWidth
        _containerHeight = containerHeight
        return checkFitState(false)
    }

    /**
     * 渲染至容器中间
     */
    fun drawToCenter(canvas: Canvas, paint: Paint? = null) {
        canvas.drawBitmapToCenter(_fitBitmap, _containerWidth, _containerHeight, paint)
    }

    /**
     * 获取适配图像的大小
     */
    fun getFitSize(): Point? {
        _fitBitmap?.let { bitmap ->
            return Point(bitmap.width, bitmap.height)
        }
        return null
    }

    /**
     * 释放资源
     */
    fun release() {
        _fitBitmap.tryRecycle()
        _fitBitmap = null
        _srcBitmap.tryRecycle()
        _srcBitmap = null
        _containerWidth = 0
        _containerHeight = 0
    }

    /**
     * 检查适配状态，判断是否需要重新生成适配图像
     * @param forceCheck true=即使尺寸无变化也强制生成(前提是适配尺寸计算成功)
     */
    private fun checkFitState(
        forceCheck: Boolean
    ): Boolean {
        _srcBitmap?.let { src ->
            val (fitWidth, fitHeight) = BitmapUtils.calculateFitBitmap(
                src,
                _containerWidth,
                _containerHeight
            )
            val isFitValid = fitWidth > 0 && fitHeight > 0
            if (!isFitValid) return false
            val isFitChanged = fitWidth != _fitBitmap?.width || fitHeight != _fitBitmap?.height
            if (forceCheck || isFitChanged) {
                _fitBitmap.tryRecycle()
                _fitBitmap = src.scale(fitWidth, fitHeight)
                return true
            }
        }
        return false
    }
}