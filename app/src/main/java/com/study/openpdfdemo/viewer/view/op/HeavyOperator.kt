package com.study.openpdfdemo.viewer.view.op

import android.content.Context
import com.study.openpdfdemo.viewer.PdfCore
import com.study.openpdfdemo.viewer.tool.PdfCoreListener
import com.study.openpdfdemo.viewer.view.PdfOperator
import com.study.openpdfdemo.viewer.view.PdfView
import com.study.openpdfdemo.viewer.view.reader.PdfReaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class HeavyOperator(
    private val context: Context,
    private val pdfReaderView: () -> PdfReaderView?
): PdfOperator {
    private val _pdfReaderView: PdfReaderView?
        get() = pdfReaderView()
    private var _pdfCore: PdfCore? = null
    override val enablePageOverlay: Boolean = true
    override var fitScale: Float = 1f

    override suspend fun initCore(file: File, password: String, listener: PdfCoreListener): PdfCore {
        val pdfCore = withContext(Dispatchers.Default) {
            // TODO: 核心工具使用多种不同的PDF库，导致构造时很卡，界面可以考虑做个loading动画
            PdfCore(file, PdfCore.getCacheDir(context), context = context,
                buildPreviewFile = true, password)
        }
        pdfCore.setCoreListener(listener)
        _pdfCore = pdfCore
        return pdfCore
    }

    override fun undo() {
        if (_pdfCore?.undo() == true) {
            _pdfReaderView?.redraw()
        }
    }

    override fun redo() {
        if (_pdfCore?.redo() == true) {
            _pdfReaderView?.redraw()
        }
    }

    override fun save() {
        _pdfCore?.save()
    }

    override fun abandonSave() {
        _pdfCore?.abandonSave()
    }

    override fun attachToPdfView(pdfView: PdfView) {

    }
}