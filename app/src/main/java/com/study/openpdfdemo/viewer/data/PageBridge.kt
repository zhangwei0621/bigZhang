package com.study.openpdfdemo.viewer.data

/**
 * 用于将数据传递到PageView
 */
class PageBridge {
    var pageState = PageState.NormalReader
    var pageTool: PageTool? = null
    var searchResult: TextSearchResult? = null
    var readerWidth: Int = 0
    var readerHeight: Int = 0
}