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
package org.lineageos.eleven.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

import org.lineageos.eleven.cache.ImageWorker.ImageType;
import org.lineageos.eleven.widgets.AlbumScrimImage;

import java.lang.ref.WeakReference;

/**
 * This will download the image (if needed) and create a blur and set the scrim as well on the
 * BlurScrimImage
 */
public class BlurBitmapWorkerTask extends BitmapWorkerTask<String, Void,
        BlurBitmapWorkerTask.ResultContainer> {

    private static final String TAG = BlurBitmapWorkerTask.class.getSimpleName();

    // container for the result
    public static class ResultContainer {
        public TransitionDrawable mImageViewBitmapDrawable;
        public int mPaletteColor;
    }

    /**
     * The {@link org.lineageos.eleven.widgets.AlbumScrimImage} used to set the result
     */
    private final WeakReference<AlbumScrimImage> mBlurScrimImage;

    /**
     * Constructor of <code>BlurBitmapWorkerTask</code>
     *
     * @param key             used for caching the image
     * @param albumScrimImage The {@link AlbumScrimImage} to use.
     * @param imageType       The type of image URL to fetch for.
     * @param fromDrawable    what drawable to transition from
     */
    public BlurBitmapWorkerTask(final String key, final AlbumScrimImage albumScrimImage,
                                final ImageType imageType, final Drawable fromDrawable,
                                final Context context) {
        super(key, albumScrimImage.getImageView(), imageType, fromDrawable, context);
        mBlurScrimImage = new WeakReference<>(albumScrimImage);

        // use the existing image as the drawable and if it doesn't exist fallback to transparent
        mFromDrawable = albumScrimImage.getImageView().getDrawable();
        if (mFromDrawable == null) {
            mFromDrawable = fromDrawable;
        }
    }

    @Override
    protected ResultContainer doInBackground(final String... params) {
        if (isCancelled()) {
            return null;
        }

        final ResultContainer result = new ResultContainer();
        final Bitmap bitmap = getBitmapInBackground(params);
        if (bitmap != null) {
            // Set the scrim color to be 50% gray
            result.mPaletteColor = 0x7f000000;

            // create the bitmap transition drawable
            result.mImageViewBitmapDrawable = createImageTransitionDrawable(bitmap,
                    ImageWorker.FADE_IN_TIME_SLOW, true);

            return result;
        }

        return null;
    }

    @Override
    protected void onPostExecute(ResultContainer resultContainer) {
        AlbumScrimImage albumScrimImage = mBlurScrimImage.get();
        if (albumScrimImage != null) {
            if (resultContainer == null) {
                // if we have no image, then signal the transition to the default state
                albumScrimImage.transitionToDefaultState();
            } else {
                // create the palette transition
                TransitionDrawable paletteTransition = ImageWorker.createPaletteTransition(
                        albumScrimImage,
                        resultContainer.mPaletteColor);

                // set the transition drawable
                albumScrimImage.setTransitionDrawable(resultContainer.mImageViewBitmapDrawable,
                        paletteTransition);
                albumScrimImage.applyBlurEffect();
            }
        }
    }

    @Override
    protected final ImageView getAttachedImageView() {
        final AlbumScrimImage blurImage = mBlurScrimImage.get();
        final BitmapWorkerTask<?, ?, ?> bitmapWorkerTask =
                ImageWorker.getBitmapWorkerTask(blurImage);
        if (this == bitmapWorkerTask) {
            return blurImage.getImageView();
        }
        return null;
    }
}
