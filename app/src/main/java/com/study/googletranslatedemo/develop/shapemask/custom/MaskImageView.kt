package com.study.googletranslatedemo.develop.shapemask.custom

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
import kotlin.math.min

/**
 * 遮罩ImageView。遮罩应该包含Alpha通道
 */
class MaskImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    // 遮罩bitmap
    private var _maskBitmap: Bitmap? = null

    val defaultMaskMode = PorterDuff.Mode.DST_OUT
    // 遮罩画笔
    private val _maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(defaultMaskMode)
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

    override fun onDraw(canvas: Canvas) {
        if (_maskBitmap == null) {
            super.onDraw(canvas)
            return
        }
        // 保存当前layer并新建一层layer
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 第二层：原图
        // 绘制原图
        super.onDraw(canvas)

        //绘制遮罩
        _maskBitmap?.let { bmp ->
            // 这里宽高比例算法应该和ImageView当前的缩放算法一致。或许可以通过矩阵实现适配
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
}