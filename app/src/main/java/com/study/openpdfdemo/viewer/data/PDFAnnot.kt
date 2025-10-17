package com.study.openpdfdemo.viewer.data

import com.lowagie.text.Rectangle

sealed class PDFAnnot {
    class MarkupAnnotWrap(
        val rect: Rectangle,
        val markupType: Int,
        val color: PDFColorWrap,
        val quadPoints: FloatArray,
    ) : PDFAnnot()

    /**
     * 画笔注解。每次只能写一条线段。
     */
    class InkAnnotWrap(
        val rect: Rectangle,
        val color: PDFColorWrap,
        val strokeWidth: Float,
        val line: FloatArray,
    ) : PDFAnnot() {
        /**
         * 获取单个点的线宽。当线段只有一个点时，需要使用更大的线宽。
         */
        fun getSinglePointStrokeWidth(): Float {
            return strokeWidth * 2f
        }

        /**
         * 根据点数获取合适的线宽
         */
        fun autoStrokeWidth(): Float {
            return if (line.size == 2) {
                getSinglePointStrokeWidth()
            } else {
                strokeWidth
            }
        }
    }
}