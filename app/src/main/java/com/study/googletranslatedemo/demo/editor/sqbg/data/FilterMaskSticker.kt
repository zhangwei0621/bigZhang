package com.study.googletranslatedemo.demo.editor.sqbg.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.isValid
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.tryRecycle

/**
 * 形状遮罩贴纸
 */
class FilterMaskSticker(
    // 遮罩bitmap
    private val maskBitmap: Bitmap,
    // 边框bitmap
    private val frameBitmap: Bitmap?
) : BaseSticker() {
    // 遮罩画笔
    private var maskPaint: Paint = Paint().apply {
        isDither = true
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
    }

    // 边框画笔
    private var framePaint: Paint = Paint().apply {
        isDither = true
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    // 是否允许渲染边框
    var frameEnable: Boolean = true

    override fun draw(canvas: Canvas) {
        // 渲染遮罩层
        maskBitmap.takeIf { it.isValid() }?.let { bitmap ->
            canvas.drawBitmap(bitmap, transformMatrix, maskPaint)
        }

        // 渲染边框
        frameBitmap.takeIf { it.isValid() && frameEnable }?.let { bitmap ->
            canvas.drawBitmap(bitmap, transformMatrix, framePaint)
        }
    }

    override fun getType(): Int {
        return 1
    }

    override fun getHeight(): Int {
        return if (maskBitmap.isValid()) {
            maskBitmap.height
        } else {
            0
        }
    }

    override fun getWidth(): Int {
        return if (maskBitmap.isValid()) {
            maskBitmap.width
        } else {
            0
        }
    }

    override fun release() {
        maskBitmap.tryRecycle()
        frameBitmap.tryRecycle()
    }

    override fun clone(type: Int): BaseSticker {
        return this
    }
}