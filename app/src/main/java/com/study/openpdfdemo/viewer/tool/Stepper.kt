package com.study.openpdfdemo.viewer.tool

import android.view.View

class Stepper(private val mPoster: View, private val mTask: Runnable) {
    private var mPending = false

    fun prod() {
        if (!mPending) {
            mPending = true
            mPoster.postOnAnimation {
                mPending = false
                mTask.run()
            }
        }
    }
}
