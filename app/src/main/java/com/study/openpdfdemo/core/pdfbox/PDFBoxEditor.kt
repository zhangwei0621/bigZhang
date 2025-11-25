package com.study.openpdfdemo.core.pdfbox

import android.content.Context
import android.util.Log
import com.study.openpdfdemo.core.IEditWorkHandler
import com.study.openpdfdemo.core.IPdfEditor
import com.study.openpdfdemo.core.openpdf.PDAnnotationInk
import com.study.openpdfdemo.viewer.data.PDFAnnot
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import java.io.File
import java.io.InputStream
import java.io.OutputStream


class PDFBoxEditor(
    private val context: Context
) : IPdfEditor {
    override fun encrypt(input: InputStream, pwd: String, os: OutputStream) {
        PDDocument.load(input).use { document ->
            val protectionPolicy = StandardProtectionPolicy(pwd, pwd, AccessPermission())
            protectionPolicy.encryptionKeyLength = 128
            document.protect(protectionPolicy)
            document.save(os)
        }
    }

    override fun decrypt(file: File, pwd: String, os: OutputStream) {
        PDDocument.load(file, pwd).use { document ->
            document.save(os)
        }
    }

    override fun merge(
        inputs: List<File>,
        output: OutputStream
    ) {
        PDDocument().use { targetDoc ->
            inputs.forEach { file ->
                PDDocument.load(file).use { sourceDoc ->
                    val pages = sourceDoc.documentCatalog.pages
                    for (i in 0 until pages.count) {
                        val page = pages[i]
                        targetDoc.addPage(page)
                    }
                }
            }
            targetDoc.save(output)
        }
    }

    override fun isEncrypted(file: File): Boolean {
        return try {
            PDDocument.load(file).use { document ->
                document.isEncrypted
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun shortWork(
        file: File,
        pwd: String,
        output: OutputStream,
        action: (IEditWorkHandler) -> Unit
    ) {
        val settings = MemoryUsageSetting.setupTempFileOnly()
            .setTempDir(context.cacheDir)
        Log.d("PDFBoxEditor", "开启短作业：载入文件")
        PDDocument.load(file, pwd, settings).use { document ->
            document.isAllSecurityToBeRemoved = true
            Log.d("PDFBoxEditor", "载入文件成功")
            action(PDFBoxEditWorkHandler(document))
            Log.d("PDFBoxEditor", "短作业结束：保存文件")
            document.save(output)
            Log.d("PDFBoxEditor", "保存文件成功")
        }
    }
}

private class PDFBoxEditWorkHandler(
    private val document: PDDocument
) : IEditWorkHandler {
    override fun addInkAnno(
        page: Int,
        annot: PDFAnnot.InkAnnotWrap,
        totalPageCount: Int
    ) {
        if (page !in 0 until totalPageCount) {
            return
        }
        val pdfPage = document.pages[page - 1]
        val inkAnnotation = createInkAnnotation(pdfPage, annot)
        pdfPage.annotations.add(inkAnnotation)
    }

    override fun addMarkupAnno(
        page: Int,
        annot: PDFAnnot.MarkupAnnotWrap,
        totalPageCount: Int
    ) {
        if (page !in 0 until totalPageCount) {
            return
        }
        val pdfPage = document.pages[page - 1]
        val highLight = createHighLightAnnotation(annot)
        pdfPage.annotations.add(highLight)
    }

    private fun createInkAnnotation(
        pdfPage: PDPage,
        inkAnnotWrap: PDFAnnot.InkAnnotWrap
    ): PDAnnotation {
        val inkAnnotation = PDAnnotationInk()

        // 设置基本属性
        val rect = inkAnnotWrap.rect.let { rect ->
            PDRectangle(rect.left, rect.bottom, rect.width, rect.height)
        }
        inkAnnotation.rectangle = rect

        // 3. 构造 InkList：一条折线
        // 注意 PDF 坐标系：左下角为 (0,0)
        val inkList = arrayOf(inkAnnotWrap.line)
        inkAnnotation.setInkList(inkList)

        // 设置颜色
        inkAnnotWrap.color.let { color ->
            val pdColor = PDColor(floatArrayOf(color.r, color.g, color.b),
                PDDeviceRGB.INSTANCE)
            inkAnnotation.color = pdColor
        }

        // 设置线宽
        val border = PDBorderStyleDictionary()
        border.width = inkAnnotWrap.strokeWidth // 线宽
        inkAnnotation.borderStyle = border

        return inkAnnotation
    }

    private fun createHighLightAnnotation(
        inkAnnotWrap: PDFAnnot.MarkupAnnotWrap
    ): PDAnnotation {
        val highLightAnnotation = PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT)

        // 设置基本属性
        val rect = inkAnnotWrap.rect.let { rect ->
            PDRectangle(rect.left, rect.bottom, rect.width, rect.height)
        }
        highLightAnnotation.rectangle = rect
        highLightAnnotation.quadPoints = inkAnnotWrap.quadPoints

        // 设置颜色
        inkAnnotWrap.color.let { color ->
            val pdColor = PDColor(floatArrayOf(color.r, color.g, color.b),
                PDDeviceRGB.INSTANCE)
            highLightAnnotation.color = pdColor
            highLightAnnotation.constantOpacity = 0.5f
        }

        return highLightAnnotation
    }

}