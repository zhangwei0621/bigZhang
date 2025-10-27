package com.study.openpdfdemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
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

context(ctx: Context)
fun String.toast() {
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
 * 将文件复制为[ByteArrayOutputStream]
 */
fun File.copyAsByteArrayOS(): ByteArrayOutputStream {
    val result = ByteArrayOutputStream()
    val fis = FileInputStream(this)
    result.write(fis.readBytes())
    return result
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
 * 判断两个矩形是否有交集
 */
fun Rectangle.isCross(target: Rectangle?): Boolean {
    if (target == null) {
        return false
    }
    val disJoin = target.right < this.left
            || target.bottom > this.top
            || target.left > this.right
            || target.top < this.bottom
    return !disJoin
}

/**
 * 判断两个矩形在Y轴方向是否具有交集
 */
fun Rectangle.isInYRange(target: Rectangle?): Boolean {
    if (target == null) {
        return false
    }
    val isOuterYRange = target.bottom > this.top
            || target.top < this.bottom
    return !isOuterYRange
}

fun Bitmap?.tryRecycle() {
    if (this != null && !this.isRecycled) {
        this.recycle()
    }
}

fun RectF.isZero(): Boolean {
    return left == 0f
            && right == 0f
            && top == 0f
            && bottom == 0f
}
