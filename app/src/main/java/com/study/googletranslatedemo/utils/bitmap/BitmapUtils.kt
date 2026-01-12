package com.study.googletranslatedemo.utils.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
//                inPremultiplied = false
            })
        } catch (_: Exception) {
        } finally {
            inputStream?.close()
        }
        return null
    }
}