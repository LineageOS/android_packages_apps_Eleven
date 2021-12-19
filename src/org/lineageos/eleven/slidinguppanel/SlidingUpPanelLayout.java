/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2021 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.eleven.slidinguppanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import org.lineageos.eleven.R;

public class SlidingUpPanelLayout extends ViewGroup {

    /**
     * Default peeking out panel height
     */
    private static final int DEFAULT_PANEL_HEIGHT = 68; // dp;

    /**
     * Default anchor point height
     */
    private static final float DEFAULT_ANCHOR_POINT = 1.0f; // In relative %

    /**
     * Default initial state for the component
     */
    private static final SlideState DEFAULT_SLIDE_STATE = SlideState.COLLAPSED;

    /**
     * Default height of the shadow above the peeking out panel
     */
    private static final int DEFAULT_SHADOW_HEIGHT = 4; // dp;

    /**
     * If no fade color is given by default it will fade to 80% gray.
     */
    private static final int DEFAULT_FADE_COLOR = 0x99000000;

    /**
     * Whether we should hook up the drag view clickable state
     */
    private static final boolean DEFAULT_DRAG_VIEW_CLICKABLE = true;

    /**
     * Default Minimum velocity that will be detected as a fling
     */
    private static final int DEFAULT_MIN_FLING_VELOCITY = 400; // dips per second
    /**
     * Default is set to false because that is how it was written
     */
    private static final boolean DEFAULT_OVERLAY_FLAG = false;
    /**
     * Default attributes for layout
     */
    private static final int[] DEFAULT_ATTRS = new int[]{
            android.R.attr.gravity
    };

    /**
     * The fade color used for the panel covered by the slider. 0 = no fading.
     */
    private int mCoveredFadeColor = DEFAULT_FADE_COLOR;

    /**
     * Default parallax length of the main view
     */
    private static final int DEFAULT_PARALLAX_OFFSET = 0;

    /**
     * Default slide panel offset when collapsed
     */
    private static final int DEFAULT_SLIDE_PANEL_OFFSET = 0;

    /**
     * Default direct offset flag
     */
    private static final boolean DEFAULT_DIRECT_OFFSET_FLAG = false;

    /**
     * The paint used to dim the main layout when sliding
     */
    private final Paint mCoveredFadePaint = new Paint();

    /**
     * Drawable used to draw the shadow between panes.
     */
    private final Drawable mShadowDrawable;

    /**
     * The size of the overhang in pixels.
     */
    private int mPanelHeight = -1;

    /**
     * Determines how much to slide the panel off when expanded
     */
    private int mSlidePanelOffset = 0;

    /**
     * The size of the shadow in pixels.
     */
    private int mShadowHeight = -1;

    /**
     * Parallax offset
     */
    private int mParallaxOffset = -1;

    /**
     * Clamps the Main view to the slide-able view
     */
    private boolean mDirectOffset = false;

    /**
     * True if the collapsed panel should be dragged up.
     */
    private boolean mIsSlidingUp;

    /**
     * Panel overlays the windows instead of putting it underneath it.
     */
    private boolean mOverlayContent = DEFAULT_OVERLAY_FLAG;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private View mDragView;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private int mDragViewResId = -1;

    /**
     * Whether clicking on the drag view will expand/collapse
     */
    private boolean mDragViewClickable = DEFAULT_DRAG_VIEW_CLICKABLE;

    /**
     * The child view that can slide, if any.
     */
    private View mSlideableView;

    /**
     * The main view
     */
    private View mMainView;

    /**
     * Current state of the slideable view.
     */
    private enum SlideState {
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        HIDDEN,
        DRAGGING
    }

    private SlideState mSlideState = SlideState.COLLAPSED;

    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = collapsed, 1 = expanded.
     */
    private float mSlideOffset;

    /**
     * How far in pixels the slide-able panel may move.
     */
    private int mSlideRange;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    private boolean mIsUnableToDrag;

    /**
     * Flag indicating that sliding feature is enabled\disabled
     */
    private boolean mIsSlidingEnabled;

    private float mInitialMotionX;
    private float mInitialMotionY;
    private float mAnchorPoint = 1.f;

    private PanelSlideListener mPanelSlideListener;

    private final ViewDragHelper mDragHelper;

    /**
     * Stores whether or not the pane was expanded the last time it was slideable.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mFirstLayout = true;

    private final Rect mTmpRect = new Rect();

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelSlideListener {
        /**
         * Called when a sliding pane's position changes.
         *
         * @param panel       The child view that was moved
         * @param slideOffset The new offset of this sliding pane within its range, from 0-1
         */
        void onPanelSlide(View panel, float slideOffset);

        /**
         * Called when a sliding panel becomes slid completely collapsed.
         *
         * @param panel The child view that was slid to an collapsed position
         */
        void onPanelCollapsed(View panel);

        /**
         * Called when a sliding panel becomes slid completely expanded.
         *
         * @param panel The child view that was slid to a expanded position
         */
        void onPanelExpanded(View panel);

        /**
         * Called when a sliding panel becomes anchored.
         *
         * @param panel The child view that was slid to a anchored position
         */
        @SuppressWarnings("unused")
        void onPanelAnchored(View panel);

        /**
         * Called when a sliding panel becomes completely hidden.
         *
         * @param panel The child view that was slid to a hidden position
         */
        @SuppressWarnings("unused")
        void onPanelHidden(View panel);
    }

    /**
     * No-op stubs for {@link PanelSlideListener}. If you only want to implement a subset
     * of the listener methods you can extend this instead of implement the full interface.
     */
    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
        }

        @Override
        public void onPanelCollapsed(View panel) {
        }

        @Override
        public void onPanelExpanded(View panel) {
        }

        @Override
        public void onPanelAnchored(View panel) {
        }

        @Override
        public void onPanelHidden(View panel) {
        }
    }

    public SlidingUpPanelLayout(Context context) {
        this(context, null);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            mShadowDrawable = null;
            mDragHelper = null;
            return;
        }

        // Minimum velocity that will be detected as a fling
        int minFlingVelocity = DEFAULT_MIN_FLING_VELOCITY;
        if (attrs != null) {
            TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);

            int gravity = defAttrs.getInt(0, Gravity.NO_GRAVITY);
            if (gravity != Gravity.TOP && gravity != Gravity.BOTTOM) {
                throw new IllegalArgumentException("gravity must be set to either top or bottom");
            }
            mIsSlidingUp = gravity == Gravity.BOTTOM;

            defAttrs.recycle();

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpPanelLayout);

            mPanelHeight = ta.getDimensionPixelSize(
                    R.styleable.SlidingUpPanelLayout_panelHeight, -1);
            mSlidePanelOffset = ta.getDimensionPixelSize(
                    R.styleable.SlidingUpPanelLayout_slidePanelOffset, DEFAULT_SLIDE_PANEL_OFFSET);
            mShadowHeight = ta.getDimensionPixelSize(
                    R.styleable.SlidingUpPanelLayout_shadowHeight, -1);
            mParallaxOffset = ta.getDimensionPixelSize(
                    R.styleable.SlidingUpPanelLayout_paralaxOffset, -1);
            mDirectOffset = ta.getBoolean(
                    R.styleable.SlidingUpPanelLayout_directOffset, DEFAULT_DIRECT_OFFSET_FLAG);

            minFlingVelocity = ta.getInt(
                    R.styleable.SlidingUpPanelLayout_flingVelocity, DEFAULT_MIN_FLING_VELOCITY);
            mCoveredFadeColor = ta.getColor(
                    R.styleable.SlidingUpPanelLayout_fadeColor, DEFAULT_FADE_COLOR);

            mDragViewResId = ta.getResourceId(
                    R.styleable.SlidingUpPanelLayout_dragView, -1);
            mDragViewClickable = ta.getBoolean(
                    R.styleable.SlidingUpPanelLayout_dragViewClickable,
                    DEFAULT_DRAG_VIEW_CLICKABLE);

            mOverlayContent = ta.getBoolean(
                    R.styleable.SlidingUpPanelLayout_overlay, DEFAULT_OVERLAY_FLAG);

            mAnchorPoint = ta.getFloat(
                    R.styleable.SlidingUpPanelLayout_anchorPoint, DEFAULT_ANCHOR_POINT);

            mSlideState = SlideState.values()[ta.getInt(
                    R.styleable.SlidingUpPanelLayout_initialState, DEFAULT_SLIDE_STATE.ordinal())];

            ta.recycle();
        }

        final float density = context.getResources().getDisplayMetrics().density;
        if (mPanelHeight == -1) {
            mPanelHeight = (int) (DEFAULT_PANEL_HEIGHT * density + 0.5f);
        }
        if (mShadowHeight == -1) {
            mShadowHeight = (int) (DEFAULT_SHADOW_HEIGHT * density + 0.5f);
        }
        if (mParallaxOffset == -1) {
            mParallaxOffset = (int) (DEFAULT_PARALLAX_OFFSET * density);
        }
        // If the shadow height is zero, don't show the shadow
        if (mShadowHeight > 0) {
            mShadowDrawable = ContextCompat.getDrawable(getContext(),
                    mIsSlidingUp ? R.drawable.above_shadow : R.drawable.below_shadow);
        } else {
            mShadowDrawable = null;
        }

        setWillNotDraw(false);

        mDragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        mDragHelper.setMinVelocity(minFlingVelocity * density);

        mIsSlidingEnabled = true;
    }

    /**
     * Set the Drag View after the view is inflated
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDragViewResId != -1) {
            setDragView(findViewById(mDragViewResId));
        }
    }

    /**
     * Set sliding enabled flag
     *
     * @param enabled flag value
     */
    public void setSlidingEnabled(boolean enabled) {
        mIsSlidingEnabled = enabled;
    }

    public boolean isSlidingEnabled() {
        return mIsSlidingEnabled && mSlideableView != null;
    }

    /**
     * Sets the panel offset when collapsed so you can exit
     * the boundaries of the top of the screen
     *
     * @param val Offset in pixels
     */
    public void setSlidePanelOffset(int val) {
        mSlidePanelOffset = val;
        requestLayout();
    }

    /**
     * @return The current paralax offset
     */
    public int getCurrentParallaxOffset() {
        if (mParallaxOffset < 0) {
            return 0;
        }

        return (int) (mParallaxOffset * getDirectionalSlideOffset());
    }

    /**
     * @return The directional slide offset
     */
    protected float getDirectionalSlideOffset() {
        return mIsSlidingUp ? -mSlideOffset : mSlideOffset;
    }

    /**
     * Sets the panel slide listener
     */
    public void setPanelSlideListener(PanelSlideListener listener) {
        mPanelSlideListener = listener;
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragView A view that will be used to drag the panel.
     */
    public void setDragView(View dragView) {
        if (mDragView != null && mDragViewClickable) {
            mDragView.setOnClickListener(null);
        }

        mDragView = dragView;
        if (mDragView != null) {
            mDragView.setClickable(true);
            mDragView.setFocusable(false);
            mDragView.setFocusableInTouchMode(false);
            if (mDragViewClickable) {
                mDragView.setOnClickListener(v -> {
                    if (!isEnabled()) return;
                    if (!isPanelExpanded() && !isPanelAnchored()) {
                        expandPanel(mAnchorPoint);
                    } else {
                        collapsePanel();
                    }
                });
            }
        }
    }

    void dispatchOnPanelSlide(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelSlide(panel, mSlideOffset);
        }
    }

    void dispatchOnPanelExpanded(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelExpanded(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelCollapsed(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelCollapsed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelAnchored(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelAnchored(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelHidden(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelHidden(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewVisibility() {
        if (getChildCount() == 0) {
            return;
        }
        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (mSlideableView != null && hasOpaqueBackground(mSlideableView)) {
            left = mSlideableView.getLeft();
            right = mSlideableView.getRight();
            top = mSlideableView.getTop();
            bottom = mSlideableView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }
        View child = mMainView;
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int vis;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            vis = INVISIBLE;
        } else {
            vis = VISIBLE;
        }
        child.setVisibility(vis);
    }

    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        final int childCount = getChildCount();

        if (childCount != 2 && childCount != 3) {
            throw new IllegalStateException(
                    "Sliding up panel layout must have exactly 2 or 3 children!");
        }

        if (childCount == 2) {
            mMainView = getChildAt(0);
            mSlideableView = getChildAt(1);
        } else {
            mMainView = getChildAt(1);
            mSlideableView = getChildAt(2);
        }

        if (mDragView == null) {
            setDragView(mSlideableView);
        }

        // If the sliding panel is not visible, then put the whole view in the hidden state
        if (mSlideableView.getVisibility() == GONE) {
            mSlideState = SlideState.HIDDEN;
        }

        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();

        // First pass. Measure based on child LayoutParams width/height.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // We always measure the sliding panel in order to know it's height
            // (needed for show panel)
            if (child.getVisibility() == GONE && child == mMainView) {
                continue;
            }

            int height = layoutHeight;
            if (child == mMainView && !mOverlayContent && mSlideState != SlideState.HIDDEN) {
                height -= mPanelHeight;
            }

            int childWidthSpec;
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
            } else if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }

            if (child == mSlideableView) {
                mSlideRange = MeasureSpec.getSize(
                        childHeightSpec) - mPanelHeight + mSlidePanelOffset;
                childHeightSpec += mSlidePanelOffset;
            }

            child.measure(childWidthSpec, childHeightSpec);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();

        if (mFirstLayout) {
            switch (mSlideState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;
                case HIDDEN:
                    int newTop = computePanelTopPosition(0.0f) +
                            (mIsSlidingUp ? +mPanelHeight : -mPanelHeight);
                    mSlideOffset = computeSlideOffset(newTop);
                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            // Always layout the sliding view on the first layout
            if (child.getVisibility() == GONE && (child == mMainView || mFirstLayout)) {
                continue;
            }

            final int childHeight = child.getMeasuredHeight();
            int childTop = paddingTop;

            if (child == mSlideableView) {
                childTop = computePanelTopPosition(mSlideOffset);
            }

            if (!mIsSlidingUp) {
                if (child == mMainView && !mOverlayContent) {
                    childTop = computePanelTopPosition(mSlideOffset) +
                            mSlideableView.getMeasuredHeight();
                }
            }
            final int childBottom = childTop + childHeight;
            final int childRight = paddingLeft + child.getMeasuredWidth();

            child.layout(paddingLeft, childTop, childRight, childBottom);
        }

        if (mFirstLayout) {
            updateObscuredViewVisibility();
        }

        mFirstLayout = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        if (h != oldh) {
            mFirstLayout = true;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            collapsePanel();
        }
        super.setEnabled(enabled);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();


        if (!isEnabled() || !mIsSlidingEnabled ||
                (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(y - mInitialMotionY);
                final int dragSlop = mDragHelper.getTouchSlop();

                if ((ady > dragSlop && adx > ady) ||
                        !isDragViewUnder((int) mInitialMotionX, (int) mInitialMotionY)) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }
        }

        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isSlidingEnabled()) {
            return super.onTouchEvent(ev);
        }
        mDragHelper.processTouchEvent(ev);
        return true;
    }

    private boolean isDragViewUnder(int x, int y) {
        if (mDragView == null) return false;
        int[] viewLocation = new int[2];
        mDragView.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + mDragView.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + mDragView.getHeight();
    }

    /*
     * Computes the top position of the panel based on the slide offset.
     */
    private int computePanelTopPosition(float slideOffset) {
        int slidingViewHeight = mSlideableView != null ? mSlideableView.getMeasuredHeight() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        // Compute the top of the panel if its collapsed
        return mIsSlidingUp
                ? getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset
                : getPaddingTop() - slidingViewHeight + mPanelHeight + slidePixelOffset;
    }

    /*
     * Computes the slide offset based on the top position of the panel
     */
    private float computeSlideOffset(int topPosition) {
        // Compute the panel top position if the panel is collapsed (offset 0)
        final int topBoundCollapsed = computePanelTopPosition(0);

        // Determine the new slide offset based on the collapsed top position and the new required
        // top position
        return (mIsSlidingUp
                ? (float) (topBoundCollapsed - topPosition) / mSlideRange
                : (float) (topPosition - topBoundCollapsed) / mSlideRange);
    }

    /**
     * Collapse the sliding pane if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     */
    public void collapsePanel() {
        if (mFirstLayout) {
            mSlideState = SlideState.COLLAPSED;
        } else {
            if (mSlideState == SlideState.HIDDEN || mSlideState == SlideState.COLLAPSED)
                return;
            smoothSlideTo(0.0f);
        }
    }

    /**
     * Expand the sliding pane if it is currently slideable.
     */
    public void expandPanel() {
        if (mFirstLayout) {
            mSlideState = SlideState.EXPANDED;
        } else {
            expandPanel(1.0f);
        }
    }

    /**
     * Partially expand the sliding panel up to a specific offset
     *
     * @param slideOffset Value between 0 and 1, where 0 is completely expanded.
     */
    public void expandPanel(float slideOffset) {
        if (mSlideableView == null || mSlideState == SlideState.EXPANDED) return;
        mSlideableView.setVisibility(View.VISIBLE);
        if (!mFirstLayout) {
            smoothSlideTo(slideOffset);
        }
    }

    /**
     * Check if the sliding panel in this layout is fully expanded.
     *
     * @return true if sliding panel is completely expanded
     */
    public boolean isPanelExpanded() {
        return mSlideState == SlideState.EXPANDED;
    }

    /**
     * Check if the sliding panel in this layout is anchored.
     *
     * @return true if sliding panel is anchored
     */
    public boolean isPanelAnchored() {
        return mSlideState == SlideState.ANCHORED;
    }

    @SuppressLint("NewApi")
    private void onPanelDragged(int newTop) {
        mSlideState = SlideState.DRAGGING;
        // Recompute the slide offset based on the new top position
        mSlideOffset = computeSlideOffset(newTop);
        // Update the parallax based on the new slide offset
        if ((mParallaxOffset > 0 || mDirectOffset) && mSlideOffset >= 0) {
            int mainViewOffset = mParallaxOffset > 0 ?
                    getCurrentParallaxOffset() :
                    (int) (getDirectionalSlideOffset() * mSlideRange);

            mMainView.setTranslationY(mainViewOffset);
        }

        // Dispatch the slide event
        dispatchOnPanelSlide(mSlideableView);
        // If the slide offset is negative, and overlay is not on, we need to increase the
        // height of the main content
        if (mSlideOffset <= 0 && !mOverlayContent) {
            // expand the main view
            LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
            lp.height = mIsSlidingUp ?
                    (newTop - getPaddingBottom()) :
                    (getHeight() - getPaddingBottom() - mSlideableView.getMeasuredHeight()
                            - newTop);
            mMainView.requestLayout();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result;
        final int save = canvas.save();

        if (isSlidingEnabled() && mMainView == child) {
            // Clip against the slider; no sense drawing what will immediately be covered,
            // Unless the panel is set to overlay content
            if (!mOverlayContent) {
                canvas.getClipBounds(mTmpRect);
                if (mIsSlidingUp) {
                    mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideableView.getTop());
                } else {
                    mTmpRect.top = Math.max(mTmpRect.top, mSlideableView.getBottom());
                }
                canvas.clipRect(mTmpRect);
            }
        }

        result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(save);

        if (mCoveredFadeColor != 0 && mSlideOffset > 0) {
            final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;
            final int image = (int) (baseAlpha * mSlideOffset);
            final int color = image << 24 | (mCoveredFadeColor & 0xffffff);
            mCoveredFadePaint.setColor(color);
            canvas.drawRect(mTmpRect, mCoveredFadePaint);
        }

        return result;
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     */
    void smoothSlideTo(float slideOffset) {
        if (!isSlidingEnabled()) {
            // Nothing to do.
            return;
        }

        int panelTop = computePanelTopPosition(slideOffset);
        if (mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), panelTop)) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            if (!isSlidingEnabled()) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        if (!isSlidingEnabled()) {
            // No need to draw a shadow if we don't have one.
            return;
        }

        final int right = mSlideableView.getRight();
        final int top;
        final int bottom;
        if (mIsSlidingUp) {
            top = mSlideableView.getTop() - mShadowHeight;
            bottom = mSlideableView.getTop();
        } else {
            top = mSlideableView.getBottom();
            bottom = mSlideableView.getBottom() + mShadowHeight;
        }
        final int left = mSlideableView.getLeft();

        if (mShadowDrawable != null) {
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(c);
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.mSlideState = mSlideState;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSlideState = ss.mSlideState;
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mIsUnableToDrag) {
                return false;
            }

            return child == mSlideableView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper == null ||
                    mDragHelper.getViewDragState() != ViewDragHelper.STATE_IDLE) {
                return;
            }

            mSlideOffset = computeSlideOffset(mSlideableView.getTop());
            if (mSlideOffset == 1) {
                if (mSlideState != SlideState.EXPANDED) {
                    updateObscuredViewVisibility();
                    mSlideState = SlideState.EXPANDED;
                    dispatchOnPanelExpanded(mSlideableView);
                }
            } else if (mSlideOffset == 0) {
                if (mSlideState != SlideState.COLLAPSED) {
                    mSlideState = SlideState.COLLAPSED;
                    dispatchOnPanelCollapsed(mSlideableView);
                }
            } else if (mSlideOffset < 0) {
                mSlideState = SlideState.HIDDEN;
                mSlideableView.setVisibility(View.GONE);
                dispatchOnPanelHidden(mSlideableView);
            } else if (mSlideState != SlideState.ANCHORED) {
                updateObscuredViewVisibility();
                mSlideState = SlideState.ANCHORED;
                dispatchOnPanelAnchored(mSlideableView);
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(top);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xVel, float yVel) {
            int target;

            // direction is always positive if we are sliding in the expanded direction
            float direction = mIsSlidingUp ? -yVel : yVel;

            if (direction > 0) {
                // swipe up -> expand
                target = computePanelTopPosition(1.0f);
            } else if (direction < 0) {
                // swipe down -> collapse
                target = computePanelTopPosition(0.0f);
            } else if (mAnchorPoint != 1 && mSlideOffset >= (1.f + mAnchorPoint) / 2) {
                // zero velocity, and far enough from anchor point => expand to the top
                target = computePanelTopPosition(1.0f);
            } else if (mAnchorPoint == 1 && mSlideOffset >= 0.5f) {
                // zero velocity, and far enough from anchor point => expand to the top
                target = computePanelTopPosition(1.0f);
            } else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint) {
                target = computePanelTopPosition(mAnchorPoint);
            } else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint / 2) {
                target = computePanelTopPosition(mAnchorPoint);
            } else {
                // settle at the bottom
                target = computePanelTopPosition(0.0f);
            }

            if (mDragHelper != null) {
                mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
            }
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int collapsedTop = computePanelTopPosition(0.f);
            final int expandedTop = computePanelTopPosition(1.0f);
            if (mIsSlidingUp) {
                return Math.min(Math.max(top, expandedTop), collapsedTop);
            } else {
                return Math.min(Math.max(top, collapsedTop), expandedTop);
            }
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[]{
                android.R.attr.layout_weight
        };

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            a.recycle();
        }
    }

    static class SavedState extends BaseSavedState {
        SlideState mSlideState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            try {
                mSlideState = Enum.valueOf(SlideState.class, in.readString());
            } catch (IllegalArgumentException e) {
                mSlideState = SlideState.COLLAPSED;
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(mSlideState.toString());
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
