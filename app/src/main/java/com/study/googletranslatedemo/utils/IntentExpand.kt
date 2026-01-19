package com.study.googletranslatedemo.utils

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.study.googletranslatedemo.BuildConfig
import java.io.Serializable

/**
 * 通过class创建一个intent
 */
fun createIntent(clazz: Class<*>) = Intent().apply {
    setClassName(BuildConfig.APPLICATION_ID, clazz.name)
}

/**
 * 添加intent参数，支持连续添加
 */
fun Intent.addParams(vararg pairs: Pair<String, Any?>): Intent {
    for ((key, value) in pairs) {
        when (value) {
            null -> putExtra(key, "")
            is String -> putExtra(key, value)
            is Boolean -> putExtra(key, value)
            is Byte -> putExtra(key, value)
            is Char -> putExtra(key, value)
            is Double -> putExtra(key, value)
            is Float -> putExtra(key, value)
            is Int -> putExtra(key, value)
            is Long -> putExtra(key, value)
            is Short -> putExtra(key, value)
            is Bundle -> putExtra(key, value)
            is BooleanArray -> putExtra(key, value)
            is ByteArray -> putExtra(key, value)
            is CharArray -> putExtra(key, value)
            is DoubleArray -> putExtra(key, value)
            is FloatArray -> putExtra(key, value)
            is IntArray -> putExtra(key, value)
            is LongArray -> putExtra(key, value)
            is ShortArray -> putExtra(key, value)
            is Serializable -> putExtra(key, value)
            is Parcelable -> putExtra(key, value)
            else -> {}
        }
    }
    return this
}

/**
 * 执行startActivity
 */
fun Intent.start(activity: Activity?, finished: Boolean = false) {
    try {
        activity?.startActivity(this)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    if (finished) {
        activity?.finish()
    }
}

fun Intent.newTask() = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun Intent.clearTask() = addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Intent.getInstanceBySerializable(name: String): T? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return getSerializableExtra(name, T::class.java)
    } else {
        getSerializableExtra(name)?.takeIf {
            it is T
        }?.let {
            return it as T
        }
        return null
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getInstanceByParcelable(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name)
    }
}

fun Intent?.getLong(key: String, default: Long = 0L): Long {
    return this?.getLongExtra(key, default) ?: default
}

fun Intent?.getInt(key: String, default: Int = 0): Int {
    return this?.getIntExtra(key, default) ?: default
}

fun Intent?.getBoolean(key: String, default: Boolean = false): Boolean {
    return this?.getBooleanExtra(key, default) ?: default
}

fun Intent?.getString(key: String, default: String = ""): String {
    return this?.getStringExtra(key) ?: default
}
