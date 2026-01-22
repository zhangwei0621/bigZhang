package com.study.googletranslatedemo.demo.editor.sqbg

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.databinding.ActivitySqbgBinding
import com.study.googletranslatedemo.demo.editor.sqbg.data.FilterMaskSticker
import com.study.googletranslatedemo.utils.FileUtils
import com.study.googletranslatedemo.utils.filter.FilterRule
import org.wysaid.nativePort.CGENativeLibrary

class SqbgActivity : BaseAct<ActivitySqbgBinding>() {
    private var _srcBitmap: Bitmap? = null

    override fun getViewBinding() = ActivitySqbgBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnBgMode.setOnClickListener { activeFunction(true) }
        binding.btnSqMode.setOnClickListener { activeFunction(false) }
        binding.btnExport.setOnClickListener { export() }
        binding.btnBgMode.performClick()
    }

    private fun activeFunction(isBgMode: Boolean) {
        val dpi = resources.displayMetrics.densityDpi
        // 构建原图和滤镜图像
        val srcBitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.sample_1,
            BitmapFactory.Options().apply {
                inDensity = dpi
                inTargetDensity = dpi
            }
        )
        val filterBitmap = CGENativeLibrary.filterImage_MultipleEffects(
            srcBitmap,
            FilterRule.DEEP_BLUR,
            1f
        ).apply {
            /**
             * 这个接口生产的dpi可能会跟原图不一样，目前不清楚具体算法，猜测是设备的dpi。
             * 这里修改和原图保持一致，否则导出最终图像会出现缩放问题(或优化导出方法以避免这个问题)。
             */
            density = dpi
        }

        // 根据模式显示图像
        if (isBgMode) {
            _srcBitmap = srcBitmap
            binding.sqbgView.setImageBitmap(srcBitmap, filterBitmap)
        } else {
            _srcBitmap = filterBitmap
            binding.sqbgView.setImageBitmap(filterBitmap, srcBitmap)
        }

        // 构建形状贴纸
        val stickerMask = BitmapFactory.decodeResource(resources, R.drawable.mask_a_mask)
        val stickerFrame = BitmapFactory.decodeResource(resources, R.drawable.mask_a_frame)
        val sticker = FilterMaskSticker(stickerMask, stickerFrame)
        binding.sqbgView.setSticker(sticker)
    }

    private fun export() {
        val exportBitmap = binding.sqbgView.export() ?: return
        FileUtils.saveBitmap(this, "sqbg", exportBitmap)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.sqbgView.release()
    }
}