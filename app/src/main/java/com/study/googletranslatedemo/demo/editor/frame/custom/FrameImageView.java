package com.study.googletranslatedemo.demo.editor.frame.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Frame边框图片View
 * 功能：
 * 1. 显示图片
 * 2. 添加外边框（可调节宽度）
 * 3. 导出处理后的图片为Bitmap
 */
public class FrameImageView extends AppCompatImageView {

    // ========== Frame相关参数 ==========
    private float frameWidth = 0f;  // 边框宽度（像素）
    private static final int FRAME_COLOR = Color.LTGRAY;  // 边框颜色

    // ========== 图片相关 ==========
    private Bitmap originalBitmap;  // 原始图片
    private Bitmap resultBitmap;    // 带边框的结果图片
    private boolean needRedraw = true;  // 是否需要重绘

    // ========== Paint对象 ==========
    private Paint framePaint;  // 边框画笔

    public FrameImageView(Context context) {
        super(context);
        init();
    }

    public FrameImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FrameImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        // 初始化边框画笔
        framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        framePaint.setColor(FRAME_COLOR);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(frameWidth);
    }

    /**
     * 设置要显示的图片
     * @param bitmap 图片Bitmap
     */
    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        this.originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        this.resultBitmap = null;
        this.needRedraw = true;
        updateDisplayBitmap();
        invalidate();
    }

    /**
     * 设置边框宽度（像素）
     * @param width 边框宽度，0表示无边框
     */
    public void setFrameWidth(float width) {
        if (width < 0) {
            width = 0;
        }
        if (this.frameWidth != width) {
            this.frameWidth = width;
            this.framePaint.setStrokeWidth(width);
            this.resultBitmap = null;
            this.needRedraw = true;
            updateDisplayBitmap();
            invalidate();
        }
    }

    /**
     * 获取当前边框宽度
     * @return 边框宽度（像素）
     */
    public float getFrameWidth() {
        return frameWidth;
    }

    /**
     * 更新显示的Bitmap（绘制Frame）
     */
    private void updateDisplayBitmap() {
        if (originalBitmap == null || originalBitmap.isRecycled()) {
            super.setImageBitmap(null);
            return;
        }

        // 如果边框宽度为0，直接显示原图
        if (frameWidth <= 0) {
            super.setImageBitmap(originalBitmap);
            resultBitmap = null;
            return;
        }

        // 如果需要重绘或结果Bitmap为空，重新生成
        if (needRedraw || resultBitmap == null || resultBitmap.isRecycled()) {
            resultBitmap = renderFrame(originalBitmap, frameWidth);
            needRedraw = false;
        }

        // 显示带边框的图片
        super.setImageBitmap(resultBitmap);
    }

    /**
     * 渲染Frame边框到图片上
     * @param bitmap 原始图片
     * @param frameWidth 边框宽度
     * @return 带边框的Bitmap
     */
    private Bitmap renderFrame(Bitmap bitmap, float frameWidth) {
        if (bitmap == null || bitmap.isRecycled() || frameWidth <= 0) {
            return bitmap;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // 创建结果Bitmap（尺寸不变，因为边框是绘制在图片内部的）
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // 绘制原图
        canvas.drawBitmap(bitmap, 0, 0, null);

        // 绘制边框（绘制在图片边缘，向内绘制）
        framePaint.setStrokeWidth(frameWidth);
        framePaint.setColor(FRAME_COLOR);
        
        // 计算边框绘制区域（考虑边框宽度，边框绘制在图片边缘内侧）
        float halfWidth = frameWidth / 2f;
        RectF frameRect = new RectF(
            halfWidth,
            halfWidth,
            width - halfWidth,
            height - halfWidth
        );
        
        canvas.drawRect(frameRect, framePaint);

        return result;
    }

    /**
     * 导出带边框的图片为Bitmap
     * @return 处理后的Bitmap，如果获取失败返回null
     */
    public Bitmap exportBitmap() {
        if (originalBitmap == null || originalBitmap.isRecycled()) {
            return null;
        }

        // 如果有缓存的resultBitmap且不需要重绘，直接返回
        if (resultBitmap != null && !resultBitmap.isRecycled() && !needRedraw) {
            return resultBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        // 重新生成
        return renderFrame(originalBitmap, frameWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 尺寸改变时可能需要重绘
        if (originalBitmap != null) {
            needRedraw = true;
            updateDisplayBitmap();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (resultBitmap != null && !resultBitmap.isRecycled()) {
            resultBitmap.recycle();
            resultBitmap = null;
        }
        // originalBitmap不在这里释放，因为可能是外部传入的
    }
}
