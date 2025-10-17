package com.study.openpdfdemo.viewer.tool

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.study.openpdfdemo.utils.copyAsByteArrayOS
import com.study.openpdfdemo.utils.copyTo
import com.study.openpdfdemo.utils.writeToFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

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

    private var coreListener: PdfCoreListener?
) {
    private var previewFile: File? = null
    private var _stamper: PdfStamper? = null
    private val _needSave = AtomicBoolean(false)
    private val operationStack = OperationStack()

    init {
        initData()
    }

    fun setCoreListener(listener: PdfCoreListener?) {
        coreListener = listener
    }

    /**
     * 获取编辑预览文件，可以通过这个文件预览编辑效果。
     */
    fun getPreviewFile() = previewFile

    /**
     * 执行Stamper编辑操作，此方法执行的编辑操作将记录为一次操作并记入操作栈
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
                previewFile.copyTo(file)
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
        _stamper?.close()
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