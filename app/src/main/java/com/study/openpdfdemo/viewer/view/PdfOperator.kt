package com.study.openpdfdemo.viewer.view

import android.graphics.PointF
import com.study.openpdfdemo.viewer.PdfCore
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import com.study.openpdfdemo.viewer.tool.PdfCoreListener
import java.io.File

interface PdfOperator {
    val enablePageOverlay: Boolean

    var fitScale: Float

    suspend fun initCore(file: File, password: String, listener: PdfCoreListener): PdfCore

    fun undo()

    fun redo()

    fun save()

    fun abandonSave()

    fun attachToPdfView(pdfView: PdfView)
}

sealed class EditOps() {
    class Ink(val line: List<PointMapper>) : EditOps() {
        fun toPdfLines(): FloatArray {
            val pdfLine = mutableListOf<Float>()
            line.forEach { pointMapper ->
                val pdfPoint = pointMapper.pdfPoint
                pdfLine += pdfPoint.x
                pdfLine += pdfPoint.y
            }
            return pdfLine.toFloatArray()
        }
    }

    class HighLight(val lines: List<WordLine>) : EditOps()
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