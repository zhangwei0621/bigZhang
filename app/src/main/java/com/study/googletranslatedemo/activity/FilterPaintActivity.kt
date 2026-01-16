package com.study.googletranslatedemo.activity

import android.widget.SeekBar
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityFilterPaintBinding
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils

/**
 * 自由画笔+滤镜测试
 */
class FilterPaintActivity : BaseAct<ActivityFilterPaintBinding>() {
    private val _photoPicker = PhotoPicker()

    override fun getViewBinding() = ActivityFilterPaintBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
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
        binding.sbRestoreIntensity.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                binding.img.setRestorePaintIntensity(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        binding.sbFilterIntensity.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                binding.img.setFilterPaintIntensity(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }
}