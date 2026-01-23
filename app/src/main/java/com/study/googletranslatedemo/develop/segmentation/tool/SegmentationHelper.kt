package com.study.googletranslatedemo.develop.segmentation.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult

class SegmentationHelper(
    // 委托运行模式
    private val delegate: Delegate = Delegate.CPU,
    // 使用的推理模型
    private val model: Model = Model.DeelLabV3Type1,
) {
    // 日志
    private val _tag = "SegmentationHelper"

    // 分割器
    private var _imageSegmenter: ImageSegmenter? = null

    /**
     * 是否已经初始化
     */
    val isInitialized: Boolean get() = _imageSegmenter != null

    /**
     * 初始化分割器
     */
    fun initialize(context: Context): Boolean {
        if (_imageSegmenter == null) {
            try {
                val baseOption = BaseOptions.builder()
                    .setDelegate(delegate)
                    .setModelAssetPath(model.assets)
                    .build()
                val option = ImageSegmenter.ImageSegmenterOptions.builder()
                    .setRunningMode(RunningMode.IMAGE) // 静态图像模式。也支持视频和相机模式。
                    .setBaseOptions(baseOption)
                    .setOutputCategoryMask(true)
                    .setOutputConfidenceMasks(false)
                    .build()
                _imageSegmenter = ImageSegmenter.createFromOptions(context, option)
                return true
            } catch (e: IllegalStateException) {
                Log.e(_tag, "initialize error: $e")
                return false
            } catch (e: RuntimeException) {
                // 不支持GPU模式会抛这个异常
                Log.e(_tag, "initialize error, not support GPU: $e")
                return false
            }
        }
        return true
    }

    /**
     * 分割图像，返回分割出来的图像
     */
    fun segmentImage(srcBitmap: Bitmap): Bitmap? {
        Log.d(
            _tag,
            "Start segment, srcBitmap: {width=${srcBitmap.width},height=${srcBitmap.height},density=${srcBitmap.density}}"
        )
        // 执行分割推理
        val srcMPImage: MPImage = BitmapImageBuilder(srcBitmap).build()
        val segmentResultMPImage = segmentImage(srcMPImage)?.categoryMask()?.get() ?: return null

        // 读取分割结果
        val resultBuffer = ByteBufferExtractor.extract(segmentResultMPImage)
        val outputPixels = IntArray(resultBuffer.capacity())

        // 读取源图像素
        val srcPixels = IntArray(srcBitmap.width * srcBitmap.height).apply {
            srcBitmap.getPixels(this, 0, srcBitmap.width, 0, 0, srcBitmap.width, srcBitmap.height)
        }

        // 处理结果
        val debugDic = mutableMapOf<Int, Int>() // 仅用于测试
        var isObjectDetected = false
        for (index in outputPixels.indices) {
            // 遍历所有量化值，如果符合推理模型的分类标签，则填充源图的像素，否则填充透明像素
            // 量化值跟模型有关。模型可能支持分割不同的图像，不同的值代表不同类型的物品。
            val q = resultBuffer.get(index).toInt()
            debugDic[q] = debugDic.getOrPut(q) { 0 } + 1
            outputPixels[index] = when {
                q in 1..19 -> {
                    // 默认值范围[1,20]，15代表人类
                    isObjectDetected = true
                    srcPixels[index]
                }

                else -> Color.TRANSPARENT
            }
        }
        Log.d(_tag, "Segment result: $debugDic")

        if (!isObjectDetected) {
            return null
        }

        // 将像素合成为bitmap
        val outputBitmap = Bitmap.createBitmap(
            outputPixels,
            srcMPImage.width,
            srcMPImage.height,
            Bitmap.Config.ARGB_8888
        )
        return outputBitmap
    }

    /**
     * 分割图像，回传原始接口结果
     */
    private fun segmentImage(mpImage: MPImage): ImageSegmenterResult? {
        if (_imageSegmenter == null) {
            Log.e(_tag, "segmenter is not initialize")
            return null
        }
        return _imageSegmenter?.segment(mpImage)
    }

    /**
     * 推理模型
     */
    enum class Model(val assets: String) {
        /**
         * 一个DeepLabV3模型
         */
        DeelLabV3Type1("model/deep_lab_v3_1.tflite"),
    }
}