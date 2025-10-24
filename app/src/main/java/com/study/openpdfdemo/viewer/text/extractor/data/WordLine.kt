package com.study.openpdfdemo.viewer.text.extractor.data

import com.lowagie.text.Rectangle

/**
 * 文本行
 */
data class WordLine(
    /**
     * 文本行矩形左下角的点
     */
    val lbPoint: PointData
) {
    val wordSpans = mutableListOf<WordSpan>()
    val text: String
        get() {
            val stringBuilder = StringBuilder()
            wordSpans.forEach { span ->
                stringBuilder.append(span.word)
            }
            return stringBuilder.toString()
        }

    fun addWord(wordSpan: WordSpan) {
        wordSpans.add(wordSpan)
    }

    fun getLineRectForAndroid(scale: Float): Rectangle? {
        val firstSpan = wordSpans.firstOrNull()
        val lastSpan = wordSpans.lastOrNull()
        val maxYSpan = wordSpans.maxByOrNull { it.lbPoint.sy }
        val minYSpan = wordSpans.minByOrNull { it.rtPoint.sy }
        if (firstSpan != null && lastSpan != null && maxYSpan != null && minYSpan != null) {
            return Rectangle(
                firstSpan.lbPoint.sx * scale,
                maxYSpan.lbPoint.sy * scale,
                lastSpan.rtPoint.sx * scale,
                minYSpan.rtPoint.sy * scale,
            )
        }
        return null
    }

    fun getLineRectForPdf(): Rectangle? {
        val firstSpan = wordSpans.firstOrNull()
        val lastSpan = wordSpans.lastOrNull()
        val maxYSpan = wordSpans.maxByOrNull { it.rtPoint.y }
        val minYSpan = wordSpans.minByOrNull { it.lbPoint.y }
        if (firstSpan != null && lastSpan != null && maxYSpan != null && minYSpan != null) {
            return Rectangle(
                firstSpan.lbPoint.x,
                minYSpan.lbPoint.y,
                lastSpan.rtPoint.x,
                maxYSpan.rtPoint.y,
            )
        }
        return null
    }
}