package com.study.googletranslatedemo.utils.gesture;

import android.graphics.PointF;

/**
 * 手势状态类
 * 存储手势检测过程中的状态信息，包括中心点、方向向量等
 */
public class GestureState {

    // 手势中心点X坐标
    private float centerX;
    
    // 手势中心点Y坐标
    private float centerY;
    
    // 初始方向向量（用于计算旋转）
    private final PointF initialDirection;
    
    // 回调对象（可以是DragPinchRotateTouchListener或其他）
    private Object callback;

    /**
     * 构造函数 - 用于DragPinchRotateTouchListener
     */
    public GestureState(DragPinchRotateTouchListener listener) {
        this.callback = listener;
        this.initialDirection = new PointF(0.0f, 0.0f);
    }

    /**
     * 手势开始时调用，保存初始状态
     */
    public void onGestureStart(PinchRotateGestureDetector detector) {
        this.centerX = detector.getCenterX();
        this.centerY = detector.getCenterY();
        PointF currentDirection = detector.getCurrentDirection();
        this.initialDirection.set(currentDirection.x, currentDirection.y);
    }

    /**
     * 更新手势状态
     */
    public void updateState(PinchRotateGestureDetector detector) {
        this.centerX = detector.getCenterX();
        this.centerY = detector.getCenterY();
        PointF currentDirection = detector.getCurrentDirection();
        this.initialDirection.set(currentDirection.x, currentDirection.y);
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public PointF getInitialDirection() {
        return initialDirection;
    }
}
