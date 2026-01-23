package com.study.googletranslatedemo.demo.editor.draw.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.expand.dp
import com.study.googletranslatedemo.utils.filter.FilterImage
import kotlin.math.abs
import kotlin.math.min

/**
 * 马赛克画板。支持操作栈。支持模糊马赛克和图案马赛克。
 */
class MosaicPaintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    // 当前有效的画笔路径
    private val _brushPathList: MutableList<BrushPath> = mutableListOf()

    // 当前被回收的画笔路径
    private val _discardBrushPathList: MutableList<BrushPath> = mutableListOf()

    // 当前绘制中的普通画笔路径
    private val _currentPath: Path = Path()

    // 上一次触摸点X坐标
    private var _lastTouchX: Float = 0f

    // 上一次触摸点Y坐标
    private var _lastTouchY: Float = 0f

    // 判断画笔移动的阈值
    private val _paintMoveThreshold: Float = 4f

    // 模糊滤镜
    private val _blurFilter = "@blur lerp 1 2"

    // 模糊着色器
    private var _blurShader: Shader? = null

    // 模糊画笔
    private val _blurPaint: Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    // 图案马赛克着色器
    private var _imageMosaicShader: Shader? = null

    // 图案马赛克画笔
    private val _imageMosaicPaint: Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    // 智能获取当前应该使用的画笔
    private val _currentPaint: Paint
        get() = when (paintConfig) {
            is MosaicPaintConfig.ImageMosaic -> _imageMosaicPaint
            else -> _blurPaint
        }

    // 当前undo可用状态
    private val _undoable: Boolean get() = _brushPathList.isNotEmpty()

    // 当前redo可用状态
    private val _redoable: Boolean get() = _discardBrushPathList.isNotEmpty()

    // 操作栈状态监听
    private var _operateStackListener: OperateStackListener? = null

    /**
     * 画笔模式
     */
    var paintConfig: MosaicPaintConfig? = null
        set(value) {
            field = value
            when (value) {
                MosaicPaintConfig.BlurMosaic, null -> Unit
                is MosaicPaintConfig.ImageMosaic -> {
                    _imageMosaicShader = BitmapShader(
                        BitmapFactory.decodeResource(context.resources, value.imageResourceId),
                        Shader.TileMode.REPEAT,
                        Shader.TileMode.REPEAT
                    )
                    _imageMosaicPaint.shader = _imageMosaicShader
                }
            }
        }

    /**
     * 画笔大小
     */
    var brushSize: Float = 0f
        set(value) {
            field = value
            _blurPaint.strokeWidth = field
            _imageMosaicPaint.strokeWidth = field
        }

    init {
        brushSize = 25f.dp(context)
    }

    /**
     * 注册操作栈状态监听
     */
    fun setOperateStackListener(listener: OperateStackListener?) {
        _operateStackListener = listener
        callOperateListener()
    }

    /**
     * 重做
     */
    fun redo() {
        _discardBrushPathList.removeLastOrNull()?.let { line ->
            _brushPathList.add(line)
            invalidate()
        }
        callOperateListener()
    }

    /**
     * 撤销
     */
    fun undo() {
        _brushPathList.removeLastOrNull()?.let { line ->
            _discardBrushPathList.add(line)
            invalidate()
        }
        callOperateListener()
    }

    private fun callOperateListener() {
        _operateStackListener?.onStackChanged(_undoable, _redoable)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        val filterBitmap = FilterImage.applyFilter(bm, _blurFilter, 1f)
        if (bm != null && filterBitmap != null) {
            _blurShader = BitmapShader(
                filterBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )

            // 计算适配矩阵
            val matrix = Matrix()
            // TODO: 这时候layout不一定已经完成，不能获取当前View的宽高，优化
            val minScale = min(width * 1f / bm.width, height * 1f / bm.height)
            val w = bm.width * minScale
            val h = bm.height * minScale
            matrix.setScale(minScale, minScale)
            matrix.postTranslate((width - w) / 2f, (height - h) / 2f)
            scaleType = ScaleType.MATRIX
            imageMatrix = matrix
            _blurShader?.setLocalMatrix(matrix)
            super.setImageBitmap(bm)
        } else {
            _blurShader = null
            super.setImageBitmap(null)
        }
        _blurPaint.shader = _blurShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        _brushPathList.forEach { brushPath ->
            canvas.drawPath(brushPath.path, brushPath.paint)
        }
        _currentPath.takeIf { !it.isEmpty }?.let { path ->
            canvas.drawPath(path, _currentPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(null)
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 重置相关参数
                _currentPath.reset()
                _currentPath.moveTo(x, y)
                _lastTouchX = x
                _lastTouchY = y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - _lastTouchX)
                val dy = abs(y - _lastTouchY)
                if (dx >= _paintMoveThreshold && dy >= _paintMoveThreshold) {
                    val midX = (x + _lastTouchX) / 2f
                    val midY = (y + _lastTouchY) / 2f
                    _currentPath.quadTo(_lastTouchX, _lastTouchY, midX, midY)
                    _lastTouchX = x
                    _lastTouchY = y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                _brushPathList.add(
                    // 复制当时使用的路径和画笔，以免受到后续参数修改的影响
                    BrushPath(
                        Path(_currentPath),
                        Paint(_currentPaint)
                    )
                )
                _currentPath.reset()
                // 清空回收栈
                _discardBrushPathList.clear()
                // 通知栈状态更新
                callOperateListener()
                // 重置数据
                _lastTouchX = 0f
                _lastTouchY = 0f
            }
        }
        invalidate()
        return true
    }

    /**
     * 画笔类型
     */
    sealed class MosaicPaintConfig {
        /**
         * 模糊马赛克
         */
        data object BlurMosaic : MosaicPaintConfig()

        /**
         * 图案马赛克
         */
        data class ImageMosaic(
            // 图案资源id
            val imageResourceId: Int
        ) : MosaicPaintConfig()
    }

    /**
     * 画笔路径数据
     */
    private data class BrushPath(
        // 路径参数
        val path: Path,
        // 画笔参数
        val paint: Paint
    )
}