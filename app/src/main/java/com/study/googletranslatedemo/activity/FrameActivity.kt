package com.study.googletranslatedemo.activity

import android.widget.SeekBar
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityFrameBinding
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils

class FrameActivity : BaseAct<ActivityFrameBinding>() {
    private val _photoPicker = PhotoPicker()

    override fun getViewBinding() = ActivityFrameBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                binding.img.setImageBitmap(bmp)
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.sbWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                binding.img.frameWidth = progress * 1f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }
}