package com.study.openpdfdemo.utils

import android.app.Activity
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

/**
 * 谷歌数字化文档工具;
 * 无需相机权限,但需要谷歌服务,首次使用需要下载数据;
 */
class GmsScanHelper() {

    private var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    fun register(activity: FragmentActivity, onResult: (Uri?) -> Unit) {
        scannerLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (Activity.RESULT_OK == it.resultCode) {
                    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                    onResult(scanResult?.pdf?.uri)
                } else {
                    onResult(null)
                }
            }
    }

    /**
     * 启动gms文件扫描器
     */
    fun startScanner(activity: FragmentActivity, onError: (String?) -> Unit = {}) {
        if (scannerLauncher == null) {
            onError.invoke("register launcher missing")
            return
        }
        val option = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)//允许导入本地图像
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)//输出格式:PDF
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)//开启全部功能
            .build()
        val scanner = GmsDocumentScanning.getClient(option)
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener {
                scannerLauncher!!.launch(
                    IntentSenderRequest.Builder(it).build()
                )
            }
            .addOnFailureListener {
                onError.invoke(it.message)
            }
    }

    /**
     * 保存pdf文件
     */
    fun savePdf(context: Context, pdfUri: Uri): String {
        var targetFile =
            File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS)
        targetFile = File(targetFile, "${context.packageName}_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(pdfUri).use { i ->
            FileOutputStream(targetFile).use { o ->
                i?.copyTo(o)
            }
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            null
        ) { _: String?, _: Uri? -> }
        return targetFile.absolutePath
    }
}