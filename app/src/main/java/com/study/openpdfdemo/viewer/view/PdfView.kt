package com.study.openpdfdemo.viewer.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.study.openpdfdemo.viewer.PdfCore
import com.study.openpdfdemo.viewer.adapter.PageAdapter
import com.study.openpdfdemo.viewer.data.PageTool
import com.study.openpdfdemo.viewer.data.SearchDirection
import com.study.openpdfdemo.viewer.tool.PdfCoreListener
import com.study.openpdfdemo.viewer.view.reader.PdfReaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    FrameLayout(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private var _pdfCore: PdfCore? = null
    private var _pdfReaderView: PdfReaderView? = null
    private var _listener: PdfViewInterface? = null

    fun setCoreListener(listener: PdfViewInterface?) {
        _listener = listener
    }

    /**
     * 建议先设置[setCoreListener]再调用本方法，否则有些初始化的数据可能无法传出
     */
    suspend fun openFile(file: File, password: String = "") {
        _pdfCore = withContext(Dispatchers.Default) {
            // TODO: 核心工具使用多种不同的PDF库，导致构造时很卡，界面可以考虑做个loading动画
            PdfCore(file, PdfCore.getCacheDir(context), password)
        }
        _pdfCore?.setCoreListener(object : PdfCoreListener {
            override fun onRedoUndoStateChanged(canUndo: Boolean, canRedo: Boolean) {
                _listener?.onRedoUndoStateChanged(canUndo, canRedo)
            }

            override fun onSaveStateChanged(needSave: Boolean) {
                _listener?.onSaveStateChanged(needSave)
            }
        })
        _pdfReaderView = PdfReaderView(context).also { pdfReaderView ->
            pdfReaderView.adapter = PageAdapter(context, _pdfCore!!, pdfReaderView.pageBridge)
            pdfReaderView.setListener(object : PdfReaderView.PdfReaderViewInterface {
                override fun onDisplayPageChanged(pageIndex: Int, total: Int) {
                    _listener?.onDisplayPageChanged(pageIndex, total)
                }
            })
            this.removeAllViews()
            this.addView(pdfReaderView)
        }
    }

    fun nextPage() {
        _pdfReaderView?.let {
            it.gotoPage(it.getCurrentPageIndex() + 1)
        }
    }

    fun lastPage() {
        _pdfReaderView?.let {
            it.gotoPage(it.getCurrentPageIndex() - 1)
        }
    }

    fun gotoPage(pageIndex: Int) {
        _pdfReaderView?.gotoPage(pageIndex)
    }

    fun setPageEditTool(tool: PageTool?) {
        _pdfReaderView?.setPageEditTool(tool)
    }

    fun redraw() {
        _pdfReaderView?.redraw()
    }

    fun save() {
        _pdfCore?.save()
    }

    fun abandonSave() {
        _pdfCore?.abandonSave()
    }

    fun undo() {
        if (_pdfCore?.undo() == true) {
            _pdfReaderView?.redraw()
        }
    }

    fun redo() {
        if (_pdfCore?.redo() == true) {
            _pdfReaderView?.redraw()
        }
    }

    fun search(keyword: String): Boolean {
        _pdfReaderView?.pageBridge?.searchResult = _pdfCore?.search(keyword)
        val hadFound = _pdfReaderView?.pageBridge?.searchResult?.hadFound() == true
        _pdfReaderView?.let { readerView ->
            readerView.pageBridge.searchResult?.let { searchResult ->
                val goto = searchResult.changeSelectPage(readerView.getCurrentPageIndex())
                if (goto != null) {
                    readerView.gotoPage(goto)
                }
            }
            readerView.redrawSearchResult()
        }

        return hadFound
    }

    fun nextSearchResult(direction: SearchDirection): Boolean {
        _pdfReaderView?.let { readerView ->
            readerView.pageBridge.searchResult?.let { searchResult ->
                val hasNext = searchResult.next(direction)
                val resultPage = searchResult.selectedPage
                if (hasNext) {
                    //如果下一个搜索结果不在当前页。跳转到结果所在页
                    if (resultPage != readerView.getCurrentPageIndex()) {
                        readerView.gotoPage(resultPage)
                    } else {
                        //否则重新渲染当前页的搜索结果
                        readerView.redrawSearchResult()
                    }
                }
                return hasNext
            }
        }
        return false
    }

    fun clearSearchResult() {
        _pdfReaderView?.pageBridge?.searchResult = null
        _pdfReaderView?.redrawSearchResult()
    }

    fun destroy() {
        _pdfReaderView?.destroy()
        _pdfCore?.destroy()
    }

    /**
     * PdfView对外接口。实现方建议做线程检查，以免在子线程上处理UI
     */
    interface PdfViewInterface {
        fun onRedoUndoStateChanged(canUndo: Boolean, canRedo: Boolean)
        fun onSaveStateChanged(needSave: Boolean)
        fun onDisplayPageChanged(pageIndex: Int, total: Int)
    }
}