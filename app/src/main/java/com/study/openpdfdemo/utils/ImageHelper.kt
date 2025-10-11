package com.study.openpdfdemo.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

object ImageHelper {
    /**
     * 将字节数组转换为bitmap
     */
    fun byteImageCovert(source: ByteArray): Bitmap? {
        try {
            if (source.isEmpty()) {
                return null
            }
            val result = BitmapFactory.decodeByteArray(
                source,
                0,
                source.size,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inScaled = false
                }
            )
            return result
        } catch (e: Exception) {

        }
        return null
    }
}