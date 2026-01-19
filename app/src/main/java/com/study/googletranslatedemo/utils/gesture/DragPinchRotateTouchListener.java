package com.study.googletranslatedemo.utils.gesture;

import android.app.Activity;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/**
 * 实现View的拖拽移动、双指缩放、双指旋转功能的触摸监听器
 * 
 * 功能说明：
 * 1. 单指拖拽：实现View的平移移动
 * 2. 双指缩放：通过双指手势实现View的缩放
 * 3. 双指旋转：通过双指手势实现View的旋转
 */
public class DragPinchRotateTouchListener implements View.OnTouchListener {

    // 当前活动的指针ID（用于跟踪单指拖拽）
    private int activePointerId = -1;
    
    // 初始触摸位置（用于计算拖拽距离）
    private float initialTouchX;
    private float initialTouchY;
    
    // Activity引用（用于特殊处理NeonLayout）
    private final Activity activity;
    
    // 双指手势检测器（处理缩放和旋转）
    private final PinchRotateGestureDetector gestureDetector;

    public DragPinchRotateTouchListener(Activity activity) {
        this.activity = activity;
        this.gestureDetector = new PinchRotateGestureDetector(new GestureState(this));
    }

    /**
     * 将View按照指定的增量进行移动
     * 使用矩阵变换来确保移动方向正确
     * 
     * @param view 要移动的View
     * @param deltaX X方向的移动增量
     * @param deltaY Y方向的移动增量
     */
    public static void translateView(View view, float deltaX, float deltaY) {
        float[] vector = {deltaX, deltaY};
        // 应用当前矩阵变换，确保移动方向正确
        view.getMatrix().mapVectors(vector);
        view.setTranslationX(view.getTranslationX() + vector[0]);
        view.setTranslationY(view.getTranslationY() + vector[1]);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
//        // 检查是否是NeonLayout的特殊处理
//        boolean isNeonLayout = activity instanceof NeonLayout;
//
//        // 如果是NeonLayout，需要特殊处理ImageView
//        if (isNeonLayout) {
//            ImageView imageView = NeonLayout.staticImageViewM3_level4;
//            // 先让手势检测器处理事件（用于检测双指手势）
//            gestureDetector.onTouchEvent(imageView, motionEvent);
//
//            // 处理单指拖拽逻辑
//            handleSingleTouchDrag(imageView, motionEvent);
//        }
        
        // 对所有View都进行手势检测
        gestureDetector.onTouchEvent(view, motionEvent);
        
        // 处理单指拖拽逻辑
        handleSingleTouchDrag(view, motionEvent);
        
        return true;
    }

    /**
     * 处理单指拖拽移动
     */
    private void handleSingleTouchDrag(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        int actionMasked = motionEvent.getActionMasked() & action;
        
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                // 手指按下，记录初始位置和指针ID
                initialTouchX = motionEvent.getX();
                initialTouchY = motionEvent.getY();
                // 创建一个Rect用于边界检查（虽然这里没有使用，但保留原逻辑）
                new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                activePointerId = motionEvent.getPointerId(0);
                break;
                
            case MotionEvent.ACTION_UP:
                // 手指抬起，重置活动指针ID
                activePointerId = -1;
                break;
                
            case MotionEvent.ACTION_MOVE:
                // 手指移动，计算移动距离并更新View位置
                int pointerIndex = motionEvent.findPointerIndex(activePointerId);
                if (pointerIndex != -1) {
                    float currentX = motionEvent.getX(pointerIndex);
                    float currentY = motionEvent.getY(pointerIndex);
                    
                    // 只有在没有进行双指手势时才允许单指拖拽
                    if (!gestureDetector.isInPinchRotateMode()) {
                        float deltaX = currentX - initialTouchX;
                        float deltaY = currentY - initialTouchY;
                        translateView(view, deltaX, deltaY);
                    }
                }
                break;
                
            case MotionEvent.ACTION_POINTER_UP:
                // 某个手指抬起（多指场景）
                int pointerUpIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                if (motionEvent.getPointerId(pointerUpIndex) == activePointerId) {
                    // 如果抬起的是当前跟踪的手指，切换到另一个手指
                    int newPointerIndex;
                    if (pointerUpIndex == 0) {
                        newPointerIndex = 1;
                    } else {
                        newPointerIndex = 0;
                    }
                    
                    if (newPointerIndex < motionEvent.getPointerCount()) {
                        initialTouchX = motionEvent.getX(newPointerIndex);
                        initialTouchY = motionEvent.getY(newPointerIndex);
                        activePointerId = motionEvent.getPointerId(newPointerIndex);
                    }
                }
                break;
                
            case MotionEvent.ACTION_CANCEL:
                // 手势取消，重置活动指针ID
                activePointerId = -1;
                break;
        }
    }
}
