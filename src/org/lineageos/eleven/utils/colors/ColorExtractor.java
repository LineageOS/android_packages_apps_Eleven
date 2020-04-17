/*
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
package org.lineageos.eleven.utils.colors;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.utils.MusicUtils;

public class ColorExtractor {
    public interface Callback {
        void onColorExtracted(final BitmapWithColors bitmapWithColors);
    }

    public static void extractColors(final Bitmap bitmap, final int bitmapKey,
            final ColorExtractor.Callback callback) {
        new ColorExtractionTask(bitmap, bitmapKey, callback).execute();
    }

    public static void extractColors(final ImageFetcher imageFetcher,
            final ColorExtractor.Callback callback) {
        new ColorExtractionTask(imageFetcher, callback).execute();
    }

    private static class ColorExtractionTask extends AsyncTask<Void, Void, BitmapWithColors> {
        private final ImageFetcher imageFetcher;
        private final ColorExtractor.Callback callback;

        private Bitmap bitmap;
        private int bitmapKey;

        ColorExtractionTask(final Bitmap bitmap, final int bitmapKey,
                final ColorExtractor.Callback callback) {
            this.bitmap = bitmap;
            this.bitmapKey = bitmapKey;

            this.imageFetcher = null;
            this.callback = callback;
        }

        ColorExtractionTask(final ImageFetcher imageFetcher,
                final ColorExtractor.Callback callback) {
            this.imageFetcher = imageFetcher;
            this.callback = callback;
        }

        @Override
        protected BitmapWithColors doInBackground(Void... voids) {
            if (bitmap == null && imageFetcher != null) {
                final String albumName = MusicUtils.getAlbumName();
                final long albumId = MusicUtils.getCurrentAlbumId();
                final String artistName = MusicUtils.getArtistName();

                // We are not playing anything, return null. Otherwise we will
                // potentially override any default colors.
                if (albumName == null && artistName == null && albumId == -1) {
                    return null;
                }

                return imageFetcher.getArtwork(albumName, albumId, artistName, true);
            }

            if (bitmap != null) {
                return new BitmapWithColors(bitmap, bitmapKey);
            }

            return null;
        }

        @Override
        protected void onPostExecute(final BitmapWithColors bitmapWithColors) {
            if (callback != null) {
                callback.onColorExtracted(bitmapWithColors);
            }
        }
    }
}
