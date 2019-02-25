/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2019 The LineageOS Project
 * Copyright (C) 2019 SHIFT GmbH
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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageWorker;

public class AlbumScrimImage extends FrameLayout {
    private static final int COLOR_GREY_30 = 0x4c000000;

    private ImageView mImageView;
    private View mScrimView;

    private boolean mUsingDefaultBlur;

    public AlbumScrimImage(Context context, AttributeSet attrs) {
        super(context, attrs);

        mUsingDefaultBlur = true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mImageView = findViewById(R.id.blurImage);
        mScrimView = findViewById(R.id.blurScrim);
    }

    public ImageView getImageView() {
        return mImageView;
    }

    /**
     * Transitions the image to the default state (default blur artwork)
     */
    public void transitionToDefaultState() {
        // if we are already showing the default blur and we are transitioning to the default blur
        // then don't do the transition at all
        if (mUsingDefaultBlur) {
            return;
        }

        Bitmap blurredBitmap = ((BitmapDrawable) ContextCompat.getDrawable(
                getContext(), R.drawable.default_artwork_blur)).getBitmap();

        TransitionDrawable imageTransition = ImageWorker.createImageTransitionDrawable(
                getResources(), mImageView.getDrawable(), blurredBitmap,
                ImageWorker.FADE_IN_TIME_SLOW, true, true);

        TransitionDrawable paletteTransition = ImageWorker.createPaletteTransition(this,
                Color.TRANSPARENT);

        setTransitionDrawable(imageTransition, paletteTransition);
        mUsingDefaultBlur = true;
    }

    /**
     * Sets the transition drawable
     *
     * @param imageTransition   the transition for the imageview
     * @param paletteTransition the transition for the scrim overlay
     */
    public void setTransitionDrawable(TransitionDrawable imageTransition,
            TransitionDrawable paletteTransition) {
        mScrimView.setBackground(paletteTransition);
        mImageView.setImageDrawable(imageTransition);
        mUsingDefaultBlur = false;
    }

    public void setGradientDrawable(GradientDrawable gradientDrawable) {
        // set scrim view to 30% grey to prevent full white layouts
        final ColorDrawable scrimDrawable = new ColorDrawable(COLOR_GREY_30);
        mScrimView.setBackground(scrimDrawable);

        mImageView.setImageDrawable(gradientDrawable);
        mUsingDefaultBlur = false;
    }
}
