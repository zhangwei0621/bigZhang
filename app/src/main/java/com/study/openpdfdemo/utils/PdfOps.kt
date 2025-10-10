package com.study.openpdfdemo.utils

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
    /**
     * 读取已有 PDF → 另存为加密版
     * @param encryption 建议先用 PdfWriter.ENCRYPTION_AES_128
     */
    fun encryptExistingPdf(
        src: File,
        dst: File,
        userPwd: String,
        ownerPwd: String,
        permissions: Int = PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
        encryption: Int = PdfWriter.ENCRYPTION_AES_128
    ) {
        ensureParent(dst)
        // 使用二进制路径，避免 URI 兼容性问题
        val reader = PdfReader(FileInputStream(src), null)
        FileOutputStream(dst).use { fos ->
            val stamper = PdfStamper(reader, fos)
            stamper.setEncryption(
                userPwd.toByteArray(),
                ownerPwd.toByteArray(),
                permissions,
                encryption
            )
            stamper.close()
        }
        reader.close()
    }

    /**
     * 解除加密（需要 ownerPwd），导出为不加密的新文件
     */
    fun decryptPdf(
        srcEncrypted: File,
        dstDecrypted: File,
        ownerPwd: String
    ) {
        ensureParent(dstDecrypted)
        val reader = PdfReader(FileInputStream(srcEncrypted), ownerPwd.toByteArray())
        FileOutputStream(dstDecrypted).use { fos ->
            val stamper = PdfStamper(reader, fos) // 不调用 setEncryption => 输出不加密
            stamper.close()
        }
        reader.close()
    }

    /**
     * 判断是否加密
     */
    fun isEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val reader = PdfReader(FileInputStream(file))
            val encrypted = reader.isEncrypted
            reader.close()
            encrypted
        } catch (e: BadPasswordException) {
            true // 文件确实被加密，只是没提供密码
        } catch (e: Exception) {
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

    // ============== 合并 / 拆分 ==============
    /**
     * 合并多个 PDF（仅页面层面，无渲染）
     */
    fun mergePdfs(inputs: List<File>, output: File, passwordIfAny: String? = null) {
        require(inputs.isNotEmpty())
        ensureParent(output)

        // 以第一页尺寸开文档（PdfCopy 会按页面自身尺寸处理，随意给一个即可）
        val firstReader = openReader(inputs.first(), passwordIfAny)
        val doc = Document(firstReader.getPageSizeWithRotation(1))
        FileOutputStream(output).use { fos ->
            val copy = PdfCopy(doc, fos)
            doc.open()
            firstReader.close() // 先关，再逐个读

            inputs.forEach { f ->
                openReader(f, passwordIfAny).use { r ->
                    val n = r.numberOfPages
                    for (i in 1..n) {
                        val page = copy.getImportedPage(r, i)
                        copy.addPage(page)
                    }
                }
            }
            doc.close()
        }
    }

    /**
     * 按页拆分：将指定 PDF 拆成多个单页 PDF
     * @param pages 需要导出的页码（1-based），为空表示拆成单页全集
     * @return 输出文件列表（与页码对应）
     */
    fun splitPdfByPages(
        src: File,
        outDir: File,
        pages: List<Int>? = null,
        passwordIfAny: String? = null,
        fileNameFactory: (pageIndex: Int) -> String = { i -> "page_$i.pdf" }
    ): List<File> {
        outDir.mkdirs()
        val files = mutableListOf<File>()

        openReader(src, passwordIfAny).use { reader ->
            val total = reader.numberOfPages
            val targetPages = (pages?.toSet()?.filter { it in 1..total } ?: (1..total).toList())
            targetPages.forEach { p ->
                val outFile = File(outDir, fileNameFactory(p))
                ensureParent(outFile)
                val doc = Document(reader.getPageSizeWithRotation(p))
                FileOutputStream(outFile).use { fos ->
                    val copy = PdfCopy(doc, fos)
                    doc.open()
                    copy.addPage(copy.getImportedPage(reader, p))
                    doc.close()
                }
                files.add(outFile)
            }
        }
        return files
    }

    // ============== 工具 ==============
    private fun openReader(file: File, passwordIfAny: String? = null): PdfReader {
        return if (passwordIfAny.isNullOrEmpty()) {
            PdfReader(FileInputStream(file))
        } else {
            PdfReader(FileInputStream(file), passwordIfAny.toByteArray())
        }
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

    private fun ensureParent(file: File) {
        file.parentFile?.mkdirs()
    }
}