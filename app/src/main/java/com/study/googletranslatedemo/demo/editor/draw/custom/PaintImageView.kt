package com.study.googletranslatedemo.demo.editor.draw.custom

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.study.googletranslatedemo.R
import com.study.googletranslatedemo.demo.editor.draw.data.PaintEffect
import com.study.googletranslatedemo.utils.OperateStackListener

/**
 * 自由画板。
 * 支持操作栈。
 * 支持橡皮擦。可以设置背景，不过橡皮擦不对背景生效。
 * 支持画笔特效。
 */
class PaintImageView @JvmOverloads constructor(
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

    // 正常画笔
    private val _paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        strokeWidth = 72f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    // 橡皮擦画笔
    private val _eraserPaint = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
        strokeWidth = 72f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // 是否处于橡皮擦模式
    private var _isEraserMode = false

    // 画笔纹理着色器
    private val _paintShader: Shader

    // 霓虹灯特效滤镜
    private val _blurMaskFilter: BlurMaskFilter

    // 用户当前操作中的路径
    private var _currentPath: Path? = null

    // 智能获取当前应该使用的画笔
    private val _currentPaint: Paint get() = if (_isEraserMode) _eraserPaint else _paint

    // 当前undo可用状态
    private val _undoable: Boolean get() = _currentLineList.isNotEmpty()

    // 当前redo可用状态
    private val _redoable: Boolean get() = _discardLineList.isNotEmpty()

    // 操作栈状态监听
    private var _operateStackListener: OperateStackListener? = null

    init {
        // 初始化画笔着色器
        val shaderBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.texture_34)
        val shaderMode = Shader.TileMode.REPEAT
        _paintShader = BitmapShader(shaderBitmap, shaderMode, shaderMode)
        // 初始化霓虹特效滤镜
        _blurMaskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.SOLID)
    }

    /**
     * 设置画笔特效
     */
    fun setPaintEffect(effect: PaintEffect) {
        when (effect) {
            PaintEffect.None -> {
                _paint.maskFilter = null
                _paint.shader = null
            }

            PaintEffect.Texture -> {
                _paint.maskFilter = null
                _paint.shader = _paintShader
            }

            PaintEffect.Neon -> {
                _paint.maskFilter = _blurMaskFilter
                _paint.shader = null
            }
        }
        invalidate()
    }

    /**
     * 注册操作栈状态监听
     */
    fun setOperateStackListener(listener: OperateStackListener?) {
        _operateStackListener = listener
        callOperateListener()
    }

    /**
     * 开关橡皮擦模式
     * @return true=开启了橡皮擦模式，false=关闭了橡皮擦模式
     */
    fun switchEraserMode(): Boolean {
        _isEraserMode = !_isEraserMode
        return _isEraserMode
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        _currentLineList.forEach { line ->
            canvas.drawPath(line.path, line.paint)
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
                            _currentLineList.add(PaintLine(path, _currentPaint))
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

    private data class PaintLine(val path: Path, val paint: Paint)
}