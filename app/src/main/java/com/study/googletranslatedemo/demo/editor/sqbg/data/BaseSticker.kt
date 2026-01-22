package com.study.googletranslatedemo.demo.editor.sqbg.data

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * 根据竞品提取的另外一种贴纸基类，功能包括变换矩阵、碰撞检测、绘制等
 */
abstract class BaseSticker {
    /** 四个角点的本地坐标（未变换前）  */
    private val localCorners = FloatArray(8)

    /** 四个角点的变换后坐标  */
    private val transformedCorners = FloatArray(8)

    /** 矩阵值缓存  */
    private val matrixValues = FloatArray(9)

    /** 边界矩形  */
    private val boundsRect = RectF()

    /** 点坐标缓存  */
    private val pointCache = FloatArray(2)

    /** 旋转后的角点坐标  */
    private val rotatedCorners = FloatArray(8)

    /** 是否可见  */
    var isVisible: Boolean = true

    /** 是否镜像显示  */
    var isMirrored: Boolean = false

    /** 变换矩阵  */
    val transformMatrix: Matrix = Matrix()

    /**
     * 检测点是否在贴纸区域内
     * @param point 屏幕坐标点 [x, y]
     * @return true 如果点在贴纸区域内
     */
    fun containsPoint(point: FloatArray?): Boolean {
        // 创建旋转矩阵，用于将点转换到贴纸的本地坐标系
        val rotationMatrix = Matrix()
        val currentMatrix = transformMatrix
        val values = this.matrixValues

        // 获取当前变换矩阵的值
        currentMatrix.getValues(values)
        currentMatrix.getValues(values)

        // 计算当前旋转角度并创建反向旋转矩阵
        val angle = atan2(values[1].toDouble(), values[0].toDouble())
        rotationMatrix.setRotate(-Math.toDegrees(-angle).toFloat())

        // 获取贴纸的四个角点（本地坐标）
        val localCorners = this.localCorners
        getLocalCorners(localCorners)

        // 将角点转换到当前变换后的坐标系
        val transformedCorners = this.transformedCorners
        currentMatrix.mapPoints(transformedCorners, localCorners)

        // 将角点转换到旋转后的坐标系（用于边界计算）
        val rotatedCorners = this.rotatedCorners
        rotationMatrix.mapPoints(rotatedCorners, transformedCorners)

        // 将检测点也转换到旋转后的坐标系
        val pointCache = this.pointCache
        rotationMatrix.mapPoints(pointCache, point)

        // 计算边界矩形
        val bounds = this.boundsRect
        bounds.set(
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        )

        // 遍历所有角点，计算边界
        var i = 1
        while (i < rotatedCorners.size) {
            val x = (rotatedCorners[i - 1] * 10.0f).roundToInt() / 10.0f
            val y = (rotatedCorners[i] * 10.0f).roundToInt() / 10.0f

            if (x < bounds.left) {
                bounds.left = x
            }
            if (y < bounds.top) {
                bounds.top = y
            }
            if (x > bounds.right) {
                bounds.right = x
            }
            if (y > bounds.bottom) {
                bounds.bottom = y
            }
            i += 2
        }

        bounds.sort()

        // 检查点是否在边界矩形内
        return bounds.contains(pointCache[0], pointCache[1])
    }

    /**
     * 在画布上绘制贴纸
     * @param canvas 画布
     */
    abstract fun draw(canvas: Canvas)

    /**
     * 获取贴纸类型
     * @return 类型标识
     */
    abstract fun getType(): Int

    /**
     * 获取贴纸的四个角点（本地坐标）
     * @param corners 输出数组，长度为8 [x0,y0, x1,y1, x2,y2, x3,y3]
     */
    private fun getLocalCorners(corners: FloatArray) {
        if (!this.isMirrored) {
            // 正常方向：左上、右上、左下、右下
            corners[0] = 0.0f
            corners[1] = 0.0f
            corners[2] = getWidth().toFloat()
            corners[3] = 0.0f
            corners[4] = 0.0f
            corners[5] = getHeight().toFloat()
            corners[6] = getWidth().toFloat()
        } else {
            // 镜像方向
            corners[0] = getWidth().toFloat()
            corners[1] = 0.0f
            corners[2] = 0.0f
            corners[3] = 0.0f
            corners[4] = getWidth().toFloat()
            corners[5] = getHeight().toFloat()
            corners[6] = 0.0f
        }
        corners[7] = getHeight().toFloat()
    }

    /**
     * 获取贴纸中心点（屏幕坐标）
     * @param center 输出点
     * @param localPoint 本地坐标点缓存
     * @param screenPoint 屏幕坐标点缓存
     */
    fun getCenterPoint(center: PointF, localPoint: FloatArray, screenPoint: FloatArray) {
        // 计算本地中心点
        center.set(getWidth() / 2.0f, getHeight() / 2.0f)

        // 转换为屏幕坐标
        screenPoint[0] = center.x
        screenPoint[1] = center.y
        transformMatrix.mapPoints(localPoint, screenPoint)
        center.set(localPoint[0], localPoint[1])
    }

    /**
     * 获取贴纸高度
     * @return 高度（像素）
     */
    abstract fun getHeight(): Int

    /**
     * 获取贴纸宽度
     * @return 宽度（像素）
     */
    abstract fun getWidth(): Int

    /**
     * 释放资源
     */
    abstract fun release()

    /**
     * 克隆贴纸
     * @param type 类型参数（可能用于特定类型的克隆）
     * @return 新的贴纸实例
     */
    abstract fun clone(type: Int): BaseSticker?

    /**
     * 设置变换矩阵
     * @param matrix 新的变换矩阵
     */
    fun setTransformMatrix(matrix: Matrix?) {
        transformMatrix.set(matrix)
    }
}