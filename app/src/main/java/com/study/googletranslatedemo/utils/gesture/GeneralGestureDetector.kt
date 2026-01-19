package com.study.googletranslatedemo.utils.gesture

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 两点触摸手势检测器（缩放、旋转、平移）（Kotlin版本）
 * 
 * 功能：
 * 1. 检测两点触摸手势（pinch zoom）
 * 2. 计算缩放比例
 * 3. 计算旋转角度
 * 4. 计算中心点坐标
 * 5. 计算平移偏移
 * 
 * 使用方式：
 * 1. 创建实例并设置监听器
 * 2. 在View的onTouchEvent中调用onTouchEvent()方法
 * 3. 通过监听器回调获取手势变化
 */
class GeneralGestureDetector(listener: OnGestureListener?) {

    // ========== 回调接口 ==========
    /**
     * 手势监听器
     */
    fun interface OnGestureListener {
        /**
         * 当手势更新时调用
         * @param detector 检测器实例
         */
        fun onGestureUpdate(detector: GeneralGestureDetector)
    }

    // ========== 监听器 ==========
    var listener: OnGestureListener? = listener

    // ========== 触摸状态 ==========
    var isInGesture = false
        private set
    private var isFirstPointer = false           // 第一个指针是否为主指针
    private var primaryPointerId = -1            // 主指针ID
    private var secondaryPointerId = -1          // 副指针ID
    private var previousEvent: MotionEvent? = null  // 上一次的MotionEvent
    private var currentEvent: MotionEvent? = null   // 当前的MotionEvent
    private var invalidState = false             // 状态是否无效

    // ========== 手势数据 ==========
    private var initialDistance = -1f            // 初始距离（缓存）
    private var previousDistance = -1f           // 上一次距离（缓存）
    private var currentDistance = -1f            // 当前距离（缓存）
    private var scaleFactor = -1f                // 缩放比例（缓存）

    // ========== 当前手势数据 ==========
    val currentVector = PointF()         // 当前两点向量（从主指针指向副指针）
    val previousVector = PointF()        // 上一次两点向量
    private var currentPressure = 0f             // 当前压力总和
    private var previousPressure = 0f            // 上一次压力总和

    // ========== 位置信息 ==========
    var centerX = 0f                             // 中心点X坐标
        private set
    var centerY = 0f                             // 中心点Y坐标
        private set
    var previousCenterX = 0f                     // 上一次中心点X坐标
        private set
    var previousCenterY = 0f                     // 上一次中心点Y坐标
        private set

    // ========== 偏移量 ==========
    val offsetX: Float                           // X方向偏移
        get() = centerX - previousCenterX
    val offsetY: Float                           // Y方向偏移
        get() = centerY - previousCenterY

    init {
        reset()
    }

    /**
     * 重置状态
     */
    fun reset() {
        recycleEvents()
        isInGesture = false
        primaryPointerId = -1
        secondaryPointerId = -1
        invalidState = false
        initialDistance = -1f
        previousDistance = -1f
        currentDistance = -1f
        scaleFactor = -1f
        currentVector.set(0f, 0f)
        previousVector.set(0f, 0f)
        currentPressure = 0f
        previousPressure = 0f
        centerX = 0f
        centerY = 0f
        previousCenterX = 0f
        previousCenterY = 0f
    }

    /**
     * 处理触摸事件
     * @param event 触摸事件
     */
    fun onTouchEvent(event: MotionEvent) {
        val actionMasked = event.actionMasked

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            reset()
        }

        if (invalidState) {
            return
        }

        if (!isInGesture) {
            // 处理单点或准备多点触摸
            handleSinglePointer(event, actionMasked)
        } else {
            // 处理多点触摸手势
            handleMultiPointer(event, actionMasked)
        }
    }

    /**
     * 处理单点触摸
     */
    private fun handleSinglePointer(event: MotionEvent, actionMasked: Int) {
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                primaryPointerId = event.getPointerId(0)
                isFirstPointer = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                reset()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 第二个手指按下，开始多点手势
                val actionIndex = event.actionIndex
                val newPointerId = event.getPointerId(actionIndex)

                // 回收之前的事件
                recycleEvents()
                previousEvent = MotionEvent.obtain(event)

                val primaryIndex = event.findPointerIndex(primaryPointerId)
                if (primaryIndex < 0 || primaryIndex == actionIndex) {
                    // 主指针无效或就是新指针，重新选择主指针
                    primaryPointerId = event.getPointerId(
                        findAlternativePointer(newPointerId, -1, event)
                    )
                }

                secondaryPointerId = newPointerId
                isFirstPointer = false
                updateGestureData(event)
                isInGesture = true
            }
        }
    }

    /**
     * 处理多点触摸
     */
    private fun handleMultiPointer(event: MotionEvent, actionMasked: Int) {
        when (actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                reset()
                return
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // 有手指抬起
                val pointerCount = event.pointerCount
                val actionIndex = event.actionIndex
                val releasedPointerId = event.getPointerId(actionIndex)

                if (pointerCount > 2) {
                    // 超过两个手指，需要重新选择主副指针
                    when {
                        releasedPointerId == primaryPointerId -> {
                            val newPrimary = findAlternativePointer(secondaryPointerId, actionIndex, event)
                            if (newPrimary >= 0) {
                                primaryPointerId = event.getPointerId(newPrimary)
                                isFirstPointer = true
                                updateGestureData(event)
                                return
                            }
                        }
                        releasedPointerId == secondaryPointerId -> {
                            val newSecondary = findAlternativePointer(primaryPointerId, actionIndex, event)
                            if (newSecondary >= 0) {
                                secondaryPointerId = event.getPointerId(newSecondary)
                                isFirstPointer = false
                                updateGestureData(event)
                                return
                            }
                        }
                    }
                }

                // 抬起的是其中一个有效指针，转换为单点模式
                val remainingPointerId = if (releasedPointerId == primaryPointerId) {
                    secondaryPointerId
                } else {
                    primaryPointerId
                }

                val remainingIndex = event.findPointerIndex(remainingPointerId)
                if (remainingIndex >= 0) {
                    centerX = event.getX(remainingIndex)
                    centerY = event.getY(remainingIndex)
                }

                reset()
                primaryPointerId = remainingPointerId
                isFirstPointer = true
            }

            MotionEvent.ACTION_MOVE -> {
                // 移动事件，更新手势数据
                updateGestureData(event)

                // 只有当压力比大于阈值时才触发手势更新（避免误触）
                if (previousEvent != null && currentPressure / previousPressure > 0.67f) {
                    listener?.onGestureUpdate(this)
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // 第三个或更多手指按下，重新初始化手势
                recycleEvents()
                previousEvent = MotionEvent.obtain(event)
                val actionIndex = event.actionIndex
                val newPointerId = event.getPointerId(actionIndex)

                if (!isFirstPointer) {
                    primaryPointerId = secondaryPointerId
                }

                secondaryPointerId = newPointerId
                isFirstPointer = false

                val primaryIndex = event.findPointerIndex(primaryPointerId)
                if (primaryIndex < 0 || primaryPointerId == secondaryPointerId) {
                    primaryPointerId = event.getPointerId(
                        findAlternativePointer(secondaryPointerId, -1, event)
                    )
                }

                updateGestureData(event)
            }
        }
    }

    /**
     * 更新手势数据
     */
    private fun updateGestureData(event: MotionEvent) {
        recycleCurrentEvent()
        currentEvent = MotionEvent.obtain(event)

        // 重置缓存
        currentDistance = -1f
        scaleFactor = -1f

        // 保存上一次的向量
        previousVector.set(currentVector)

        if (previousEvent == null) {
            previousEvent = MotionEvent.obtain(event)
        }

        val previousEvent = this.previousEvent ?: return
        val primaryIndex = previousEvent.findPointerIndex(primaryPointerId)
        val secondaryIndex = previousEvent.findPointerIndex(secondaryPointerId)
        val currentPrimaryIndex = event.findPointerIndex(primaryPointerId)
        val currentSecondaryIndex = event.findPointerIndex(secondaryPointerId)

        if (primaryIndex >= 0 && secondaryIndex >= 0 &&
            currentPrimaryIndex >= 0 && currentSecondaryIndex >= 0
        ) {

            // 上一次的位置
            val prevPrimaryX = previousEvent.getX(primaryIndex)
            val prevPrimaryY = previousEvent.getY(primaryIndex)
            val prevSecondaryX = previousEvent.getX(secondaryIndex)
            val prevSecondaryY = previousEvent.getY(secondaryIndex)

            // 当前的位置
            val currentPrimaryX = event.getX(currentPrimaryIndex)
            val currentPrimaryY = event.getY(currentPrimaryIndex)
            val currentSecondaryX = event.getX(currentSecondaryIndex)
            val currentSecondaryY = event.getY(currentSecondaryIndex)

            // 上一次的向量（从主指针指向副指针）
            val prevVecX = prevSecondaryX - prevPrimaryX
            val prevVecY = prevSecondaryY - prevPrimaryY
            previousVector.set(prevVecX, prevVecY)

            // 当前的向量
            val currentVecX = currentSecondaryX - currentPrimaryX
            val currentVecY = currentSecondaryY - currentPrimaryY
            currentVector.set(currentVecX, currentVecY)

            // 中心点
            previousCenterX = (prevVecX * 0.5f) + prevPrimaryX
            previousCenterY = (prevVecY * 0.5f) + prevPrimaryY
            centerX = (currentVecX * 0.5f) + currentPrimaryX
            centerY = (currentVecY * 0.5f) + currentPrimaryY

            // 压力值
            previousPressure = previousEvent.getPressure(primaryIndex) +
                    previousEvent.getPressure(secondaryIndex)
            currentPressure = event.getPressure(currentPrimaryIndex) +
                    event.getPressure(currentSecondaryIndex)
        } else {
            invalidState = true
            if (isInGesture) {
                listener?.onGestureUpdate(this)
            }
        }

        // 更新previousEvent为当前event，用于下次计算
        this.previousEvent?.recycle()
        this.previousEvent = MotionEvent.obtain(event)
    }

    /**
     * 查找替代指针（排除指定的指针）
     */
    private fun findAlternativePointer(
        excludePointerId: Int,
        excludeIndex: Int,
        event: MotionEvent
    ): Int {
        val pointerCount = event.pointerCount
        val excludePointerIndex = event.findPointerIndex(excludePointerId)

        for (i in 0 until pointerCount) {
            if (i != excludeIndex && i != excludePointerIndex) {
                return i
            }
        }
        return -1
    }

    /**
     * 回收事件对象
     */
    private fun recycleEvents() {
        recycleCurrentEvent()
        previousEvent?.recycle()
        previousEvent = null
    }

    /**
     * 回收当前事件对象
     */
    private fun recycleCurrentEvent() {
        currentEvent?.recycle()
        currentEvent = null
    }

    /**
     * 获取初始距离（两点之间的距离）
     * 注意：需要在首次两点触摸时调用，之后距离会被缓存
     */
    fun getInitialDistance(): Float {
        if (initialDistance == -1f) {
            // 如果没有缓存，使用previousVector计算
            if (previousVector.x != 0f || previousVector.y != 0f) {
                initialDistance = sqrt(
                    previousVector.x * previousVector.x +
                            previousVector.y * previousVector.y
                )
            } else {
                initialDistance = getCurrentDistance()
            }
        }
        return initialDistance
    }

    /**
     * 获取当前距离（两点之间的距离）
     */
    fun getCurrentDistance(): Float {
        if (currentDistance == -1f) {
            val dx = currentVector.x
            val dy = currentVector.y
            currentDistance = sqrt(dx * dx + dy * dy)
        }
        return currentDistance
    }

    /**
     * 获取上一次距离（两点之间的距离）
     */
    fun getPreviousDistance(): Float {
        if (previousDistance == -1f) {
            val dx = previousVector.x
            val dy = previousVector.y
            previousDistance = sqrt(dx * dx + dy * dy)
        }
        return previousDistance
    }

    /**
     * 获取缩放比例（当前距离 / 上一次距离）
     * 用于增量缩放计算
     */
    fun getScaleFactor(): Float {
        if (scaleFactor == -1f) {
            val prevDist = getPreviousDistance()
            val currDist = getCurrentDistance()
            scaleFactor = if (prevDist > 0) {
                currDist / prevDist
            } else {
                1.0f
            }
        }
        return scaleFactor
    }

    /**
     * 获取总缩放比例（当前距离 / 初始距离）
     */
    fun getTotalScaleFactor(): Float {
        val initDist = getInitialDistance()
        val currDist = getCurrentDistance()
        return if (initDist > 0) {
            currDist / initDist
        } else {
            1.0f
        }
    }

    /**
     * 获取旋转角度（度），相对于上一次的旋转
     */
    fun getRotationAngle(): Float {
        if (previousVector.x == 0f && previousVector.y == 0f) {
            return 0f
        }

        // 归一化向量
        val prevLen = sqrt(
            previousVector.x * previousVector.x +
                    previousVector.y * previousVector.y
        )
        val currLen = sqrt(
            currentVector.x * currentVector.x +
                    currentVector.y * currentVector.y
        )

        if (prevLen == 0f || currLen == 0f) {
            return 0f
        }

        val prevNormX = previousVector.x / prevLen
        val prevNormY = previousVector.y / prevLen
        val currNormX = currentVector.x / currLen
        val currNormY = currentVector.y / currLen

        // 计算角度差（弧度转角度）
        val angle = atan2(currNormY, currNormX) - atan2(prevNormY, prevNormX)
        return Math.toDegrees(angle.toDouble()).toFloat()
    }

    /**
     * 释放资源
     */
    fun release() {
        recycleEvents()
        reset()
    }
}
