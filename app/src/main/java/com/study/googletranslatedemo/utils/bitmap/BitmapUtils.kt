package com.study.googletranslatedemo.utils.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object BitmapUtils {
    fun uriToBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null
            return BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } catch (_: Exception) {
        } finally {
            inputStream?.close()
        }
        return null
    }

    fun Bitmap?.tryRecycle() {
        if (this != null && !this.isRecycled) {
            this.recycle()
        }
    }

    /**
     * 将bitmap保存为png图像文件
     */
    fun writeBitmapToPng(
        bitmap: Bitmap,
        path: String,
        quality: Int = 80
    ) {
        try {
            val file = File(path)
            if (file.exists()) {
                return
            }
            FileOutputStream(file).use { fos ->
                bitmap.compress(CompressFormat.PNG, quality, fos)
            }
        } catch (_: Exception) {
        }
    }
}