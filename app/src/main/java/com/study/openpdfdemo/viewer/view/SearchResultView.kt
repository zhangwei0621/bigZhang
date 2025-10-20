package com.study.openpdfdemo.viewer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.study.openpdfdemo.viewer.data.TextSearchData
import com.study.openpdfdemo.viewer.tool.PdfOverlayListener

/**
 * 该控件为文档内文本搜索结果添加高光
 */
class SearchResultView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    View(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private var pageScale: Float = 1f
    private var listener: PdfOverlayListener? = null
    private val searchResult = mutableListOf<TextSearchData>()
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        color = "#99EE2F2E".toColorInt()
    }
    private val path = Path()

    fun setPageScale(scale: Float) {
        pageScale = scale
    }

    fun setOverlayListener(newListener: PdfOverlayListener?) {
        listener = newListener
    }

    /**
     * 传入搜索结果，渲染搜索高光效果
     */
    fun setSearchResult(result: List<TextSearchData>) {
        searchResult.clear()
        searchResult.addAll(result)
        invalidate()
    }

    /**
     * 清除搜索结果和高光效果
     */
    fun clear() {
        searchResult.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        searchResult.forEach { data ->
            path.reset()
            val lb = data.getAndroidLBPoint(pageScale)
            val rt = data.getAndroidRTPoint(pageScale)
            path.moveTo(lb.x, lb.y)
            path.lineTo(rt.x, lb.y)
            path.lineTo(rt.x, rt.y)
            path.lineTo(lb.x, rt.y)
            path.close()
            canvas.drawPath(path, paint)
        }
    }
}