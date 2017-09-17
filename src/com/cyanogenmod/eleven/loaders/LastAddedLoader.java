/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.cyanogenmod.eleven.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import com.cyanogenmod.eleven.model.Song;
import com.cyanogenmod.eleven.sectionadapter.SectionCreator;
import com.cyanogenmod.eleven.utils.Lists;
import com.cyanogenmod.eleven.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query {@link MediaStore.Audio.Media.EXTERNAL_CONTENT_URI} and return
 * the Song the user added over the past four of weeks.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class LastAddedLoader extends SectionCreator.SimpleListLoader<Song> {
    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * Constructor of <code>LastAddedHandler</code>
     *
     * @param context The {@link Context} to use.
     */
    public LastAddedLoader(final Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the xCursor
        Cursor cursor = makeLastAddedCursor(getContext());
        // Gather the data
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Copy the song Id
                final long id = cursor.getLong(0);

                // Copy the song name
                final String songName = cursor.getString(1);

                // Copy the artist name
                final String artist = cursor.getString(2);

                // Copy the album id
                final long albumId = cursor.getLong(3);

                // Copy the album name
                final String album = cursor.getString(4);

                // Copy the duration
                final long duration = cursor.getLong(5);

                // Convert the duration into seconds
                final int durationInSecs = (int) duration / 1000;

                // Grab the Song Year
                final int year = cursor.getInt(6);

                // Create a new song
                final Song song = new Song(id, songName, artist, album, albumId, durationInSecs, year);

                // Add everything up
                mSongList.add(song);
            } while (cursor.moveToNext());
        }
        // Close the cursor
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        return mSongList;
    }

    /**
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the song query.
     */
    public static final Cursor makeLastAddedCursor(final Context context) {
        // timestamp of four weeks ago
        long fourWeeksAgo = (System.currentTimeMillis() / 1000) - (4 * 3600 * 24 * 7);
        // possible saved timestamp caused by user "clearing" the last added playlist
        long cutoff = PreferenceUtils.getInstance(context).getLastAddedCutoff() / 1000;
        // use the most recent of the two timestamps
        if(cutoff < fourWeeksAgo) { cutoff = fourWeeksAgo; }

        String selection = (AudioColumns.IS_MUSIC + "=1") +
                " AND " + AudioColumns.TITLE + " != ''" +
                " AND " + MediaStore.Audio.Media.DATE_ADDED + ">" +
                cutoff;

        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] {
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        AudioColumns.TITLE,
                        /* 2 */
                        AudioColumns.ARTIST,
                        /* 3 */
                        AudioColumns.ALBUM_ID,
                        /* 4 */
                        AudioColumns.ALBUM,
                        /* 5 */
                        AudioColumns.DURATION,
                        /* 6 */
                        AudioColumns.YEAR,
                }, selection, null, MediaStore.Audio.Media.DATE_ADDED + " DESC");
    }
}
