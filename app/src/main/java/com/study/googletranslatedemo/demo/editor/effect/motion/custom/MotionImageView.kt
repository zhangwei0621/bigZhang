package com.study.googletranslatedemo.demo.editor.effect.motion.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.study.googletranslatedemo.utils.bitmap.BitmapFitDrawHelper
import kotlin.math.cos
import kotlin.math.sin

/**
 * 残影特效控件
 */
class MotionImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val _paint = Paint().apply {
        isAntiAlias = true
    }
    private var _effectConfig: EffectConfig? = null
    private var _bitmapDrawHelper = BitmapFitDrawHelper()

    /**
     * 设置动态特效Bitmap
     */
    fun setMotionBitmap(bitmap: Bitmap) {
        post {
            _bitmapDrawHelper.setSrcBitmap(bitmap, width, height)
            invalidate()
        }
    }

    /**
     * 设置特效配置
     */
    fun setEffectConfig(config: EffectConfig) {
        _effectConfig = config
        postInvalidate()
    }

    /**
     * 导出最终图像。建议控件启用adjustBounds，以免导出时有多余的空白图像
     */
    fun export(): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    /**
     * 释放资源
     */
    fun release() {
        _bitmapDrawHelper.release()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width
        val viewHeight = height
        _bitmapDrawHelper.setContainerSize(viewWidth, viewHeight)
        val fitBitmapWidth = _bitmapDrawHelper.getFitSize()?.x ?: 0
        val config = _effectConfig

        if (config != null && config.count > 0 && fitBitmapWidth > 0) {
            canvas.withSave {
                // 设置透明度
                _paint.alpha = config.alpha

                val radians = Math.toRadians(-config.rotate.toDouble())
                val cos = cos(radians)
                val sin = sin(radians)
                val baseOffset = fitBitmapWidth / 2f / config.count
                // 绘制残影特效
                repeat(config.count) {
                    val times = config.count - it
                    val spaceDistance = baseOffset * times
                    val translateX = (spaceDistance * cos).toFloat()
                    val translateY = (spaceDistance * sin).toFloat()
                    withTranslation(translateX, translateY) {
                        _bitmapDrawHelper.drawToCenter(this, _paint)
                    }
                }
            }

            // 最后绘制一个不受特效参数影响的、置于顶层的残影特效
            _bitmapDrawHelper.drawToCenter(canvas)
        }
    }

    /**
     * 特效配置数据
     */
    data class EffectConfig(
        var count: Int,
        var alpha: Int,
        var rotate: Int
    )
}