package com.study.googletranslatedemo.demo.editor.sqbg.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.createBitmap
import com.study.googletranslatedemo.demo.editor.sqbg.data.FilterMaskSticker
import com.study.googletranslatedemo.utils.bitmap.BitmapFitDrawHelper
import java.util.Random
import kotlin.math.atan2
import kotlin.math.sqrt

class SqbgView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // 初始变换矩阵（用于保存操作前的状态）
    private val _initialMatrix = Matrix()

    // 当前变换矩阵（用于实时变换）
    private val _currentMatrix = Matrix()

    // 本地坐标点缓存
    private val _localPoint = FloatArray(2)

    // 屏幕坐标点缓存
    private val _screenPoint = FloatArray(2)

    // 上次触摸点X坐标
    private var _lastTouchX = 0f

    // 上次触摸点Y坐标
    private var _lastTouchY = 0f

    // 双指中心点
    private val _pinchCenter = PointF()

    // 初始双指距离
    private var _initialPinchDistance = 0f

    // 初始双指角度
    private var _initialPinchAngle = 0f

    // 当前触摸状态
    private var _currentTouchState = TouchState.None

    // 背景渲染工具
    private var _backgroundDrawHelper = BitmapFitDrawHelper()

    // 前景渲染工具
    private var _foregroundDrawHelper = BitmapFitDrawHelper()

    // 贴纸
    private var _sticker: FilterMaskSticker? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    /**
     * @param backgroundBitmap 背景bitmap，即遮罩内显示的图像
     * @param foregroundBitmap 前景bitmap，即遮罩外显示的图像
     */
    fun setImageBitmap(
        backgroundBitmap: Bitmap,
        foregroundBitmap: Bitmap
    ) {
        val getWork = {
            val viewWidth = width
            val viewHeight = height
            _backgroundDrawHelper.setSrcBitmap(backgroundBitmap, viewWidth, viewHeight)
            _foregroundDrawHelper.setSrcBitmap(foregroundBitmap, viewWidth, viewHeight)
            invalidate()
        }
        if (isLaidOut) {
            getWork()
        } else {
            post { getWork() }
        }
    }

    /**
     * 设置遮罩贴纸
     */
    fun setSticker(sticker: FilterMaskSticker) {
        if (isLaidOut) {
            // 布局已经完成，直接设置
            applySticker(sticker)
        } else {
            // 否则延迟设置
            post { applySticker(sticker) }
        }
    }

    /**
     * 导出结果
     * @param drawTheFrame 是否渲染形状遮罩的边框
     */
    fun export(drawTheFrame: Boolean = false): Bitmap? {
        _backgroundDrawHelper.getFitSize()?.let { (fitWidth, fitHeight) ->
            // TODO: 这种直接用View渲染的方法虽然方便，所见即所得，但受限于性能，View不宜直接显示照片原图。对于高质量的照片，最终导出的图像质量可能会下降。需要优化，所有的编辑功能都需要考虑这个问题。
            val exportBitmap = createBitmap(fitWidth, fitHeight)
            val canvas = Canvas(exportBitmap)
            Matrix().apply {
                // 确保画布居中
                postTranslate(-(width - fitWidth) / 2f, -(height - fitHeight) / 2f)
                canvas.setMatrix(this)
            }
            val frameEnable = _sticker?.frameEnable?.apply {
                // 临时修改边框参数
                _sticker?.frameEnable = drawTheFrame
            }
            draw(canvas)
            frameEnable?.apply { _sticker?.frameEnable = this }
            return exportBitmap
        }
        return null
    }

    fun release() {
        _backgroundDrawHelper.release()
        _foregroundDrawHelper.release()
    }

    override fun onDraw(canvas: Canvas) {
        val viewWidth = width
        val viewHeight = height

        // 绘制背景bitmap
        _backgroundDrawHelper.setContainerSize(viewWidth, viewHeight)
        _backgroundDrawHelper.drawToCenter(canvas)

        // 新建一个图层，确保遮罩不会裁剪到背景
        val layerId = canvas.saveLayer(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), null)

        // 绘制前景bitmap
        _foregroundDrawHelper.setContainerSize(viewWidth, viewHeight)
        _foregroundDrawHelper.drawToCenter(canvas)

        // 绘制遮罩bitmap
        _sticker?.takeIf { it.isVisible }?.draw(canvas)

        canvas.restoreToCount(layerId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(null)
        val x = event.x
        val y = event.y
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event, x, y)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleActionUp()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                handlePointerDown(event)
                true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                _currentTouchState = TouchState.None
                true
            }

            else -> {
                true
            }
        }
    }

    /**
     * 处理按下事件
     */
    private fun handleActionDown(x: Float, y: Float): Boolean {
        _currentTouchState = TouchState.Move
        _lastTouchX = x
        _lastTouchY = y

        // 贴纸模式：检查是否点击在贴纸上
        _sticker?.getCenterPoint(_pinchCenter, _localPoint, _screenPoint)

        val center: PointF = _pinchCenter
        val dx = (center.x - _lastTouchX).toDouble()
        val dy = (center.y - _lastTouchY).toDouble()
        _initialPinchDistance = sqrt(dy * dy + dx * dx).toFloat()
        _initialPinchAngle = Math.toDegrees(
            atan2((center.y - _lastTouchY).toDouble(), (center.x - _lastTouchX).toDouble())
        ).toFloat()

        // 检查触摸点是否在贴纸内
        _screenPoint[0] = _lastTouchX
        _screenPoint[1] = _lastTouchY
        val isOnSticker = _sticker?.containsPoint(_screenPoint) == true
        if (isOnSticker) {
            // 保存当前变换矩阵
            _sticker?.let { sticker -> _initialMatrix.set(sticker.transformMatrix) }
        } else {
            invalidate()
            return false
        }

        invalidate()
        return true
    }

    /**
     * 处理移动事件
     */
    private fun handleActionMove(event: MotionEvent, x: Float, y: Float): Boolean {
        synchronized(this) {
            when (_currentTouchState) {
                TouchState.None -> Unit
                TouchState.Move -> {
                    // 单点移动：平移贴纸
                    _currentMatrix.set(_initialMatrix)
                    _currentMatrix.postTranslate(x - _lastTouchX, y - _lastTouchY)
                    _sticker?.setTransformMatrix(_currentMatrix)
                }

                TouchState.ScaleRotate -> {
                    // 双指操作：缩放和旋转
                    val currentDistance = calculateDistance(event)
                    val currentAngle = calculateAngle(event)

                    _currentMatrix.set(_initialMatrix)

                    // 计算缩放比例
                    val scale: Float = currentDistance / _initialPinchDistance
                    val center: PointF = _pinchCenter
                    _currentMatrix.postScale(scale, scale, center.x, center.y)

                    // 计算旋转角度
                    val rotation: Float = currentAngle - _initialPinchAngle
                    _currentMatrix.postRotate(rotation, center.x, center.y)

                    _sticker?.setTransformMatrix(_currentMatrix)
                }
            }
        }

        invalidate()
        return true
    }

    /**
     * 处理抬起事件
     */
    private fun handleActionUp(): Boolean {
        _currentTouchState = TouchState.None
        invalidate()
        return true
    }

    /**
     * 处理多点按下事件
     */
    private fun handlePointerDown(event: MotionEvent) {
        // 计算初始双指距离和角度
        _initialPinchDistance = calculateDistance(event)
        _initialPinchAngle = calculateAngle(event)

        // 计算双指中心点
        if (event.pointerCount < 2) {
            _pinchCenter.set(0.0f, 0.0f)
        } else {
            _pinchCenter.set(
                (event.getX(1) + event.getX(0)) / 2.0f,
                (event.getY(1) + event.getY(0)) / 2.0f
            )
        }

        // 检查第二个触摸点是否在贴纸上
        _sticker?.let { sticker ->
            _screenPoint[0] = event.getX(1)
            _screenPoint[1] = event.getY(1)
            if (sticker.containsPoint(_screenPoint)) {
                _currentTouchState = TouchState.ScaleRotate
                return
            }
        } ?: { _currentTouchState = TouchState.None }
    }

    /**
     * 计算两个触摸点之间的距离
     *
     * @param event 触摸事件
     * @return 距离（像素）
     */
    private fun calculateDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) {
            return 0.0f
        }
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val dx = (x0 - event.getX(1)).toDouble()
        val dy = (y0 - event.getY(1)).toDouble()
        return sqrt(dy * dy + dx * dx).toFloat()
    }

    /**
     * 计算两个触摸点之间的角度
     *
     * @param event 触摸事件
     * @return 角度（度）
     */
    private fun calculateAngle(event: MotionEvent): Float {
        if (event.pointerCount < 2) {
            return 0.0f
        }
        val x0 = event.getX(0)
        return Math.toDegrees(
            atan2(
                (event.getY(0) - event.getY(1)).toDouble(),
                (x0 - event.getX(1)).toDouble()
            )
        ).toFloat()
    }

    /**
     * 应用贴纸到视图
     *
     * @param sticker 贴纸对象
     */
    private fun applySticker(sticker: FilterMaskSticker) {
        _sticker?.release()
        _sticker = sticker

        val stickerWidth = sticker.getWidth()
        val stickerHeight = sticker.getHeight()

        if (stickerWidth > 0 && stickerHeight > 0) {
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            // 计算缩放比例，使贴纸占据视图的80%
            val scale: Float = if (viewWidth > viewHeight) {
                (4.0f * viewHeight) / 5.0f / stickerHeight
            } else {
                (4.0f * viewWidth) / 5.0f / stickerWidth
            }

            // 重置变换矩阵
            _initialMatrix.reset()
            _currentMatrix.set(_initialMatrix)

            // 应用缩放
            _currentMatrix.postScale(scale, scale)

            // 添加随机旋转（-10度到+10度）
            val center: PointF = _pinchCenter
            center.set(0.0f, 0.0f)
            val random = Random()
            val randomRotation = (random.nextInt(20) - 10).toFloat()
            _currentMatrix.postRotate(randomRotation, center.x, center.y)

            // 居中显示
            val scaledWidth = stickerWidth * scale
            val scaledHeight = stickerHeight * scale
            val translateX = (viewWidth - scaledWidth) / 2.0f
            val translateY = (viewHeight - scaledHeight) / 2.0f
            _currentMatrix.postTranslate(translateX, translateY)

            // 应用变换到贴纸
            sticker.setTransformMatrix(_currentMatrix)

            // 保存初始矩阵
            _initialMatrix.set(_currentMatrix)
        }

        invalidate()
    }

    /**
     * 界面触摸状态
     */
    private enum class TouchState {
        None, Move, ScaleRotate
    }
}