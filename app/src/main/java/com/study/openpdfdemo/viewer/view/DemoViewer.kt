package com.study.openpdfdemo.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.study.openpdfdemo.PdfViewerActivity
import com.study.openpdfdemo.viewer.data.TextSearchData
import com.study.openpdfdemo.viewer.tool.PdfOverlayListener

class DemoViewer(context: Context, attrs: AttributeSet?, defStyle: Int) :
    ViewGroup(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val contentView = AppCompatImageView(context)
    private val overlayView = PdfViewerOverlay(context)
    private val searchResultView = SearchResultView(context)

    init {
        addView(contentView)
        addView(overlayView)
        addView(searchResultView)
    }

    fun setSearchResult(result: List<TextSearchData>) {
        searchResultView.setSearchResult(result)
    }

    fun clearSearchResult() {
        searchResultView.clear()
    }

    fun setToolState(state: PdfViewerActivity.DemoToolState) {
        overlayView.setToolState(state)
    }

    fun setOverlayListener(listener: PdfOverlayListener?) {
        overlayView.setOverlayListener(listener)
        searchResultView.setOverlayListener(listener)
    }

    fun setBitmap(bitmap: Bitmap, scale: Float) {
        contentView.setImageBitmap(bitmap)
        //将控制视图大小设定为bitmap大小，这样安卓的坐标换算到PDF坐标就只需要考虑scale缩放
        overlayView.setPageScale(scale)
        overlayView.measure(
            MeasureSpec.EXACTLY or bitmap.width,
            MeasureSpec.EXACTLY or bitmap.height
        )
        searchResultView.setPageScale(scale)
        searchResultView.measure(
            MeasureSpec.EXACTLY or bitmap.width,
            MeasureSpec.EXACTLY or bitmap.height
        )
        searchResultView.clear()
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int
    ) {
        val width = r - l
        val height = b - t
        contentView.layout(0, 0, width, height)
        layoutToCenter(overlayView, width, height)
        layoutToCenter(searchResultView, width, height)
    }

    private fun layoutToCenter(targetView: View, parentWidth: Int, parentHeight: Int) {
        val viewWidth = targetView.measuredWidth
        val viewHeight = targetView.measuredHeight
        if (viewWidth <= 0 || viewHeight <= 0) {
            return
        }
        val left = (parentWidth - viewWidth) / 2
        val top = (parentHeight - viewHeight) / 2
        val right = left + viewWidth
        val bottom = top + viewHeight
        targetView.layout(left, top, right, bottom)
    }
}