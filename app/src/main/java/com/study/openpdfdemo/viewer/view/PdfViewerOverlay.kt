package com.study.openpdfdemo.viewer.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.lowagie.text.Rectangle
import com.study.openpdfdemo.PdfViewerActivity
import com.study.openpdfdemo.utils.TextStripper
import com.study.openpdfdemo.utils.isCross
import com.study.openpdfdemo.viewer.tool.PdfOverlayListener
import kotlin.math.abs

/**
 * PDF编辑控制视图。这个控件接收用户输入，实现画笔和文本选择的坐标逻辑。
 */
class PdfViewerOverlay(context: Context, attrs: AttributeSet?, defStyle: Int) :
    View(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private var listener: PdfOverlayListener? = null
    private val moveThreshold = 2f
    private var focusX = 0f
    private var focusY = 0f
    private var pageScale: Float = 1f
    private var currentLine = mutableListOf<PointMapper>()
    private var baseStrokeWidth = 4f
    private val inkPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = baseStrokeWidth
    }
    private val textSelectPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        color = "#80C59172".toColorInt()
    }
    private val previewPath = Path()
    private val textSelectData = TextSelectData()
    private var toolState = PdfViewerActivity.DemoToolState.Nothing

    fun setToolState(state: PdfViewerActivity.DemoToolState) {
        toolState = state
        if (state == PdfViewerActivity.DemoToolState.Ink) {
            inkPaint.color = state.colorWrap.asInt()
        }
    }

    fun setOverlayListener(newListener: PdfOverlayListener?) {
        listener = newListener
    }

    fun setPageScale(scale: Float) {
        pageScale = scale
        calStrokeWidth()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawInkPreview(canvas)
        drawSelectTextPreview(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)
        when (toolState) {
            PdfViewerActivity.DemoToolState.Ink -> {
                handleInkJob(event)
            }

            PdfViewerActivity.DemoToolState.Highlight,
            PdfViewerActivity.DemoToolState.Underline,
            PdfViewerActivity.DemoToolState.StrokeOut -> {
                handelSelectTextJob(event)
            }

            else -> return false
        }
        return true
    }

    //执行选择文本的输入逻辑
    private fun handelSelectTextJob(event: MotionEvent) {
        val x = event.x
        val y = event.y
        //先将原点转为左下角，再缩放
        val pdfX = x / pageScale
        val pdfY = (height - y) / pageScale
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                focusX = x
                focusX = y
                // TODO: 优化initData 现在这样会比较卡，考虑提前拿结构化文本
                textSelectData.initData(listener?.requireStructuredText())
                textSelectData.setSelectBoxLB(pdfX, pdfY)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - focusX)
                val dy = abs(y - focusY)
                if (dx >= moveThreshold || dy >= moveThreshold) {
                    focusX = x
                    focusX = y
                    textSelectData.setSelectBoxRT(pdfX, pdfY)
                    if (textSelectData.calculateSelect()) {
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                listener?.onSelectTextResult(textSelectData.getSelectedLines())
                textSelectData.reset()
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                textSelectData.reset()
                invalidate()
            }
        }
    }

    //执行画笔的输入逻辑
    private fun handleInkJob(event: MotionEvent) {
        val x = event.x
        val y = event.y
        //先将原点转为左下角，再缩放
        val pdfX = x / pageScale
        val pdfY = (height - y) / pageScale
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                focusX = x
                focusX = y
                currentLine = mutableListOf()
                currentLine += PointMapper(x, y, pdfX, pdfY)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - focusX)
                val dy = abs(y - focusY)
                if (dx >= moveThreshold || dy >= moveThreshold) {
                    focusX = x
                    focusX = y
                    currentLine += PointMapper(x, y, pdfX, pdfY)
                }
            }

            MotionEvent.ACTION_UP -> {
                val pdfLine = mutableListOf<Float>()
                currentLine.forEach { pointMapper ->
                    val pdfPoint = pointMapper.pdfPoint
                    pdfLine += pdfPoint.x
                    pdfLine += pdfPoint.y
                }
                currentLine.clear()
                listener?.onInkFinish(pdfLine.toFloatArray())
            }

            MotionEvent.ACTION_CANCEL -> {
                currentLine.clear()
            }
        }
        invalidate()
    }

    private fun calStrokeWidth() {
        inkPaint.strokeWidth = baseStrokeWidth * pageScale
    }

    private fun drawInkPreview(canvas: Canvas) {
        currentLine.let { points ->
            if (points.isEmpty()) {
                return
            }
            previewPath.reset()
            points.forEachIndexed { i, pointMapper ->
                val pointF = pointMapper.androidPoint
                if (points.size >= 2) {
                    if (i == 0) {
                        previewPath.moveTo(pointF.x, pointF.y)
                    } else {
                        previewPath.lineTo(pointF.x, pointF.y)
                    }
                } else {
                    canvas.drawCircle(
                        pointF.x,
                        pointF.y,
                        inkPaint.strokeWidth / 2f,
                        inkPaint
                    )
                }
            }
        }
        canvas.drawPath(previewPath, inkPaint)
    }

    private fun drawSelectTextPreview(canvas: Canvas) {
        textSelectData.getSelectedLines().let { lines ->
            if (lines.isEmpty()) {
                return
            }
            lines.forEach { line ->
                val lineRect = line.getLineRect(true, pageScale)
                if (lineRect != null) {
                    previewPath.reset()
                    previewPath.moveTo(lineRect.left, lineRect.bottom)
                    previewPath.lineTo(lineRect.right, lineRect.bottom)
                    previewPath.lineTo(lineRect.right, lineRect.top)
                    previewPath.lineTo(lineRect.left, lineRect.top)
                    previewPath.close()
                    canvas.drawPath(previewPath, textSelectPaint)
                }
            }
        }
    }

    data class PointMapper(
        val androidPoint: PointF,
        val pdfPoint: PointF
    ) {
        constructor(x1: Float, y1: Float, x2: Float, y2: Float) : this(
            PointF(x1, y1),
            PointF(x2, y2)
        )
    }

    class TextSelectData {
        private val currentLines = mutableListOf<TextStripper.Line>()
        private val selectLines = mutableSetOf<TextStripper.Line>()
        private var selectBoxRect: Rectangle = Rectangle(0f, 0f, 0f, 0f)

        fun initData(lines: List<TextStripper.Line>?) {
            reset()
            lines?.let {
                currentLines.addAll(it)
            }
        }

        fun setSelectBoxLB(left: Float, bottom: Float) {
            selectBoxRect.left = left
            selectBoxRect.bottom = bottom
        }

        fun setSelectBoxRT(right: Float, top: Float) {
            selectBoxRect.right = right
            selectBoxRect.top = top
        }

        /**
         * 计算选择文本。先按行抓取，如果用单个文本可能会很卡。
         * @return true=数据有变，需要刷新界面。
         */
        fun calculateSelect(): Boolean {
            val selectRect = Rectangle(selectBoxRect)
            selectRect.normalize()
            var needRefresh = false
            currentLines.forEach { line ->
                // TODO: 优化选择效果，目前逻辑可能会导致宽度较小的文本行难以选中
                if (line.getLineRect(false)?.isCross(selectRect) == true) {
                    if (selectLines.add(line)) {
                        needRefresh = true
                    }
                } else {
                    if (selectLines.remove(line)) {
                        needRefresh = true
                    }
                }
            }
            return needRefresh
        }

        fun getSelectedLines() = selectLines.toList()

        fun reset() {
            selectBoxRect = Rectangle(0f, 0f, 0f, 0f)
            selectLines.clear()
            currentLines.clear()
        }
    }
}