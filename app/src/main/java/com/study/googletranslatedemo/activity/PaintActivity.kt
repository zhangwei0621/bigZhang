package com.study.googletranslatedemo.activity

import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.data.PaintEffect
import com.study.googletranslatedemo.databinding.ActivityPaintBinding
import com.study.googletranslatedemo.utils.DataLooper
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils

/**
 * 自由画笔测试
 */
class PaintActivity : BaseAct<ActivityPaintBinding>() {
    private val _photoPicker = PhotoPicker()
    private val _paintEffectSet = DataLooper(
        listOf(
            PaintEffect.Texture,
            PaintEffect.Neon,
            PaintEffect.None
        )
    )

    override fun getViewBinding() = ActivityPaintBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                binding.img.setImageBitmap(bmp)
            }
        }
        binding.btnPaintEffect.setOnClickListener {
            val effect = _paintEffectSet.next()
            if (effect != null) {
                binding.btnPaintEffect.text = "当前画笔特效：$effect"
                binding.img.setPaintEffect(effect)
            }
        }
        binding.btnEraser.setOnClickListener {
            val info = if (binding.img.switchEraserMode()) "ON" else "OFF"
            binding.btnEraser.text = "橡皮擦模式：$info"
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
    }
}