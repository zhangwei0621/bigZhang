package com.study.googletranslatedemo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.tryRecycle
import org.wysaid.nativePort.CGENativeLibrary
import java.io.IOException
import java.io.InputStream


class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 注册CGE图像加载方法
        CGENativeLibrary.setLoadImageCallback(object : CGENativeLibrary.LoadImageCallback {
            // 加载图像为bitmap
            override fun loadImage(name: String?, arg: Any?): Bitmap? {
                if (name.isNullOrBlank()) return null
                // 目前只处理assets里的图像
                val am = assets
                val inputStream: InputStream?
                try {
                    inputStream = am.open(name)
                } catch (_: IOException) {
                    return null
                }

                return BitmapFactory.decodeStream(inputStream)
            }

            // 回收bitmap
            override fun loadImageOK(bmp: Bitmap?, arg: Any?) {
                bmp?.tryRecycle()
            }

        }, null)
    }
}