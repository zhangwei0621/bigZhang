package com.study.openpdfdemo.viewer.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.graphics.toColorInt
import com.study.openpdfdemo.viewer.data.PageBridge

/**
 * 该控件为文档内文本搜索结果添加高光
 */
@SuppressLint("ViewConstructor")
class SearchResultView(
    context: Context,
    private val _pageBridge: PageBridge,
    private val overlayInterface: ResultOverlayInterface
) : View(context) {
    private val selectedColor = "#99EE2F2E".toColorInt()
    private val unselectedColor = "#4D159B53".toColorInt()
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
    }
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pageIndex = overlayInterface.getPage()
        val fitScale = overlayInterface.getFitScale()
        _pageBridge.searchResult?.let { searchResult ->
            searchResult.getPageResult(pageIndex)?.forEachIndexed { index, data ->
                path.reset()
                val lb = data.lbPoint.newScaled(fitScale)
                val rt = data.rtPoint.newScaled(fitScale)
                path.moveTo(lb.sx, lb.sy)
                path.lineTo(rt.sx, lb.sy)
                path.lineTo(rt.sx, rt.sy)
                path.lineTo(lb.sx, rt.sy)
                path.close()
                if (pageIndex == searchResult.selectedPage && index == searchResult.selectedIndex) {
                    paint.color = selectedColor
                } else {
                    paint.color = unselectedColor
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    interface ResultOverlayInterface {
        fun getPage(): Int

        fun getFitScale(): Float
    }
}