package com.study.openpdfdemo.viewer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.RectI
import com.artifex.mupdf.fitz.SeekableInputStream
import com.artifex.mupdf.fitz.SeekableStream
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfAnnotation
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfBorderArray
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.study.openpdfdemo.utils.TextStripper
import com.study.openpdfdemo.utils.copyTo
import com.study.openpdfdemo.utils.toQuad
import com.study.openpdfdemo.utils.writeToFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.random.Random

/**
 * PDF核心工具类，主要使用OpenPDF。页码从1开始。
 */
class PdfCoreCore(
    val file: File,
    context: Context
) {
    private val tag = "PDFCore"
    private val pdfMime = "application/pdf"
    private val zoom = 160f / 72f

    private var _pdfReader: PdfReader = PdfReader(file.absolutePath)
    private val _editCacheDir = File(context.cacheDir, "stamper_cache/")
    private var _editHelper: PdfEditHelper? = null

    var pageCount: Int = 0
        private set

    init {
        pageCount = _pdfReader.numberOfPages
    }

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
        _editHelper?.initData()
    }

    /**
     * 渲染指定页面为Bitmap
     */
    fun renderPage(page: Int, containerWidth: Int, containerHeight: Int): Pair<Float, Bitmap>? {
        try {
            // TODO: 这里先用mupdf的渲染，之后需要换成别的
            val targetFile = _editHelper?.getPreviewFile() ?: file
            val cis = ContentInputStream(targetFile.absolutePath)
            val mupdfDoc =
                Document.openDocument(cis, pdfMime)
            val mupdfPage = mupdfDoc?.loadPage(page - 1) ?: throw Exception("Page加载失败")
            val mupdfPageBounds = mupdfPage.bounds ?: throw Exception("Bounds获取失败")
            val pageWidth = mupdfPageBounds.x1 - mupdfPageBounds.x0
            val pageHeight = mupdfPageBounds.y1 - mupdfPageBounds.y0
            val scale = min(containerWidth / pageWidth, containerHeight / pageHeight)
            val bitmapWidth = (pageWidth * scale).toInt()
            val bitmapHeight = (pageHeight * scale).toInt()
            val mupdfCtm = Matrix(zoom, zoom).also {
                val box = RectI(mupdfPageBounds.transform(it))
                val sx = bitmapWidth / (box.x1 - box.x0).toFloat()
                val sy = bitmapHeight / (box.y1 - box.y0).toFloat()
                it.scale(sx, sy)
            }
            val bitmap = createBitmap(bitmapWidth, bitmapHeight)
            val dev = AndroidDrawDevice(bitmap)
            mupdfPage.run(dev, mupdfCtm)
            mupdfPage.destroy()
            dev.close()
            mupdfDoc.destroy()
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

// TODO: 整合这几个工具类
data class PDFColorWrap(
    val r: Float, val g: Float, val b: Float
) {
    fun asFloatArray() = floatArrayOf(r, g, b)

    fun asInt(): Int {
        return -0x1000000 or
                ((r * 255.0f + 0.5f).toInt() shl 16) or
                ((g * 255.0f + 0.5f).toInt() shl 8) or (b * 255.0f + 0.5f).toInt()
    }
}

sealed class PDFAnnot {
    class MarkupAnnotWrap(
        val rect: Rectangle,
        val markupType: Int,
        val color: PDFColorWrap,
        val quadPoints: FloatArray,
    ) : PDFAnnot()

    /**
     * 画笔注解。每次只能写一条线段。
     */
    class InkAnnotWrap(
        val rect: Rectangle,
        val color: PDFColorWrap,
        val strokeWidth: Float,
        val line: FloatArray,
    ) : PDFAnnot() {
        /**
         * 获取单个点的线宽。当线段只有一个点时，需要使用更大的线宽。
         */
        fun getSinglePointStrokeWidth(): Float {
            return strokeWidth * 2f
        }

        /**
         * 根据点数获取合适的线宽
         */
        fun autoStrokeWidth(): Float {
            return if (line.size == 2) {
                getSinglePointStrokeWidth()
            } else {
                strokeWidth
            }
        }
    }
}

/**
 * openpdf编辑辅助工具。
 */
class PdfEditHelper(
    /**
     * 原始文件
     */
    private val file: File,

    /**
     * 工作缓存目录
     */
    private val cacheDir: File,
) {
    // TODO: 后续根据使用的渲染库，可以考虑把预览文件换成数据流的形式
    private var previewFile: File? = null
    private var _stamper: PdfStamper? = null
    private val _needSave = AtomicBoolean(false)

    val needSave get() = _needSave.get()

    init {
        initData()
    }

    /**
     * 获取编辑预览文件，可以通过这个文件预览编辑效果。
     */
    fun getPreviewFile() = previewFile

    /**
     * 执行Stamper编辑操作
     */
    fun executeStamperOperation(working: (PdfStamper) -> Unit) {
        // TODO: 实现redo和undo
        previewFile?.let { previewFile ->
            val reader = PdfReader(previewFile.absolutePath)
            val outputStream = ByteArrayOutputStream()
            _stamper = PdfStamper(reader, outputStream)
            //执行编辑工作
            _stamper?.let { working.invoke(it) }
            //关闭写入，数据开始写入到outputStream
            _stamper?.close()
            _stamper = null
            reader.close()
            //将编辑效果写入预览文件
            outputStream.use { os ->
                os.writeToFile(previewFile)
            }
            _needSave.set(true)
        }
    }

    /**
     * 将编辑结果保存到原文件中
     */
    fun executeSaveOperation() {
        if (_needSave.getAndSet(false)) {
            previewFile?.copyTo(file)
        }
    }

    /**
     * 销毁，删除缓存文件。销毁之后这个对象就不能复用了，需要重新生成。
     */
    fun destroy() {
        _stamper?.close()
        previewFile?.delete()
    }

    /**
     * 初始化数据，将原文件是数据复制到预览文件中。调用该方法也意味着抛弃之前已存在的编辑操作。
     */
    fun initData() {
        val time = System.currentTimeMillis()
        cacheDir.mkdirs()
        clearCache()
        previewFile = File(cacheDir, "temp_${time}_input_${file.name}").apply {
            file.copyTo(this)
        }
    }

    private fun clearCache() {
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
}

/**
 * 用于mupdf的输入流
 */
class ContentInputStream : SeekableInputStream {
    private var stream: InputStream? = null
    private var length: Long = 0
    private var p: Long = 0
    private var mustReopenStream: Boolean

    constructor(path: String) {
        mustReopenStream = false
        val file = File(path)
        val fileInputStream = FileInputStream(file)
        this.length = fileInputStream.available().toLong()
        reopenStream(fileInputStream)
    }

    constructor(context: Context, uri: Uri) {
        mustReopenStream = false
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            this.length = inputStream.available().toLong()
            reopenStream(inputStream)
        }
    }

    @Throws(IOException::class)
    override fun seek(offset: Long, whence: Int): Long {
        var newp = p
        when (whence) {
            SeekableStream.SEEK_SET -> newp = offset
            SeekableStream.SEEK_CUR -> newp = p + offset
            SeekableStream.SEEK_END -> {
                if (length < 0) {
                    val buf = ByteArray(16384)
                    var k: Int
                    while ((stream!!.read(buf).also { k = it }) != -1) p += k.toLong()
                    length = p
                }
                newp = length + offset
            }
        }

        if (newp < p) {
            if (!mustReopenStream) {
                try {
                    stream!!.skip(newp - p)
                } catch (x: IOException) {
                    mustReopenStream = true
                }
            }
            if (mustReopenStream) {
                reopenStream(stream)
                stream!!.skip(newp)
            }
        } else if (newp > p) {
            stream!!.skip(newp - p)
        }
        return newp.also { p = it }
    }

    @Throws(IOException::class)
    override fun position(): Long {
        return p
    }

    @Throws(IOException::class)
    override fun read(buf: ByteArray): Int {
        val n = stream!!.read(buf)
        if (n > 0) p += n.toLong()
        else if (n < 0 && length < 0) length = p
        return n
    }

    @Throws(IOException::class)
    fun reopenStream(inputStream: InputStream?) {
        if (stream != null) {
            stream!!.close()
            stream = null
        }
        stream = inputStream
        p = 0
    }
}
