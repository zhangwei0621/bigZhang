package com.study.openpdfdemo

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
    }
}