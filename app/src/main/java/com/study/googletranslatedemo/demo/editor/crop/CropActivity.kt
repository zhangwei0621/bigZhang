package com.study.googletranslatedemo.demo.editor.crop

import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivityCropBinding
import com.study.googletranslatedemo.utils.PhotoPicker
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils

/**
 * 裁切测试
 */
class CropActivity : BaseAct<ActivityCropBinding>() {
    private val _photoPicker = PhotoPicker()

    override fun getViewBinding() = ActivityCropBinding.inflate(layoutInflater)

    override fun initView() {
        _photoPicker.register(this) { uri ->
            BitmapUtils.uriToBitmap(this, uri)?.let { bmp ->
                binding.cropImageView.setImageBitmap(bmp)
            }
        }
        binding.btnImage.setOnClickListener { _photoPicker.request() }
        binding.btnCrop.setOnClickListener {
            val bitmap = binding.cropImageView.getCroppedImage()
            binding.cropImageView.setImageBitmap(bitmap)
        }
        binding.btnRotate.setOnClickListener {
            binding.cropImageView.rotateImage(90)
        }
    }
}