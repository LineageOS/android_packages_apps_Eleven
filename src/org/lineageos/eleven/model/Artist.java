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
 * A class that represents an artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Artist {

    /**
     * The unique Id of the artist
     */
    public final long mArtistId;

    /**
     * The artist name
     */
    public final String mArtistName;

    /**
     * The number of albums for the artist
     */
    public final int mAlbumNumber;

    /**
     * The number of songs for the artist
     */
    public final int mSongNumber;

    /**
     * Bucket label for the artist name if it exists
     */
    public String mBucketLabel;

    /**
     * Constructor of <code>Artist</code>
     *
     * @param artistId    The Id of the artist
     * @param artistName  The artist name
     * @param songNumber  The number of songs for the artist
     * @param albumNumber The number of albums for the artist
     */
    public Artist(final long artistId, final String artistName, final int songNumber,
                  final int albumNumber) {
        super();
        mArtistId = artistId;
        mArtistName = artistName;
        mSongNumber = songNumber;
        mAlbumNumber = albumNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return mArtistId == artist.mArtistId &&
                mAlbumNumber == artist.mAlbumNumber &&
                mSongNumber == artist.mSongNumber &&
                Objects.equals(mArtistName, artist.mArtistName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mArtistId, mArtistName, mAlbumNumber, mSongNumber);
    }

    @NonNull
    @Override
    public String toString() {
        return mArtistName;
    }
}
