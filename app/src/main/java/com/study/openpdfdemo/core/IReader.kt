package com.study.openpdfdemo.core

import android.graphics.Bitmap
import android.graphics.Matrix
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import java.io.File

interface IReader {
    fun load(file: File, password: String)

    fun close()

    fun getPageSize(index: Int): PageRect

    val pageCount: Int

    fun createTextExtractor(): TextExtractor

    fun renderPage(file: File, pageIndex: Int, output: Bitmap, option: RenderOption = RenderOption())
}

class RenderOption(
    val renderAnnot: Boolean = true,
    val matrix: Matrix? = null,
)

interface TextExtractor {
    fun extract(zeroBasePage: Int): List<WordLine>
}

class PageRect(
    val width: Float,
    val height: Float,
)