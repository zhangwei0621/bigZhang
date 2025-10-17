package com.study.openpdfdemo.viewer

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.graphics.createBitmap
import com.lowagie.text.pdf.PdfAnnotation
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfBorderArray
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.study.openpdfdemo.utils.TextStripper
import com.study.openpdfdemo.utils.toQuad
import com.study.openpdfdemo.viewer.data.PDFAnnot
import com.study.openpdfdemo.viewer.data.PDFColorWrap
import com.study.openpdfdemo.viewer.tool.PdfCoreListener
import com.study.openpdfdemo.viewer.tool.PdfEditHelper
import io.legere.pdfiumandroid.PdfiumCore
import java.io.File
import kotlin.math.min
import kotlin.random.Random

/**
 * PDF核心工具类，主要使用OpenPDF。对外接口的页码都从1开始。
 */
// TODO: 考虑文件加密情况
class PdfCoreCore(
    val file: File,
    context: Context
) {
    private val tag = "PDFCore"
    private var _pdfReader: PdfReader = PdfReader(file.absolutePath)
    private val _editCacheDir = File(context.cacheDir, "stamper_cache/")
    private var _editHelper: PdfEditHelper? = null
    private val _pdfiumCore = PdfiumCore()
    private var _coreListener: PdfCoreListener? = null

    var pageCount: Int = 0
        private set

    init {
        pageCount = _pdfReader.numberOfPages
    }

    /**
     * 设置侦听以便接收Redo、Undo之类的状态
     */
    fun setCoreListener(listener: PdfCoreListener?) {
        _coreListener = listener
        getEditHelper()?.setCoreListener(_coreListener)
    }

    fun undo(): Boolean = getEditHelper()?.undo() == true

    fun redo(): Boolean = getEditHelper()?.redo() == true

    /**
     * 销毁Core对象
     */
    fun destroy() = runCatching {
        _pdfReader.close()
        _editHelper?.destroy()
    }

    /**
     * 保存编辑操作
     */
    fun save() {
        _editHelper?.executeSaveOperation()
    }

    /**
     * 抛弃编辑操作
     */
    fun abandonSave() {
        _editHelper?.abandonSave()
    }

    /**
     * 使用Pdfium的渲染
     */
    fun renderPage(
        page: Int,
        containerWidth: Int,
        containerHeight: Int
    ): Pair<Float, Bitmap>? {
        try {
            val targetFile = _editHelper?.getPreviewFile() ?: file
            val pageIndex = page - 1
            val fd = ParcelFileDescriptor(
                ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
            )
            val doc = _pdfiumCore.newDocument(fd)
            val pdfPage = doc.openPage(pageIndex)
            //由于pdfium获取页面大小是经过dpi换算的，不是原始大小，这里使用openpdf的接口
            val pageSize = _pdfReader.getPageSize(page)
            val scale =
                min(containerWidth * 1f / pageSize.width, containerHeight * 1f / pageSize.height)
            val bitmapWidth = (pageSize.width * scale).toInt()
            val bitmapHeight = (pageSize.height * scale).toInt()
            val bitmap = createBitmap(bitmapWidth, bitmapHeight)
            pdfPage.renderPageBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, true)
            pdfPage.close()
            doc.close()
            fd.close()
            return scale to bitmap
        } catch (e: Exception) {
            Log.e(tag, "渲染页面($page)错误：$e")
        }
        return null
    }

    /**
     * 仅测试使用。为指定页面所有可结构化的文本添加TextMarkup注解。
     */
    fun executeMarkupTest(page: Int, type: Int, color: PDFColorWrap) {
        getEditHelper()?.executeStamperOperation { stamper ->
            val lines = TextStripper().extract(file, page)
            lines.forEach { line ->
                val rect = line.getLineRect(false)
                if (rect != null) {
                    val annotWrap = PDFAnnot.MarkupAnnotWrap(
                        rect,
                        type,
                        color,
                        rect.toQuad()
                    )
                    addMarkupAnnotation(page, annotWrap, stamper)
                }
            }
        }
    }

    /**
     * 仅测试使用。为指定页面添加随机Ink注解。
     */
    fun executeInkTest(page: Int, color: PDFColorWrap) {
        getEditHelper()?.executeStamperOperation { stamper ->
            val rect = _pdfReader.getPageSize(page)
            val randomX = { Random.nextInt(0, rect.width.toInt()).toFloat() }
            val randomY = { Random.nextInt(0, rect.height.toInt()).toFloat() }
            //若干点组成的一条线
            val line = floatArrayOf(
                randomX(), randomY(),
                randomX(), randomY(),
                randomX(), randomY(),
                randomX(), randomY(),
                randomX(), randomY(),
            )
            val annotWrap = PDFAnnot.InkAnnotWrap(
                rect,
                color,
                4f,
                line
            )
            addInkAnnotation(page, annotWrap, stamper)
        }
    }

    fun requireStructuredText(page: Int): List<TextStripper.Line> {
        return TextStripper().extract(file, page)
    }

    fun addInk(page: Int, line: FloatArray, color: PDFColorWrap) {
        getEditHelper()?.executeStamperOperation { stamper ->
            val rect = _pdfReader.getPageSize(page)
            val annotWrap = PDFAnnot.InkAnnotWrap(
                rect,
                color,
                4f,
                line
            )
            addInkAnnotation(page, annotWrap, stamper)
        }
    }

    fun addMarkup(page: Int, type: Int, textLines: List<TextStripper.Line>, color: PDFColorWrap) {
        getEditHelper()?.executeStamperOperation { stamper ->
            textLines.forEach { line ->
                val rect = line.getLineRect(false)
                if (rect != null) {
                    // TODO: 研究quadPoints是不是可以一次加入多行文本(但是一次多行意味着需要重新计算Rect)
                    val annotWrap = PDFAnnot.MarkupAnnotWrap(
                        rect,
                        type,
                        color,
                        rect.toQuad()
                    )
                    addMarkupAnnotation(page, annotWrap, stamper)
                }
            }
        }
    }

    @Synchronized
    private fun getEditHelper(): PdfEditHelper? {
        if (_editHelper == null) {
            _editHelper = PdfEditHelper(file, _editCacheDir)
            _editHelper?.setCoreListener(_coreListener)
        }
        return _editHelper
    }

    private fun addMarkupAnnotation(
        page: Int,
        annot: PDFAnnot.MarkupAnnotWrap,
        stamper: PdfStamper
    ) {
        try {
            if (page <= 0 || page > pageCount) {
                throw Exception("页码($page)超出范围")
            }
            if (annot.markupType != PdfAnnotation.MARKUP_HIGHLIGHT
                && annot.markupType != PdfAnnotation.MARKUP_UNDERLINE
                && annot.markupType != PdfAnnotation.MARKUP_STRIKEOUT
            ) {
                throw Exception("Markup类型(${annot.markupType})错误")
            }
            if (annot.quadPoints.isEmpty()) {
                throw Exception("Quad数组为空")
            }
            val pdfAnnot = PdfAnnotation.createMarkup(
                stamper.writer,
                annot.rect,
                "",
                annot.markupType,
                annot.quadPoints
            ).apply {
                put(PdfName.C, PdfArray(annot.color.asFloatArray()))
            }
            stamper.addAnnotation(pdfAnnot, page)
        } catch (e: Exception) {
            Log.e(tag, "添加Markup注解错误：$e")
        }
    }

    private fun addInkAnnotation(page: Int, annot: PDFAnnot.InkAnnotWrap, stamper: PdfStamper) {
        try {
            if (page <= 0 || page > pageCount) {
                throw Exception("页码($page)超出范围")
            }
            if (annot.line.isEmpty()) {
                throw Exception("Line为空")
            }
            val pdfAnnot = PdfAnnotation.createInk(
                stamper.writer,
                annot.rect,
                "",
                arrayOf(annot.line)
            ).apply {
                put(PdfName.C, PdfArray(annot.color.asFloatArray()))
                setBorder(PdfBorderArray(0f, 0f, annot.autoStrokeWidth()))
            }
            stamper.addAnnotation(pdfAnnot, page)
        } catch (e: Exception) {
            Log.e(tag, "添加Ink注解错误：$e")
        }
    }
}
