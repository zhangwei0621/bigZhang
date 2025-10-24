package com.study.openpdfdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.study.openpdfdemo.databinding.ActivityPdfViewerBinding
import com.study.openpdfdemo.utils.DataLooper
import com.study.openpdfdemo.utils.Tools.copyPdfFromAssets
import com.study.openpdfdemo.utils.toast
import com.study.openpdfdemo.viewer.data.PageTool
import com.study.openpdfdemo.viewer.data.SearchDirection
import com.study.openpdfdemo.viewer.view.PdfView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetTextI18n")
class PdfViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewerBinding
    private val testAsset = "ASM4_2.pdf"
    private val testLockedAsset = "ASM4_2_locked_password_111.pdf"
    private val editToolLoop = DataLooper(PageTool.entries)

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
        binding.tvPath.text = file.absolutePath
        binding.pdfView.setCoreListener(object : PdfView.PdfViewInterface {
            override fun onRedoUndoStateChanged(canUndo: Boolean, canRedo: Boolean) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        binding.btnUndo.isEnabled = canUndo
                        binding.btnRedo.isEnabled = canRedo
                    }
                }
            }

            override fun onSaveStateChanged(needSave: Boolean) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        binding.btnSave.isEnabled = needSave
                        binding.btnDontSave.isEnabled = needSave
                    }
                }
            }

            override fun onDisplayPageChanged(pageIndex: Int, total: Int) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        binding.tvIndex.text = "${pageIndex + 1}/$total"
                    }
                }
            }
        })
        lifecycleScope.launch {
            binding.pdfView.openFile(file, "111")
        }
        binding.btnRefreshPage.setOnClickListener {
            binding.pdfView.redraw()
        }
        binding.btnLastPage.setOnClickListener {
            binding.pdfView.lastPage()
        }
        binding.btnNextPage.setOnClickListener {
            binding.pdfView.nextPage()
        }
        binding.btnTool.setOnClickListener {
            editToolLoop.next()?.let {
                binding.btnTool.text = "工具：${it.name}"
                binding.pdfView.setPageEditTool(it)
            }
        }
        binding.btnTool.setOnLongClickListener {
            editToolLoop.reset()
            binding.btnTool.text = "工具：无"
            binding.pdfView.setPageEditTool(null)
            true
        }
        binding.btnTool.performLongClick()
        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    binding.pdfView.save()
                }
                binding.pdfView.redraw()
            }
        }
        binding.btnDontSave.setOnClickListener {
            binding.pdfView.abandonSave()
            binding.pdfView.redraw()
        }
        binding.btnUndo.setOnClickListener {
            binding.pdfView.undo()
        }
        binding.btnRedo.setOnClickListener {
            binding.pdfView.redo()
        }
        binding.btnSearch.setOnClickListener {
            if (binding.pdfView.search("程序")) {
                "搜索到文本"
            } else {
                "啥也没找到"
            }.toast(this)
        }
        binding.btnSearch.setOnLongClickListener {
            binding.pdfView.clearSearchResult()
            true
        }
        binding.tvPath.setOnClickListener {
            binding.llBtn.visibility = if (binding.llBtn.isVisible) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        binding.btnSearchLast.setOnClickListener {
            if (!binding.pdfView.nextSearchResult(SearchDirection.BACKWARD)) {
                "没有了".toast(this)
            }
        }
        binding.btnSearchNext.setOnClickListener {
            if (!binding.pdfView.nextSearchResult(SearchDirection.FORWARD)) {
                "没有了".toast(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pdfView.destroy()
    }
}