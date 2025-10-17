package com.study.openpdfdemo.viewer.tool

import java.io.ByteArrayOutputStream
import java.util.Stack

/**
 * 辅助记录操作栈
 */
// TODO: 优化操作栈。目前使用文件流记录操作栈，每个流都记录完整的文件数据，用户随便几个编辑操作就会产生很多流对象，可能有性能问题，需要测试
class OperationStack {
    private val _currentStack = Stack<ByteArrayOutputStream>()
    private val _recycleStack = Stack<ByteArrayOutputStream>()
    private var _originalStream: ByteArrayOutputStream? = null

    /**
     * true=可以撤销操作
     */
    val canUndo: Boolean get() = _currentStack.isNotEmpty()

    /**
     * true=可以重做操作
     */
    val canRedo: Boolean get() = _recycleStack.isNotEmpty()

    /**
     * 设置原始数据流
     */
    fun setOriginalStream(stream: ByteArrayOutputStream) {
        _originalStream = stream
    }

    /**
     * 销毁
     */
    fun destroy() {
        clear()
        _originalStream?.close()
    }

    /**
     * 清空所有操作栈数据，关闭所有数据流
     */
    fun clear() {
        clearRecycleStack()
        clearCurrentStack()
    }

    /**
     * 将数据写入当前栈，这意味着抛弃回收栈的全部数据
     */
    fun add(value: ByteArrayOutputStream) {
        clearRecycleStack()
        _currentStack.add(value)
    }

    /**
     * 撤销。将当前栈的最新数据移动到回收栈中
     * @return 如果成功，返回移动后当前栈的最新数据或原始数据，否则返回null
     */
    fun undo(): ByteArrayOutputStream? {
        if (!canUndo) {
            return null
        }
        val currentTop = runCatching { _currentStack.pop() }.getOrNull()
        if (currentTop != null) {
            _recycleStack.push(currentTop)
        }
        //如果已经撤销所有操作，则回传原始数据
        return runCatching { _currentStack.peek() }.getOrNull() ?: _originalStream
    }

    /**
     * 重做。将回收栈最新的数据移动到当前栈中
     * @return 如果成功，返回移动后当前栈的最新数据，否则返回null
     */
    fun redo(): ByteArrayOutputStream? {
        if (!canRedo) {
            return null
        }
        val recycleTop = runCatching { _recycleStack.pop() }.getOrNull()
        if (recycleTop != null) {
            _currentStack.push(recycleTop)
        }
        return runCatching { _currentStack.peek() }.getOrNull()
    }

    private fun clearCurrentStack() {
        _currentStack.apply {
            forEach { it.close() }
            clear()
        }
    }

    private fun clearRecycleStack() {
        _recycleStack.apply {
            forEach { it.close() }
            clear()
        }
    }
}