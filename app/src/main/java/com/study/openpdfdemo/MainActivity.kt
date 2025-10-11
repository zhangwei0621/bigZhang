package com.study.openpdfdemo

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.study.openpdfdemo.databinding.ActivityMainBinding
import com.study.openpdfdemo.utils.GmsScanHelper
import com.study.openpdfdemo.utils.PdfOps
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val gmsScanHelper = GmsScanHelper()

    private var createPdfPaths = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        gmsScanHelper.register(this) {
            if (it == null) return@register
            createPdfPaths += gmsScanHelper.savePdf(this, it)
        }
        binding.apply {
            createPdf.setOnClickListener {
                createPdf()
            }
            previewPdf.setOnClickListener {
                // TODO: 预览pdf
            }
            encryptionPdf.setOnClickListener {
                encryptionPdf()
            }
            decryptPdf.setOnClickListener {
                decryptPdf()
            }
            mergePdf.setOnClickListener {
                mergePdf()
            }
            splitPdf.setOnClickListener {
                splitPdf()
            }
        }
    }


    /**
     * 创建pdf
     */
    private fun createPdf() {
        gmsScanHelper.startScanner(this)
    }

    /**
     * 加密pdf
     */
    private fun encryptionPdf() {
        val filePaths = createPdfPaths.firstOrNull()
        if (filePaths.isNullOrEmpty()) {
            Log.i(PdfOps.TAG, "encryptionPdf: 文件不存在")
            return
        }
        val file = File(filePaths)
        if (!file.exists()) {
            Log.i(PdfOps.TAG, "encryptionPdf: 文件不存在")
            return
        }
        if (PdfOps.isEncrypted(file)) {
            Log.i(PdfOps.TAG, "encryptionPdf: 文件已加密")
            return
        }
        PdfOps.encryptPdfPlace(this, file, "123456")
    }

    /**
     * 解密pdf
     */
    private fun decryptPdf() {
        val filePaths = createPdfPaths.firstOrNull()
        if (filePaths.isNullOrEmpty()) {
            Log.i(PdfOps.TAG, "decryptPdf: 文件不存在")
            return
        }
        val file = File(filePaths)
        if (!file.exists()) {
            Log.i(PdfOps.TAG, "decryptPdf: 文件不存在")
            return
        }
        if (!PdfOps.isEncrypted(file)) {
            Log.i(PdfOps.TAG, "decryptPdf: 文件未加密")
            return
        }
        PdfOps.decryptPdfReplace(this, file, "123456")
    }

    /**
     * 合并pdf
     */
    private fun mergePdf() {
        //原pdf文件
        val sourceFiles = createPdfPaths.map {
            File(it)
        }.filter { it.exists() }
        //合并pdf文件
        var targetFile =
            File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS)
        targetFile = File(targetFile, "${packageName}_${System.currentTimeMillis()}_merge.pdf")
        //合并
        PdfOps.mergePdfs(this, sourceFiles, targetFile)
    }

    /**
     * 拆分pdf
     */
    private fun splitPdf() {
        val filePaths = createPdfPaths.firstOrNull()
        if (filePaths.isNullOrEmpty()) {
            Log.i(PdfOps.TAG, "decryptPdf: 文件不存在")
            return
        }
        val file = File(filePaths)
        if (!file.exists()) {
            Log.i(PdfOps.TAG, "decryptPdf: 文件不存在")
            return
        }
        //拆分pdf文件
        var targetFile =
            File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS)
        targetFile = File(targetFile, "${packageName}_${System.currentTimeMillis()}_split.pdf")
        PdfOps.randomPageSplitPdf(this,file, targetFile)
    }

}