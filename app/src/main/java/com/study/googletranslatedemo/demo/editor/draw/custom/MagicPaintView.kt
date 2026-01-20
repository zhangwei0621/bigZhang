package com.study.googletranslatedemo.demo.editor.draw.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import com.study.googletranslatedemo.utils.OperateStackListener
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.isValid
import com.study.googletranslatedemo.utils.bitmap.BitmapUtils.tryRecycle
import com.study.googletranslatedemo.utils.expand.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 画板。支持操作栈。模式：1.颜料画笔 2.霓虹画笔 3.橡皮擦 4.魔术
 */
class MagicPaintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // 当前有效的画笔路径
    private val _brushPathList: MutableList<BrushPath> = mutableListOf()

    // 当前被回收的画笔路径
    private val _discardBrushPathList: MutableList<BrushPath> = mutableListOf()

    // 当前绘制中的普通画笔路径
    private val _currentPath: Path = Path()

    // 当前绘制中的魔术图案合集
    private val _currentMagicItemList: MutableList<MagicItem> = mutableListOf()

    // 判断画笔移动的阈值
    private val _paintMoveThreshold: Float = 4f

    // 图案间距，也决定图案的大小
    private var _magicItemSpacing: Int = 25f.dp(context).roundToInt()

    // 图案间距容错
    private var _magicItemSpacingTolerance: Int = 3f.dp(context).roundToInt()

    // 上一次触摸点X坐标
    private var _lastTouchX: Float = 0f

    // 上一次触摸点Y坐标
    private var _lastTouchY: Float = 0f

    // 魔术画笔配置
    private var _magicPaintConfig: MagicPaintConfig? = null

    // 橡皮擦画笔
    private var _eraserPaint: Paint = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
        isDither = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // 颜料画笔
    private val _colorPaint: Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    // 霓虹画笔
    private val _neonPaint: Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        // 霓虹特效关键
        maskFilter = BlurMaskFilter(25.0f, BlurMaskFilter.Blur.OUTER)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    // 魔术画笔
    private val _magicPaint: Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    // 智能获取当前应该使用的普通画笔
    private val _currentNormalPaint: Paint
        get() = when (brushMode) {
            BrushMode.Neon -> _neonPaint
            BrushMode.Eraser -> _eraserPaint
            else -> _colorPaint
        }

    // 当前undo可用状态
    private val _undoable: Boolean get() = _brushPathList.isNotEmpty()

    // 当前redo可用状态
    private val _redoable: Boolean get() = _discardBrushPathList.isNotEmpty()

    // 操作栈状态监听
    private var _operateStackListener: OperateStackListener? = null

    /**
     * 画笔大小
     */
    var brushSize: Float = 0f
        set(value) {
            field = value
            _colorPaint.strokeWidth = field
            _neonPaint.strokeWidth = field
            _eraserPaint.strokeWidth = field
            _magicItemSpacing = field.roundToInt()
        }

    /**
     * 画笔透明度
     */
    var brushAlpha: Int = 0
        set(value) {
            field = value.coerceIn(0, 255)
            _colorPaint.alpha = field
            _neonPaint.alpha = field
            _magicPaint.alpha = field
        }

    /**
     * 画笔颜色
     */
    var brushColor: Int = 0
        set(value) {
            field = value
            _colorPaint.color = field
            _neonPaint.color = field
            // 修改颜色后Alpha会失效，这里重新设置一下
            brushAlpha = brushAlpha
        }

    /**
     * 画笔模式
     */
    var brushMode: BrushMode

    init {
        brushSize = 25f.dp(context)
        brushAlpha = 255
        brushMode = BrushMode.Normal

        // 橡皮擦关键设置，这样被橡皮擦擦除的地方才能正常显示该View背后的其它View，否则会是纯黑色
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * 将当前画板内容绘制到[srcBitmap]上
     */
    fun paintTo(srcBitmap: Bitmap): Bitmap? {
        if (!srcBitmap.isValid()) return null
        val viewWidth = width
        val viewHeight = height
        val resultBitmap = createBitmap(viewWidth, viewHeight)
        val canvas = Canvas(resultBitmap)
        val rectF = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())

        // 绘制原图
        canvas.drawBitmap(
            srcBitmap,
            null,
            rectF,
            null
        )

        // 新建图层来渲染画板内容，防止橡皮擦把原图也擦掉
        val layerId = canvas.saveLayer(rectF, null)
        // 把画板内容绘制上去
        drawBrushPath(canvas)
        canvas.restoreToCount(layerId)
        return resultBitmap
    }

    /**
     * 设置魔术画笔使用的图案资源合集
     */
    fun setMagicItemResources(resourceIds: List<Int>) {
        _magicPaintConfig?.releaseBitmap()
        _magicPaintConfig = MagicPaintConfig(resourceIds)
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制已入栈的路径
        drawBrushPath(canvas)

        // 绘制预览路径
        _currentMagicItemList.forEach { magicItem ->
            canvas.drawBitmap(
                magicItem.bitmap,
                null,
                magicItem.rect,
                _magicPaint
            )
        }
        _currentPath.takeIf { !it.isEmpty }?.let { path ->
            canvas.drawPath(path, _currentNormalPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(null)
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 重置相关参数
                _currentPath.reset()
                _currentPath.moveTo(x, y)
                _currentMagicItemList.clear()
                _lastTouchX = x
                _lastTouchY = y
            }

            MotionEvent.ACTION_MOVE -> {
                if (isNormalPaintMode()) {
                    handlePaintMove(x, y)
                } else {
                    handleMagicPaintMove(x, y)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isNormalPaintMode()) {
                    handelPaintUp()
                } else {
                    handleMagicPaintUp()
                }
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

    // 绘制画板内容
    private fun drawBrushPath(canvas: Canvas) {
        _brushPathList.forEach { brushPath ->
            when (brushPath) {
                is BrushPath.MagicPath -> {
                    brushPath.items.forEach { magicItem ->
                        canvas.drawBitmap(
                            magicItem.bitmap,
                            null,
                            magicItem.rect,
                            brushPath.paint
                        )
                    }
                }

                is BrushPath.NormalPath -> {
                    canvas.drawPath(brushPath.path, brushPath.paint)
                }
            }
        }
    }

    // 判断当前是否为普通画笔模式
    private fun isNormalPaintMode(): Boolean {
        return when (brushMode) {
            BrushMode.Normal,
            BrushMode.Neon,
            BrushMode.Eraser -> true

            BrushMode.Magic -> false
        }
    }

    // 普通画笔移动
    private fun handlePaintMove(x: Float, y: Float) {
        val dx = abs(x - _lastTouchX)
        val dy = abs(y - _lastTouchY)
        if (dx < _paintMoveThreshold && dy < _paintMoveThreshold) return
        val midX = (x + _lastTouchX) / 2f
        val midY = (y + _lastTouchY) / 2f
        _currentPath.quadTo(_lastTouchX, _lastTouchY, midX, midY)
        _lastTouchX = x
        _lastTouchY = y
    }

    // 魔术画笔移动
    private fun handleMagicPaintMove(x: Float, y: Float) {
        val paintConfig = _magicPaintConfig ?: return
        val dx = abs(x - _lastTouchX)
        val dy = abs(y - _lastTouchY)
        if (dx < _paintMoveThreshold && dy < _paintMoveThreshold) return
        if (dx < _magicItemSpacing + _magicItemSpacingTolerance && dy < _magicItemSpacing + _magicItemSpacingTolerance) {
            return
        }
        val magicBitmap = paintConfig.getNextMagicBitmap(context) ?: return
        _currentMagicItemList += MagicItem(
            magicBitmap,
            x.toInt(),
            y.toInt(),
            (x + _magicItemSpacing).toInt(),
            (y + _magicItemSpacing).toInt()
        )
        _lastTouchX = x
        _lastTouchY = y
    }

    // 普通画笔抬手
    private fun handelPaintUp() {
        _brushPathList.add(
            // 复制当时使用的路径和画笔，以免受到后续参数修改的影响
            BrushPath.NormalPath(
                Path(_currentPath),
                Paint(_currentNormalPaint)
            )
        )
        _currentPath.reset()
    }

    // 魔术画笔抬手
    private fun handleMagicPaintUp() {
        _brushPathList.add(
            BrushPath.MagicPath(_currentMagicItemList.toList(), Paint(_magicPaint))
        )
        _currentMagicItemList.clear()
    }

    /**
     * 画笔模式
     */
    enum class BrushMode {
        /**
         * 颜料画笔
         */
        Normal,

        /**
         * 霓虹画笔
         */
        Neon,

        /**
         * 橡皮擦
         */
        Eraser,

        /**
         * 魔术画笔
         */
        Magic
    }

    /**
     * 画笔路径数据
     */
    private sealed class BrushPath {
        /**
         * 普通画笔路径
         */
        data class NormalPath(
            // 路径参数
            val path: Path,
            // 画笔参数
            val paint: Paint
        ) : BrushPath()

        /**
         * 魔术画笔路径
         */
        data class MagicPath(
            // 属于该路径的图案合集
            val items: List<MagicItem>,
            // 画笔参数
            val paint: Paint
        ) : BrushPath()
    }

    /**
     * 魔术图案数据
     */
    private data class MagicItem(
        // 图案bitmap
        val bitmap: Bitmap,

        // 位置信息
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val rect: Rect get() = Rect(left, top, right, bottom)
    }

    /**
     * 魔术画笔配置
     */
    private data class MagicPaintConfig(
        // 图案资源合集
        private val resourceIds: List<Int>,
    ) {
        // 图案bitmap字典
        private val _resourceBitmapMap: MutableMap<Int, Bitmap> = mutableMapOf()

        // 最近一次绘制的图案资源index
        private var _lastDrawResourceIndex: Int? = null

        /**
         * 获取随机图案
         */
        fun getNextMagicBitmap(context: Context): Bitmap? {
            if (resourceIds.isEmpty()) return null
            val lastIndex = _lastDrawResourceIndex
            var index: Int
            if (resourceIds.size == 1) {
                index = 0
            } else {
                // 随机获取图案，并避免与上次使用的图案相同
                do {
                    index = Random.nextInt(0, resourceIds.size)
                } while (index == lastIndex)
            }
            _lastDrawResourceIndex = index
            return getBitmap(context, index)
        }

        /**
         * 释放图案资源
         */
        fun releaseBitmap() {
            _resourceBitmapMap.forEach { (_, bitmap) ->
                bitmap.tryRecycle()
            }
            _resourceBitmapMap.clear()
        }

        private fun getBitmap(context: Context, index: Int): Bitmap? {
            val resourceId = resourceIds.getOrNull(index) ?: return null
            return _resourceBitmapMap.getOrPut(index) {
                BitmapFactory.decodeResource(context.resources, resourceId)
            }
        }
    }
}