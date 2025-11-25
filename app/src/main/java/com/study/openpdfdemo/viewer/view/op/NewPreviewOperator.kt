package com.study.openpdfdemo.viewer.view.op

import android.content.Context
import android.view.ViewGroup
import com.study.openpdfdemo.viewer.PdfCore
import com.study.openpdfdemo.viewer.data.PageState
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import com.study.openpdfdemo.viewer.tool.PdfCoreListener
import com.study.openpdfdemo.viewer.view.PdfOperator
import com.study.openpdfdemo.viewer.view.PdfView
import com.study.openpdfdemo.viewer.view.PdfViewerOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NewPreviewOperator(
    private val context: Context,
    readerState: () -> PageState,
    private val pageIndex: () -> Int,
) : PdfOperator {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _editorPreview = PdfEditorPreview(context, readerState, object : PdfViewerOverlay.OverlayBaseInterface {
        override fun requireStructuredText(): List<WordLine> {
            val pageIndex = pageIndex()
            return if (pageIndex >= 0) {
                _pdfCore?.getText(pageIndex).orEmpty()
            } else {
                emptyList()
            }
        }

        override fun getFitScale(): Float {
            return fitScale
        }

    })
    private var _pdfCore: PdfCore? = null
    override val enablePageOverlay: Boolean = false
    override var fitScale: Float = 1f

    override suspend fun initCore(
        file: File,
        password: String,
        listener: PdfCoreListener
    ): PdfCore {
        val pdfCore = withContext(Dispatchers.Default) {
            PdfCore(file, PdfCore.getCacheDir(context), context = context,
                buildPreviewFile = false, password)
        }
        val canSave = _editorPreview.canUndo.combine(_editorPreview.canRedo) { canUndo, canRedo ->
            listener.onRedoUndoStateChanged(canUndo, canRedo)
            canUndo
        }
        scope.launch(Dispatchers.Main) {
            canSave.collect { needSave ->
                listener.onSaveStateChanged(needSave)
            }
        }
        _pdfCore = pdfCore
        return pdfCore
    }

    override fun undo() {
        _editorPreview.undo()
    }

    override fun redo() {
        _editorPreview.redo()
    }

    override fun save() {
        val ops = _editorPreview.save()
        _editorPreview.invalidate()
        _pdfCore?.save(pageIndex() + 1, ops)
    }

    override fun abandonSave() {
        _pdfCore?.abandonSave()
    }

    override fun attachToPdfView(pdfView: PdfView) {
        pdfView.addView(
            _editorPreview,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }
}