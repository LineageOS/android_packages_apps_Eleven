/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2021 The LineageOS Project
 * Copyright (C) 2021 SHIFT GmbH
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
package org.lineageos.eleven.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class ImageUtils {
    /**
     * Scale the bitmap to an image view. The bitmap will fill the image view bounds.
     * The bitmap will be scaled while maintaining the aspect ratio and cropped if it exceeds
     * the image-view bounds.
     */
    public static Bitmap scaleBitmapForImageView(Bitmap src, ImageView imageView) {
        if (src == null || imageView == null) {
            return src;
        }
        // get bitmap properties
        int srcHeight = src.getHeight();
        int srcWidth = src.getWidth();

        // get image view bounds
        int viewHeight = imageView.getHeight();
        int viewWidth = imageView.getWidth();

        int deltaWidth = viewWidth - srcWidth;
        int deltaHeight = viewHeight - srcHeight;

        if (deltaWidth <= 0 && deltaHeight <= 0) {
            // nothing to do if src bitmap is bigger than image-view
            return src;
        }

        // scale bitmap along the dimension that is lacking the greatest
        float scale = Math.max(((float) viewWidth) / srcWidth, ((float) viewHeight) / srcHeight);

        // calculate the new bitmap dimensions
        int dstHeight = (int) Math.ceil(srcHeight * scale);
        int dstWidth = (int) Math.ceil(srcWidth * scale);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, false);

        return Bitmap.createBitmap(scaledBitmap, 0, 0, viewWidth, viewHeight);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        final Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
