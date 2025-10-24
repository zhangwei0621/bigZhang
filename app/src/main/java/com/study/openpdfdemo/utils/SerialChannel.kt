package com.study.openpdfdemo.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * 串行渠道
 * @param consumerScope 消费端Scope，默认使用默认调度器
 */
class SerialChannel(
    private val consumerScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val producerScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    private val channel = Channel<Int>(1)

    /**
     * 串行执行代码块。不建议一口气搞太多，容易崩溃。
     */
    fun runBlock(block: suspend CoroutineScope.() -> Unit) {
        channel.serialRun(block)
    }

    private fun Channel<Int>.serialRun(block: suspend CoroutineScope.() -> Unit) {
        producerScope.launch {
            send(0)
            consumerScope.launch {
                block()
                receive()
            }
        }
    }
}