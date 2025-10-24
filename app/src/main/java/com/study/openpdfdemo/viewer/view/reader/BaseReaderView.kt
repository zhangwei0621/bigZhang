package com.study.openpdfdemo.viewer.view.reader

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.SparseArray
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.widget.AdapterView
import android.widget.Scroller
import androidx.core.util.size
import androidx.core.view.children
import com.study.openpdfdemo.viewer.adapter.PageAdapter
import com.study.openpdfdemo.viewer.adapter.PageView
import com.study.openpdfdemo.viewer.data.PageBridge
import com.study.openpdfdemo.viewer.data.PageState
import com.study.openpdfdemo.viewer.data.PageTool
import com.study.openpdfdemo.viewer.tool.Stepper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class BaseReaderView(context: Context) : AdapterView<PageAdapter>(context),
    GestureDetector.OnGestureListener, OnScaleGestureListener, Runnable {
    companion object {
        private const val MOVING_DIAGONALLY = 0
        private const val MOVING_LEFT = 1
        private const val MOVING_RIGHT = 2
        private const val MOVING_UP = 3
        private const val MOVING_DOWN = 4
        private const val FLING_MARGIN = 100
        private const val MIN_SCALE = 1.0f
        private const val MAX_SCALE = 4.0f
        private var HORIZONTAL_SCROLLING = false

        private fun directionOfTravel(vx: Float, vy: Float): Int {
            return if (abs(vx) > 2 * abs(vy)) if (vx > 0) MOVING_RIGHT else MOVING_LEFT
            else if (abs(vy) > 2 * abs(vx)) if (vy > 0) MOVING_DOWN else MOVING_UP
            else MOVING_DIAGONALLY
        }

        private fun withinBoundsInDirectionOfTravel(bounds: Rect, vx: Float, vy: Float): Boolean {
            return when (directionOfTravel(vx, vy)) {
                MOVING_DIAGONALLY -> bounds.contains(0, 0)
                MOVING_LEFT -> bounds.left <= 0
                MOVING_RIGHT -> bounds.right >= 0
                MOVING_UP -> bounds.top <= 0
                MOVING_DOWN -> bounds.bottom >= 0
                else -> throw NoSuchElementException()
            }
        }
    }

    private var mResetLayout = false
    private val mChildViews = SparseArray<View>(3)

    //todo PageView缓存复用

    // private val mViewCache = LinkedList<View>()
    private var mUserInteracting = false
    private var mScaling = false
    private var mScale = 1.0f
    private var mXScroll = 0
    private var mYScroll = 0
    private var mGestureDetector: GestureDetector = GestureDetector(context, this)
    private var mScaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private var mScroller: Scroller = Scroller(context)
    private var mStepper: Stepper = Stepper(this, this)
    private var mScrollerLastX = 0
    private var mScrollerLastY = 0
    private var mLastScaleFocusX = 0f
    private var mLastScaleFocusY = 0f
    private var mPageState = PageState.NormalReader
    private var mPageTool: PageTool? = null

    protected var mAdapter: PageAdapter? = null
    protected var mCurrent: Int = 0

    val pageBridge = PageBridge()

    fun destroy() {
        applyToAllPageView {
            it.destroy()
        }
    }

    override fun run() {
        if (!mScroller.isFinished) {
            mScroller.computeScrollOffset()
            val x = mScroller.currX
            val y = mScroller.currY
            mXScroll += x - mScrollerLastX
            mYScroll += y - mScrollerLastY
            mScrollerLastX = x
            mScrollerLastY = y
            requestLayout()
            mStepper.prod()
        } else if (!mUserInteracting) {
            // End of an inertial scroll and the user is not interacting.
            // The layout is stable
            mChildViews.get(mCurrent)?.let {
                postSettle(it)
            }
        }
    }

    fun getCurrentPageIndex(): Int {
        return mCurrent
    }

    fun gotoPage(index: Int) {
        val total = mAdapter?.count ?: 0
        if (index in 0..<total) {
            onMoveOffChild(index)
            mCurrent = index
            onMoveToChild(index, total)
            mResetLayout = true
            requestLayout()
        }
    }

    fun setPageEditTool(tool: PageTool?) {
        mPageTool = tool
        onPageEditToolChanged(tool)
        val newPateState = mPageTool?.pageState ?: PageState.NormalReader
        if (PageState.NormalReader == newPateState && mPageState != newPateState) {
            //切换到阅读模式或者状态有切换时，让所有PageView清除预览效果
            applyToAllPageView { it.clearOverlayPreview() }
            mScaling = false
        }
        mPageState = newPateState
        pageBridge.pageTool = mPageTool
        pageBridge.pageState = mPageState
    }

    fun refresh() {
        mResetLayout = true
        mScale = MIN_SCALE
        mYScroll = 0
        mXScroll = 0
        mScaling = false
        val numChildren = mChildViews.size
        for (i in 0..<numChildren) {
            val v = mChildViews.valueAt(i)
            onNotInUse(v)
            removeViewInLayout(v)
        }
        mChildViews.clear()
        //todo PageView缓存复用

        // mViewCache.clear()
        requestLayout()
    }

    private fun getOrCreateChild(i: Int): View {
        mAdapter.let { adapter ->
            if (adapter == null) {
                throw Exception("adapter is null")
            }
            var v = mChildViews.get(i)
            if (v == null) {
                v = adapter.getView(i, getCached(), this@BaseReaderView)
                addAndMeasureChild(i, v!!)
                onChildSetup(i, v)
            }
            return v
        }
    }

    private fun getCached(): View? {
        return null
        //todo PageView缓存复用

//        return if (mViewCache.isEmpty()) null
//        else mViewCache.removeFirst()
    }

    private fun addAndMeasureChild(i: Int, v: View) {
        var params = v.layoutParams
        if (params == null) {
            params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        addViewInLayout(v, 0, params, true)
        mChildViews.append(i, v) // Record the view against its adapter index
        measureView(v)
    }

    private fun measureView(v: View) {
        // See what size the view wants to be
        v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        // Work out a scale that will fit it to this view
        val scale = min(
            width.toFloat() / v.measuredWidth.toFloat(),
            height.toFloat() / v.measuredHeight.toFloat()
        )
        // Use the fitting values scaled by our current scale factor
        val pw = this.measuredWidth
        val ph = this.measuredHeight
        val cw = v.measuredWidth
        val ch = v.measuredHeight
        v.measure(
            MeasureSpec.EXACTLY or (cw * scale * mScale).toInt(),
            MeasureSpec.EXACTLY or (ch * scale * mScale).toInt()
        )
    }

    private fun slideViewOntoScreen(v: View) {
        val corr = getCorrection(getScrollBounds(v))
        if (corr.x != 0 || corr.y != 0) {
            mScrollerLastY = 0
            mScrollerLastX = 0
            mScroller.startScroll(0, 0, corr.x, corr.y, 400)
            mStepper.prod()
        }
    }

    private fun getCorrection(bounds: Rect): Point {
        return Point(
            min(max(0, bounds.left), bounds.right),
            min(max(0, bounds.top), bounds.bottom)
        )
    }

    private fun getScrollBounds(v: View): Rect {
        return getScrollBounds(
            v.left + mXScroll,
            v.top + mYScroll,
            v.left + v.measuredWidth + mXScroll,
            v.top + v.measuredHeight + mYScroll
        )
    }

    private fun getScrollBounds(left: Int, top: Int, right: Int, bottom: Int): Rect {
        var xMin = width - right
        var xMax = -left
        var yMin = height - bottom
        var yMax = -top
        if (xMin > xMax) {
            xMax = (xMin + xMax) / 2
            xMin = xMax
        }
        if (yMin > yMax) {
            yMax = (yMin + yMax) / 2
            yMin = yMax
        }
        return Rect(xMin, yMin, xMax, yMax)
    }

    private fun subScreenSizeOffset(v: View): Point {
        return Point(
            max((width - v.measuredWidth) / 2, 0),
            max((height - v.measuredHeight) / 2, 0)
        )
    }

    private fun postSettle(v: View) {
        post { onSettle(v) }
    }

    private fun postUnsettle(v: View) {
        post { onUnsettle(v) }
    }

    protected fun getView(i: Int): View? {
        return mChildViews.get(i)
    }

    protected fun applyToCurrentPageView(work: (PageView) -> Unit) {
        getDisplayedView()?.let {
            if (it is PageView) {
                work.invoke(it)
            }
        }
    }

    protected fun applyToAllPageView(work: (PageView) -> Unit) {
        children.forEach {
            if (it is PageView) {
                work.invoke(it)
            }
        }
    }

    protected fun getDisplayedView(): View? {
        return getView(mCurrent)
    }

    protected fun onChildSetup(i: Int, v: View) {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        applyToAllPageView { measureView(it) }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        pageBridge.readerWidth = right - left
        pageBridge.readerHeight = bottom - top
        try {
            var cv = mChildViews.get(mCurrent)
            var cvOffset: Point?

            if (!mResetLayout) {
                // Move to next or previous if current is sufficiently off center
                if (cv != null) {
                    cvOffset = subScreenSizeOffset(cv)
                    // cv.getRight() may be out of date with the current scale
                    // so add left to the measured width for the correct position
                    var move: Boolean =
                        if (HORIZONTAL_SCROLLING) cv.left + cv.measuredWidth + cvOffset.x + mXScroll < width / 2
                        else cv.top + cv.measuredHeight + cvOffset.y + mYScroll < height / 2
                    if (move && mCurrent + 1 < mAdapter!!.getCount()) {
                        postUnsettle(cv)
                        mStepper.prod()
                        onMoveOffChild(mCurrent)
                        mCurrent++
                        onMoveToChild(mCurrent, mAdapter!!.getCount())
                    }

                    move = if (HORIZONTAL_SCROLLING) cv.left - cvOffset.x + mXScroll >= width / 2
                    else cv.top - cvOffset.y + mYScroll >= height / 2
                    if (move && mCurrent > 0) {
                        postUnsettle(cv)
                        mStepper.prod()
                        onMoveOffChild(mCurrent)
                        mCurrent--
                        onMoveToChild(mCurrent, mAdapter!!.getCount())
                    }
                }

                // Remove not needed children and hold them for reuse
                val numChildren = mChildViews.size
                val childIndices = IntArray(numChildren)
                for (i in 0..<numChildren) childIndices[i] = mChildViews.keyAt(i)

                for (i in 0..<numChildren) {
                    val ai = childIndices[i]
                    if (ai < mCurrent - 1 || ai > mCurrent + 1) {
                        val v = mChildViews.get(ai)
                        onNotInUse(v)
                        //todo PageView缓存复用

                        // mViewCache.add(v)
                        removeViewInLayout(v)
                        mChildViews.remove(ai)
                    }
                }
            } else {
                mResetLayout = false
                mYScroll = 0
                mXScroll = 0

                // Remove all children and hold them for reuse
                val numChildren = mChildViews.size
                for (i in 0..<numChildren) {
                    val v = mChildViews.valueAt(i)
                    onNotInUse(v)
                    //todo PageView缓存复用

                    // mViewCache.add(v)
                    removeViewInLayout(v)
                }
                mChildViews.clear()

                // post to ensure generation of hq area
                mStepper.prod()
            }

            // Ensure current view is present
            var cvLeft: Int
            var cvTop: Int
            val notPresent = (mChildViews.get(mCurrent) == null)
            cv = getOrCreateChild(mCurrent)
            // When the view is sub-screen-size in either dimension we
            // offset it to center within the screen area, and to keep
            // the views spaced out
            cvOffset = subScreenSizeOffset(cv)
            if (notPresent) {
                // Main item not already present. Just place it top left
                cvLeft = cvOffset.x
                cvTop = cvOffset.y
            } else {
                // Main item already present. Adjust by scroll offsets
                cvLeft = cv.left + mXScroll
                cvTop = cv.top + mYScroll
            }
            // Scroll values have been accounted for
            mYScroll = 0
            mXScroll = 0
            var cvRight: Int = cvLeft + cv.measuredWidth
            var cvBottom: Int = cvTop + cv.measuredHeight

            if (!mUserInteracting && mScroller.isFinished) {
                val corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom))
                cvRight += corr.x
                cvLeft += corr.x
                cvTop += corr.y
                cvBottom += corr.y
            } else if (HORIZONTAL_SCROLLING && cv.measuredHeight <= height) {
                // When the current view is as small as the screen in height, clamp
                // it vertically
                val corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom))
                cvTop += corr.y
                cvBottom += corr.y
            } else if (!HORIZONTAL_SCROLLING && cv.measuredWidth <= width) {
                // When the current view is as small as the screen in width, clamp
                // it horizontally
                val corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom))
                cvRight += corr.x
                cvLeft += corr.x
            }

            cv.layout(cvLeft, cvTop, cvRight, cvBottom)

            if (mCurrent > 0) {
                val lv = getOrCreateChild(mCurrent - 1)
                val leftOffset = subScreenSizeOffset(lv)
                if (HORIZONTAL_SCROLLING) {
                    val gap = leftOffset.x + cvOffset.x
                    lv.layout(
                        cvLeft - lv.measuredWidth - gap,
                        (cvBottom + cvTop - lv.measuredHeight) / 2,
                        cvLeft - gap,
                        (cvBottom + cvTop + lv.measuredHeight) / 2
                    )
                } else {
                    val gap = leftOffset.y + cvOffset.y
                    lv.layout(
                        (cvLeft + cvRight - lv.measuredWidth) / 2,
                        cvTop - lv.measuredHeight - gap,
                        (cvLeft + cvRight + lv.measuredWidth) / 2,
                        cvTop - gap
                    )
                }
            }

            if (mCurrent + 1 < mAdapter!!.getCount()) {
                val rv = getOrCreateChild(mCurrent + 1)
                val rightOffset = subScreenSizeOffset(rv)
                if (HORIZONTAL_SCROLLING) {
                    val gap = cvOffset.x + rightOffset.x
                    rv.layout(
                        cvRight + gap,
                        (cvBottom + cvTop - rv.measuredHeight) / 2,
                        cvRight + rv.measuredWidth + gap,
                        (cvBottom + cvTop + rv.measuredHeight) / 2
                    )
                } else {
                    val gap = cvOffset.y + rightOffset.y
                    rv.layout(
                        (cvLeft + cvRight - rv.measuredWidth) / 2,
                        cvBottom + gap,
                        (cvLeft + cvRight + rv.measuredWidth) / 2,
                        cvBottom + gap + rv.measuredHeight
                    )
                }
            }
            invalidate()
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (mScaling) return true
        if (PageState.NormalReader != mPageState) return true

        val v = mChildViews.get(mCurrent)
        if (v != null) {
            val bounds = getScrollBounds(v)
            when (directionOfTravel(velocityX, velocityY)) {
                MOVING_LEFT -> if (HORIZONTAL_SCROLLING && bounds.left >= 0) {
                    // Fling off to the left bring next view onto screen
                    val vl = mChildViews.get(mCurrent + 1)

                    if (vl != null) {
                        slideViewOntoScreen(vl)
                        return true
                    }
                }

                MOVING_UP -> if (!HORIZONTAL_SCROLLING && bounds.top >= 0) {
                    // Fling off to the top bring next view onto screen
                    val vl = mChildViews.get(mCurrent + 1)

                    if (vl != null) {
                        slideViewOntoScreen(vl)
                        return true
                    }
                }

                MOVING_RIGHT -> if (HORIZONTAL_SCROLLING && bounds.right <= 0) {
                    // Fling off to the right bring previous view onto screen
                    val vr = mChildViews.get(mCurrent - 1)

                    if (vr != null) {
                        slideViewOntoScreen(vr)
                        return true
                    }
                }

                MOVING_DOWN -> if (!HORIZONTAL_SCROLLING && bounds.bottom <= 0) {
                    // Fling off to the bottom bring previous view onto screen
                    val vr = mChildViews.get(mCurrent - 1)

                    if (vr != null) {
                        slideViewOntoScreen(vr)
                        return true
                    }
                }
            }
            mScrollerLastY = 0
            mScrollerLastX = 0
            // If the page has been dragged out of bounds then we want to spring back
            // nicely. fling jumps back into bounds instantly, so we don't want to use
            // fling in that case. On the other hand, we don't want to forgo a fling
            // just because of a slightly off-angle drag taking us out of bounds other
            // than in the direction of the drag, so we test for out of bounds only
            // in the direction of travel.
            //
            // Also don't fling if out of bounds in any direction by more than fling
            // margin
            val expandedBounds = Rect(bounds)
            expandedBounds.inset(-FLING_MARGIN, -FLING_MARGIN)

            if (withinBoundsInDirectionOfTravel(bounds, velocityX, velocityY)
                && expandedBounds.contains(0, 0)
            ) {
                mScroller.fling(
                    0,
                    0,
                    velocityX.toInt(),
                    velocityY.toInt(),
                    bounds.left,
                    bounds.right,
                    bounds.top,
                    bounds.bottom
                )
                mStepper.prod()
            }
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)

        if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            mUserInteracting = true
        }
        if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            mUserInteracting = false
            val v = mChildViews.get(mCurrent)
            if (v != null) {
                //这里只在非编辑模式下处理页面滚动(切换页面和重新渲染缩放)
                if (mScroller.isFinished && PageState.NormalReader == mPageState) {
                    slideViewOntoScreen(v)
                    postSettle(v)
                }
            }
        }
        requestLayout()
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val previousScale = mScale
        mScale = min(max(mScale * detector.getScaleFactor(), MIN_SCALE), MAX_SCALE)
        val factor = mScale / previousScale

        val v = mChildViews.get(mCurrent)
        if (v != null) {
            val currentFocusX = detector.focusX
            val currentFocusY = detector.focusY
            // Work out the focus point relative to the view top left
            val viewFocusX = currentFocusX.toInt() - (v.left + mXScroll)
            val viewFocusY = currentFocusY.toInt() - (v.top + mYScroll)
            // Scroll to maintain the focus point
            mXScroll += (viewFocusX - viewFocusX * factor).toInt()
            mYScroll += (viewFocusY - viewFocusY * factor).toInt()

            if (mLastScaleFocusX >= 0) mXScroll += (currentFocusX - mLastScaleFocusX).toInt()
            if (mLastScaleFocusY >= 0) mYScroll += (currentFocusY - mLastScaleFocusY).toInt()

            mLastScaleFocusX = currentFocusX
            mLastScaleFocusY = currentFocusY
            requestLayout()
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        mScaling = true
        // Ignore any scroll amounts yet to be accounted for: the
        // screen is not showing the effect of them, so they can
        // only confuse the user
        mYScroll = 0
        mXScroll = 0
        mLastScaleFocusY = -1f
        mLastScaleFocusX = mLastScaleFocusY
        return PageState.NormalReader == mPageState
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        mScaling = false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!mScaling && PageState.NormalReader == mPageState) {
            mXScroll -= distanceX.toInt()
            mYScroll -= distanceY.toInt()
            requestLayout()
        }
        return true
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (PageState.NormalReader == mPageState) {
            onSingleTapUp()
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onDown(e: MotionEvent): Boolean {
        mScroller.forceFinished(true)
        return true
    }

    override fun getAdapter(): PageAdapter? {
        return mAdapter
    }

    override fun setAdapter(adapter: PageAdapter?) {
        mAdapter = adapter
        requestLayout()
    }

    override fun getSelectedView(): View? {
        return null
    }

    override fun setSelection(position: Int) {
    }

    abstract fun onSettle(v: View)

    abstract fun onUnsettle(v: View)

    protected fun onNotInUse(v: View) {
        if (v is PageView) {
            v.destroy()
        }
    }

    abstract fun onSingleTapUp()

    abstract fun onMoveOffChild(i: Int)

    abstract fun onMoveToChild(i: Int, total: Int)

    abstract fun onPageStatusChanged(currentPageView: View?)

    abstract fun onPageEditToolChanged(tool: PageTool?)
}