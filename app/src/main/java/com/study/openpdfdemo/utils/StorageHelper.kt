package com.study.openpdfdemo.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * @author chancey
 * @date 2023/2/23   13:29
 */
class StorageHelper {
    private var intentLauncher: ActivityResultLauncher<Intent>? = null
    private var stringLauncher: ActivityResultLauncher<String>? = null

    companion object {
        fun hasPermissions(ctx: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                    ctx.applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

        /**
         * 存储权限状态
         */
        fun getRationaleStatus(): Boolean {
            return false
        }

        fun shouldShowRequestPermissionRationale(act: ComponentActivity?): Boolean {
            return act?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    false
                } else {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            } ?: false
        }
    }

    fun register(frg: Fragment, callback: (Boolean) -> Unit) {
        if (hasPermissions(frg.requireContext())) return
        intentLauncher =
            frg.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val granted = hasPermissions(frg.requireContext())
                callback(granted)
            }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            stringLauncher =
                frg.registerForActivityResult(ActivityResultContracts.RequestPermission(), callback)
        }
    }

    fun register(activity: AppCompatActivity, callback: (Boolean) -> Unit) {
        if (hasPermissions(activity)) return
        intentLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                callback(hasPermissions(activity))
            }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            stringLauncher =
                activity.registerForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                    callback
                )
        }
    }

    fun launch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .packageName("com.study.openpdfdemo")
                intentLauncher?.launch(intent)
            } catch (e: Exception) {
                val intent = createIntent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intentLauncher?.launch(intent)
            }
        } else if (!getRationaleStatus()) {
            val intent = createIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .packageName("com.study.openpdfdemo")
            intentLauncher?.launch(intent)
        } else {
            stringLauncher?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun createIntent(action: String) = Intent().apply {
        this.action = action
    }

    private fun Intent.packageName(pkg: String): Intent {
        data = Uri.fromParts("package", pkg, null)
        return this
    }
}
