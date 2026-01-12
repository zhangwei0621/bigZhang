package com.study.googletranslatedemo.activity

import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityAdjustBinding
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.filter.FilterImage
import com.study.googletranslatedemo.utils.processor.BlurProcessor
import com.study.googletranslatedemo.utils.processor.BrightnessProcessor
import com.study.googletranslatedemo.utils.processor.ContrastProcessor
import com.study.googletranslatedemo.utils.processor.SaturationProcessor
import com.study.googletranslatedemo.utils.processor.SharpenProcessor
import com.study.googletranslatedemo.utils.processor.base.BaseProcessor

/**
 * 测试基本的参数调节。还有很多参数可用，这里只测试几个简单的
 */
class AdjustActivity : BaseAct<ActivityAdjustBinding>() {
    private val _photoPicker = PhotoPicker()
    private val _filterImage = FilterImage()
    private val _brightness = BrightnessProcessor { applyProcessor() }
    private val _contrast = ContrastProcessor { applyProcessor() }
    private val _saturation = SaturationProcessor { applyProcessor() }
    private val _sharpen = SharpenProcessor { applyProcessor() }
    private val _blur = BlurProcessor { applyProcessor() }

    override fun getViewBinding() = ActivityAdjustBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                _filterImage.setSrcBitmap(bmp)
                reset()
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.sbBrightness.bindProcessor(_brightness)
        binding.sbContrast.bindProcessor(_contrast)
        binding.sbSaturation.bindProcessor(_saturation)
        binding.sbSharpen.bindProcessor(_sharpen)
        binding.sbBlur.bindProcessor(_blur)
    }

    private fun reset() {
        binding.apply {
            sbBrightness.progress = _brightness.defaultValue
            sbContrast.progress = _contrast.defaultValue
            sbSaturation.progress = _saturation.defaultValue
            sbSharpen.progress = _sharpen.defaultValue
            sbBlur.progress = _blur.defaultValue
        }
        applyProcessor()
    }

    private fun applyProcessor() {
        // 处理器之间的空格不是必须的，只是为了好看
        val process = StringBuilder().apply {
            append(_brightness.buildProcessor())
            append("\n")
            append(_contrast.buildProcessor())
            append("\n")
            append(_saturation.buildProcessor())
            append("\n")
            append(_sharpen.buildProcessor())
            append("\n")
            append(_blur.buildProcessor())
        }.toString()
        binding.tvRule.text = process
        binding.img.setImageBitmap(_filterImage.applyProcessor(process))
    }

    private fun AppCompatSeekBar.bindProcessor(processor: BaseProcessor) {
        min = processor.minValue
        max = processor.maxValue
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                processor.setValue(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }
}