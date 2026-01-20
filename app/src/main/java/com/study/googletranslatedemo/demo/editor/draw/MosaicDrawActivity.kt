package com.study.googletranslatedemo.demo.editor.draw

import android.widget.SeekBar
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityDrawMosaicBinding
import com.study.googletranslatedemo.demo.editor.draw.custom.MosaicPaintView
import com.study.googletranslatedemo.utils.DataLooper
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.expand.dp

class MosaicDrawActivity : BaseAct<ActivityDrawMosaicBinding>() {
    private val _photoPicker = PhotoPicker()
    private val _modeSet = DataLooper(
        listOf(
            MosaicPaintView.MosaicPaintConfig.BlurMosaic,
            MosaicPaintView.MosaicPaintConfig.ImageMosaic(R.drawable.mosaic_1),
            MosaicPaintView.MosaicPaintConfig.ImageMosaic(R.drawable.mosaic_2),
            MosaicPaintView.MosaicPaintConfig.ImageMosaic(R.drawable.mosaic_3),
            MosaicPaintView.MosaicPaintConfig.ImageMosaic(R.drawable.mosaic_4),
        )
    )

    override fun getViewBinding() = ActivityDrawMosaicBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                binding.paintView.setImageBitmap(bmp)
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.paintView.setOperateStackListener(object : OperateStackListener {
            override fun onStackChanged(canUndo: Boolean, canRedo: Boolean) {
                binding.btnUndo.isEnabled = canUndo
                binding.btnRedo.isEnabled = canRedo
            }
        })
        binding.btnUndo.setOnClickListener { binding.paintView.undo() }
        binding.btnRedo.setOnClickListener { binding.paintView.redo() }
        binding.btnExport.setOnClickListener { export() }
        binding.btnMode.apply {
            setOnClickListener {
                val mode = _modeSet.next()
                if (mode != null) {
                    binding.paintView.paintConfig = mode
                }
            }
            performClick()
        }
        binding.sbBrushSize.apply {
            min = 10
            max = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val size = ((seekBar?.progress)?.toFloat() ?: 25f).dp(this@MosaicDrawActivity)
                    binding.paintView.brushSize = size
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
            progress = 25
        }
    }

    private fun export() {
        // TODO:
    }
}