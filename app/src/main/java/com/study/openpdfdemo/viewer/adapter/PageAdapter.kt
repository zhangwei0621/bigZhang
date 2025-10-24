package com.study.openpdfdemo.viewer.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.study.openpdfdemo.viewer.PdfCore
import com.study.openpdfdemo.viewer.data.PageBridge

class PageAdapter(
    private val context: Context,
    private val pdfCore: PdfCore,
    private var pageBridge: PageBridge
) : BaseAdapter() {
    override fun getCount(): Int = pdfCore.pageCount

    override fun getItem(position: Int): Any? = null

    override fun getItemId(position: Int): Long = 0L

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View? {
        val pageView = if (convertView != null) {
            convertView as PageView
        } else {
            PageView(
                context,
                pdfCore,
                pageBridge
            )
        }
        pageView.openPage(position)
        return pageView
    }
}