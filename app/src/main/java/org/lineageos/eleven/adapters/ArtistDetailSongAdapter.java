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
package org.lineageos.eleven.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Song;

public class ArtistDetailSongAdapter extends DetailSongAdapter {
    public ArtistDetailSongAdapter(FragmentActivity activity) {
        super(activity);
    }

    @Override
    protected int rowLayoutId() {
        return R.layout.artist_detail_song;
    }

    @Override
    protected Config.IdType getSourceType() {
        return Config.IdType.Artist;
    }

    @Override
    protected Holder newHolder(View root, ImageFetcher fetcher) {
        return new ArtistHolder(root, fetcher);
    }

    private static class ArtistHolder extends Holder {
        final ImageView art;
        final TextView album;

        protected ArtistHolder(View root, ImageFetcher fetcher) {
            super(root, fetcher);
            art = root.findViewById(R.id.album_art);
            album = root.findViewById(R.id.album);
        }

        protected void update(Song song) {
            title.setText(song.mSongName);
            album.setText(song.mAlbumName);

            if (song.mAlbumId >= 0) {
                fetcher.loadAlbumImage(song.mArtistName, song.mAlbumName, song.mAlbumId, art);
            }
        }
    }
}
