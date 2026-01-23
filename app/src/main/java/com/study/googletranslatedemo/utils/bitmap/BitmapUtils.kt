package com.study.googletranslatedemo.utils.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object BitmapUtils {
    private const val TAG = "BitmapUtils"

    @Deprecated("推荐优先使用 smartUriToBitmap")
    fun uriToBitmap(
        context: Context,
        uri: Uri,
        density: Int = 240,
        targetDensity: Int = 120
    ): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null
            return BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                inDensity = density
                inTargetDensity = targetDensity
            })
        } catch (_: Exception) {
        } finally {
            inputStream?.close()
        }
        return null
    }

    /**
     * 从Uri加载一个bitmap，并智能控制其尺寸
     */
    fun smartUriToBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? {
        var inputStream: InputStream? = null
        var measureIs: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            measureIs = context.contentResolver.openInputStream(uri)
            if (inputStream == null || measureIs == null) return null

            val measureOption = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(measureIs, null, measureOption)

            val originalWidth = measureOption.outWidth
            val originalHeight = measureOption.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return null
            val deviceDpi = context.resources.displayMetrics.densityDpi
            val deviceWidth = context.resources.displayMetrics.widthPixels
            val deviceHeight = context.resources.displayMetrics.heightPixels
            val minScale = min(deviceWidth * 1f / originalWidth, deviceHeight * 1f / originalHeight)

            return BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                inDensity = deviceDpi
                inTargetDensity = (deviceDpi * minScale).toInt()
            })
        } catch (e: Exception) {
            Log.e(TAG, "smartUriToBitmap error: $e")
        } finally {
            measureIs?.close()
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
     * 校验bitmap是否有效
     */
    fun Bitmap?.isValid(): Boolean {
        if (this == null) return false
        if (this.isRecycled) return false
        if (this.width <= 0 || this.height <= 0) return false
        return true
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

    /**
     * 计算宽高适配后的bitmap的宽高参数
     * @return <Width, Height>
     */
    fun calculateFitBitmap(
        srcBitmap: Bitmap?,
        containerWidth: Int,
        containerHeight: Int
    ): Pair<Int, Int> {
        val invalidResult = 0 to 0
        if (!srcBitmap.isValid()) return invalidResult
        if (containerWidth <= 0 || containerHeight <= 0) return invalidResult
        if (srcBitmap == null) return invalidResult
        val minScale =
            min(containerWidth * 1f / srcBitmap.width, containerHeight * 1f / srcBitmap.height)
        return Pair(
            (srcBitmap.width * minScale).toInt(),
            (srcBitmap.height * minScale).toInt()
        )
    }

    /**
     * 生成宽高适配的新bitmap
     */
    fun createFitBitmap(
        srcBitmap: Bitmap?,
        containerWidth: Int,
        containerHeight: Int
    ): Bitmap? {
        if (!srcBitmap.isValid()) return null
        if (containerWidth <= 0 || containerHeight <= 0) return null
        val minScale =
            min(containerWidth * 1f / srcBitmap!!.width, containerHeight * 1f / srcBitmap.height)
        val scaledBitmap =
            srcBitmap.scale(
                (srcBitmap.width * minScale).toInt(),
                (srcBitmap.height * minScale).toInt()
            )
        return scaledBitmap
    }
}