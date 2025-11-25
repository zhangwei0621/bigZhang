package com.study.openpdfdemo.core

import com.study.openpdfdemo.viewer.data.PDFAnnot
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface IPdfEditor {
    fun encrypt(input: InputStream, pwd: String, os: OutputStream)

    fun decrypt(file: File, pwd: String, os: OutputStream)

    fun merge(inputs: List<File>, output: OutputStream)

    fun isEncrypted(file: File): Boolean

    fun shortWork(file: File, pwd: String, output: OutputStream, action: (IEditWorkHandler) -> Unit)
}

interface IEditWorkHandler {
    fun addInkAnno(page: Int, annot: PDFAnnot.InkAnnotWrap, totalPageCount: Int)

    fun addMarkupAnno(page: Int, annot: PDFAnnot.MarkupAnnotWrap, totalPageCount: Int)
}