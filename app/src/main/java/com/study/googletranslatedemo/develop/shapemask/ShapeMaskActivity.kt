package com.study.googletranslatedemo.develop.shapemask

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.widget.Toast
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityShapeMaskBinding
import com.study.googletranslatedemo.utils.DataLooper
import com.study.googletranslatedemo.utils.FileUtils
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.tryRecycle

/**
 * 这里测试形状遮罩效果
 */
class ShapeMaskActivity : BaseAct<ActivityShapeMaskBinding>() {
    private val _photoPicker = PhotoPicker()
    private var _maskBitmap: Bitmap? = null
    private var _isMaskEnable = true
    private val _maskModeSet = DataLooper(PorterDuff.Mode.entries)

    override fun getViewBinding() = ActivityShapeMaskBinding.inflate(layoutInflater)

    override fun initView() {
        _maskBitmap = BitmapFactory.decodeResource(resources, R.drawable.mask_1)
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                binding.img.setImageBitmap(bmp)
                _isMaskEnable = true
                binding.img.setMaskBitmap(_maskBitmap)
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.btnMaskEnable.setOnClickListener {
            _isMaskEnable = !_isMaskEnable
            binding.img.setMaskBitmap(if (_isMaskEnable) _maskBitmap else null)
        }
        binding.btnMaskMode.text = "切换遮罩模式：${PorterDuff.Mode.DST_OUT}"
        binding.btnMaskMode.setOnClickListener {
            val mode = _maskModeSet.next()
            if (mode != null) {
                binding.img.setMaskMode(mode)
                binding.btnMaskMode.text = "切换遮罩模式：$mode"
            }
        }
        binding.btnExport.setOnClickListener {
            val bitmap = binding.img.export()
            FileUtils.saveBitmap(this, "shape_mask", bitmap)
            Toast.makeText(this, "保存至filesDir/output", Toast.LENGTH_SHORT).show()
            bitmap.tryRecycle()
        }
    }
}