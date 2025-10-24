package com.study.openpdfdemo.viewer.tool

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.lowagie.text.pdf.PdfWriter
import com.study.openpdfdemo.utils.copyAsByteArrayOS
import com.study.openpdfdemo.utils.copyTo
import com.study.openpdfdemo.utils.writeToFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * openpdf编辑辅助工具
 * @param file 原始文件
 * @param cacheDir 工作缓存目录
 * @param password 密码。如果文件有加密，需要密码才能编辑。编辑过程中的文件是没用加密的。如果该参数不为空字符串，保存时才会将密码重新写入到输出文件中。
 */
// TODO: 线程优化
class PdfEditHelper(
    private val file: File,
    private val cacheDir: File,
    private val password: String = ""
) {
    //预览文件，暂存操作效果到该文件
    private var previewFile: File? = null

    //是否可以保存
    private val _needSave = AtomicBoolean(false)

    //操作栈实现
    private val operationStack = OperationStack()

    private var coreListener: PdfCoreListener? = null

    init {
        initData()
    }

    fun setCoreListener(listener: PdfCoreListener?) {
        coreListener = listener
        coreListener?.onRedoUndoStateChanged(
            operationStack.canUndo,
            operationStack.canRedo
        )
        coreListener?.onSaveStateChanged(_needSave.get())
    }

    /**
     * 获取编辑预览文件，可以通过这个文件预览编辑效果。
     */
    fun getPreviewFile() = previewFile

    /**
     * 执行Stamper编辑操作，此方法执行的编辑操作将记录为一次操作并记入操作栈
     */
    fun executeStamperOperation(working: (PdfStamper) -> Unit) {
        previewFile?.let { previewFile ->
            val reader = PdfReader(previewFile.absolutePath, password.toByteArray())
            val outputStream = ByteArrayOutputStream()
            val stamper = PdfStamper(reader, outputStream)
            //执行编辑工作
            stamper.let { working.invoke(it) }
            //关闭写入，数据开始写入到outputStream
            stamper.close()
            reader.close()
            outputStream.writeToFile(previewFile)
            operationStack.add(outputStream)
            _needSave.set(true)
            coreListener?.onRedoUndoStateChanged(
                operationStack.canUndo,
                operationStack.canRedo
            )
            coreListener?.onSaveStateChanged(_needSave.get())
        }
    }

    /**
     * 将编辑结果保存到原文件中
     */
    fun executeSaveOperation() {
        if (_needSave.getAndSet(false)) {
            previewFile?.let { previewFile ->
                //写到正式文件中
                if (password.isNotEmpty()) {
                    FileInputStream(previewFile).use { fis ->
                        PdfReader(fis, password.toByteArray()).use { reader ->
                            FileOutputStream(file).use { fos ->
                                PdfStamper(reader, fos).use { stamper ->
                                    stamper.setEncryption(
                                        password.toByteArray(),
                                        password.toByteArray(),
                                        PdfWriter.ALLOW_COPY or PdfWriter.ALLOW_COPY,
                                        PdfWriter.ENCRYPTION_AES_128
                                    )
                                }
                            }
                        }
                    }
                } else {
                    previewFile.copyTo(file)
                }
                //重置操作栈数据
                operationStack.clear()
                operationStack.setOriginalStream(previewFile.copyAsByteArrayOS())
            }
        }
        coreListener?.onRedoUndoStateChanged(operationStack.canUndo, operationStack.canRedo)
        coreListener?.onSaveStateChanged(_needSave.get())
    }

    /**
     * 抛弃编辑结果
     */
    fun abandonSave() {
        initData()
    }

    /**
     * 撤销
     * @return true=操作成功
     */
    fun undo(): Boolean {
        var isSuccess = false
        previewFile?.let { previewFile ->
            isSuccess = operationStack.undo()?.writeToFile(previewFile) == true
        }
        coreListener?.onRedoUndoStateChanged(operationStack.canUndo, operationStack.canRedo)
        return isSuccess
    }

    /**
     * 重做
     * @return true=操作成功
     */
    fun redo(): Boolean {
        var isSuccess = false
        previewFile?.let { previewFile ->
            isSuccess = operationStack.redo()?.writeToFile(previewFile) == true
        }
        coreListener?.onRedoUndoStateChanged(operationStack.canUndo, operationStack.canRedo)
        return isSuccess
    }

    /**
     * 销毁，删除缓存文件。销毁之后这个对象就不能复用了，需要重新生成。
     */
    fun destroy() {
        previewFile?.delete()
        operationStack.destroy()
    }

    /**
     * 初始化数据，将原文件是数据复制到预览文件中。调用该方法也意味着抛弃之前已存在的编辑操作。
     */
    fun initData() {
        val time = System.currentTimeMillis()
        cacheDir.mkdirs()
        clearCache()
        _needSave.set(false)
        previewFile = File(cacheDir, "temp_${time}_input_${file.name}").apply {
            file.copyTo(this)
            operationStack.clear()
            operationStack.setOriginalStream(this.copyAsByteArrayOS())
        }
        coreListener?.onRedoUndoStateChanged(operationStack.canUndo, operationStack.canRedo)
        coreListener?.onSaveStateChanged(_needSave.get())
    }

    private fun clearCache() {
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
}