package com.study.googletranslatedemo.utils

import android.content.Context
import android.graphics.Bitmap
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import java.io.File

object FileUtils {
    fun saveBitmap(
        context: Context,
        subfix: String,
        bitmap: Bitmap
    ) {
        val dir = File(context.filesDir, "output").apply { mkdirs() }
        val file = File(dir, "${subfix}_${System.currentTimeMillis()}.png")
        BitmapUtils.writeBitmapToPng(bitmap, file.absolutePath)
    }
}