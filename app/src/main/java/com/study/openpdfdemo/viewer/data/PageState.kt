package com.study.openpdfdemo.viewer.data

/**
 * 阅读器状态
 */
enum class PageState {
    /**
     * 普通状态，即阅读状态
     */
    NormalReader,

    /**
     * 选择文本
     */
    SelectText,

    /**
     * 画笔状态
     */
    DrawInk
}