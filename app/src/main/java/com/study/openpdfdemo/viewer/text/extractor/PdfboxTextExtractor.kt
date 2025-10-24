package com.study.openpdfdemo.viewer.text.extractor

import com.study.openpdfdemo.viewer.text.extractor.data.PointData
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import com.study.openpdfdemo.viewer.text.extractor.data.WordSpan
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs

/**
 * PDF文本结构化工具，使用Pdfbox的方法。注意使用前先调用PdfBox的初始化。
 * 速度较慢
 */
class PdfboxTextExtractor : PDFTextStripper() {
    private val _spans = mutableListOf<WordSpan>()
    private val lineMergeThreshold = 2f

    init {
        // 按照视觉顺序抓取文本
        sortByPosition = true
        // 设置分词器
        wordSeparator = " "

        addMoreFormatting = false
    }

    /**
     * 解析该页文本。
     * @param page 1-base
     */
    fun extract(document: PDDocument, page: Int): List<WordLine> {
        try {
            _spans.clear()
            //设置页面
            startPage = page
            endPage = page
            //触发抓取
            getText(document)
            return toStructured()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

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
                //获取文本矩形左下角的点
                val x = tp.xDirAdj//x坐标
                val ltCoordinateY = tp.yDirAdj//y坐标，但原点在左上角
                val lbCoordinateY = tp.endY//y坐标，但原点在左下角
                //文本宽高
                val textWidth = tp.width
                val textHeight = tp.height
                _spans.add(
                    WordSpan(
                        tp.unicode,
                        PointData(
                            x,
                            lbCoordinateY,
                            x,
                            ltCoordinateY
                        ),
                        PointData(
                            x + textWidth,
                            lbCoordinateY + textHeight,
                            x + textWidth,
                            ltCoordinateY - textHeight
                        )
                    )
                )
            }
        }
    }

    /**
     * 聚合为文本行
     */
    private fun toStructured(): List<WordLine> {
        //聚合成行。由于Span输入时就按视觉排序了(从上到下、从左到右)，这里就不需要再排序。(除非输入时的排序出问题)
        val lines = mutableListOf<WordLine>()
        _spans.forEach { span ->
            val currentLine = lines.lastOrNull()
            //首次聚合 或者 当前行与当前文本Y轴差值超过分行阈值，则新建一行
            if (currentLine == null || abs(currentLine.lbPoint.y - span.lbPoint.y) > lineMergeThreshold) {
                val line = WordLine(span.lbPoint)
                line.addWord(span)
                lines.add(line)
            } else {
                //否则将当前文本加入当前行
                currentLine.addWord(span)
            }
        }
        return lines
    }
}