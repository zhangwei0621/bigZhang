package com.study.googletranslatedemo.utils.gesture;

import android.view.MotionEvent;
import android.view.View;

/**
 * 拖拽手势触摸监听器
 * 功能：
 * 1. 单指拖拽：平移View（考虑View的矩阵变换）
 * 2. 双指手势：通过手势检测器处理缩放、旋转等（可选）
 * 3. 自动处理指针切换（当有手指抬起时）
 * 所有逻辑都封装在此类中，不依赖其他文件
 */
public class DragGestureTouchListener implements View.OnTouchListener {

    // ========== 手势检测器（可选） ==========
    private GeneralGestureDetector gestureDetector;
    private boolean enableMultiTouchGesture = false;  // 是否启用双指手势

    // ========== 单指拖拽状态 ==========
    private float dragStartX;                    // 拖拽起始X坐标
    private float dragStartY;                    // 拖拽起始Y坐标
    private int dragPointerId = -1;              // 拖拽指针ID

    // ========== 配置参数 ==========
    private boolean enableDrag = true;           // 是否启用拖拽

    /**
     * 构造函数（仅单指拖拽）
     */
    public DragGestureTouchListener() {
        this(null);
    }

    /**
     * 构造函数（支持双指手势）
     * @param gestureDetector 手势检测器，如果为null则只支持单指拖拽
     */
    public DragGestureTouchListener(GeneralGestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
        this.enableMultiTouchGesture = (gestureDetector != null);
    }

    /**
     * 设置是否启用拖拽
     */
    public void setEnableDrag(boolean enableDrag) {
        this.enableDrag = enableDrag;
    }

    /**
     * 设置手势检测器
     */
    public void setGestureDetector(GeneralGestureDetector detector) {
        this.gestureDetector = detector;
        this.enableMultiTouchGesture = (detector != null);
    }

    /**
     * 获取手势检测器
     */
    public GeneralGestureDetector getGestureDetector() {
        return gestureDetector;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getAction();
        int actionMasked = event.getActionMasked();

        // 如果启用了双指手势，先处理手势检测器
        if (enableMultiTouchGesture && gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }

        // 处理单指拖拽
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                // 开始拖拽
                dragStartX = event.getX();
                dragStartY = event.getY();
                dragPointerId = event.getPointerId(0);
                return true;

            case MotionEvent.ACTION_MOVE:
                // 如果不在双指手势中，处理单指拖拽
                if (enableDrag && (!enableMultiTouchGesture || gestureDetector == null || !gestureDetector.isInGesture())) {
                    int pointerIndex = event.findPointerIndex(dragPointerId);
                    if (pointerIndex != -1) {
                        float currentX = event.getX(pointerIndex);
                        float currentY = event.getY(pointerIndex);
                        float dx = currentX - dragStartX;
                        float dy = currentY - dragStartY;
                        applyTranslation(view, dx, dy);
                        dragStartX = currentX;
                        dragStartY = currentY;
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 结束拖拽
                dragPointerId = -1;
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                // 有手指抬起，如果是拖拽指针抬起，切换到另一个指针
                int actionIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) 
                                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int releasedPointerId = event.getPointerId(actionIndex);

                if (releasedPointerId == dragPointerId) {
                    // 找到剩余的手指
                    int newActionIndex = (actionIndex == 0) ? 1 : 0;
                    if (newActionIndex < event.getPointerCount()) {
                        dragStartX = event.getX(newActionIndex);
                        dragStartY = event.getY(newActionIndex);
                        dragPointerId = event.getPointerId(newActionIndex);
                    } else {
                        dragPointerId = -1;
                    }
                }
                return true;
        }

        return true;
    }

    /**
     * 应用平移变换（考虑View的矩阵变换）
     * 这是核心方法，确保平移方向正确
     */
    private void applyTranslation(View view, float dx, float dy) {
        // 将平移向量通过View的矩阵变换，确保平移方向正确
        float[] delta = {dx, dy};
        view.getMatrix().mapVectors(delta);
        view.setTranslationX(view.getTranslationX() + delta[0]);
        view.setTranslationY(view.getTranslationY() + delta[1]);
    }

    /**
     * 重置状态
     */
    public void reset() {
        dragPointerId = -1;
        if (gestureDetector != null) {
            gestureDetector.reset();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (gestureDetector != null) {
            gestureDetector.release();
        }
    }
}
