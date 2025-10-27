package com.study.openpdfdemo.core.pdfbox

import android.graphics.Bitmap
import com.study.openpdfdemo.core.IReader
import com.study.openpdfdemo.core.PageRect
import com.study.openpdfdemo.core.RenderOption
import com.study.openpdfdemo.core.TextExtractor
import com.study.openpdfdemo.viewer.text.extractor.PdfboxTextExtractor
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

class PDFBoxReader : IReader {
    private var _pdDocument: PDDocument? = null
    override fun load(file: File, password: String) {
        _pdDocument = PDDocument.load(file, password)
    }

    override fun close() {
        _pdDocument?.close()
    }

    override fun getPageSize(index: Int): PageRect {
        return _pdDocument?.getPage(index)?.let {
            val mediaBox = it.mediaBox
            PageRect(mediaBox.width, mediaBox.height)
        } ?: PageRect(0f, 0f)
    }

    override val pageCount: Int
        get() = _pdDocument?.numberOfPages ?: 0

    override fun createTextExtractor(): TextExtractor {
        val extractor = PdfboxTextExtractor()
        return object : TextExtractor {
            override fun extract(zeroBasePage: Int): List<WordLine> {
                val realPage = zeroBasePage + 1
                return extractor.extract(_pdDocument ?: return emptyList(), realPage)
            }
        }
    }

    override fun renderPage(
        file: File,
        pageIndex: Int,
        output: Bitmap,
        option: RenderOption
    ) {
        TODO("Not yet implemented")
    }
}