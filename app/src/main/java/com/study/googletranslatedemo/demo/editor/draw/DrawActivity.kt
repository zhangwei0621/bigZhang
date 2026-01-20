package com.study.googletranslatedemo.demo.editor.draw

import android.graphics.Canvas
import android.graphics.Color
import android.widget.SeekBar
import androidx.core.graphics.createBitmap
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityDrawBinding
import com.study.googletranslatedemo.demo.editor.draw.custom.MagicPaintView
import com.study.googletranslatedemo.utils.DataLooper
import com.study.googletranslatedemo.utils.FileUtils
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.expand.dp

/**
 * Draw功能demo，不包括马赛克模式
 */
class DrawActivity : BaseAct<ActivityDrawBinding>() {
    private val _modeSet = DataLooper(MagicPaintView.BrushMode.entries)
    private val _colorSet = DataLooper(
        listOf(
            Color.RED, Color.WHITE, Color.BLUE, Color.BLACK
        )
    )

    override fun getViewBinding() = ActivityDrawBinding.inflate(layoutInflater)

    override fun initView() {
        initMagicResource()
        binding.llController.setOnClickListener { }
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
                    text = "模式：${mode.name}"
                    binding.paintView.brushMode = mode
                }
            }
            performClick()
        }
        binding.btnBrushColor.apply {
            setOnClickListener {
                val color = _colorSet.next()
                if (color != null) {
                    setBackgroundColor(color)
                    binding.paintView.brushColor = color
                }
            }
            performClick()
        }
        binding.sbBrushSize.apply {
            min = 10
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val size = (seekBar?.progress?.toFloat() ?: 20f).dp(this@DrawActivity)
                    binding.paintView.brushSize = size
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
            progress = 25
        }
        binding.sbBrushAlpha.apply {
            min = 0
            max = 255
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val alpha = seekBar?.progress ?: 255
                    binding.paintView.brushAlpha = alpha
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
            progress = 255
        }
    }

    private fun initMagicResource() {
        binding.paintView.setMagicItemResources(
            listOf(
                R.drawable.magic_a_1,
                R.drawable.magic_a_2,
                R.drawable.magic_a_3,
                R.drawable.magic_a_4
            )
        )
    }

    private fun export() {
        // 构建原图，仅测试，正式使用时一般会用编辑器当前的图像bitmap
        val srcBitmap = createBitmap(binding.imgSrc.width, binding.imgSrc.height)
        val srcCanvas = Canvas(srcBitmap)
        binding.imgSrc.draw(srcCanvas)

        // 开始导出
        val exportBitmap = binding.paintView.paintTo(srcBitmap) ?: return
        FileUtils.saveBitmap(this, "draw", exportBitmap)
    }
}