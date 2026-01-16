package com.study.googletranslatedemo.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Frame边框图片View（边框在外）
 * 功能：
 * 1. 显示图片
 * 2. 添加外边框（边框绘制在图像外面，可调节宽度）
 * 3. 自动缩放图像以适应View（如果View放不下，缩小图像）
 * 4. 导出处理后的图片为Bitmap
 */
public class FrameImageViewOuter extends View {

    // ========== Frame相关参数 ==========
    private float frameWidth = 0f;  // 边框宽度（像素）
    private static final int FRAME_COLOR = Color.LTGRAY;  // 边框颜色

    // ========== 图片相关 ==========
    private Bitmap originalBitmap;  // 原始图片
    private Bitmap resultBitmap;    // 带边框的结果图片（用于显示）
    private boolean needRedraw = true;  // 是否需要重绘

    // ========== Paint对象 ==========
    private Paint framePaint;  // 边框画笔
    private Paint bitmapPaint;  // 图片画笔

    // ========== 矩阵和缩放相关 ==========
    private Matrix drawMatrix;  // 绘制矩阵（用于缩放）

    public FrameImageViewOuter(Context context) {
        super(context);
        init();
    }

    public FrameImageViewOuter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FrameImageViewOuter(Context context, AttributeSet attrs, int defStyleAttr) {
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
        
        // 初始化图片画笔
        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        
        // 初始化矩阵
        drawMatrix = new Matrix();
    }

    /**
     * 设置要显示的图片
     * @param bitmap 图片Bitmap
     */
    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            this.originalBitmap = null;
            this.resultBitmap = null;
            invalidate();
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
            resultBitmap = null;
            return;
        }

        // 如果边框宽度为0，直接使用原图
        if (frameWidth <= 0) {
            resultBitmap = originalBitmap;
            return;
        }

        // 生成带边框的Bitmap
        if (needRedraw || resultBitmap == null || resultBitmap.isRecycled()) {
            resultBitmap = renderFrame(originalBitmap, frameWidth);
            needRedraw = false;
        }
    }

    /**
     * 渲染Frame边框到图片上（边框绘制在外面）
     * @param bitmap 原始图片
     * @param frameWidth 边框宽度
     * @return 带边框的Bitmap（尺寸 = 原图尺寸 + 边框宽度*2）
     */
    private Bitmap renderFrame(Bitmap bitmap, float frameWidth) {
        if (bitmap == null || bitmap.isRecycled() || frameWidth <= 0) {
            return bitmap;
        }

        int origWidth = bitmap.getWidth();
        int origHeight = bitmap.getHeight();

        // 计算结果Bitmap尺寸（原图 + 左右边框 + 上下边框）
        int resultWidth = (int) (origWidth + frameWidth * 2);
        int resultHeight = (int) (origHeight + frameWidth * 2);

        // 创建结果Bitmap（尺寸更大，因为边框在外面）
        Bitmap result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // 绘制原图（居中，留出边框空间）
        canvas.drawBitmap(bitmap, frameWidth, frameWidth, bitmapPaint);

        // 绘制边框（绘制在图片外面的矩形）
        framePaint.setStrokeWidth(frameWidth);
        framePaint.setColor(FRAME_COLOR);
        
        // 边框矩形（围绕在图片外面）
        float halfWidth = frameWidth / 2f;
        RectF frameRect = new RectF(
            halfWidth,
            halfWidth,
            resultWidth - halfWidth,
            resultHeight - halfWidth
        );
        
        canvas.drawRect(frameRect, framePaint);

        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (resultBitmap == null || resultBitmap.isRecycled()) {
            return;
        }

        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        int bitmapWidth = resultBitmap.getWidth();
        int bitmapHeight = resultBitmap.getHeight();

        // 计算缩放比例，确保整个图片+边框都能显示在View中
        float scaleX = (float) viewWidth / bitmapWidth;
        float scaleY = (float) viewHeight / bitmapHeight;
        float scale = Math.min(scaleX, scaleY);  // 取较小值，确保完整显示

        // 计算缩放后的尺寸
        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;

        // 计算居中位置
        float dx = (viewWidth - scaledWidth) / 2f + getPaddingLeft();
        float dy = (viewHeight - scaledHeight) / 2f + getPaddingTop();

        // 设置变换矩阵
        drawMatrix.reset();
        drawMatrix.postScale(scale, scale);
        drawMatrix.postTranslate(dx, dy);

        // 绘制图片
        canvas.drawBitmap(resultBitmap, drawMatrix, bitmapPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 尺寸改变时重绘
        invalidate();
    }

    /**
     * 导出带边框的图片为Bitmap（原始尺寸，不缩放）
     * @return 处理后的Bitmap，如果获取失败返回null
     */
    public Bitmap exportBitmap() {
        if (originalBitmap == null || originalBitmap.isRecycled()) {
            return null;
        }

        // 如果边框宽度为0，直接返回原图
        if (frameWidth <= 0) {
            return originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        // 重新生成带边框的Bitmap（不缓存，确保返回最新结果）
        return renderFrame(originalBitmap, frameWidth);
    }

    /**
     * 释放资源
     */
    public void release() {
        if (resultBitmap != null && !resultBitmap.isRecycled() && resultBitmap != originalBitmap) {
            resultBitmap.recycle();
            resultBitmap = null;
        }
        // originalBitmap不在这里释放，因为可能是外部传入的
    }
}
