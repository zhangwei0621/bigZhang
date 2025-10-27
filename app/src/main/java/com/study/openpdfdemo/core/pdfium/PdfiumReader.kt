package com.study.openpdfdemo.core.pdfium

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.ParcelFileDescriptor
import com.study.openpdfdemo.core.IReader
import com.study.openpdfdemo.core.PageRect
import com.study.openpdfdemo.core.RenderOption
import com.study.openpdfdemo.core.TextExtractor
import com.study.openpdfdemo.viewer.text.extractor.PdfiumTextExtractor
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import java.io.File

class PdfiumReader : IReader {
    private var _pdfiumCore = PdfiumCore()
    private var _doc: PdfDocument? = null
    private var _fd: ParcelFileDescriptor? = null
    private var _password = ""
    override fun load(file: File, password: String) {
        val fd = ParcelFileDescriptor(
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        )
        _password = password
        _fd = fd
        _doc = _pdfiumCore.newDocument(fd, password)
    }

    override fun close() {
        _fd?.close()
        _doc?.close()
    }

    override fun getPageSize(index: Int): PageRect {
        val page = _doc?.openPage(index)
        page?.getPageWidthPoint()
        return page?.let {
            PageRect(it.getPageWidthPoint().toFloat(), it.getPageHeightPoint().toFloat())
        } ?: PageRect(0f, 0f)
    }

    override val pageCount: Int
        get() = _doc?.getPageCount() ?: 0

    override fun createTextExtractor(): TextExtractor {
        val extractor = PdfiumTextExtractor()
        return object : TextExtractor {
            override fun extract(zeroBasePage: Int): List<WordLine> {
                return extractor.extract(_doc ?: return emptyList(), zeroBasePage)
            }
        }
    }

    override fun renderPage(
        file: File,
        pageIndex: Int,
        output: Bitmap,
        option: RenderOption
    ) {
        val fd = ParcelFileDescriptor(
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        )
        val doc = _pdfiumCore.newDocument(fd, _password)
        val pdfPage = doc.openPage(pageIndex)
        val matrix = option.matrix
        if (matrix != null) {
            pdfPage.renderPageBitmap(
                output,
                option.matrix,
                RectF(0f, 0f, output.width.toFloat(), output.height.toFloat()),
                option.renderAnnot
            )
        } else {
            pdfPage.renderPageBitmap(
                output,
                0,
                0,
                output.width,
                output.height,
                option.renderAnnot
            )
        }
        fd.close()
        pdfPage.close()
        doc.close()
    }
}