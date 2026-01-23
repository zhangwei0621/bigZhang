package com.study.googletranslatedemo.develop.segmentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivitySegmentationBinding
import com.study.googletranslatedemo.develop.segmentation.tool.SegmentationHelper
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SegmentationActivity : BaseAct<ActivitySegmentationBinding>() {
    private val _segmentationHelper = SegmentationHelper()
    private val _photoPicker = PhotoPicker()

    override fun getViewBinding() = ActivitySegmentationBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { startFromUri(it) }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.btnLocal.setOnClickListener { startLocalTest() }
    }

    private fun startFromUri(uri: Uri) = lifecycleScope.launch {
        binding.tvInfo.text = "执行中"
        clearImageView()
        val srcBitmap = withContext(Dispatchers.IO) { loadImageFromUri(uri) }
        if (srcBitmap == null) {
            binding.tvInfo.text = "解析uri失败"
            return@launch
        }
        startTest(srcBitmap)
    }

    private fun startLocalTest() = lifecycleScope.launch {
        binding.tvInfo.text = "执行中"
        clearImageView()
        val srcBitmap = withContext(Dispatchers.IO) { loadLocalTestImage() }
        startTest(srcBitmap)
    }

    private fun loadImageFromUri(uri: Uri): Bitmap? {
        val dpi = resources.displayMetrics.densityDpi
        return BitmapUtils.uriToBitmap(
            this,
            uri,
            dpi,
            dpi / 4
        )
    }

    private fun loadLocalTestImage(): Bitmap {
        val dpi = resources.displayMetrics.densityDpi
        return BitmapFactory.decodeResource(
            resources,
            R.drawable.human_3,
            BitmapFactory.Options().apply {
                inDensity = dpi
                inTargetDensity = dpi / 4
            }
        )
    }

    private suspend fun startTest(srcBitmap: Bitmap) {
        binding.tvInfo.text = "执行中"
        clearImageView()
        if (!_segmentationHelper.isInitialized) {
            if (!_segmentationHelper.initialize(this)) {
                binding.tvInfo.text = "初始化失败"
                return
            }
        }
        val result = withContext(Dispatchers.IO) {
            _segmentationHelper.segmentImage(srcBitmap)
        }
        binding.tvInfo.text = if (result != null) {
            "成功"
        } else {
            "失败"
        }
        binding.imgSrc.setImageBitmap(srcBitmap)
        binding.imgOutput.setImageBitmap(result)
    }

    private fun clearImageView() {
        binding.imgSrc.setImageBitmap(null)
        binding.imgOutput.setImageBitmap(null)
    }
}