package com.study.googletranslatedemo.activity

import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityAdjustBinding
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.adjust.BlurAdjust
import com.study.googletranslatedemo.utils.adjust.BrightnessAdjust
import com.study.googletranslatedemo.utils.adjust.ContrastAdjust
import com.study.googletranslatedemo.utils.adjust.SaturationAdjust
import com.study.googletranslatedemo.utils.adjust.SharpenAdjust
import com.study.googletranslatedemo.utils.adjust.base.BaseAdjust
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.filter.FilterImage

/**
 * 测试基本的参数调节。还有很多参数可用，这里只测试几个简单的
 */
class AdjustActivity : BaseAct<ActivityAdjustBinding>() {
    private val _photoPicker = PhotoPicker()
    private val _filterImage = FilterImage()
    private val _brightness = BrightnessAdjust { applyAdjust() }
    private val _contrast = ContrastAdjust { applyAdjust() }
    private val _saturation = SaturationAdjust { applyAdjust() }
    private val _sharpen = SharpenAdjust { applyAdjust() }
    private val _blur = BlurAdjust { applyAdjust() }

    override fun getViewBinding() = ActivityAdjustBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                _filterImage.setSrcBitmap(bmp)
                reset()
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.sbBrightness.bindAdjust(_brightness)
        binding.sbContrast.bindAdjust(_contrast)
        binding.sbSaturation.bindAdjust(_saturation)
        binding.sbSharpen.bindAdjust(_sharpen)
        binding.sbBlur.bindAdjust(_blur)
    }

    private fun reset() {
        binding.apply {
            sbBrightness.progress = _brightness.defaultValue
            sbContrast.progress = _contrast.defaultValue
            sbSaturation.progress = _saturation.defaultValue
            sbSharpen.progress = _sharpen.defaultValue
            sbBlur.progress = _blur.defaultValue
        }
        applyAdjust()
    }

    private fun applyAdjust() {
        // rule之间的空格不是必须的，只是为了好看
        val rule = StringBuilder().apply {
            append(_brightness.buildRule())
            append(" ")
            append(_contrast.buildRule())
            append(" ")
            append(_saturation.buildRule())
            append(" ")
            append(_sharpen.buildRule())
            append(" ")
            append(_blur.buildRule())
        }.toString()
        binding.tvRule.text = rule
        binding.img.setImageBitmap(_filterImage.applyRule(rule))
    }

    private fun AppCompatSeekBar.bindAdjust(adjust: BaseAdjust) {
        min = adjust.minValue
        max = adjust.maxValue
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                adjust.setValue(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }
}