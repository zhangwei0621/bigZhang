package com.study.openpdfdemo.viewer.tool

import com.study.openpdfdemo.utils.TextStripper
import com.study.openpdfdemo.viewer.data.TextSearchData

/**
 * PDF文档内文本搜索工具
 */
class TextSearchHelper {
    /**
     * 执行搜索
     * @param lines 结构化文本行
     * @param keyword 关键字
     * @param page 当前页码，后续全文档搜索可能需要
     */
    fun search(lines: List<TextStripper.Line>, keyword: String, page: Int): List<TextSearchData> {
        val result = mutableListOf<TextSearchData>()
        if (lines.isEmpty() || keyword.isBlank()) {
            return result
        }
        //按文本行搜索
        lines.forEach { line ->
            val text = line.text
            var currentIndex = 0
            while (currentIndex <= text.length - keyword.length) {
                val foundIndex = text.indexOf(keyword, currentIndex, true)
                if (foundIndex <= -1) {
                    break
                }
                val start = foundIndex
                val end = start + keyword.length - 1
                val spans = line.spans
                val startSpan = spans[start]
                val endSpan = spans[end]
                result +=
                    TextSearchData(page, startSpan.leftBottomPoint, endSpan.rightTopPoint)
                currentIndex = foundIndex + 1
            }
        }
        return result
    }
}