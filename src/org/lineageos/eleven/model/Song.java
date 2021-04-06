/*
 * Copyright (C) 2012 Andrew Neal
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

package org.lineageos.eleven.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A class that represents a song.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Song {

    /**
     * The unique Id of the song
     */
    public long mSongId;

    /**
     * The song name
     */
    public String mSongName;

    /**
     * The song artist
     */
    public String mArtistName;

    /**
     * The song album
     */
    public String mAlbumName;

    /**
     * The album id
     */
    public long mAlbumId;

    /**
     * The song duration in seconds
     */
    public int mDuration;

    /**
     * The year the song was recorded
     */
    public int mYear;

    /**
     * Bucket label for the name - may not necessarily be the name - for example songs sorted by
     * artists would be the artist bucket label and not the song name bucket label
     */
    public String mBucketLabel;

    /**
     * Constructor of <code>Song</code>
     *
     * @param songId     The Id of the song
     * @param songName   The name of the song
     * @param artistName The song artist
     * @param albumName  The song album
     * @param duration   The duration of a song in seconds
     * @param year       The year the song was recorded
     */
    public Song(final long songId, final String songName, final String artistName,
                final String albumName, final long albumId, final int duration, final int year) {
        mSongId = songId;
        mSongName = songName;
        mArtistName = artistName;
        mAlbumName = albumName;
        mAlbumId = albumId;
        mDuration = duration;
        mYear = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return mSongId == song.mSongId &&
                mAlbumId == song.mAlbumId &&
                mDuration == song.mDuration &&
                mYear == song.mYear &&
                Objects.equals(mSongName, song.mSongName) &&
                Objects.equals(mArtistName, song.mArtistName) &&
                Objects.equals(mAlbumName, song.mAlbumName);
    }

    @NonNull
    @Override
    public String toString() {
        return mSongName;
    }
}
