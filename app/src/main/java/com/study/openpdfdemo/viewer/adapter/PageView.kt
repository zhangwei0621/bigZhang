package com.study.openpdfdemo.viewer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import com.lowagie.text.pdf.PdfAnnotation
import com.study.openpdfdemo.utils.SerialChannel
import com.study.openpdfdemo.utils.tryRecycle
import com.study.openpdfdemo.viewer.PdfCore
import com.study.openpdfdemo.viewer.data.PDFColorWrap
import com.study.openpdfdemo.viewer.data.PageBridge
import com.study.openpdfdemo.viewer.data.PageTool
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import com.study.openpdfdemo.viewer.view.PdfViewerOverlay
import com.study.openpdfdemo.viewer.view.SearchResultView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

@SuppressLint("ViewConstructor")
class PageView(
    private val _context: Context,
    private val _pdfCore: PdfCore,
    private val _pageBridge: PageBridge,
) : ViewGroup(_context) {
    private val _contentView = AppCompatImageView(_context)
    private val _hqContentView = AppCompatImageView(_context)
    private val _overlayView =
        PdfViewerOverlay(context, _pageBridge, buildOverlayInterface())
    private val _searchResultView =
        SearchResultView(context, _pageBridge, buildSearchOverlayInterface())
    private val _contentViewMatrix = Matrix()

    //pdf原始尺寸
    private var _pdfWidth: Float = 0f
    private var _pdfHeight: Float = 0f

    //PageView相对于PDF的适配缩放参数，PageView尺寸也受用户手势缩放影响而变化
    private var _fitScale: Float = 1f

    //ReaderView相对于PDF的适配缩放参数，一般是固定的
    private var _pdfFitScale: Float = 1f

    private var _bitmapWidth: Int = _pageBridge.readerWidth
    private var _bitmapHeight: Int = _pageBridge.readerHeight
    private var _pageIndex: Int = -1
    private var _contentBitmap: Bitmap? = null
    private var _hqContentBitmap: Bitmap? = null

    /**
     * channel串行执行渲染和销毁工作，以免渲染和销毁可能同步进行导致bitmap回收错误。
     * 所以目前ReaderView不启用PageView复用，每个页面都创建新的PageView实例
     */
    private val _jobChannel = SerialChannel()

    //局部刷新的参数
    private var _hqViewWidth: Int = 0
    private var _hqViewHeight: Int = 0
    private var _hqViewRect: Rect? = Rect()
    private val _hqScaleThreshold = 0.01f

    init {
        _overlayView.visibility = GONE
        _searchResultView.visibility = GONE
        _hqContentView.scaleType = ImageView.ScaleType.MATRIX
        _contentView.scaleType = ImageView.ScaleType.MATRIX
        addView(_contentView)
        addView(_hqContentView)
        addView(_overlayView)
        addView(_searchResultView)
    }

    fun refreshSearchResult() {
        _searchResultView.invalidate()
    }

    fun clearOverlayPreview() {
        _overlayView.clearPreview()
    }

    fun destroy() = _jobChannel.runBlock {
        withContext(Dispatchers.Main) {
            _contentView.setImageBitmap(null)
            _hqContentView.setImageBitmap(null)
            _contentBitmap.tryRecycle()
            _hqContentBitmap.tryRecycle()
        }
    }

    fun openPage(pageIndex: Int) {
        drawPage(pageIndex)
        checkHqDraw()
    }

    fun redrawPage(onRenderFinished: () -> Unit = {}) {
        drawPage(pageIndex = _pageIndex)
        checkHqDraw(true, onRenderFinished)
    }

    /**
     * 检查缩放区域
     * @param forceDraw true=缩放区域没有变化也强制刷新
     */
    fun checkHqDraw(
        forceDraw: Boolean = false,
        onRenderFinished: () -> Unit = {}
    ) = _jobChannel.runBlock {
        try {
            calculateFitScale()
            if (abs(_fitScale - _pdfFitScale) < _hqScaleThreshold) {
                clearHq()
                throw Exception("no need to scale")
            }
            val pageViewRect = Rect(left, top, right, bottom)
            val hqViewWidth = pageViewRect.width()
            val hqViewHeight = pageViewRect.height()
            val hqViewRect = Rect(0, 0, _pageBridge.readerWidth, _pageBridge.readerHeight)
            //计算HqViewRect：取默认HqView区域和PageView区域的交集作为新的HqView区域
            if (!hqViewRect.intersect(pageViewRect)) {
                throw Exception("invalid scale area")
            }
            //把HqView的Rect移到中间
            hqViewRect.offset(-pageViewRect.left, -pageViewRect.top)
            //Hq区域没有变化，跳过
            if (hqViewWidth == _hqViewWidth
                && hqViewHeight == _hqViewHeight
                && hqViewRect == _hqViewRect
            ) {
                if (!forceDraw) {
                    throw Exception("scale not changed")
                }
            }

            //开始处理渲染
            if (hqViewWidth <= 0 || hqViewHeight <= 0) {
                throw Exception("invalid scale size")
            }
            if (hqViewRect.isEmpty) {
                throw Exception("invalid scale rect")
            }
            if (_pageIndex < 0) {
                throw Exception("invalid index")
            }
            val scaledWidth: Int = (_pdfWidth * _fitScale).toInt()
            val scaledHeight: Int = (_pdfHeight * _fitScale).toInt()
            if (scaledWidth <= 0 || scaledHeight <= 0) {
                throw Exception("invalid size")
            }
            val isSizeChange = _hqViewWidth != hqViewWidth || _hqViewHeight != hqViewHeight
            if (isSizeChange) {
                withContext(Dispatchers.Main) {
                    _hqContentView.setImageBitmap(null)
                }
                _hqContentBitmap.tryRecycle()
                _hqContentBitmap = null
                _hqContentBitmap = createBitmap(hqViewWidth, hqViewHeight)
            } else if (_hqContentBitmap == null) {
                _hqContentBitmap = createBitmap(hqViewWidth, hqViewHeight)
            }
            _hqContentBitmap?.let { bitmap ->
                _pdfCore.tiledRender(
                    _pageIndex,
                    bitmap,
                    _fitScale,
                    hqViewRect.left.toFloat(),
                    hqViewRect.top.toFloat()
                )
                _hqViewWidth = hqViewWidth
                _hqViewHeight = hqViewHeight
                _hqViewRect = hqViewRect
                withContext(Dispatchers.Main) {
                    showOverlayView()
                    _hqContentView.setImageBitmap(bitmap)
                    this@PageView.requestLayout()
                }
            }
        } catch (e: Exception) {
        }
        withContext(Dispatchers.Main) {
            onRenderFinished.invoke()
        }
    }

    /**
     * 设置要显示的PDF页面，计算适配尺寸并渲染
     * @param pageIndex 0-base
     * @param skipRenderIfBitmapSizeNotChanged 如果重新计算适配的尺寸没有变化，跳过渲染流程
     */
    private fun drawPage(
        pageIndex: Int,
        skipRenderIfBitmapSizeNotChanged: Boolean = false,
        onRenderFinished: () -> Unit = {}
    ) = _jobChannel.runBlock {
        try {
            if (pageIndex < 0) {
                throw Exception("invalid index")
            }
            _pageIndex = pageIndex
            val pdfSize = _pdfCore.getPageSize(pageIndex)
            _pdfWidth = pdfSize.width
            _pdfHeight = pdfSize.height
            calculateFitScale()
            //这里bitmap用ReaderView的缩放适配参数，因为我们不在这里处理用户的手势缩放
            val bitmapWidth: Int = (_pdfWidth * _pdfFitScale).toInt()
            val bitmapHeight: Int = (_pdfHeight * _pdfFitScale).toInt()
            if (bitmapWidth <= 0 || bitmapHeight <= 0) {
                throw Exception("invalid bitmap size")
            }
            if (skipRenderIfBitmapSizeNotChanged) {
                if (bitmapWidth == _bitmapWidth && bitmapHeight == _bitmapHeight) {
                    throw Exception("skip")
                }
            }

            val isSizeChanged = _pdfWidth != pdfSize.width || _pdfHeight != pdfSize.height
            _bitmapWidth = bitmapWidth
            _bitmapHeight = bitmapHeight
            if (isSizeChanged) {
                withContext(Dispatchers.Main) {
                    _contentView.setImageBitmap(null)
                }
                _contentBitmap.tryRecycle()
                _contentBitmap = null
                _contentBitmap = createBitmap(bitmapWidth, _bitmapHeight)
            } else if (_contentBitmap == null) {
                _contentBitmap = createBitmap(bitmapWidth, _bitmapHeight)
            }
            _contentBitmap?.let { bitmap ->
                _pdfCore.renderPage(pageIndex, bitmap)
                withContext(Dispatchers.Main) {
                    showOverlayView()
                    _contentView.setImageBitmap(bitmap)
                    this@PageView.requestLayout()
                }
            }
        } catch (_: Exception) {
        }
        withContext(Dispatchers.Main) {
            onRenderFinished.invoke()
        }
    }

    private fun clearHq() {
        _hqContentView.setImageBitmap(null)
        _hqContentView.invalidate()
    }

    private fun calculateFitScale() {
        if (_pdfWidth > 0 && _pdfHeight > 0) {
            val viewWidth = width
            val viewHeight = height
            val containerWidth = if (viewWidth > 0) viewWidth else _pageBridge.readerWidth
            val containerHeight = if (viewHeight > 0) viewHeight else _pageBridge.readerHeight
            _fitScale = min(
                containerWidth * 1f / _pdfWidth,
                containerHeight * 1f / _pdfHeight
            )
            _pdfFitScale = min(
                _pageBridge.readerWidth * 1f / _pdfWidth,
                _pageBridge.readerHeight * 1f / _pdfHeight
            )
        }
    }

    private fun showOverlayView() {
        if (!_overlayView.isVisible) {
            _overlayView.visibility = VISIBLE
        }
        if (!_searchResultView.isVisible) {
            _searchResultView.visibility = VISIBLE
        }
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int
    ) {
        val width = r - l
        val height = b - t
        _contentView.let {
            if (it.width != width || it.height != height) {
                it.imageMatrix = _contentViewMatrix.apply {
                    setScale(width * 1f / _bitmapWidth, height * 1f / _bitmapHeight)
                }
            }
            it.layout(0, 0, width, height)
        }
        layoutHq(width, height)
        _overlayView.layout(0, 0, width, height)
        _searchResultView.layout(0, 0, width, height)
    }

    private fun layoutHq(width: Int, height: Int) {
        val hqWidth = _hqViewWidth
        val hqHeight = _hqViewHeight
        if (hqWidth == width && hqHeight == height) {
            _hqViewRect?.let { rect ->
                _hqContentView.layout(rect.left, rect.top, rect.right, rect.bottom)
            }
        } else {
            // FIXME: 有时候会导致hq无效
            clearHq()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            //父容器在测量该View的尺寸，我们回传bitmap的尺寸
            _bitmapWidth
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }
        val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            _bitmapHeight
        } else {
            MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    private fun buildOverlayInterface(): PdfViewerOverlay.OverlayInterface {
        return object : PdfViewerOverlay.OverlayInterface {
            override fun onInkFinish(line: FloatArray) {
                if (_pageIndex >= 0) {
                    _pdfCore.addInk(_pageIndex, line, PDFColorWrap(1f, 0f, 0f))
                    redrawPage()
                    checkHqDraw(true) {
                        _overlayView.invalidate()
                    }
                }
            }

            override fun requireStructuredText(): List<WordLine> {
                return if (_pageIndex >= 0) {
                    _pdfCore.getText(_pageIndex)
                } else {
                    emptyList()
                }
            }

            override fun onSelectTextResult(textLines: List<WordLine>) {
                var needRedraw = false
                if (_pageIndex < 0 || textLines.isEmpty()) {
                    return
                }
                when (_pageBridge.pageTool) {
                    PageTool.CopyText -> {
                        // TODO: 复制文本实现

                    }

                    PageTool.Highlight -> {
                        _pdfCore.addMarkup(
                            _pageIndex,
                            PdfAnnotation.MARKUP_HIGHLIGHT,
                            textLines,
                            PDFColorWrap(1f, 1f, 0f)
                        )
                        needRedraw = true
                    }

                    PageTool.Underline -> {
                        _pdfCore.addMarkup(
                            _pageIndex,
                            PdfAnnotation.MARKUP_UNDERLINE,
                            textLines,
                            PDFColorWrap(0f, 0f, 1f)
                        )
                        needRedraw = true
                    }

                    PageTool.StrokeOut -> {
                        _pdfCore.addMarkup(
                            _pageIndex,
                            PdfAnnotation.MARKUP_STRIKEOUT,
                            textLines,
                            PDFColorWrap(1f, 0f, 0f)
                        )
                        needRedraw = true
                    }

                    else -> {}
                }
                if (needRedraw) {
                    redrawPage()
                    checkHqDraw(true) {
                        _overlayView.invalidate()
                    }
                }
            }

            override fun getFitScale(): Float {
                calculateFitScale()
                return _fitScale
            }
        }
    }

    private fun buildSearchOverlayInterface(): SearchResultView.ResultOverlayInterface {
        return object : SearchResultView.ResultOverlayInterface {
            override fun getPage(): Int {
                return _pageIndex
            }

            override fun getFitScale(): Float {
                calculateFitScale()
                return _fitScale
            }
        }
    }
}