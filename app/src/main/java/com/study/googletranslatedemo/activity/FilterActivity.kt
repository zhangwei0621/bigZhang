package com.study.googletranslatedemo.activity

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
    private val _filterImage = FilterImage()
    private val _filterSet = DataLooper(
        listOf(
            FilterRule.BLACK,
            FilterRule.SKETCH,
            FilterRule.BLUR,
            FilterRule.BLACK_AND_BLUR,
            ""
        )
    )

    override fun getViewBinding() = ActivityFilterBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                _filterImage.clear()
                _filterImage.setSrcBitmap(bmp)
                binding.img.setImageBitmap(_filterImage.getDstBitmap())
                binding.tvRule.text = ""
                _filterSet.reset()
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.btnFilter.setOnClickListener {
            val rule = _filterSet.next()
            binding.tvRule.text = rule
            binding.img.setImageBitmap(_filterImage.applyRule(rule))
        }
    }
}