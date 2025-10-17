package com.study.openpdfdemo.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.study.openpdfdemo.PdfViewerActivity

class DemoViewer(context: Context, attrs: AttributeSet?, defStyle: Int) :
    ViewGroup(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val contentView = AppCompatImageView(context)
    private val overlayView = PdfViewerOverlay(context)

    init {
        addView(contentView)
        addView(overlayView)
    }

    fun setToolState(state: PdfViewerActivity.DemoToolState) {
        overlayView.setToolState(state)
    }

    fun setOverlayListener(listener: PdfViewerOverlay.ActionListener?) {
        overlayView.setActionListener(listener)
    }

    fun setBitmap(bitmap: Bitmap, scale: Float) {
        contentView.setImageBitmap(bitmap)
        overlayView.setPageScale(bitmap.width / scale, bitmap.height / scale, scale)
        //将控制视图大小设定为bitmap大小，这样安卓的坐标换算到PDF坐标就只需要考虑scale缩放
        overlayView.measure(
            MeasureSpec.EXACTLY or bitmap.width,
            MeasureSpec.EXACTLY or bitmap.height
        )
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
        layoutOverlayView(width, height)
    }

    private fun layoutOverlayView(parentWidth: Int, parentHeight: Int) {
        val viewWidth = overlayView.measuredWidth
        val viewHeight = overlayView.measuredHeight
        if (viewWidth <= 0 || viewHeight <= 0) {
            return
        }
        val left = (parentWidth - viewWidth) / 2
        val top = (parentHeight - viewHeight) / 2
        val right = left + viewWidth
        val bottom = top + viewHeight
        overlayView.layout(left, top, right, bottom)
    }
}