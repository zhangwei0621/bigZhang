package com.study.openpdfdemo.utils

import com.lowagie.text.Rectangle
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import kotlin.math.abs

/**
 * PDF文本提取器。注意使用前先调用PdfBox的初始化。
 */
class TextStripper(
    private val password: String = ""
) : PDFTextStripper() {
    init {
        // 按照视觉顺序抓取文本
        sortByPosition = true
        // 设置分词器
        wordSeparator = " "

        addMoreFormatting = false
    }

    private val _spans = mutableListOf<Span>()

    /**
     * 回传抓取到文本。会受到分词器的影响。
     * @param text 抓到的文本内容，可能是多个字符
     * @param textPositions [text]中字符对应位置信息
     */
    override fun writeString(text: String?, textPositions: List<TextPosition?>?) {
        if (text.isNullOrBlank() || textPositions.isNullOrEmpty()) return
        //将每个字符拆成独立的Span
        textPositions.forEach { tp ->
            if (tp != null) {
                val fontName = try {
                    tp.font.name
                } catch (_: Exception) {
                    "unknow"
                }
                // TODO: Y坐标和高度并不是很准确，区域没有完全包裹文本的顶部、底部，特别是在非英文的文本上
                //原点在左上角的坐标
                val x0 = tp.xDirAdj
                val y0 = tp.yDirAdj

                //分别用宽、高减去x0、y0得到的坐标
                val endX0 = tp.endX
                val endY0 = tp.endY

                val width = tp.width
                val height = tp.height
                _spans += Span(
                    tp.unicode,
                    SpanPoint(x0, y0, endX0, endY0),
                    SpanPoint(x0 + width, y0 - height, endX0 - width, endY0 + height),
                    fontName
                )
            }
        }
    }

    /**
     * 将文本[Span]聚合为段落、文本行[Line]
     * @param lineMergeTolerance 分行阈值
     */
    private fun toStructured(lineMergeTolerance: Float): List<Line> {
        //聚合成行。由于Span输入时就按视觉排序了(从上到下、从左到右)，这里就不需要再排序。(除非输入时的排序出问题)
        val lines = mutableListOf<Line>()
        _spans.forEach { span ->
            val currentLine = lines.lastOrNull()
            //首次聚合 或者 当前行与当前文本Y轴差值超过分行阈值，则新建一行
            if (currentLine == null || abs(currentLine.endY0 - span.leftBottomPoint.endY) > lineMergeTolerance) {
                val line = Line(span.leftBottomPoint.endY)
                line.addSpan(span)
                lines += line
            } else {
                //否则将当前文本加入当前行
                currentLine.addSpan(span)
            }
        }
//        val debug = StringBuilder("解析文本：")
//        lines.forEachIndexed { i, line ->
//            debug.append("\n${i + 1}：${line.text}")
//        }
        return lines
    }

    fun extract(file: File, page: Int, lineMergeTolerance: Float = 2f): List<Line> {
        try {
            PDDocument.load(file, password).use { doc ->
                //设置页面
                startPage = page
                endPage = page

                //触发抓取
                getText(doc)
                return toStructured(lineMergeTolerance)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    /**
     * 文本坐标点。由于安卓的坐标和PDF的坐标Y轴方向不同，这里收录两种方向坐标，可根据实际使用选择。
     *
     * 在安卓，坐标原点在左上角，使用([x],[y])。在PDF，坐标原点在左下角(仅限最底层PDF接口，如果是其它库则不一定)，使用([x],[endY])。
     * @param x X轴坐标，X轴正方向：右
     * @param y Y轴坐标，Y轴正方向：下
     * @param endX X轴坐标，X轴正方向：左
     * @param endY Y轴坐标，Y轴正方向：上
     */
    data class SpanPoint(
        val x: Float,
        val y: Float,
        val endX: Float,
        val endY: Float
    )

    /**
     * 文本行中某个子段落的文本。
     */
    data class Span(
        val text: String,
        val leftBottomPoint: SpanPoint,
        val rightTopPoint: SpanPoint,
        val fontName: String = "unknow"
    )

    /**
     * 文本行数据。
     */
    data class Line(
        val endY0: Float
    ) {
        private val _spans = mutableListOf<Span>()
        val spans get() = _spans.toList()
        val text: String
            get() {
                val stringBuilder = StringBuilder()
                _spans.forEach { span ->
                    stringBuilder.append(span.text)
                }
                return stringBuilder.toString()
            }

        fun addSpan(span: Span) {
            _spans += span
        }

        fun getLineRect(isAndroidCoordinate: Boolean, scale: Float = 1f): Rectangle? {
            val firstSpan = _spans.firstOrNull()
            val lastSpan = _spans.lastOrNull()
            if (firstSpan != null && lastSpan != null) {
                return if (isAndroidCoordinate) {
                    Rectangle(
                        firstSpan.leftBottomPoint.x * scale,
                        firstSpan.leftBottomPoint.y * scale,
                        lastSpan.rightTopPoint.x * scale,
                        lastSpan.rightTopPoint.y * scale,
                    )
                } else {
                    Rectangle(
                        firstSpan.leftBottomPoint.x * scale,
                        firstSpan.leftBottomPoint.endY * scale,
                        lastSpan.rightTopPoint.x * scale,
                        lastSpan.rightTopPoint.endY * scale,
                    )
                }
            }
            return null
        }
    }
}