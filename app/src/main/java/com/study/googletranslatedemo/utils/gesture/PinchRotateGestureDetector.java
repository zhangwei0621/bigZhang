package com.study.googletranslatedemo.utils.gesture;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

/**
 * 双指手势检测器
 * 负责检测和处理双指缩放和旋转手势
 */
public class PinchRotateGestureDetector {

    // 是否正在处理手势
    private boolean isProcessing = false;
    
    // 第一个指针的ID
    private int firstPointerId = -1;
    
    // 第二个指针的ID
    private int secondPointerId = -1;
    
    // 上一次的MotionEvent（用于计算增量）
    private MotionEvent previousEvent;
    
    // 当前手势开始时的MotionEvent
    private MotionEvent initialEvent;
    
    // 当前两个指针之间的向量（用于计算旋转）
    private float currentVectorX;
    private float currentVectorY;
    
    // 初始两个指针之间的向量
    private float initialVectorX;
    private float initialVectorY;
    
    // 当前中心点坐标
    private float centerX;
    private float centerY;
    
    // 初始距离（用于计算缩放比例）
    private float initialDistance = -1.0f;
    
    // 上一次的距离
    private float previousDistance = -1.0f;
    
    // 当前缩放比例
    private float currentScaleRatio = -1.0f;
    
    // 当前方向向量（归一化后的）
    private final PointF currentDirection = new PointF(0.0f, 0.0f);
    
    // 手势状态回调
    private final GestureState gestureState;
    
    // 是否处于双指手势模式
    private boolean isInPinchRotateMode = false;
    
    // 是否发生错误（指针索引无效）
    private boolean hasError = false;
    
    // 是否交换了指针顺序
    private boolean pointerSwapped = false;

    public PinchRotateGestureDetector(GestureState gestureState) {
        this.gestureState = gestureState;
    }

    /**
     * 查找除了指定指针外的另一个指针索引
     * 
     * @param excludePointerId 要排除的指针ID
     * @param excludeIndex 要排除的索引
     * @param motionEvent 触摸事件
     * @return 找到的指针索引，如果没找到返回-1
     */
    public static int findOtherPointerIndex(int excludePointerId, int excludeIndex, MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        int excludePointerIndex = motionEvent.findPointerIndex(excludePointerId);
        
        for (int i = 0; i < pointerCount; i++) {
            if (i != excludeIndex && i != excludePointerIndex) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 处理触摸事件
     */
    public void onTouchEvent(View view, MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        
        // 如果是ACTION_DOWN，重置所有状态
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            reset();
        }
        
        if (!hasError) {
            if (isInPinchRotateMode) {
                // 正在处理双指手势
                handlePinchRotateGesture(view, motionEvent, actionMasked);
            } else {
                // 等待双指手势开始
                waitForPinchRotateStart(view, motionEvent, actionMasked);
            }
        }
    }

    /**
     * 等待双指手势开始
     */
    private void waitForPinchRotateStart(View view, MotionEvent motionEvent, int actionMasked) {
        switch (actionMasked) {
            case MotionEvent.ACTION_POINTER_DOWN:
                // 第二个手指按下，开始双指手势
                handlePointerDown(view, motionEvent);
                break;
                
            case MotionEvent.ACTION_DOWN:
                // 第一个手指按下，记录指针ID
                firstPointerId = motionEvent.getPointerId(0);
                pointerSwapped = true;
                break;
                
            case MotionEvent.ACTION_UP:
                // 手指抬起，重置状态
                reset();
                break;
        }
    }

    /**
     * 处理双指手势
     */
    private void handlePinchRotateGesture(View view, MotionEvent motionEvent, int actionMasked) {
        switch (actionMasked) {
            case MotionEvent.ACTION_UP:
                // 手指抬起，结束手势
                reset();
                break;
                
            case MotionEvent.ACTION_MOVE:
                // 手指移动，更新手势
                updateGesture(view, motionEvent);
                break;
                
            case MotionEvent.ACTION_CANCEL:
                // 手势取消
                gestureState.getClass(); // 原代码中的调用，可能是为了触发某些逻辑
                reset();
                break;
                
            case MotionEvent.ACTION_POINTER_DOWN:
                // 第三个手指按下（多指场景）
                handleAdditionalPointerDown(view, motionEvent);
                break;
                
            case MotionEvent.ACTION_POINTER_UP:
                // 某个手指抬起
                handlePointerUp(view, motionEvent);
                break;
        }
    }

    /**
     * 处理第二个手指按下
     */
    private void handlePointerDown(View view, MotionEvent motionEvent) {
        // 保存之前的状态
        int oldFirstPointerId = firstPointerId;
        int oldSecondPointerId = secondPointerId;
        
        // 重置状态（但保留指针ID信息）
        reset();
        
        // 保存初始事件（对应原代码中的 f8856o）
        initialEvent = MotionEvent.obtain(motionEvent);
        
        // 确定两个指针ID
        int actionIndex = motionEvent.getActionIndex();
        int newPointerId = motionEvent.getPointerId(actionIndex);
        
        // 根据原代码逻辑：如果 pointerSwapped 为 false，则交换指针ID
        // 原代码：if (!this.f8844a) { i5 = i6; }
        if (!pointerSwapped) {
            // 如果没有交换过，使用旧的第二个指针ID作为第一个
            firstPointerId = oldSecondPointerId;
        } else {
            // 如果交换过，使用旧的第一个指针ID
            firstPointerId = oldFirstPointerId;
        }
        
        secondPointerId = newPointerId;
        pointerSwapped = false;
        
        // 确保两个指针ID不同且有效
        int firstPointerIndex = motionEvent.findPointerIndex(firstPointerId);
        if (firstPointerIndex < 0 || firstPointerId == secondPointerId) {
            int otherIndex = findOtherPointerIndex(secondPointerId, -1, motionEvent);
            if (otherIndex >= 0) {
                firstPointerId = motionEvent.getPointerId(otherIndex);
            }
        }
        
        // 更新手势数据（对应原代码中的 d(motionEvent)）
        updateGestureData(motionEvent);
        
        // 通知手势开始（对应原代码中的 vVar.b(this)）
        gestureState.onGestureStart(this);
        isInPinchRotateMode = true;
    }

    /**
     * 处理第三个手指按下（多指场景）
     */
    private void handleAdditionalPointerDown(View view, MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        int actionIndex = motionEvent.getActionIndex();
        int pointerId = motionEvent.getPointerId(actionIndex);
        
        if (pointerCount > 2) {
            // 如果按下的是第一个指针，需要切换到另一个指针
            if (pointerId == firstPointerId) {
                int newIndex = findOtherPointerIndex(secondPointerId, actionIndex, motionEvent);
                if (newIndex >= 0) {
                    gestureState.getClass(); // 原代码调用，可能是为了触发某些逻辑
                    firstPointerId = motionEvent.getPointerId(newIndex);
                    pointerSwapped = true;
                    // 更新初始事件
                    if (initialEvent != null) {
                        initialEvent.recycle();
                    }
                    initialEvent = MotionEvent.obtain(motionEvent);
                    updateGestureData(motionEvent);
                    gestureState.onGestureStart(this);
                    isInPinchRotateMode = true;
                    // 再次更新事件（原代码中有两次）
                    initialEvent.recycle();
                    initialEvent = MotionEvent.obtain(motionEvent);
                    updateGestureData(motionEvent);
                }
            } else if (pointerId == secondPointerId) {
                // 如果按下的是第二个指针，需要切换到另一个指针
                int newIndex = findOtherPointerIndex(firstPointerId, actionIndex, motionEvent);
                if (newIndex >= 0) {
                    gestureState.getClass(); // 原代码调用
                    secondPointerId = motionEvent.getPointerId(newIndex);
                    pointerSwapped = false;
                    // 更新初始事件
                    if (initialEvent != null) {
                        initialEvent.recycle();
                    }
                    initialEvent = MotionEvent.obtain(motionEvent);
                    updateGestureData(motionEvent);
                    gestureState.onGestureStart(this);
                    isInPinchRotateMode = true;
                }
            }
            
            // 更新事件（原代码中在最后还会更新一次）
            if (initialEvent != null) {
                initialEvent.recycle();
            }
            initialEvent = MotionEvent.obtain(motionEvent);
            updateGestureData(motionEvent);
        }
        
        // 更新手势数据（原代码中在最后会调用一次 d(motionEvent)）
        updateGestureData(motionEvent);
        
        // 计算中心点（原代码中会计算剩余指针的位置）
        int firstIndex = motionEvent.findPointerIndex(firstPointerId);
        int secondIndex = motionEvent.findPointerIndex(secondPointerId);
        if (firstIndex >= 0 && secondIndex >= 0) {
            centerX = motionEvent.getX(secondIndex);
            centerY = motionEvent.getY(secondIndex);
        }
        
        // 重置状态（原代码中会调用 c()）
        gestureState.getClass(); // 原代码调用
        reset();
        
        // 重新设置指针（原代码中会保留剩余的一个指针）
        int remainingPointerId = (pointerId == firstPointerId) ? secondPointerId : firstPointerId;
        firstPointerId = remainingPointerId;
        pointerSwapped = true;
    }

    /**
     * 处理手指抬起（ACTION_POINTER_UP）
     */
    private void handlePointerUp(View view, MotionEvent motionEvent) {
        // 更新手势数据（原代码中会先调用 d(motionEvent)）
        updateGestureData(motionEvent);
        
        // 确定哪个指针抬起了，保留另一个指针
        int actionIndex = motionEvent.getActionIndex();
        int pointerId = motionEvent.getPointerId(actionIndex);
        int remainingPointerId;
        
        if (pointerId == firstPointerId) {
            remainingPointerId = secondPointerId;
        } else {
            remainingPointerId = firstPointerId;
        }
        
        // 计算剩余指针的位置（原代码中会保存到 f8852j/f8853k）
        int remainingIndex = motionEvent.findPointerIndex(remainingPointerId);
        if (remainingIndex >= 0) {
            centerX = motionEvent.getX(remainingIndex);
            centerY = motionEvent.getY(remainingIndex);
        }
        
        // 重置状态（原代码中会调用 c()）
        gestureState.getClass(); // 原代码调用
        reset();
        
        // 重新设置指针（原代码中会保留剩余的一个指针）
        firstPointerId = remainingPointerId;
        pointerSwapped = true;
    }

    /**
     * 更新手势（处理移动事件）
     */
    private void updateGesture(View view, MotionEvent motionEvent) {
        updateGestureData(motionEvent);
        
        // 计算压力值的比例，用于判断是否应该处理手势
        // 如果压力变化太大，可能是指针切换，不处理
        float currentPressure = getCurrentPressure();
        float initialPressure = getInitialPressure();
        if (initialPressure > 0 && currentPressure / initialPressure > 0.67f) {
            // 计算缩放比例
            if (currentScaleRatio == -1.0f) {
                // 计算当前距离（当前事件中两个指针之间的距离）
                if (initialDistance == -1.0f) {
                    initialDistance = (float) Math.sqrt(currentVectorX * currentVectorX + currentVectorY * currentVectorY);
                }
                
                // 计算初始距离（初始事件中两个指针之间的距离）
                if (previousDistance == -1.0f) {
                    previousDistance = (float) Math.sqrt(initialVectorX * initialVectorX + initialVectorY * initialVectorY);
                }
                
                // 缩放比例 = 当前距离 / 初始距离
                currentScaleRatio = initialDistance / previousDistance;
            }
            
            // 计算旋转角度
            // 获取初始方向（从gestureState中获取，在updateGestureData中设置）
            PointF initialDirection = gestureState.getInitialDirection();
            
            // 归一化初始方向向量（原代码中会修改gestureState中的向量）
            normalizeVector(initialDirection);
            
            // 归一化当前方向向量
            normalizeVector(currentDirection);
            
            // 计算角度差（弧度转角度，57.29577951308232 = 180 / PI）
            float angleDelta = (float) ((Math.atan2(currentDirection.y, currentDirection.x) 
                    - Math.atan2(initialDirection.y, initialDirection.x)) * 57.29577951308232d);
            
            // 计算平移增量（当前中心点相对于初始中心点的偏移）
            float deltaX = centerX - gestureState.getCenterX();
            float deltaY = centerY - gestureState.getCenterY();
            
            // 更新View的变换
            applyTransform(view, deltaX, deltaY, currentScaleRatio, angleDelta);
        }
    }

    /**
     * 应用变换到View
     */
    private void applyTransform(View view, float deltaX, float deltaY, float scaleRatio, float angleDelta) {
        // 更新中心点（如果需要）
        float pivotX = gestureState.getCenterX();
        float pivotY = gestureState.getCenterY();
        
        // 如果View的pivot点与手势中心点不同，需要调整
        if (view.getPivotX() != pivotX || view.getPivotY() != pivotY) {
            float[] oldPoint = {0.0f, 0.0f};
            view.getMatrix().mapPoints(oldPoint);
            
            view.setPivotX(pivotX);
            view.setPivotY(pivotY);
            
            float[] newPoint = {0.0f, 0.0f};
            view.getMatrix().mapPoints(newPoint);
            
            // 调整translation以补偿pivot变化
            float deltaTranslationX = newPoint[0] - oldPoint[0];
            float deltaTranslationY = newPoint[1] - oldPoint[1];
            view.setTranslationX(view.getTranslationX() - deltaTranslationX);
            view.setTranslationY(view.getTranslationY() - deltaTranslationY);
        }
        
        // 应用平移
        DragPinchRotateTouchListener.translateView(view, deltaX, deltaY);
        
        // 应用缩放（限制在0-10倍之间）
        float newScale = Math.max(0.0f, Math.min(10.0f, view.getScaleX() * scaleRatio));
        view.setScaleX(newScale);
        view.setScaleY(newScale);
        
        // 应用旋转（限制在-180到180度之间）
        float newRotation = view.getRotation() + angleDelta;
        if (newRotation > 180.0f) {
            newRotation -= 360.0f;
        } else if (newRotation < -180.0f) {
            newRotation += 360.0f;
        }
        view.setRotation(newRotation);
    }

    /**
     * 更新手势数据
     * 对应原代码中的 d() 方法
     */
    private void updateGestureData(MotionEvent motionEvent) {
        // 回收之前保存的事件
        if (previousEvent != null) {
            previousEvent.recycle();
        }
        previousEvent = MotionEvent.obtain(motionEvent);
        
        // 重置计算值（这些值会在需要时重新计算）
        initialDistance = -1.0f;
        previousDistance = -1.0f;
        currentScaleRatio = -1.0f;
        
        // 重置当前方向向量
        currentDirection.set(0.0f, 0.0f);
        
        if (initialEvent != null) {
            // 在初始事件中查找两个指针的索引
            int initialFirstIndex = initialEvent.findPointerIndex(firstPointerId);
            int initialSecondIndex = initialEvent.findPointerIndex(secondPointerId);
            
            // 在当前事件中查找两个指针的索引
            int currentFirstIndex = motionEvent.findPointerIndex(firstPointerId);
            int currentSecondIndex = motionEvent.findPointerIndex(secondPointerId);
            
            if (initialFirstIndex >= 0 && initialSecondIndex >= 0 
                    && currentFirstIndex >= 0 && currentSecondIndex >= 0) {
                // 获取初始事件中两个指针的坐标
                float initialX1 = initialEvent.getX(initialFirstIndex);
                float initialY1 = initialEvent.getY(initialFirstIndex);
                float initialX2 = initialEvent.getX(initialSecondIndex);
                float initialY2 = initialEvent.getY(initialSecondIndex);
                
                // 计算初始向量（从第一个指针指向第二个指针）
                // 这个向量保存在 initialVectorX/Y 中，用于计算初始距离
                float prevVectorX = initialX2 - initialX1;
                float prevVectorY = initialY2 - initialY1;
                
                // 获取当前事件中两个指针的坐标
                float currentX1 = motionEvent.getX(currentFirstIndex);
                float currentY1 = motionEvent.getY(currentFirstIndex);
                float currentX2 = motionEvent.getX(currentSecondIndex);
                float currentY2 = motionEvent.getY(currentSecondIndex);
                
                // 计算当前向量（从第一个指针指向第二个指针）
                currentVectorX = currentX2 - currentX1;
                currentVectorY = currentY2 - currentY1;
                
                // 设置当前方向向量（用于计算旋转）
                // 对应原代码中的 this.i.set(x12, y11)，即 f8848e/f8849f
                currentDirection.set(currentVectorX, currentVectorY);
                
                // 保存初始向量（用于后续计算缩放比例）
                // 对应原代码中的 f8857p/f8858q，保存的是初始事件中的向量
                initialVectorX = prevVectorX;
                initialVectorY = prevVectorY;
                
                // 更新gestureState中的初始方向（用于计算旋转）
                // 注意：原代码中会在计算旋转时从gestureState获取初始方向
                gestureState.getInitialDirection().set(prevVectorX, prevVectorY);
                
                // 计算中心点（当前两个指针的中点）
                centerX = (currentX1 + currentX2) / 2.0f;
                centerY = (currentY1 + currentY2) / 2.0f;
                
                // 记录事件时间（虽然原代码没有使用，但保留）
                motionEvent.getEventTime();
                initialEvent.getEventTime();
                
                // 记录压力值（用于判断手势是否有效）
                // 当前压力
                float currentPressure = motionEvent.getPressure(currentSecondIndex) 
                        + motionEvent.getPressure(currentFirstIndex);
                // 初始压力（保存在成员变量中，供后续使用）
                // 注意：原代码中 f8851h 是当前压力，f8860s 是初始压力
            } else {
                // 指针索引无效，标记错误
                hasError = true;
                if (isInPinchRotateMode) {
                    gestureState.getClass(); // 原代码调用，可能是为了触发某些逻辑
                }
            }
        }
    }

    /**
     * 归一化向量
     */
    private void normalizeVector(PointF vector) {
        float length = (float) Math.sqrt(vector.x * vector.x + vector.y * vector.y);
        if (length > 0) {
            vector.x /= length;
            vector.y /= length;
        }
    }

    /**
     * 获取当前压力值
     */
    private float getCurrentPressure() {
        if (previousEvent != null) {
            int firstIndex = previousEvent.findPointerIndex(firstPointerId);
            int secondIndex = previousEvent.findPointerIndex(secondPointerId);
            if (firstIndex >= 0 && secondIndex >= 0) {
                return previousEvent.getPressure(secondIndex) + previousEvent.getPressure(firstIndex);
            }
        }
        return 0.0f;
    }

    /**
     * 获取初始压力值
     */
    private float getInitialPressure() {
        if (initialEvent != null) {
            int firstIndex = initialEvent.findPointerIndex(firstPointerId);
            int secondIndex = initialEvent.findPointerIndex(secondPointerId);
            if (firstIndex >= 0 && secondIndex >= 0) {
                return initialEvent.getPressure(secondIndex) + initialEvent.getPressure(firstIndex);
            }
        }
        return 0.0f;
    }

    /**
     * 重置所有状态
     */
    private void reset() {
        if (initialEvent != null) {
            initialEvent.recycle();
            initialEvent = null;
        }
        if (previousEvent != null) {
            previousEvent.recycle();
            previousEvent = null;
        }
        
        isInPinchRotateMode = false;
        firstPointerId = -1;
        secondPointerId = -1;
        hasError = false;
        pointerSwapped = false;
    }

    /**
     * 是否处于双指手势模式
     */
    public boolean isInPinchRotateMode() {
        return isInPinchRotateMode;
    }

    // Getter方法供GestureState使用
    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public PointF getCurrentDirection() {
        return currentDirection;
    }
}
