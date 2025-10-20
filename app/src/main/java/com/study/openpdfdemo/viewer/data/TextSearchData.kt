package com.study.openpdfdemo.viewer.data

import android.graphics.PointF
import com.study.openpdfdemo.viewer.tool.TextStripper

/**
 * 搜索结果数据
 * @param page 该结果属于哪页
 * @param pdfLBPoint 结果矩形左下角坐标，pdf坐标系
 * @param pdfRTPoint 结果矩形右上角坐标，pdf坐标系
 */
data class TextSearchData(
    val page: Int,
    val pdfLBPoint: TextStripper.SpanPoint,
    val pdfRTPoint: TextStripper.SpanPoint
) {
    /**
     * 将左下角点输出为安卓坐标系
     */
    fun getAndroidLBPoint(scale: Float): PointF {
        return PointF(
            pdfLBPoint.x * scale,
            pdfLBPoint.y * scale
        )
    }

    /**
     * 将右上角坐标输出为安卓坐标系
     */
    fun getAndroidRTPoint(scale: Float): PointF {
        return PointF(
            pdfRTPoint.x * scale,
            pdfRTPoint.y * scale
        )
    }
}