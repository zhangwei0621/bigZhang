package com.study.openpdfdemo

import android.os.Bundle
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

    private var newsPdfUrl: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        gmsScanHelper.register(this) {
            if (it == null) return@register
            newsPdfUrl = gmsScanHelper.savePdf(this, it)
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
                // TODO: 合并pdf
            }
            splitPdf.setOnClickListener {
                // TODO: 拆分pdf
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
        if (newsPdfUrl.isEmpty()) return
        val file = File(newsPdfUrl)
        if (!file.exists()) return
        if (PdfOps.isEncrypted(file)) {
            Log.i("TAG", "encryptionPdf: 文件已加密")
            return
        }
        PdfOps.encryptPdfInPlace(file, "123456",this)
    }

    /**
     * 解密pdf
     */
    private fun decryptPdf() {
        if (newsPdfUrl.isEmpty()) return
        val file = File(newsPdfUrl)
        if (!file.exists()) return
        if (!PdfOps.isEncrypted(file)) {
            Log.i("TAG", "encryptionPdf: 文件未加密")
            return
        }
        PdfOps.decryptPdfReplace(file, "123456",this)
    }

}