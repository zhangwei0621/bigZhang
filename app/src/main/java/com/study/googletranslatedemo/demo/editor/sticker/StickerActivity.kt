package com.study.googletranslatedemo.demo.editor.sticker

import android.graphics.Canvas
import android.view.View
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityStickerBinding
import com.study.googletranslatedemo.utils.FileUtils
import com.study.googletranslatedemo.utils.gesture.DragPinchRotateTouchListener

class StickerActivity : BaseAct<ActivityStickerBinding>() {
    private var _currentStickerView: AppCompatImageView? = null

    override fun getViewBinding() = ActivityStickerBinding.inflate(layoutInflater)

    override fun initView() {
        binding.root.setOnClickListener { selectSticker(null) }
        binding.btnAddSticker.setOnClickListener { addNewSticker() }
        binding.btnExport.setOnClickListener {
            binding.frameContainer.apply {
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)
                draw(canvas)
                FileUtils.saveBitmap(this@StickerActivity, "sticker", bitmap)
            }
        }
        setImage()
    }

    private fun setImage() {
        // 设置原图
        binding.srcImage.setImageResource(R.drawable.sample_1)
        // 设置防遮挡图层，没有的话可以不设置
        binding.srcImageForeground.setImageResource(R.drawable.sample_1_d)
    }

    private fun addNewSticker() {
        AppCompatImageView(this).apply {
            setImageResource(R.drawable.sticker)
            layoutParams = RelativeLayout.LayoutParams(350, 350)
            // 这个是Ai扒竞品代码实现的，需要审核一下
            setOnTouchListener(DragPinchRotateTouchListener())
            setOnClickListener { selectSticker(this) }
            binding.frameSticker.addView(this)
            selectSticker(this)
        }
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