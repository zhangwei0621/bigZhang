package com.study.openpdfdemo.viewer.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.lowagie.text.Rectangle
import com.study.openpdfdemo.utils.isInYRange
import com.study.openpdfdemo.viewer.data.PageBridge
import com.study.openpdfdemo.viewer.data.PageState
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import kotlin.math.abs

/**
 * PDF编辑控制视图。这个控件接收用户输入，实现画笔和文本选择的坐标逻辑。
 */
@SuppressLint("ViewConstructor")
class PdfViewerOverlay(
    context: Context,
    private val pageBridge: PageBridge,
    private val overlayInterface: OverlayInterface,
) : View(context) {
    private val moveThreshold = 2f
    private var focusX = 0f
    private var focusY = 0f
    private var currentLine = mutableListOf<PointMapper>()
    private var baseStrokeWidth = 4f

    //画笔效果的预览画笔
    private val inkPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.BUTT //线段端点的样式，需要和渲染库的保持一致，我们目前用的Pdfium端点是没有圆的
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = baseStrokeWidth
    }

    //选择文本的画笔
    private val textSelectPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        color = "#80C59172".toColorInt()
    }
    private val previewPath = Path()
    private val textSelectData = TextSelectData()

    fun clearPreview() {
        currentLine.clear()
        textSelectData.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawInkPreview(canvas)
        drawSelectTextPreview(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)
        when (pageBridge.pageState) {
            PageState.NormalReader -> {
                return false
            }

            PageState.SelectText -> {
                handelSelectTextJob(event)
            }

            PageState.DrawInk -> {
                handleInkJob(event)
            }
        }
        return true
    }

    //执行选择文本的输入逻辑
    private fun handelSelectTextJob(event: MotionEvent) {
        val x = event.x
        val y = event.y
        val fitScale = overlayInterface.getFitScale()
        //先将原点转为左下角，再缩放
        val pdfX = x / fitScale
        val pdfY = (height - y) / fitScale
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                focusX = x
                focusX = y
                textSelectData.initData(overlayInterface.requireStructuredText())
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
                overlayInterface.onSelectTextResult(textSelectData.getSelectedLines())
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
        val fitScale = overlayInterface.getFitScale()
        //先将原点转为左下角，再缩放
        val pdfX = x / fitScale
        val pdfY = (height - y) / fitScale
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                focusX = x
                focusX = y
                currentLine = mutableListOf()
                currentLine += PointMapper(x, y, pdfX, pdfY)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - focusX)
                val dy = abs(y - focusY)
                if (dx >= moveThreshold || dy >= moveThreshold) {
                    focusX = x
                    focusX = y
                    currentLine += PointMapper(x, y, pdfX, pdfY)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                val pdfLine = mutableListOf<Float>()
                currentLine.forEach { pointMapper ->
                    val pdfPoint = pointMapper.pdfPoint
                    pdfLine += pdfPoint.x
                    pdfLine += pdfPoint.y
                }
                currentLine.clear()
                overlayInterface.onInkFinish(pdfLine.toFloatArray())
            }

            MotionEvent.ACTION_CANCEL -> {
                currentLine.clear()
                invalidate()
            }
        }
    }

    private fun drawInkPreview(canvas: Canvas) {
        currentLine.let { points ->
            if (points.isEmpty()) {
                return
            }
            val fitScale = overlayInterface.getFitScale()
            inkPaint.strokeWidth = baseStrokeWidth * fitScale
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
            val fitScale = overlayInterface.getFitScale()
            inkPaint.strokeWidth = baseStrokeWidth * fitScale
            lines.forEach { line ->
                val lineRect = line.getLineRectForAndroid(fitScale)
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

    interface OverlayInterface {
        fun onInkFinish(line: FloatArray)

        fun requireStructuredText(): List<WordLine>

        fun onSelectTextResult(textLines: List<WordLine>)

        fun getFitScale(): Float
    }

    /**
     * 存储一个点的坐标
     */
    data class PointMapper(
        /**
         * 该点在客户端的坐标，用于绘制预览
         */
        val androidPoint: PointF,

        /**
         * 该点在PDF坐标系的坐标，用于写到PDF文件中
         */
        val pdfPoint: PointF
    ) {
        constructor(androidX: Float, androidY: Float, pdfX: Float, pdfY: Float) : this(
            PointF(androidX, androidY),
            PointF(pdfX, pdfY)
        )
    }

    class TextSelectData {
        private val currentLines = mutableListOf<WordLine>()
        private val selectLines = mutableSetOf<WordLine>()
        private var selectBoxRect: Rectangle = Rectangle(0f, 0f, 0f, 0f)

        fun initData(lines: List<WordLine>?) {
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
                if (selectRect.isInYRange(line.getLineRectForPdf())) {
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