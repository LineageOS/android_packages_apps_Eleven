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
package org.lineageos.eleven.dragdrop;

import android.graphics.Point;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;

/**
 * Class that starts and stops item drags on a {@link DragSortListView} based on
 * touch gestures. This class also inherits from {@link SimpleFloatViewManager},
 * which provides basic float View creation. An instance of this class is meant
 * to be passed to the methods {@link DragSortListView#setOnTouchListener} and
 * {@link DragSortListView#setFloatViewManager} of your
 * {@link DragSortListView} instance.
 */
public class DragSortController extends SimpleFloatViewManager implements View.OnTouchListener,
        GestureDetector.OnGestureListener {

    public final static int ON_DOWN = 0;

    public final static int ON_DRAG = 1;

    public final static int ON_LONG_PRESS = 2;

    public final static int FLING_RIGHT_REMOVE = 0;

    public final static int FLING_LEFT_REMOVE = 1;

    public final static int SLIDE_RIGHT_REMOVE = 2;

    public final static int SLIDE_LEFT_REMOVE = 3;

    public final static int MISS = -1;

    private final GestureDetector mDetector;

    private final GestureDetector mFlingRemoveDetector;

    private final int mTouchSlop;

    private final int[] mTempLoc = new int[2];

    private final DragSortListView mDslv;

    private boolean mSortEnabled = true;

    private boolean mRemoveEnabled = false;

    private boolean mDragging = false;

    private int mDragInitMode = ON_DOWN;

    private int mRemoveMode;

    private int mHitPos = MISS;

    private int mItemX;

    private int mItemY;

    private int mCurrX;

    private int mCurrY;

    private final int mDragHandleId;

    private final float mOrigFloatAlpha;

    /**
     * By default, sorting is enabled, and removal is disabled.
     *
     * @param dslv         The DSLV instance
     * @param dragHandleId The resource id of the View that represents the drag
     *                     handle in a list item.
     */
    public DragSortController(DragSortListView dslv, int dragHandleId,
                              int dragInitMode, int removeMode) {
        super(dslv);
        mDslv = dslv;
        mDetector = new GestureDetector(dslv.getContext(), this);
        GestureDetector.OnGestureListener flingRemoveListener =
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                                 float velocityY) {
                        if (mRemoveEnabled) {
                            float flingSpeed = 500f;
                            switch (mRemoveMode) {
                                case FLING_RIGHT_REMOVE:
                                    if (velocityX > flingSpeed) {
                                        mDslv.stopDrag(true);
                                    }
                                    break;
                                case FLING_LEFT_REMOVE:
                                    if (velocityX < -flingSpeed) {
                                        mDslv.stopDrag(true);
                                    }
                                    break;
                            }
                        }
                        return false;
                    }
                };
        mFlingRemoveDetector = new GestureDetector(dslv.getContext(), flingRemoveListener);
        mFlingRemoveDetector.setIsLongpressEnabled(false);
        mTouchSlop = ViewConfiguration.get(dslv.getContext()).getScaledTouchSlop();
        mDragHandleId = dragHandleId;
        setRemoveMode(removeMode);
        setDragInitMode(dragInitMode);
        mOrigFloatAlpha = dslv.getFloatAlpha();
    }

    /**
     * Set how a drag is initiated. Needs to be one of ON_DOWN,
     * ON_DRAG, or ON_LONG_PRESS.
     *
     * @param mode The drag init mode.
     */
    public void setDragInitMode(int mode) {
        mDragInitMode = mode;
    }

    /**
     * Enable/Disable list item sorting. Disabling is useful if only item
     * removal is desired. Prevents drags in the vertical direction.
     *
     * @param enabled Set <code>true</code> to enable list item sorting.
     */
    public void setSortEnabled(boolean enabled) {
        mSortEnabled = enabled;
    }

    /**
     * @return True if sort is enabled, false otherwise.
     */
    public boolean isSortEnabled() {
        return mSortEnabled;
    }

    /**
     * One of FLING_RIGHT_REMOVE, FLING_LEFT_REMOVE, SLIDE_RIGHT_REMOVE, or SLIDE_LEFT_REMOVE.
     */
    public void setRemoveMode(int mode) {
        mRemoveMode = mode;
    }

    /**
     * Enable/Disable item removal without affecting remove mode.
     */
    public void setRemoveEnabled(boolean enabled) {
        mRemoveEnabled = enabled;
    }

    /**
     * Sets flags to restrict certain motions of the floating View based on
     * DragSortController settings (such as remove mode). Starts the drag on the
     * DragSortListView.
     *
     * @param position The list item position (includes headers).
     * @param deltaX   Touch x-coord minus left edge of floating View.
     * @param deltaY   Touch y-coord minus top edge of floating View.
     * @return True if drag started, false otherwise.
     */
    public boolean startDrag(int position, int deltaX, int deltaY) {

        int dragFlags = 0;
        if (mSortEnabled) {
            dragFlags |= DragSortListView.DRAG_POS_Y | DragSortListView.DRAG_NEG_Y;
        }

        if (mRemoveEnabled) {
            if (mRemoveMode == FLING_RIGHT_REMOVE) {
                dragFlags |= DragSortListView.DRAG_POS_X;
            } else if (mRemoveMode == FLING_LEFT_REMOVE) {
                dragFlags |= DragSortListView.DRAG_NEG_X;
            }
        }

        mDragging = mDslv.startDrag(position - mDslv.getHeaderViewsCount(),
                dragFlags, deltaX, deltaY);
        return mDragging;
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        mDetector.onTouchEvent(ev);
        if (mRemoveEnabled && mDragging
                && (mRemoveMode == FLING_RIGHT_REMOVE || mRemoveMode == FLING_LEFT_REMOVE)) {
            mFlingRemoveDetector.onTouchEvent(ev);
        }

        final int mAction = ev.getAction() & MotionEvent.ACTION_MASK;

        switch (mAction) {
            case MotionEvent.ACTION_DOWN:
                mCurrX = (int) ev.getX();
                mCurrY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (mRemoveEnabled) {
                    final int x = (int) ev.getX();
                    int thirdW = mDslv.getWidth() / 3;
                    int twoThirdW = mDslv.getWidth() - thirdW;
                    if ((mRemoveMode == SLIDE_RIGHT_REMOVE && x > twoThirdW)
                            || (mRemoveMode == SLIDE_LEFT_REMOVE && x < thirdW)) {
                        mDslv.stopDrag(true);
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                break;
        }
        return false;
    }

    /**
     * Overrides to provide fading when slide removal is enabled.
     */
    @Override
    public void onDragFloatView(View floatView, Point position, Point touch) {
        if (mRemoveEnabled) {
            int x = touch.x;
            if (mRemoveMode == SLIDE_RIGHT_REMOVE) {
                int width = mDslv.getWidth();
                int thirdWidth = width / 3;

                float alpha;
                if (x < thirdWidth) {
                    alpha = 1.0f;
                } else if (x < width - thirdWidth) {
                    alpha = ((float) (width - thirdWidth - x)) / ((float) thirdWidth);
                } else {
                    alpha = 0.0f;
                }
                mDslv.setFloatAlpha(mOrigFloatAlpha * alpha);
            } else if (mRemoveMode == SLIDE_LEFT_REMOVE) {
                int width = mDslv.getWidth();
                int thirdWidth = width / 3;

                float alpha;
                if (x < thirdWidth) {
                    alpha = 0.0f;
                } else if (x < width - thirdWidth) {
                    alpha = ((float) (x - thirdWidth)) / ((float) thirdWidth);
                } else {
                    alpha = 1.0f;
                }
                mDslv.setFloatAlpha(mOrigFloatAlpha * alpha);
            }
        }
    }

    /**
     * Get the position to start dragging based on the ACTION_DOWN MotionEvent.
     * This function simply calls {@link #dragHandleHitPosition(MotionEvent)}.
     * Override to change drag handle behavior; this function is called
     * internally when an ACTION_DOWN event is detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.
     * @return The list position to drag if a drag-init gesture is detected;
     * MISS if unsuccessful.
     */
    public int startDragPosition(MotionEvent ev) {
        return dragHandleHitPosition(ev);
    }

    /**
     * Checks for the touch of an item's drag handle and returns that item's position if a
     * drag handle touch was detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.
     * @return The list position of the item whose drag handle was touched; MISS
     * if unsuccessful.
     */
    public int dragHandleHitPosition(MotionEvent ev) {
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        int touchPos = mDslv.pointToPosition(x, y);

        final int numHeaders = mDslv.getHeaderViewsCount();
        final int numFooters = mDslv.getFooterViewsCount();
        final int count = mDslv.getCount();

        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders
                && touchPos < (count - numFooters)) {
            final View item = mDslv.getChildAt(touchPos - mDslv.getFirstVisiblePosition());
            final int rawX = (int) ev.getRawX();
            final int rawY = (int) ev.getRawY();

            View dragBox = item.findViewById(mDragHandleId);
            if (dragBox != null) {
                dragBox.getLocationOnScreen(mTempLoc);

                if (rawX > mTempLoc[0] && rawY > mTempLoc[1]
                        && rawX < mTempLoc[0] + dragBox.getWidth()
                        && rawY < mTempLoc[1] + dragBox.getHeight()) {

                    mItemX = item.getLeft();
                    mItemY = item.getTop();

                    return touchPos;
                }
            }
        }
        return MISS;
    }

    @Override
    public boolean onDown(MotionEvent ev) {
        mHitPos = startDragPosition(ev);

        if (mHitPos != MISS && mDragInitMode == ON_DOWN) {
            startDrag(mHitPos, (int) ev.getX() - mItemX, (int) ev.getY() - mItemY);
        }

        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mHitPos != MISS && mDragInitMode == ON_DRAG && !mDragging) {
            final int x1 = (int) e1.getX();
            final int y1 = (int) e1.getY();
            final int x2 = (int) e2.getX();
            final int y2 = (int) e2.getY();

            boolean start = false;
            if (mRemoveEnabled && mSortEnabled) {
                start = true;
            } else if (mRemoveEnabled) {
                start = Math.abs(x2 - x1) > mTouchSlop;
            } else if (mSortEnabled) {
                start = Math.abs(y2 - y1) > mTouchSlop;
            }

            if (start) {
                startDrag(mHitPos, x2 - mItemX, y2 - mItemY);
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mHitPos != MISS && mDragInitMode == ON_LONG_PRESS) {
            mDslv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            startDrag(mHitPos, mCurrX - mItemX, mCurrY - mItemY);
        }
    }

    @Override
    public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent ev) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent ev) {
    }
}
