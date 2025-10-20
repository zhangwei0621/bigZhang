package com.study.openpdfdemo.viewer.tool

import com.study.openpdfdemo.viewer.tool.TextStripper

interface PdfOverlayListener {
    fun onInkFinish(line: FloatArray)

    fun requireStructuredText(): List<TextStripper.Line>

    fun onSelectTextResult(textLines: List<TextStripper.Line>)
}