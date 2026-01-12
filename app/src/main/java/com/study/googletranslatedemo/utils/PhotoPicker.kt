package com.study.googletranslatedemo.utils

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class PhotoPicker {
    private var _launcher: ActivityResultLauncher<String>? = null

    fun register(activity: ComponentActivity, handleResult: (Uri) -> Unit) {
        _launcher =
            activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    handleResult(uri)
                }
            }
    }

    fun request() {
        _launcher?.launch("image/*")
    }
}