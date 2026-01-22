package com.study.googletranslatedemo.utils.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.isValid

object CanvasUtils {
    fun Canvas.drawBitmapToCenter(
        bitmap: Bitmap?,
        containerWidth: Int,
        containerHeight: Int
    ) {
        if (!bitmap.isValid()) return
        drawBitmap(
            bitmap!!,
            (containerWidth - bitmap.width) / 2f,
            (containerHeight - bitmap.height) / 2f,
            null
        )
    }
}