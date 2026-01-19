package com.study.googletranslatedemo.utils.gesture

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 支持缩放、旋转、平移的View触摸监听器（Kotlin版本）
 *
 * 功能：
 * 1. 单指拖拽：平移View
 * 2. 双指手势：缩放、旋转、平移中心点
 * 3. 自动处理pivot point的动态调整
 * 4. 限制缩放和旋转范围
 * 5. 支持点击事件（单指点击）
 *
 * 所有逻辑都封装在此类中，不依赖其他文件
 */
class GeneralGestureTouchListener : View.OnTouchListener {

    // ========== 手势检测器 ==========
    private val gestureDetector: GeneralGestureDetector =
        GeneralGestureDetector {
            // 这里可以添加额外的处理逻辑，如果需要的话
            // 当前实现直接在onTouch中处理，所以这里可以留空
        }

    // ========== 单指拖拽状态 ==========
    private var isSingleTapPossible = true  // 是否可能是单次点击
    private var dragStartX = 0f              // 拖拽起始X坐标
    private var dragStartY = 0f              // 拖拽起始Y坐标
    private var dragPointerId = -1           // 拖拽指针ID

    // ========== 变换状态（用于保存上一次的中心点） ==========
    private var savedCenterX = 0f            // 保存的中心点X坐标
    private var savedCenterY = 0f            // 保存的中心点Y坐标
    private val savedVector = PointF()       // 保存的向量（用于计算旋转）

    // ========== 配置参数 ==========
    var minScale = 0.5f                      // 最小缩放比例
    var maxScale = 10.0f                     // 最大缩放比例
    var enableClick = true                   // 是否启用点击事件

    // ========== 旋转角度限制 ==========
    companion object {
        private const val MIN_ROTATION = -180.0f
        private const val MAX_ROTATION = 180.0f
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val action = event.action
        val actionMasked = event.actionMasked

        // 先处理手势检测器（必须在处理单指操作之前调用）
        gestureDetector.onTouchEvent(event)

        // 处理单指操作
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 重置单指拖拽状态
                isSingleTapPossible = true
                dragStartX = event.x
                dragStartY = event.y
                dragPointerId = event.getPointerId(0)

                // 保存初始中心点（用于双指手势）
                if (gestureDetector.isInGesture) {
                    savedCenterX = gestureDetector.centerX
                    savedCenterY = gestureDetector.centerY
                    val vec = gestureDetector.previousVector
                    savedVector.set(vec)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // 处理双指手势
                if (gestureDetector.isInGesture) {
                    handleMultiTouchGesture(view, event)
                } else {
                    // 处理单指拖拽
                    val pointerIndex = event.findPointerIndex(dragPointerId)
                    if (pointerIndex != -1) {
                        isSingleTapPossible = false  // 移动了，不是点击
                        val currentX = event.getX(pointerIndex)
                        val currentY = event.getY(pointerIndex)
                        val dx = currentX - dragStartX
                        val dy = currentY - dragStartY
                        applyTranslation(view, dx, dy)
                        dragStartX = currentX
                        dragStartY = currentY
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                // 处理点击事件
                if (isSingleTapPossible && enableClick && !gestureDetector.isInGesture) {
                    view.performClick()
                }
                dragPointerId = -1
                reset()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                dragPointerId = -1
                reset()
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // 有手指抬起，如果是拖拽指针抬起，切换到另一个指针
                val actionIndex = (action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr
                        MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val releasedPointerId = event.getPointerId(actionIndex)

                if (releasedPointerId == dragPointerId) {
                    // 找到剩余的手指
                    val newActionIndex = if (actionIndex == 0) 1 else 0
                    if (newActionIndex < event.pointerCount) {
                        dragStartX = event.getX(newActionIndex)
                        dragStartY = event.getY(newActionIndex)
                        dragPointerId = event.getPointerId(newActionIndex)
                    } else {
                        dragPointerId = -1
                    }
                }
                return true
            }
        }

        return true
    }

    /**
     * 处理多点触摸手势（双指缩放、旋转、平移）
     */
    private fun handleMultiTouchGesture(view: View, event: MotionEvent) {
        if (!gestureDetector.isInGesture) {
            return
        }

        // 获取当前手势数据
        val centerX = gestureDetector.centerX
        val centerY = gestureDetector.centerY
        val prevCenterX = gestureDetector.previousCenterX
        val prevCenterY = gestureDetector.previousCenterY
        val currentVector = gestureDetector.currentVector
        val previousVector = gestureDetector.previousVector

        // 计算初始距离和上一次距离（用于缩放计算）
        var initialDistance = -1f
        var previousDistance = -1f

        if (previousVector.x != 0f || previousVector.y != 0f) {
            // 使用保存的上一次向量计算初始距离
            if (savedVector.x == 0f && savedVector.y == 0f) {
                // 首次，保存当前向量作为初始
                savedVector.set(previousVector)
                initialDistance = sqrt(
                    previousVector.x * previousVector.x + previousVector.y * previousVector.y
                )
            } else {
                initialDistance = sqrt(
                    savedVector.x * savedVector.x + savedVector.y * savedVector.y
                )
            }
            previousDistance = sqrt(
                previousVector.x * previousVector.x + previousVector.y * previousVector.y
            )
        }

        // 计算当前距离
        val currentDistance = sqrt(
            currentVector.x * currentVector.x + currentVector.y * currentVector.y
        )

        // 计算缩放比例
        val scaleFactor = when {
            previousDistance > 0 -> currentDistance / previousDistance
            initialDistance > 0 -> currentDistance / initialDistance
            else -> 1.0f
        }

        // 计算旋转角度
        var rotationAngle = 0f
        if (previousVector.x != 0f || previousVector.y != 0f) {
            // 归一化向量
            val prevLen = if (previousDistance > 0) {
                previousDistance
            } else {
                sqrt(previousVector.x * previousVector.x + previousVector.y * previousVector.y)
            }
            val currLen = currentDistance

            if (prevLen > 0 && currLen > 0) {
                val prevNormX = previousVector.x / prevLen
                val prevNormY = previousVector.y / prevLen
                val currNormX = currentVector.x / currLen
                val currNormY = currentVector.y / currLen

                // 计算角度差（弧度转角度）
                val angle = atan2(currNormY, currNormX) -
                        atan2(prevNormY, prevNormX)
                rotationAngle = Math.toDegrees(angle.toDouble()).toFloat()
            }
        }

        // 应用变换
        applyTransformToView(
            view, centerX, centerY, prevCenterX, prevCenterY,
            scaleFactor, rotationAngle
        )
    }

    /**
     * 应用变换到View
     */
    private fun applyTransformToView(
        view: View,
        centerX: Float,
        centerY: Float,
        prevCenterX: Float,
        prevCenterY: Float,
        scaleFactor: Float,
        rotationAngle: Float
    ) {
        // 动态调整pivot point
        val currentPivotX = view.pivotX
        val currentPivotY = view.pivotY

        if (currentPivotX != centerX || currentPivotY != centerY) {
            // 保存当前(0,0)点在屏幕坐标系中的位置
            val pointBefore = floatArrayOf(0f, 0f)
            view.matrix.mapPoints(pointBefore)

            // 设置新的pivot point
            view.pivotX = centerX
            view.pivotY = centerY

            // 计算新pivot point后(0,0)点的位置
            val pointAfter = floatArrayOf(0f, 0f)
            view.matrix.mapPoints(pointAfter)

            // 补偿平移，保持View位置不变
            val deltaX = pointAfter[0] - pointBefore[0]
            val deltaY = pointAfter[1] - pointBefore[1]
            view.translationX -= deltaX
            view.translationY -= deltaY
        }

        // 应用中心点平移
        val offsetX = centerX - prevCenterX
        val offsetY = centerY - prevCenterY
        applyTranslation(view, offsetX, offsetY)

        // 应用缩放（限制范围）
        val currentScale = view.scaleX
        val newScale = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
        view.scaleX = newScale
        view.scaleY = newScale

        // 应用旋转（限制在-180到180度）
        var newRotation = view.rotation + rotationAngle
        when {
            newRotation > MAX_ROTATION -> newRotation -= 360f
            newRotation < MIN_ROTATION -> newRotation += 360f
        }
        view.rotation = newRotation
    }

    /**
     * 应用平移变换（考虑View的矩阵变换）
     */
    private fun applyTranslation(view: View, dx: Float, dy: Float) {
        // 将平移向量通过View的矩阵变换，确保平移方向正确
        val delta = floatArrayOf(dx, dy)
        view.matrix.mapVectors(delta)
        view.translationX += delta[0]
        view.translationY += delta[1]
    }

    /**
     * 获取手势检测器（用于外部调用）
     */
    fun getGestureDetector(): GeneralGestureDetector {
        return gestureDetector
    }

    /**
     * 重置状态
     */
    private fun reset() {
        savedCenterX = 0f
        savedCenterY = 0f
        savedVector.set(0f, 0f)
        gestureDetector.reset()
    }

    /**
     * 重置所有状态（供外部调用）
     */
    fun resetState() {
        reset()
    }

    /**
     * 释放资源
     */
    fun release() {
        gestureDetector.release()
    }
}
