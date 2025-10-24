package com.study.openpdfdemo.viewer.data

/**
 * 阅读器工具
 * @param needText 该工具是否需要当前页面具有可结构化文本
 * @param pageState 该工具对应的阅读器状态
 */
enum class PageTool(
    val needText: Boolean,
    val pageState: PageState
) {
    /**
     * 复制文本
     */
    CopyText(true, PageState.SelectText),

    /**
     * 高光效果
     */
    Highlight(true, PageState.SelectText),

    /**
     * 下划线效果
     */
    Underline(true, PageState.SelectText),

    /**
     * 划除线效果
     */
    StrokeOut(true, PageState.SelectText),

    /**
     * 画笔工具
     */
    Ink(false, PageState.DrawInk)
}