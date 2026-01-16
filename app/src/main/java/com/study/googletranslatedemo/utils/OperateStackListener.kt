package com.study.googletranslatedemo.utils

interface OperateStackListener {
    fun onStackChanged(canUndo: Boolean, canRedo: Boolean)
}