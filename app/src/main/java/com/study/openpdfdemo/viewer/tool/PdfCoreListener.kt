package com.study.openpdfdemo.viewer.tool

interface PdfCoreListener {
    fun onRedoUndoStateChanged(canUndo: Boolean, canRedo: Boolean)
    fun onSaveStateChanged(needSave: Boolean)
}