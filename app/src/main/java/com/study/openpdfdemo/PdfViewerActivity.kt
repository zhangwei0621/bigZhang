package com.study.openpdfdemo

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.study.openpdfdemo.databinding.ActivityPdfViewerBinding
import com.study.openpdfdemo.viewer.PdfCore
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewerBinding
    private lateinit var pdfCore: PdfCore
    private var current = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        val testFile = File("/storage/emulated/0/ASM4_1.pdf")
        pdfCore = PdfCore(testFile)
        pdfCore.renderPage(current).apply {
            //第一次先使用安卓的方法，确保文件没有异常
            binding.imgPdf.setImageBitmap(this)
        }
        binding.imgPdf.setOnClickListener {
            current++
            //这里测试openpdf的渲染
            pdfCore.renderPageByOpenPdf(current).apply {
                binding.imgPdf.setImageBitmap(this)
                if (this == null) {
                    current = 0
                }
            }
        }
    }
}