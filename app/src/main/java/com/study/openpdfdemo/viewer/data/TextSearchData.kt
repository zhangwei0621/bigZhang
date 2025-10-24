package com.study.openpdfdemo.viewer.data

import com.study.openpdfdemo.viewer.text.extractor.data.PointData

/**
 * 搜索结果数据
 */
data class TextSearchData(
    /**
     * 左下角的点
     */
    val lbPoint: PointData,

    /**
     * 右上角的点
     */
    val rtPoint: PointData
)