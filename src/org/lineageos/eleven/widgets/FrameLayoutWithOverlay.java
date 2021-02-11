/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.lineageos.eleven.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A FrameLayout whose contents are kept beneath an
 * {@link AlphaTouchInterceptorOverlay}. If necessary, you can specify your own
 * alpha-layer and manually manage its z-order.
 */
public class FrameLayoutWithOverlay extends FrameLayout {

    private final AlphaTouchInterceptorOverlay mOverlay;

    /**
     * @param context The {@link Context} to use
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public FrameLayoutWithOverlay(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        /* Programmatically create touch-interceptor View. */
        mOverlay = new AlphaTouchInterceptorOverlay(context);

        addView(mOverlay);
    }

    /**
     * After adding the View, bring the overlay to the front to ensure it's
     * always on top.
     */
    @Override
    public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        mOverlay.bringToFront();
    }
}
