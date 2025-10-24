package com.study.openpdfdemo.viewer.data

/**
 * author cxr
 * date 2024/4/1
 * @param selectedPage 被选中关键字所在页,对应外部PDFView的页码
 * @param selectedIndex 被选中关键字下标位置,对应本数据链表的下标
 * @param pageList 结果页码合集
 * @param searchDataList 结果合集
 *
 * pageIndex page pageIndex pageResult
 *     0      2     0     List<TextSearchData>
 *     1      6     1     List<TextSearchData>
 *     2      9     2     List<TextSearchData>
 *     3      17    3     List<TextSearchData>
 */
class TextSearchResult {
    var selectedPage: Int = 0
        private set
    var selectedIndex: Int = 0
        private set
    val pageList: MutableList<Int> = mutableListOf()
    val searchDataList: MutableList<List<TextSearchData>> = mutableListOf()

    /**
     * 切换选中页面
     * @param currentPage 当前外部PDFView的页码
     * @return 返回值不为空时，表示currentPage没有搜索结果，需要跳转到返回值页面;返回值对应PDFView页码
     */
    fun changeSelectPage(currentPage: Int): Int? {
        if (hadFound()) {
            val pageIndex = pageList.indexOf(currentPage)
            //选中第一个搜索结果
            selectedIndex = 0
            if (pageIndex >= 0) {
                //如果当前页面有搜索结果，优先显示当前页面
                selectedPage = currentPage
            } else {
                //否则跳转到第一个出现搜索结果的页面
                selectedPage = pageList[selectedIndex]
                return selectedPage
            }
        }
        return null
    }

    fun hadFound(): Boolean {
        return pageList.isNotEmpty()
    }

    /**
     * 获取指定页面的搜索结果
     */
    fun getPageResult(page: Int): List<TextSearchData>? {
        val pageIndex = pageList.indexOf(page)
        if (pageIndex == -1) {
            return null
        }
        return searchDataList[pageIndex]
    }

    /**
     * 选中下一个搜索结果
     * @param searchDirection 标识往前或往后
     * @return true=有下一个结果并选中成功
     */
    fun next(searchDirection: SearchDirection): Boolean {
        val direction = searchDirection.direction
        val currentPageIndex = pageList.indexOf(selectedPage)
        //判断当前页是否存在前(后)关键字
        var nextSelectedIndex = -1
        val currentResult = searchDataList.getOrNull(currentPageIndex)
        if (currentResult?.isNotEmpty() == true) {
            nextSelectedIndex = try {
                currentResult[selectedIndex + direction]
                selectedIndex + direction
            } catch (_: Exception) {
                -1
            }
        }
        if (nextSelectedIndex != -1) {
            selectedIndex = nextSelectedIndex
            return true
        }
        //判断前(后)页是否存在结果
        val nextPageIndex: Int = try {
            pageList[currentPageIndex + direction]
            currentPageIndex + direction
        } catch (e: Exception) {
            -1
        }
        if (nextPageIndex != -1) {
            selectedIndex = if (direction == 1) {
                0
            } else {
                //如果往后搜应该选中最后一个结果
                val nextResult = searchDataList.getOrNull(nextPageIndex)
                (nextResult?.size?.minus(1)) ?: 0
            }
            selectedPage = pageList[nextPageIndex]
            return true
        }
        return false
    }
}