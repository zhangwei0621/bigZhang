package com.study.openpdfdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lowagie.text.pdf.PdfAnnotation
import com.study.openpdfdemo.databinding.ActivityPdfViewerBinding
import com.study.openpdfdemo.utils.toast
import com.study.openpdfdemo.viewer.PdfCoreCore
import com.study.openpdfdemo.viewer.data.PDFColorWrap
import com.study.openpdfdemo.viewer.tool.PdfCoreListener
import com.study.openpdfdemo.viewer.tool.PdfOverlayListener
import com.study.openpdfdemo.viewer.tool.TextSearchHelper
import com.study.openpdfdemo.viewer.tool.TextStripper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@SuppressLint("SetTextI18n")
class PdfViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewerBinding
    private lateinit var pdfCoreCore: PdfCoreCore
    private var current = 1
    private val toolStateFlow = MutableStateFlow(DemoToolState.Nothing)
    private val testAsset = "ASM4_2.pdf"
    private val testLockedAsset = "ASM4_2_locked_password_111.pdf"

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PdfViewerActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        val file = copyPdfFromAssets(this, testAsset, false)
        if (!file.exists()) return
        pdfCoreCore = PdfCoreCore(file, "111", PdfCoreCore.getCacheDir(this))
        binding.tvPath.text = file.absolutePath
        binding.btnRefreshPage.setOnClickListener {
            renderCurrentPage()
        }
        binding.btnLastPage.setOnClickListener {
            lastPage()
        }
        binding.btnNextPage.setOnClickListener {
            nextPage()
        }
        binding.btnTool.setOnClickListener {
            var next = toolStateFlow.value.ordinal + 1
            if (next >= DemoToolState.entries.size) {
                next = 0
            }
            toolStateFlow.value = DemoToolState.entries[next]
        }
        binding.btnSave.setOnClickListener {
            pdfCoreCore.save()
            "保存完成".toast(this)
            renderCurrentPage()
        }
        binding.btnDontSave.setOnClickListener {
            pdfCoreCore.abandonSave()
            "抛弃编辑操作完成".toast(this)
            renderCurrentPage()
        }
        renderCurrentPage()
        binding.demoViewer.setOverlayListener(object : PdfOverlayListener {
            override fun onInkFinish(line: FloatArray) {
                pdfCoreCore.addInk(current, line, DemoToolState.Ink.colorWrap)
                renderCurrentPage()
            }

            override fun requireStructuredText(): List<TextStripper.Line> {
                return pdfCoreCore.requireStructuredText(current)
            }

            override fun onSelectTextResult(textLines: List<TextStripper.Line>) {
                val currentTool = toolStateFlow.value
                val markupType = when (currentTool) {
                    DemoToolState.Highlight -> PdfAnnotation.MARKUP_HIGHLIGHT
                    DemoToolState.Underline -> PdfAnnotation.MARKUP_UNDERLINE
                    DemoToolState.StrokeOut -> PdfAnnotation.MARKUP_STRIKEOUT
                    else -> return
                }
                pdfCoreCore.addMarkup(
                    current,
                    markupType,
                    textLines,
                    currentTool.colorWrap
                )
                renderCurrentPage()
            }
        })
        lifecycleScope.launch {
            toolStateFlow.collect {
                binding.demoViewer.setToolState(it)
                binding.btnTool.text = "激活工具：${it.toolName}"
            }
        }
        pdfCoreCore.setCoreListener(object : PdfCoreListener {
            override fun onRedoUndoStateChanged(canUndo: Boolean, canRedo: Boolean) {
                binding.btnUndo.isEnabled = canUndo
                binding.btnRedo.isEnabled = canRedo
            }

            override fun onSaveStateChanged(needSave: Boolean) {
                binding.btnSave.isEnabled = needSave
                binding.btnDontSave.isEnabled = needSave
            }
        })
        binding.btnUndo.setOnClickListener {
            if (pdfCoreCore.undo()) {
                renderCurrentPage()
            }
        }
        binding.btnRedo.setOnClickListener {
            if (pdfCoreCore.redo()) {
                renderCurrentPage()
            }
        }
        binding.btnSearch.setOnClickListener {
            val keyword = "类"
            val searchResult = TextSearchHelper().search(
                pdfCoreCore.requireStructuredText(current),
                keyword,
                current
            )
            binding.demoViewer.setSearchResult(searchResult)
            if (searchResult.isEmpty()) {
                "啥也没找到"
            } else {
                "找到${searchResult.size}个结果"
            }.toast(this)
        }
    }

    private fun renderCurrentPage() {
        val dm = resources.displayMetrics
        pdfCoreCore.renderPage(current, dm.widthPixels, dm.heightPixels)?.apply {
            binding.demoViewer.setBitmap(second, first)
        }
        binding.tvIndex.text = "${current}/${pdfCoreCore.pageCount}"
    }

    private fun lastPage() {
        current = (current - 1).coerceIn(1, pdfCoreCore.pageCount)
        renderCurrentPage()
    }

    private fun nextPage() {
        current = (current + 1).coerceIn(1, pdfCoreCore.pageCount)
        renderCurrentPage()
    }

    fun copyPdfFromAssets(context: Context, assetName: String, forceOverwrite: Boolean): File {
        val file = File(context.cacheDir, assetName)
        if (!file.exists() || file.length() <= 0L || forceOverwrite) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfCoreCore.destroy()
    }

    enum class DemoToolState(
        val toolName: String,
        val colorWrap: PDFColorWrap,
    ) {
        Nothing("无", PDFColorWrap(0f, 0f, 0f)),
        Ink("画笔", PDFColorWrap(0f, 1f, 0f)),
        Highlight("高光", PDFColorWrap(1f, 1f, 0f)),
        Underline("下划线", PDFColorWrap(0f, 0f, 1f)),
        StrokeOut("划除线", PDFColorWrap(1f, 0f, 0f))
    }
}