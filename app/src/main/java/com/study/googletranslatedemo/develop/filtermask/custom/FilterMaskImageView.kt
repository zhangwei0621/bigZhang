package com.study.googletranslatedemo.develop.filtermask.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.tryRecycle
import com.study.googletranslatedemo.utils.filter.FilterRule
import org.wysaid.nativePort.CGENativeLibrary
import kotlin.math.min

/**
 * 遮罩ImageView，支持滤镜。遮罩应该包含Alpha通道
 */
class FilterMaskImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    // 原图bitmap
    private var _bitmap: Bitmap? = null

    // 遮罩bitmap
    private var _maskBitmap: Bitmap? = null

    // 滤镜
    private var _filter: String? = FilterRule.SKETCH

    // 滤镜处理后的bitmap
    private var _filterBitmap: Bitmap? = null

    val defaultMaskMode = PorterDuff.Mode.DST_OUT

    // 遮罩画笔
    private val _maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(defaultMaskMode)
    }

    fun setFilter(filter: String) {
        _filter = filter
        applyFilter()
    }

    /**
     * 设置遮罩bitmap
     */
    fun setMaskBitmap(maskBitmap: Bitmap?) {
        _maskBitmap = maskBitmap
        invalidate()
    }

    /**
     * 设置遮罩模式
     */
    fun setMaskMode(mode: PorterDuff.Mode) {
        _maskPaint.xfermode = PorterDuffXfermode(mode)
        invalidate()
    }

    /**
     * 导出当前图像
     */
    fun export(): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        _bitmap = bm
        applyFilter()
    }

    override fun onDraw(canvas: Canvas) {
        if (_maskBitmap == null || _filterBitmap == null) {
            super.onDraw(canvas)
            return
        }
        // 绘制原图作为背景
        super.onDraw(canvas)
        // 保存当前layer并新建一层layer
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        //绘制滤镜处理后的图像
        _filterBitmap?.let { bmp ->
            // 这里宽高比例算法应该和ImageView当前的缩放算法一致。或许可以通过矩阵实现适配
            val minScale = min(width * 1f / bmp.width, height * 1f / bmp.height)
            val scaledBitmap =
                bmp.scale((bmp.width * minScale).toInt(), (bmp.height * minScale).toInt())
            // 居中
            canvas.drawBitmap(
                scaledBitmap,
                (width - scaledBitmap.width) / 2f,
                (height - scaledBitmap.height) / 2f,
                null
            )
            if (!scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }

        //绘制遮罩
        _maskBitmap?.let { bmp ->
            // 这里宽高比例算法应该和ImageView当前的缩放算法一致。或许可以通过矩阵实现适配
            imageMatrix
            val minScale = min(width * 1f / bmp.width, height * 1f / bmp.height)
            val scaledBitmap =
                bmp.scale((bmp.width * minScale).toInt(), (bmp.height * minScale).toInt())
            // 居中
            canvas.drawBitmap(
                scaledBitmap,
                (width - scaledBitmap.width) / 2f,
                (height - scaledBitmap.height) / 2f,
                _maskPaint
            )
            if (!scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }

        // 合并图层
        canvas.restoreToCount(layerId)
    }

    private fun applyFilter() {
        val filter = _filter
        if (filter.isNullOrBlank()) return
        _bitmap?.let { bmp ->
            _filterBitmap.tryRecycle()
            _filterBitmap = CGENativeLibrary.filterImage_MultipleEffects(bmp, filter, 1f)
            invalidate()
        }
    }
}