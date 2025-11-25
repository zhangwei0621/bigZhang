package com.study.openpdfdemo.core.openpdf

import android.util.Log
import com.lowagie.text.Document
import com.lowagie.text.exceptions.BadPasswordException
import com.lowagie.text.pdf.PdfAnnotation
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfBorderArray
import com.lowagie.text.pdf.PdfCopy
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.pdf.RandomAccessFileOrArray
import com.study.openpdfdemo.core.IEditWorkHandler
import com.study.openpdfdemo.core.IPdfEditor
import com.study.openpdfdemo.viewer.PdfCore.Companion.TAG
import com.study.openpdfdemo.viewer.data.PDFAnnot
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

class OpenPdfEditor : IPdfEditor {
    override fun encrypt(input: InputStream, pwd: String, os: OutputStream) {
        PdfReader(input).use { reader ->
            PdfStamper(reader, os).use { stamper ->
                stamper.setEncryption(
                    pwd.toByteArray(),
                    pwd.toByteArray(),
                    PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
                    PdfWriter.ENCRYPTION_AES_128
                )
            }
        }
    }

    override fun decrypt(file: File, pwd: String, os: OutputStream) {
        PdfReader(FileInputStream(file), pwd.toByteArray()).use { reader ->
            PdfStamper(reader, os)
        }
    }

    override fun merge(
        inputs: List<File>,
        output: OutputStream
    ) {
        val doc = Document()
        val copy = PdfCopy(doc, output)
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

    override fun isEncrypted(file: File): Boolean {
        return try {
            PdfReader(FileInputStream(file)).use { reader ->
                reader.isEncrypted
            }
        } catch (_: BadPasswordException) {
            // 文件确实被加密，只是没提供密码
            true
        } catch (_: Exception) {
            // 其他错误，不认为是加密（可能是损坏）
            false
        }
    }

    override fun shortWork(
        file: File,
        pwd: String,
        output: OutputStream,
        action: (IEditWorkHandler) -> Unit
    ) {
        Document.plainRandomAccess = true
        Log.d(TAG, "使用随机访问打开文件")
        val reader = PdfReader(
            RandomAccessFileOrArray(file.absolutePath),
            pwd.toByteArray()
        )
        Log.d(TAG, "打开文件成功")
        val stamper = PdfStamper(reader, output)
        //执行编辑工作
        action.invoke(OpenPdfEditWork(stamper))
        //关闭写入，数据开始写入到outputStream
        stamper.close()
        reader.close()
        Log.d(TAG, "写入完成")
    }

}

private class OpenPdfEditWork(
    private val stamper: PdfStamper
) : IEditWorkHandler {
    override fun addInkAnno(page: Int, annot: PDFAnnot.InkAnnotWrap, totalPageCount: Int) {
        try {
            if (page !in 1..totalPageCount) {
                throw Exception("页码($page)超出范围")
            }
            if (annot.line.isEmpty()) {
                throw Exception("Line为空")
            }
            val pdfAnnot = PdfAnnotation.createInk(
                stamper.writer,
                annot.rect,
                "",
                arrayOf(annot.line)
            ).apply {
                put(PdfName.C, PdfArray(annot.color.asFloatArray()))
                setBorder(PdfBorderArray(10f, 10f, annot.autoStrokeWidth()))
            }
            stamper.addAnnotation(pdfAnnot, page)
        } catch (e: Exception) {
            Log.e(TAG, "添加Ink注解错误：$e")
        }
    }

    override fun addMarkupAnno(
        page: Int,
        annot: PDFAnnot.MarkupAnnotWrap,
        totalPageCount: Int
    ) {
        try {
            if (page !in 1..totalPageCount) {
                throw Exception("页码($page)超出范围")
            }
            if (annot.markupType != PdfAnnotation.MARKUP_HIGHLIGHT
                && annot.markupType != PdfAnnotation.MARKUP_UNDERLINE
                && annot.markupType != PdfAnnotation.MARKUP_STRIKEOUT
            ) {
                throw Exception("Markup类型(${annot.markupType})错误")
            }
            if (annot.quadPoints.isEmpty()) {
                throw Exception("Quad数组为空")
            }
            val pdfAnnot = PdfAnnotation.createMarkup(
                stamper.writer,
                annot.rect,
                "",
                annot.markupType,
                annot.quadPoints
            ).apply {
                put(PdfName.C, PdfArray(annot.color.asFloatArray()))
            }
            stamper.addAnnotation(pdfAnnot, page)
        } catch (e: Exception) {
            Log.e(TAG, "添加Markup注解错误：$e")
        }
    }

}