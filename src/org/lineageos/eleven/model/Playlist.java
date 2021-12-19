/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2019-2021 The LineageOS Project
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

package org.lineageos.eleven.model;

import java.util.Comparator;
import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * A class that represents a playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Playlist {

    /**
     * The unique Id of the playlist
     */
    public final long mPlaylistId;

    /**
     * The playlist name
     */
    public String mPlaylistName;

    /**
     * The number of songs in this playlist
     */
    public final int mSongCount;

    /**
     * Constructor of <code>Genre</code>
     *
     * @param playlistId   The Id of the playlist
     * @param playlistName The playlist name
     */
    public Playlist(final long playlistId, final String playlistName, final int songCount) {
        super();
        mPlaylistId = playlistId;
        mPlaylistName = playlistName;
        mSongCount = songCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return mPlaylistId == playlist.mPlaylistId &&
                mSongCount == playlist.mSongCount &&
                Objects.equals(mPlaylistName, playlist.mPlaylistName);
    }

    @NonNull
    @Override
    public String toString() {
        return "Playlist[playlistId=" + mPlaylistId
                + ", playlistName=" + mPlaylistName
                + ", songCount=" + mSongCount
                + "]";
    }

    /**
     * @return true if this is a smart playlist
     */
    public boolean isSmartPlaylist() {
        return mPlaylistId < 0;
    }

    public static class IgnoreCaseComparator implements Comparator<Playlist> {
        @Override
        public int compare(Playlist p1, Playlist p2) {
            return p1.mPlaylistName.compareToIgnoreCase(p2.mPlaylistName);
        }
    }
}
