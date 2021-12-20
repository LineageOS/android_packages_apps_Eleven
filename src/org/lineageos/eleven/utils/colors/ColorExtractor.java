/*
 * Copyright (C) 2019-2021 The LineageOS Project
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

import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.utils.MusicUtils;

import java.util.concurrent.Callable;

public class ColorExtractor {
    public interface Callback {
        void onColorExtracted(final BitmapWithColors bitmapWithColors);
    }

    public static class ColorExtractionTask implements Callable<BitmapWithColors> {
        private final ImageFetcher imageFetcher;

        public ColorExtractionTask(final ImageFetcher imageFetcher) {
            this.imageFetcher = imageFetcher;
        }

        @Override
        public BitmapWithColors call() {
            if (imageFetcher == null) {
                return null;
            }
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
    }
}
