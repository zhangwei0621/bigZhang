package com.study.openpdfdemo.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfAnnotation
import com.study.openpdfdemo.core.IPdfEditor
import com.study.openpdfdemo.core.IReader
import com.study.openpdfdemo.core.PageRect
import com.study.openpdfdemo.core.RenderOption
import com.study.openpdfdemo.core.pdfbox.PDFBoxEditor
import com.study.openpdfdemo.core.pdfium.PdfiumReader
import com.study.openpdfdemo.utils.toQuad
import com.study.openpdfdemo.viewer.data.PDFAnnot
import com.study.openpdfdemo.viewer.data.PDFColorWrap
import com.study.openpdfdemo.viewer.data.TextSearchResult
import com.study.openpdfdemo.viewer.text.extractor.data.WordLine
import com.study.openpdfdemo.viewer.tool.PdfCoreListener
import com.study.openpdfdemo.viewer.tool.PdfEditHelper
import com.study.openpdfdemo.viewer.tool.TextSearchHelper
import com.study.openpdfdemo.viewer.view.EditOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF核心工具类
 *
 * 用到的几个库说明和页码起始坐标：
 * OpenPdf：1 核心编辑库；
 * PdfBox：1 用于结构化文本；
 * Pdfium：0 用于实现渲染，也用于结构化文本；
 * 这里对外接口页码统一从0开始算，方法内部再做转换
 *
 * @param editCacheDir 缓存路径。存放编辑过程中产生的缓存
 */
class PdfCore(
    val file: File,
    private val editCacheDir: File,
    context: Context,
    private val buildPreviewFile: Boolean,
    val password: String = ""
) {
    companion object {
        const val TAG = "PDFCore"

        /**
         * 获取缓存目录
         */
        fun getCacheDir(context: Context): File {
            return File(context.cacheDir, "stamper_cache")
        }
    }

    private var _pdfReader: IReader = PdfiumReader().apply {
        Log.d(TAG, "Using Reader")
        load(file, password)
        Log.d(TAG, "Reader Load Finish")
    }
    private val _pdfEditor: IPdfEditor = PDFBoxEditor(context)
    private var _editHelper: PdfEditHelper? = null
    private var _coreListener: PdfCoreListener? = null

    var pageCount: Int = 0
        private set

    init {
        pageCount = _pdfReader.pageCount
        Log.d(TAG, "Page Count: $pageCount")
    }

    /**
     * 销毁Core对象
     */
    fun destroy() = runCatching {
        _pdfReader.close()
        _editHelper?.destroy()
    }

    /**
     * 获取页面结构化文本
     * @return 以行形式的数据合集
     */
    fun getText(pageIndex: Int): List<WordLine> {
        return _pdfReader.createTextExtractor().extract(pageIndex)
    }

    fun search(keyword: String): TextSearchResult {
        return searchTextImpl(keyword)
    }

    /**
     * 设置侦听以便接收Redo、Undo之类的状态
     */
    fun setCoreListener(listener: PdfCoreListener?) {
        _coreListener = listener
        _editHelper?.setCoreListener(_coreListener) ?: run {
            _coreListener?.onRedoUndoStateChanged(
                canUndo = false,
                canRedo = false
            )
            _coreListener?.onSaveStateChanged(false)
        }
    }

    fun undo(): Boolean = getEditHelper()?.undo() == true

    fun redo(): Boolean = getEditHelper()?.redo() == true

    /**
     * 保存编辑操作
     */
    fun save() {
        _editHelper?.executeSaveOperation()
    }

    fun save(page: Int, ops: List<EditOps>) {
        saveAnnotations(page, ops)
    }

    /**
     * 抛弃编辑操作
     */
    fun abandonSave() {
        _editHelper?.abandonSave()
    }

    /**
     * 获取页面原始尺寸
     */
    fun getPageSize(pageIndex: Int): PageRect {
        return _pdfReader.getPageSize(pageIndex + 1)
    }

    /**
     * 渲染页面到[output]
     */
    fun renderPage(
        pageIndex: Int,
        output: Bitmap,
        renderAnnot: Boolean = true
    ) {
        try {
            val start = System.currentTimeMillis()
            val targetFile = _editHelper?.getPreviewFile() ?: file
            _pdfReader.renderPage(targetFile, pageIndex, output, RenderOption(renderAnnot))
            val end = System.currentTimeMillis()
            Log.d(TAG, "render page $pageIndex : ${(end - start) / 1000f}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "渲染页面($pageIndex)错误：$e")
        }
    }

    /**
     * 局部渲染。
     * 可以确定bitmap使用的内存会更小，但是渲染时间不确定会不会更小。暂时无法确定是否具有更好的性能表现，而且前端界面的逻辑更加复杂了。
     */
    fun tiledRender(
        pageIndex: Int,
        output: Bitmap,
        scale: Float,
        startX: Float,
        startY: Float,
        renderAnnot: Boolean = true
    ) {
        try {
            val start = System.currentTimeMillis()
            val targetFile = _editHelper?.getPreviewFile() ?: file
            _pdfReader.renderPage(
                targetFile, pageIndex, output,
                RenderOption(renderAnnot, matrix = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(-startX, -startY)
                })
            )
            val end = System.currentTimeMillis()
            if (pageIndex == 1) {
                Log.d(TAG, "tiled render page $pageIndex : ${(end - start) / 1000f}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "渲染局部页面($pageIndex)错误：$e")
        }
    }

    /**
     * 添加Ink注解
     * @param pageIndex 0-base
     */
    suspend fun addInk(
        pageIndex: Int, line: FloatArray, color: PDFColorWrap
    ) = withContext(Dispatchers.IO) {
        getEditHelper()?.executeStamperOperation { stamper ->
            val page = pageIndex + 1
            val rect = _pdfReader.getPageSize(page)
            val annotWrap = PDFAnnot.InkAnnotWrap(
                Rectangle(rect.width, rect.height),
                color,
                4f,
                line
            )
            stamper.addInkAnno(page, annotWrap, pageCount)
        }
    }

    /**
     * 添加TextMarkup注解
     * @param pageIndex 0-base
     */
    fun addMarkup(
        pageIndex: Int,
        type: Int,
        textLines: List<WordLine>,
        color: PDFColorWrap
    ) {
        getEditHelper()?.executeStamperOperation { stamper ->
            val page = pageIndex + 1
            textLines.forEach { line ->
                val rect = line.getLineRectForPdf()
                if (rect != null) {
                    val annotWrap = PDFAnnot.MarkupAnnotWrap(
                        rect,
                        type,
                        color,
                        rect.toQuad()
                    )
                    stamper.addMarkupAnno(page, annotWrap, pageCount)
                }
            }
        }
    }

    fun saveAnnotations(page: Int, annotations: List<EditOps>) {
        getEditHelper()?.saveOperation { stamper ->
            val rect = _pdfReader.getPageSize(page)
            for (op in annotations) {
                when (op) {
                    is EditOps.HighLight -> {
                        op.lines.forEach { line ->
                            val rect = line.getLineRectForPdf()
                            if (rect != null) {
                                val annotWrap = PDFAnnot.MarkupAnnotWrap(
                                    rect,
                                    PdfAnnotation.MARKUP_HIGHLIGHT,
                                    PDFColorWrap(1f, 1f, 0f),
                                    rect.toQuad()
                                )
                                stamper.addMarkupAnno(page, annotWrap, pageCount)
                            }
                        }
                    }
                    is EditOps.Ink -> {
                        val annotWrap = PDFAnnot.InkAnnotWrap(
                            Rectangle(rect.width, rect.height),
                            PDFColorWrap(1f, 0f, 0f),
                            4f,
                            op.toPdfLines()
                        )
                        stamper.addInkAnno(page, annotWrap, pageCount)
                    }
                }
            }
        }
    }

    //全文搜索
    private fun searchTextImpl(keyword: String): TextSearchResult {
        val start = System.currentTimeMillis()
        val result = TextSearchResult()
        if (keyword.isNotEmpty()) {
            val searchHelper = TextSearchHelper()
            val extractor = _pdfReader.createTextExtractor()
            repeat(pageCount) { pageIndex ->
                val textLines = extractor.extract(pageIndex)
                val searchData = searchHelper.search(textLines, keyword)
                if (searchData.isNotEmpty()) {
                    result.pageList.add(pageIndex)
                    result.searchDataList.add(searchData)
                }
            }
        }
        val end = System.currentTimeMillis()
        Log.d(TAG, "pdfium search time：${(end - start) / 1000f}")
        return result
    }

    @Synchronized
    private fun getEditHelper(): PdfEditHelper? {
        if (_editHelper == null) {
            _editHelper = PdfEditHelper(file, editCacheDir, password, _pdfEditor,
                buildPreviewFile = buildPreviewFile)
            _editHelper?.setCoreListener(_coreListener)
        }
        return _editHelper
    }

}
