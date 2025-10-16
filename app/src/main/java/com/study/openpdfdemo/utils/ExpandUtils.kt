package com.study.openpdfdemo.utils

import android.content.Context
import android.widget.Toast
import com.lowagie.text.Rectangle
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 将openpdf的矩形转为数组形式
 */
fun Rectangle.toQuad() = floatArrayOf(
    left, top,//左上
    right, top,//右上
    left, bottom,//左下
    right, bottom,//右下
)

fun String.toast(ctx: Context) {
    Toast.makeText(ctx, this, Toast.LENGTH_SHORT).show()
}

/**
 * 将文件复制到目标
 */
fun File.copyTo(destFile: File): Boolean {
    try {
        FileInputStream(this).use { fileInputStream ->
            FileOutputStream(destFile).use { fileOutputStream ->
                fileInputStream.channel.use { inputChannel ->
                    fileOutputStream.channel.use { outputChannel ->
                        outputChannel.transferFrom(
                            inputChannel,
                            0,
                            inputChannel.size()
                        )
                    }
                }
            }
        }
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

/**
 * 将该流写入目标文件中
 */
fun ByteArrayOutputStream.writeToFile(destFile: File): Boolean {
    try {
        val byteArray = this.toByteArray()
        FileOutputStream(destFile).use { fos ->
            fos.write(byteArray)
            fos.flush()
        }
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

/**
 * 判断两个矩形是否有交集。
 */
fun Rectangle.isCross(rect: Rectangle): Boolean {
    val disJoin = rect.right < this.left
            || rect.bottom > this.top
            || rect.left > this.right
            || rect.top < this.bottom
    return !disJoin
}

