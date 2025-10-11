package com.study.openpdfdemo.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import com.lowagie.text.pdf.PdfReader
import com.study.openpdfdemo.utils.ImageHelper
import org.openpdf.renderer.PDFFile
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import javax.imageio.ImageIO

class PdfCore(
    private val file: File
) {
    //文件描述，用于安卓的PDF渲染
    private lateinit var pfd: ParcelFileDescriptor

    //安卓的PDF渲染器
    private lateinit var renderer: PdfRenderer

    //openpdf的阅读器
    private lateinit var reader: PdfReader

    init {
        runCatching {
            pfd = ParcelFileDescriptor(
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            )
            renderer = PdfRenderer(pfd)
            reader = PdfReader(file.absolutePath)
        }
    }

    /**
     * 渲染指定页面为bitmap。openpdf不提供安卓的图像渲染，这里只能用安卓自带的渲染方法了。
     *
     * 但是这种方法无法获取页面的数据结构，可能不利于后续实现编辑操作，
     */
    fun renderPage(page: Int): Bitmap? {
        try {
//            if (page !in 0..<renderer.pageCount) {
//                return null
//            }
            val page = renderer.openPage(page)
            val bitmap = createBitmap(page.width, page.height)
            //todo 这里需要计算缩放矩阵。1.获得最好显示比例和清晰度；2.方便后续实现手势缩放；
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 渲染指定页面为bitmap。使用openpdf的方法，通过ImageIo转换为字节流。
     */
    fun renderPageByOpenPdf(page: Int): Bitmap? {
//        if (page !in 0..<renderer.pageCount) {
//            return null
//        }
        // TODO: 子线程操作
        val fis = FileInputStream(file)
        val fc = fis.channel
        val bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
        val pdfFile = PDFFile(bb)
        val pdfPage = pdfFile.getPage(page, true)
        val pageBox = pdfPage.bBox
        val rect = Rectangle(0, 0, pageBox.width.toInt(), pageBox.height.toInt())
        // TODO: 对宽高做缩放处理 
        val image = pdfPage.getImage(
            rect.width, rect.height, rect,
            null,
            false, true
        )
        val bufferedImage = BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB)
        bufferedImage.createGraphics().apply {
            drawImage(image, 0, 0, null)
            dispose()
        }

        //将图像转换为bitmap
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", outputStream)
        outputStream.flush()
        val byteArray = outputStream.toByteArray()
        val bitmap = ImageHelper.byteImageCovert(byteArray)
        outputStream.close()

        //测试输出为png文件
//        val testOutput = File("/storage/emulated/0/ASM4_1.pdf.png")
//        ImageIO.write(bufferedImage, "png", testOutput)
        return bitmap
    }

    /**
     * 释放资源
     */
    fun destroy() = try {
        renderer.close()
        pfd.close()
    } catch (_: Exception) {
    }
}