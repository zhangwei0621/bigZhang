package com.study.openpdfdemo.viewer.view.reader

import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.study.openpdfdemo.viewer.adapter.PageView
import com.study.openpdfdemo.viewer.data.PageTool

class PdfReaderView(context: Context) : BaseReaderView(context) {
    private var _listener: PdfReaderViewInterface? = null

    fun setListener(listener: PdfReaderViewInterface?) {
        _listener = listener
        val total = mAdapter?.count ?: 0
        if (total > 0) {
            _listener?.onDisplayPageChanged(mCurrent, total)
        }
    }

    fun redraw() {
        applyToAllPageView {
            it.redrawPage()
        }
    }

    fun redrawSearchResult() {
        applyToAllPageView {
            it.refreshSearchResult()
        }
    }

    override fun onSettle(v: View) {
        if (v is PageView) {
            v.checkHqDraw()
        }
    }

    override fun onUnsettle(v: View) {

    }

    override fun onSingleTapUp() {

    }

    override fun onMoveOffChild(i: Int) {

    }

    override fun onMoveToChild(i: Int, total: Int) {
        _listener?.onDisplayPageChanged(i, total)
    }

    override fun onPageStatusChanged(currentPageView: View?) {

    }

    override fun onPageEditToolChanged(tool: PageTool?) {

    }

    override fun onShowPress(e: MotionEvent) {

    }

    interface PdfReaderViewInterface {
        fun onDisplayPageChanged(pageIndex: Int, total: Int)
    }
}