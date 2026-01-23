package com.study.googletranslatedemo.demo.editor.sticker

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityStickerBinding
import com.study.googletranslatedemo.develop.segmentation.tool.SegmentationHelper
import com.study.googletranslatedemo.utils.FileUtils
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.expand.toast
import com.study.googletranslatedemo.utils.gesture.DragPinchRotateTouchListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StickerActivity : BaseAct<ActivityStickerBinding>() {
    private val _segmentHelper = SegmentationHelper()
    private val _photoPicker = PhotoPicker()
    private var _currentStickerView: AppCompatImageView? = null
    private var _hasSetSrcImage: Boolean = false
        set(value) {
            field = value
            binding.btnAddSticker.isEnabled = value
            binding.btnExport.isEnabled = value
        }

    override fun getViewBinding() = ActivityStickerBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { startFunction(it) }
        binding.root.setOnClickListener { selectSticker(null) }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.btnAddSticker.setOnClickListener { addNewSticker() }
        binding.btnExport.setOnClickListener {
            binding.frameContainer.apply {
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)
                draw(canvas)
                FileUtils.saveBitmap(this@StickerActivity, "sticker", bitmap)
            }
        }
    }

    private fun startFunction(uri: Uri) = lifecycleScope.launch {
        val context: Context = this@StickerActivity
        binding.pbLoading.visibility = View.VISIBLE
        // 加载测试图像
        val srcBitmap = withContext(Dispatchers.IO) {
            BitmapUtils.uriToBitmap(context, uri)
        }
        if (srcBitmap == null) {
            binding.pbLoading.visibility = View.GONE
            return@launch
        }

        // 图像分割
        if (!_segmentHelper.isInitialized) {
            _segmentHelper.initialize(context)
        }
        val segmentBitmap = withContext(Dispatchers.IO) {
            _segmentHelper.segmentImage(srcBitmap)
        }
        if (segmentBitmap == null) {
            // 失败也没关系，只不过没有防遮挡的效果
            context.toast("分割图像失败")
        } else {
            context.toast("分割图像成功")
        }

        clearAllSticker()
        binding.srcImage.setImageBitmap(srcBitmap)
        binding.srcImageForeground.setImageBitmap(segmentBitmap)
        _hasSetSrcImage = true
        binding.pbLoading.visibility = View.GONE
    }

    private fun addNewSticker() {
        AppCompatImageView(this).apply {
            setImageResource(R.drawable.sticker)
            layoutParams = RelativeLayout.LayoutParams(350, 350)
            setOnTouchListener(DragPinchRotateTouchListener())
            setOnClickListener { selectSticker(this) }
            binding.frameSticker.addView(this)
            selectSticker(this)
        }
    }

    private fun clearAllSticker() {
        binding.frameSticker.removeAllViews()
    }

    private fun selectSticker(view: AppCompatImageView?) {
        _currentStickerView = view
        updateController()
    }

    private fun updateController() {
        val stickerView = _currentStickerView
        binding.sbAlpha.apply {
            max = 255
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    stickerView?.imageAlpha = progress
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
            progress = stickerView?.imageAlpha ?: 255
        }
        binding.llController.visibility = if (stickerView != null) View.VISIBLE else View.GONE
        binding.btnDelete.setOnClickListener {
            if (stickerView != null && stickerView.parent != null) {
                binding.frameSticker.removeView(stickerView)
                selectSticker(null)
            }
        }
    }
}