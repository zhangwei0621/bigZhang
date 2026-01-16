package com.study.googletranslatedemo.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.IntRange
import androidx.appcompat.widget.AppCompatImageView
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.filter.FilterRule
import org.wysaid.nativePort.CGENativeLibrary
import kotlin.math.min

/**
 * 自由画板测试
 */
class FilterPaintImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    // 用户是否在操作
    private var _isUserPainting = false

    // 当前操作栈
    private var _currentLineList = mutableListOf<PaintLine>()

    // 抛弃操作栈
    private var _discardLineList = mutableListOf<PaintLine>()

    // 恢复画笔
    private val _restorePaint = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
        strokeWidth = 100f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    // 滤镜画笔
    private val _filterPaint = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
        strokeWidth = 100f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    // 是否处于滤镜模式
    private var _isFilterMode = false

    // 原图着色器
    private var _restoreShader: Shader? = null

    // 滤镜着色器
    private var _filterShader: Shader? = null

    // 用户当前操作中的路径
    private var _currentPath: Path? = null

    // 智能获取当前应该使用的画笔
    private val _currentPaint: Paint get() = if (_isFilterMode) _filterPaint else _restorePaint

    // 当前undo可用状态
    private val _undoable: Boolean get() = _currentLineList.isNotEmpty()

    // 当前redo可用状态
    private val _redoable: Boolean get() = _discardLineList.isNotEmpty()

    // 操作栈状态监听
    private var _operateStackListener: OperateStackListener? = null

    // 滤镜画笔强度
    private var _filterPaintIntensity: Int = 125
        set(value) {
            field = value
            _filterPaint.alpha = value.coerceIn(0, 255)
            invalidate()
        }

    // 恢复画笔强度
    private var _restorePaintIntensity: Int = 125
        set(value) {
            field = value
            _restorePaint.alpha = value.coerceIn(0, 255)
            invalidate()
        }

    init {
        _filterPaintIntensity = 200
        _restorePaintIntensity = 200
    }

    fun setFilterPaintIntensity(@IntRange(0, 255) value: Int) {
        _filterPaintIntensity = value
    }

    fun setRestorePaintIntensity(@IntRange(0, 255) value: Int) {
        _restorePaintIntensity = value
    }

    /**
     * 注册操作栈状态监听
     */
    fun setOperateStackListener(listener: OperateStackListener?) {
        _operateStackListener = listener
        callOperateListener()
    }

    fun switchPaintMode(): Boolean {
        _isFilterMode = !_isFilterMode
        return _isFilterMode
    }

    /**
     * 重做
     */
    fun redo() {
        _discardLineList.removeLastOrNull()?.let { line ->
            _currentLineList.add(line)
            invalidate()
        }
        callOperateListener()
    }

    /**
     * 撤销
     */
    fun undo() {
        _currentLineList.removeLastOrNull()?.let { line ->
            _discardLineList.add(line)
            invalidate()
        }
        callOperateListener()
    }

    private fun callOperateListener() {
        _operateStackListener?.onStackChanged(_undoable, _redoable)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        if (bm != null) {
            // 生成滤镜处理图像和着色器
            val tileMode = Shader.TileMode.CLAMP
            _restoreShader = BitmapShader(bm, tileMode, tileMode)
            val filterBitmap =
                CGENativeLibrary.filterImage_MultipleEffects(bm, FilterRule.BLACK, 1f)
            _filterShader = BitmapShader(filterBitmap, tileMode, tileMode)

            // 计算适配矩阵
            val matrix = Matrix()
            val minScale = min(width * 1f / bm.width, height * 1f / bm.height)
            val w = bm.width * minScale
            val h = bm.height * minScale
            matrix.setScale(minScale, minScale)
            matrix.postTranslate((width - w) / 2f, (height - h) / 2f)
            _restoreShader?.setLocalMatrix(matrix)
            _filterShader?.setLocalMatrix(matrix)
            scaleType = ScaleType.MATRIX
            imageMatrix = matrix
            super.setImageBitmap(filterBitmap)
        } else {
            _restoreShader = null
            _filterShader = null
            super.setImageBitmap(null)
        }
        _filterPaint.shader = _filterShader
        _restorePaint.shader = _restoreShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        _currentLineList.forEach { line ->
            canvas.drawPath(line.path, Paint(line.paint).apply {
                alpha = line.alpha
            })
        }
        _currentPath?.let { path ->
            canvas.drawPath(path, _currentPaint)
        }
        canvas.restoreToCount(layerId)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(null)
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!_isUserPainting) {
                    _isUserPainting = true
                    _currentPath = Path().apply {
                        moveTo(x, y)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (_isUserPainting) {
                    _currentPath?.lineTo(x, y)
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (_isUserPainting) {
                    _isUserPainting = false
                    _currentPath?.lineTo(x, y)
                    _currentPath?.let { path ->
                        if (!path.isEmpty) {
                            val paint = _currentPaint
                            _currentLineList.add(PaintLine(path, paint, paint.alpha))
                            _currentPath = null
                            _discardLineList.clear()
                            callOperateListener()
                        }
                    }
                }
            }
        }
        invalidate()
        return true
    }

    private data class PaintLine(val path: Path, val paint: Paint, val alpha: Int)
}