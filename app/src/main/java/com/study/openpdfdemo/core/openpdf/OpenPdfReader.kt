package com.study.openpdfdemo.core.openpdf

import android.graphics.Bitmap
import com.lowagie.text.pdf.PdfReader
import com.study.openpdfdemo.core.IReader
import com.study.openpdfdemo.core.PageRect
import com.study.openpdfdemo.core.RenderOption
import com.study.openpdfdemo.core.TextExtractor
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import java.io.File

class OpenPdfReader : IReader {
    private var _reader: PdfReader? = null
    // TODO: 对文件大小有限制 大文件会OOM
    override fun load(file: File, password: String) {
        _reader = PdfReader(file.inputStream(), password.toByteArray())
    }

    override fun close() {
        _reader?.close()
    }

    override fun getPageSize(index: Int): PageRect {
        return PageRect(
            _reader?.getPageSize(index)?.width ?: 0f,
            _reader?.getPageSize(index)?.height ?: 0f
        )
    }

    override val pageCount: Int
        get() = _reader?.numberOfPages ?: 0

    override fun createTextExtractor(): TextExtractor {
        return object : TextExtractor {
            override fun extract(zeroBasePage: Int): List<WordLine> {
                TODO("Not yet implemented")
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