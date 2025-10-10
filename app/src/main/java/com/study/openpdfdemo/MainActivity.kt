package com.study.openpdfdemo

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.study.openpdfdemo.databinding.ActivityMainBinding
import com.study.openpdfdemo.utils.GmsScanHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val gmsScanHelper = GmsScanHelper()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        gmsScanHelper.register(this) {
            if (it == null) return@register
            gmsScanHelper.savePdf(this, it)
        }
        binding.apply {
            createPdf.setOnClickListener {
                createPdf()
            }
            previewPdf.setOnClickListener {
                // TODO: 预览pdf
            }
            encryptionPdf.setOnClickListener {
                // TODO: 加密pdf
            }
            decryptPdf.setOnClickListener {
                // TODO: 解密pdf
            }
            mergePdf.setOnClickListener {
                // TODO: 合并pdf
            }
            splitPdf.setOnClickListener {
                // TODO: 拆分pdf
            }
        }
    }


    private fun createPdf() {
        gmsScanHelper.startScanner(this)
    }
}