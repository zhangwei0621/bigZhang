package com.study.openpdfdemo.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * author ZhangWei
 * date 2025-10-11
 */
object Tools {
    /**
     * 刷新媒体文件
     */
    fun refreshMedia(
        context: Context,
        file: File
    ) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null
        ) { _: String?, _: Uri? ->

        }
    }

    fun copyPdfFromAssets(context: Context, assetName: String, forceOverwrite: Boolean): File {
        val file = File(context.cacheDir, assetName)
        if (!file.exists() || file.length() <= 0L || forceOverwrite) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }
}