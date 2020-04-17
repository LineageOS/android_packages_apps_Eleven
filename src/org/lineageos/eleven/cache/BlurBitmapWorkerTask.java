/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
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
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.widget.ImageView;

import org.lineageos.eleven.cache.ImageWorker.ImageType;
import org.lineageos.eleven.widgets.AlbumScrimImage;

import java.lang.ref.WeakReference;

/**
 * This will download the image (if needed) and create a blur and set the scrim as well on the
 * BlurScrimImage
 */
public class BlurBitmapWorkerTask extends BitmapWorkerTask<String, Void, BlurBitmapWorkerTask.ResultContainer> {

    private static final String TAG = BlurBitmapWorkerTask.class.getSimpleName();

    // if the image is too small, the blur will look bad post scale up so we use the min size
    // to scale up before bluring
    private static final int MIN_BITMAP_SIZE = 500;
    // number of times to run the blur
    private static final int NUM_BLUR_RUNS = 8;
    // 25f is the max blur radius possible
    private static final float BLUR_RADIUS = 25f;

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
     * RenderScript used to blur the image
     */
    protected final RenderScript mRenderScript;

    /**
     * Constructor of <code>BlurBitmapWorkerTask</code>
     * @param key used for caching the image
     * @param albumScrimImage The {@link AlbumScrimImage} to use.
     * @param imageType The type of image URL to fetch for.
     * @param fromDrawable what drawable to transition from
     */
    public BlurBitmapWorkerTask(final String key, final AlbumScrimImage albumScrimImage,
                                final ImageType imageType, final Drawable fromDrawable,
                                final Context context, final RenderScript renderScript) {
        super(key, albumScrimImage.getImageView(), imageType, fromDrawable, context);
        mBlurScrimImage = new WeakReference<>(albumScrimImage);
        mRenderScript = renderScript;

        // use the existing image as the drawable and if it doesn't exist fallback to transparent
        mFromDrawable = albumScrimImage.getImageView().getDrawable();
        if (mFromDrawable == null) {
            mFromDrawable = fromDrawable;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultContainer doInBackground(final String... params) {
        if (isCancelled()) {
            return null;
        }

        Bitmap bitmap = getBitmapInBackground(params);

        ResultContainer result = new ResultContainer();

        Bitmap output = null;

        if (bitmap != null) {
            // now create the blur bitmap
            Bitmap input = bitmap;

            // if the image is too small, scale it up before running through the blur
            if (input.getWidth() < MIN_BITMAP_SIZE || input.getHeight() < MIN_BITMAP_SIZE) {
                float multiplier = Math.max(MIN_BITMAP_SIZE / (float)input.getWidth(),
                        MIN_BITMAP_SIZE / (float)input.getHeight());
                input = input.createScaledBitmap(bitmap, (int)(input.getWidth() * multiplier),
                        (int)(input.getHeight() * multiplier), true);
                // since we created a new bitmap, we can re-use the bitmap for our output
                output = input;
            } else {
                // if we aren't creating a new bitmap, create a new output bitmap
                output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), input.getConfig());
            }

            // run the blur multiple times
            for (int i = 0; i < NUM_BLUR_RUNS; i++) {
                try {
                    final Allocation inputAlloc = Allocation.createFromBitmap(mRenderScript, input);
                    final Allocation outputAlloc = Allocation.createTyped(mRenderScript,
                            inputAlloc.getType());
                    final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(mRenderScript,
                            Element.U8_4(mRenderScript));

                    script.setRadius(BLUR_RADIUS);
                    script.setInput(inputAlloc);
                    script.forEach(outputAlloc);
                    outputAlloc.copyTo(output);

                    // if we run more than 1 blur, the new input should be the old output
                    input = output;
                } catch (RuntimeException e) {
                    Log.w(TAG, "Cannot blur image. " + e.getMessage());
                    break;
                }
            }

            // Set the scrim color to be 50% gray
            result.mPaletteColor = 0x7f000000;

            // create the bitmap transition drawable
            result.mImageViewBitmapDrawable = createImageTransitionDrawable(output,
                    ImageWorker.FADE_IN_TIME_SLOW, true, true);

            return result;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
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
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final ImageView getAttachedImageView() {
        final AlbumScrimImage blurImage  = mBlurScrimImage.get();
        final BitmapWorkerTask bitmapWorkerTask = ImageWorker.getBitmapWorkerTask(blurImage);
        if (this == bitmapWorkerTask) {
            return blurImage.getImageView();
        }
        return null;
    }
}
