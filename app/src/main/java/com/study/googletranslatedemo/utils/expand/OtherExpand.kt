package com.study.googletranslatedemo.utils.expand

import android.content.Context
import android.util.TypedValue

fun Float.dp(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    )
}