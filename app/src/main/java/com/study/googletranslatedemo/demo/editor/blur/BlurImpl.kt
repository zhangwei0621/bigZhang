package com.study.googletranslatedemo.demo.editor.blur

import android.app.Activity
import com.study.googletranslatedemo.demo.editor.effect.splash.FilterPaintActivity
import com.study.googletranslatedemo.utils.filter.FilterRule

object BlurImpl {
    fun start(activity: Activity) {
        FilterPaintActivity.start(activity, FilterRule.BLUR)
    }
}