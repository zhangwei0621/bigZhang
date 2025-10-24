package com.study.openpdfdemo.utils

class DataLooper<T>(
    private val data: List<T>
) {
    private var _index = -1

    fun next(): T? {
        _index++
        if (_index >= data.size) {
            _index = 0
        }
        return data.getOrNull(_index)
    }

    fun reset() {
        _index = -1
    }
}