package com.study.openpdfdemo.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import java.io.File

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
}