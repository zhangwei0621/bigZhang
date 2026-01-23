package com.study.googletranslatedemo.demo.editor.effect.splash

import android.app.Activity
import android.widget.SeekBar
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityFilterPaintBinding
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.expand.addParams
import com.study.googletranslatedemo.utils.expand.createIntent
import com.study.googletranslatedemo.utils.expand.getString
import com.study.googletranslatedemo.utils.expand.start

/**
 * 自由画笔+滤镜测试
 */
class FilterPaintActivity : BaseAct<ActivityFilterPaintBinding>() {
    companion object {
        private const val EXTRA_FILTER = "extra_filter"
        fun start(activity: Activity, filter: String) {
            createIntent(FilterPaintActivity::class.java)
                .addParams(EXTRA_FILTER to filter)
                .start(activity)
        }
    }

    private val _photoPicker = PhotoPicker()

    override fun getViewBinding() = ActivityFilterPaintBinding.inflate(layoutInflater)

    override fun initView() {
        intent?.getString(EXTRA_FILTER, "")?.takeIf { it.isNotBlank() }?.let { filter ->
            binding.img.filter = filter
        }
        _photoPicker.register(this) { uri ->
            BitmapUtils.smartUriToBitmap(this, uri)?.let { bmp ->
                binding.img.setImageBitmap(bmp)
            }
        }
        binding.btnEraser.setOnClickListener {
            val info = if (binding.img.switchPaintMode()) "滤镜" else "原图"
            binding.btnEraser.text = "画笔模式：$info"
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.img.setOperateStackListener(object : OperateStackListener {
            override fun onStackChanged(canUndo: Boolean, canRedo: Boolean) {
                binding.btnUndo.isEnabled = canUndo
                binding.btnRedo.isEnabled = canRedo
            }
        })
        binding.btnUndo.setOnClickListener { binding.img.undo() }
        binding.btnRedo.setOnClickListener { binding.img.redo() }
        binding.sbRestoreIntensity.apply {
            setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.img.restorePaintIntensity = progress
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
            progress = 175
        }
        binding.sbFilterIntensity.apply {
            setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.img.filterPaintIntensity = progress
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
            progress = 175
        }
    }
}