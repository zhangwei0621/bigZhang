package com.study.openpdfdemo.viewer.text.extractor

import com.study.openpdfdemo.utils.isZero
import com.study.openpdfdemo.viewer.text.extractor.data.PointData
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import com.study.openpdfdemo.viewer.text.extractor.data.WordSpan
import io.legere.pdfiumandroid.PdfDocument
import kotlin.math.abs

/**
 * PDF文本结构化工具，使用Pdfium的方法
 *
 * 优点：1.速度较快；2.矩形较准确
 *
 * 缺点：
 * 1.拿到文本矩形是按字形计算的，比如“。”句号，它的矩形实际会很小，不便于将文本分成“文本行”，可能导致误判；
 * 2.有的文本字符获取错误，比如“提”字会变成奇怪的字符，后续看能不能拿unicode我们自己做转换；
 */
class PdfiumTextExtractor {
    /**
     * 文本行聚合阈值，效果只能实际测试，效果还跟字号大小有关。
     * 太小的话本来是同一行的文本可能被拆分成多行(这个接口回传的文本矩形是按照具体字形计算的)，
     * 太大的话不同的行可能被误判为一行
     */
    private val lineMergeThreshold = 8f

    /**
     * 结构化该页文本
     * @param pageIndex 0-base
     */
    fun extract(document: PdfDocument, pageIndex: Int): List<WordLine> {
        document.openPage(pageIndex).use { page ->
            page.openTextPage().use { textPage ->
                val wordLines = mutableListOf<WordLine>()
                //查询该页字符数量
                val charsCount = textPage.textPageCountChars()
                if (charsCount <= 0) {
                    return wordLines
                }
                //获取该页文本的矩形
                val wordsRectList = textPage.textPageGetRectsForRanges(buildWordRanges(charsCount))
                if (wordsRectList == null) {
                    return wordLines
                }
                //获取该页的文本
                val words = textPage.textPageGetText(0, charsCount)
                if (words.isNullOrBlank()) {
                    return wordLines
                }
                val pageWidth = page.getPageWidthPoint()
                val pageHeight = page.getPageHeightPoint()
                val wordSpans = mutableListOf<WordSpan>()
                //开始封装数据
                wordsRectList.forEach { wordRangeRect ->
                    if (!wordRangeRect.rect.isZero()) {
                        val word = words.getOrNull(wordRangeRect.rangeStart)?.toString()
                        if (!word.isNullOrBlank()) {
                            if (!word.contains("\n") && !word.contains("\r")) {
                                //虽然接口回传的是安卓Rect，但坐标原点在左下角，这里还要再换算一下拿安卓的坐标
                                val lbX = wordRangeRect.rect.left
                                val lbY = wordRangeRect.rect.bottom
                                val rtX = wordRangeRect.rect.right
                                val rtY = wordRangeRect.rect.top
                                val wordSpan = WordSpan(
                                    word,
                                    PointData(lbX, lbY, lbX, pageHeight - lbY),
                                    PointData(rtX, rtY, rtX, pageHeight - rtY)
                                )
                                wordSpans.add(wordSpan)
                            }
                        }
                    }
                }
                //这个接口文本排序有些小问题。这里我们手动排序一下，按从左到右、从上到下排序
                wordSpans.sortedWith(compareByDescending<WordSpan> { it.lbPoint.y }.thenComparing { it.lbPoint.x })
                //将文本聚合为"文本行"
                wordSpans.forEach { wordSpan ->
                    val lastLine = wordLines.lastOrNull()
                    if (lastLine == null || abs(lastLine.lbPoint.y - wordSpan.lbPoint.y) >= lineMergeThreshold) {
                        val line = WordLine(wordSpan.lbPoint)
                        line.addWord(wordSpan)
                        wordLines.add(line)
                    } else {
                        lastLine.addWord(wordSpan)
                    }
                }
                //确保每个文本行内的文本按左到右排序
                wordLines.forEach { wordLine ->
                    wordLine.wordSpans.sortedWith(compareByDescending { it.lbPoint.x })
                }
                return wordLines
            }
        }
    }

    private fun buildWordRanges(charsCount: Int): IntArray {
        val result = mutableListOf<Int>()
        //每个字符都单独提取
        repeat(charsCount) { index ->
            result.add(index)
            result.add(1)
        }
        return result.toIntArray()
    }
}