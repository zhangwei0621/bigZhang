package com.study.googletranslatedemo.demo.editor.effect.motion

import android.net.Uri
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityMotionBinding
import com.study.googletranslatedemo.demo.editor.effect.motion.custom.MotionImageView
import com.study.googletranslatedemo.develop.segmentation.tool.SegmentationHelper
import com.study.googletranslatedemo.utils.FileUtils
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.isValid
import com.study.googletranslatedemo.utils.expand.simpleBind
import com.study.googletranslatedemo.utils.expand.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MotionActivity : BaseAct<ActivityMotionBinding>() {
    private val _photoPicker = PhotoPicker()
    private val _config = MotionImageView.EffectConfig(3, 175, 0)
    private val _segmentHelper = SegmentationHelper()

    override fun getViewBinding() = ActivityMotionBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { startFunction(it) }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.btnExport.setOnClickListener { export() }
        binding.sbCount.apply {
            min = 0
            max = 50
            progress = _config.count
            simpleBind {
                _config.count = it
                applyConfig()
            }
        }
        binding.sbAlpha.apply {
            min = 0
            max = 255
            progress = _config.alpha
            simpleBind {
                _config.alpha = it
                applyConfig()
            }
        }
        binding.sbRotate.apply {
            min = 0
            max = 360
            progress = _config.rotate
            simpleBind {
                _config.rotate = it
                applyConfig()
            }
        }
    }

    private fun startFunction(uri: Uri) = lifecycleScope.launch {
        val context = this@MotionActivity
        binding.pbLoading.visibility = View.VISIBLE
        val onFinish: (String) -> Unit = { msg ->
            binding.pbLoading.visibility = View.GONE
            context.toast(msg)
        }
        val srcBitmap = withContext(Dispatchers.IO) { BitmapUtils.smartUriToBitmap(context, uri) }
        if (srcBitmap == null) {
            onFinish("解析Uri失败")
            return@launch
        }

        if (!_segmentHelper.isInitialized) {
            if (!_segmentHelper.initialize(context)) {
                onFinish("分割器初始化失败")
                return@launch
            }
        }

        val segmentBitmap = withContext(Dispatchers.IO) { _segmentHelper.segmentImage(srcBitmap) }
        if (segmentBitmap?.isValid() != true) {
            onFinish("图像分割失败")
            return@launch
        }

        binding.imgMotion.setImageBitmap(srcBitmap)
        binding.imgMotion.setMotionBitmap(segmentBitmap)
        applyConfig()
        onFinish("成功")
    }

    private fun applyConfig() {
        binding.imgMotion.setEffectConfig(_config)
    }

    private fun export() {
        val bitmap = binding.imgMotion.export()
        FileUtils.saveBitmap(this, "motion", bitmap)
        toast("保存成功")
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.imgMotion.release()
    }
}