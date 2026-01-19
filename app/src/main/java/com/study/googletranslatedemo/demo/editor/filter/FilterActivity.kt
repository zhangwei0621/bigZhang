package com.study.googletranslatedemo.demo.editor.filter

import android.graphics.Bitmap
import android.widget.SeekBar
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityFilterBinding
import com.study.googletranslatedemo.utils.DataLooper
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.filter.FilterImage
import com.study.googletranslatedemo.utils.filter.FilterRule

/**
 * 测试基本滤镜功能
 */
class FilterActivity : BaseAct<ActivityFilterBinding>() {
    private val _photoPicker = PhotoPicker()
    private val _filterSet = DataLooper(
        listOf(
            FilterRule.BLACK,
            FilterRule.SKETCH,
            FilterRule.BLUR,
            FilterRule.BLACK_AND_BLUR,
            FilterRule.EFFECT_BLEND_1,
            FilterRule.EFFECT_BLEND_2,
            FilterRule.LUT_1,
            FilterRule.LUT_2,
            FilterRule.LUT_3,
            FilterRule.LUT_4,
            FilterRule.LUT_5,
            FilterRule.LUT_6,
            ""
        )
    )
    private var _srcBitmap: Bitmap? = null
    private var _intensity: Float = 1f
    private var _filter: String? = ""

    override fun getViewBinding() = ActivityFilterBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                _srcBitmap = bmp
                binding.img.setImageBitmap(bmp)
                _filterSet.reset()
                _filter = ""
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.btnFilter.setOnClickListener {
            _filter = _filterSet.next()
            updateImage()
        }
        binding.sbIntensity.apply {
            progress = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    _intensity = progress / 100f
                    updateImage()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            })
        }
    }

    private fun updateImage() {
        binding.tvRule.text = _filter
        binding.img.setImageBitmap(FilterImage.applyFilter(_srcBitmap, _filter, _intensity))
    }
}