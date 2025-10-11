package com.study.openpdfdemo.utils

import android.content.Context
import android.util.Log
import com.lowagie.text.Document
import com.lowagie.text.exceptions.BadPasswordException
import com.lowagie.text.pdf.PdfCopy
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.lowagie.text.pdf.PdfWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * author ZhangWei
 * date 2025-10-09
 */
object PdfOps {
    const val TAG = "PdfOps"

    /**
     * 读取已有 PDF → 导出为加密的新文件
     */
    fun encryptPdfPlace(context: Context, inputFile: File, pwd: String) {
        runCatching {
            val tmp = File(inputFile.parentFile, "${inputFile.nameWithoutExtension}_enc.pdf")
            PdfReader(FileInputStream(inputFile)).use { reader ->
                PdfStamper(reader, FileOutputStream(tmp)).use { stamper ->
                    stamper.setEncryption(
                        pwd.toByteArray(),
                        pwd.toByteArray(),
                        PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
                        PdfWriter.ENCRYPTION_AES_128
                    )
                }
            }
            if (!inputFile.delete() || !tmp.renameTo(inputFile)) {
                tmp.delete()
            }
            Log.i(TAG, "加密成功")
            Tools.refreshMedia(context, inputFile)
        }.getOrElse {
            Log.i(TAG, "加密失败")
        }
    }


    /**
     * 解除加密（需要 ownerPwd），导出为不加密的新文件
     */
    fun decryptPdfReplace(context: Context, inputFile: File, pwd: String) {
        runCatching {
            val tmp = File(inputFile.parentFile, "${inputFile.nameWithoutExtension}_dec.pdf")
            PdfReader(FileInputStream(inputFile), pwd.toByteArray()).use { reader ->
                PdfStamper(reader, FileOutputStream(tmp)).use { }
            }
            if (!inputFile.delete() || !tmp.renameTo(inputFile)) {
                tmp.delete()
            }
            Log.i(TAG, "解密成功")
            Tools.refreshMedia(context, inputFile)
        }.getOrElse {
            Log.i(TAG, "解密失败")
        }
    }

    /**
     * 判断是否加密
     */
    fun isEncrypted(file: File): Boolean {
        return try {
            val reader = PdfReader(FileInputStream(file))
            val encrypted = reader.isEncrypted
            reader.close()
            encrypted
        } catch (_: BadPasswordException) {
            // 文件确实被加密，只是没提供密码
            true
        } catch (_: Exception) {
            // 其他错误，不认为是加密（可能是损坏）
            false
        }
    }

    /**
     * 校验口令是否正确（任一口令）
     */
    fun checkPassword(file: File, password: String): Boolean {
        return try {
            PdfReader(FileInputStream(file), password.toByteArray()).close()
            true
        } catch (_: BadPasswordException) {
            false
        }
    }

    /**
     * 合并多个 PDF
     */
    fun mergePdfs(
        context: Context,
        inputs: List<File>,
        output: File,
    ) = runCatching {
        val doc = Document()
        FileOutputStream(output).use { fos ->
            val copy = PdfCopy(doc, fos)
            doc.open()
            inputs.forEach { f ->
                PdfReader(FileInputStream(f)).use { r ->
                    for (i in 1..r.numberOfPages) {
                        val page = copy.getImportedPage(r, i)
                        copy.addPage(page)
                    }
                }
            }
            doc.close()
        }
        Log.i(TAG, "合并成功")
        Tools.refreshMedia(context, output)
    }.getOrElse {
        Log.i(TAG, "合并失败")
    }


    /**
     *选取n个随机页面拆分成新PDF
     */
    fun randomPageSplitPdf(
        context: Context,
        input: File,
        output: File,
    ) = runCatching {
        FileInputStream(input).use { fis ->
            PdfReader(fis).use {
                val total = it.numberOfPages
                val selected = (1..total)
                    .shuffled()
                    .take(minOf(3, total - 1))
                    .sorted()
                val doc = Document()
                FileOutputStream(output).use { fos ->
                    val copy = PdfCopy(doc, fos)
                    doc.open()
                    selected.forEach { page ->
                        val imported = copy.getImportedPage(it, page)
                        copy.addPage(imported)
                    }
                    doc.close()
                }
            }
        }
        Log.i(TAG, "拆分成功")
        Tools.refreshMedia(context, output)
    }.getOrElse {
        Log.i(TAG, "拆分失败")
    }


    private fun PdfReader.use(block: (PdfReader) -> Unit) {
        try {
            block(this)
        } finally {
            try {
                this.close()
            } catch (_: Throwable) {
            }
        }
    }

}