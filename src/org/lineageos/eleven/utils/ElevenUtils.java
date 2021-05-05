/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2018-2021 The LineageOS Project
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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import org.lineageos.eleven.cache.ImageCache;
import org.lineageos.eleven.cache.ImageFetcher;

/**
 * Mostly general and UI helpers.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class ElevenUtils {

    /* This class is never initiated */
    public ElevenUtils() {
    }

    /**
     * Used to determine if the device is currently in landscape mode
     *
     * @param context The {@link Context} to use.
     * @return True if the device is in landscape mode, false otherwise.
     */
    public static boolean isLandscape(final Context context) {
        final int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Execute an {@link AsyncTask} on a thread pool
     *
     * @param task        Task to execute
     * @param args        Optional arguments to pass to
     *                    {@link AsyncTask#execute(Object[])}
     */
    @SafeVarargs
    public static <T> void execute(final AsyncTask<T, ?, ?> task, final T... args) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    }

    /**
     * Display a {@link Toast} letting the user know what an item does when long
     * pressed.
     *
     * @param view The {@link View} to copy the content description from.
     */
    public static void showCheatSheet(final View view) {

        final int[] screenPos = new int[2]; // origin is device display
        final Rect displayFrame = new Rect(); // includes decorations (e.g.
        // status bar)
        view.getLocationOnScreen(screenPos);
        view.getWindowVisibleDisplayFrame(displayFrame);

        final Context context = view.getContext();
        final int viewWidth = view.getWidth();
        final int viewHeight = view.getHeight();
        final int viewCenterX = screenPos[0] + viewWidth / 2;
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        final int estimatedToastHeight =
                (int) (48 * context.getResources().getDisplayMetrics().density);

        final Toast cheatSheet = Toast.makeText(context, view.getContentDescription(),
                Toast.LENGTH_SHORT);
        final boolean showBelow = screenPos[1] < estimatedToastHeight;
        if (showBelow) {
            // Show below
            // Offsets are after decorations (e.g. status bar) are factored in
            cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, viewCenterX
                    - screenWidth / 2, screenPos[1] - displayFrame.top + viewHeight);
        } else {
            // Show above
            // Offsets are after decorations (e.g. status bar) are factored in
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, viewCenterX
                    - screenWidth / 2, displayFrame.bottom - screenPos[1]);
        }
        cheatSheet.show();
    }

    /**
     * Creates a new instance of the {@link ImageCache} and {@link ImageFetcher}
     *
     * @param activity The {@link Activity} to use.
     * @return A new {@link ImageFetcher} used to fetch images asynchronously.
     */
    public static ImageFetcher getImageFetcher(final FragmentActivity activity) {
        final ImageFetcher imageFetcher = ImageFetcher.getInstance(activity);
        imageFetcher.setImageCache(ImageCache.findOrCreateCache(activity));
        return imageFetcher;
    }

    /**
     * Gets the action bar height in pixels
     *
     * @return action bar height in pixels
     */
    public static int getActionBarHeight(Context context) {
        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data,
                    context.getResources().getDisplayMetrics());
        }

        return 0;
    }
}
