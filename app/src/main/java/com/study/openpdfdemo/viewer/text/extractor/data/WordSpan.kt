package com.study.openpdfdemo.viewer.text.extractor.data

/**
 * 一个或多个字符
 */
data class WordSpan(
    val word: String,
    /**
     * 文本矩形左下角的点
     */
    val lbPoint: PointData,
    /**
     * 文本矩形右上角的点
     */
    val rtPoint: PointData
)