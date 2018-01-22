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

package org.lineageos.eleven.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Artists;

import org.lineageos.eleven.model.Artist;
import org.lineageos.eleven.provider.LocalizedStore;
import org.lineageos.eleven.provider.LocalizedStore.SortParameter;
import org.lineageos.eleven.sectionadapter.SectionCreator;
import org.lineageos.eleven.utils.Lists;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PreferenceUtils;
import org.lineageos.eleven.utils.SortOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query {@link MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI} and
 * return the artists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistLoader extends SectionCreator.SimpleListLoader<Artist> {

    /**
     * The result
     */
    private ArrayList<Artist> mArtistsList = Lists.newArrayList();

    /**
     * Constructor of <code>ArtistLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public ArtistLoader(final Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Artist> loadInBackground() {
        // Create the Cursor
        Cursor cursor = makeArtistCursor(getContext());
        // Gather the data
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Copy the artist id
                final long id = cursor.getLong(0);

                // Copy the artist name
                final String artistName = cursor.getString(1);

                // Copy the number of albums
                final int albumCount = cursor.getInt(2);

                // Copy the number of songs
                final int songCount = cursor.getInt(3);

                // as per designer's request, don't show unknown artist
                if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
                    continue;
                }

                // Create a new artist
                final Artist artist = new Artist(id, artistName, songCount, albumCount);

                if (cursor instanceof SortedCursor) {
                    artist.mBucketLabel = (String)((SortedCursor) cursor).getExtraData();
                }

                mArtistsList.add(artist);
            } while (cursor.moveToNext());
        }
        // Close the cursor
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }

        return mArtistsList;
    }

    /**
     * For string-based sorts, return the localized store sort parameter, otherwise return null
     * @param sortOrder the song ordering preference selected by the user
     */
    private static LocalizedStore.SortParameter getSortParameter(String sortOrder) {
        if (sortOrder.equals(SortOrder.ArtistSortOrder.ARTIST_A_Z) ||
                sortOrder.equals(SortOrder.ArtistSortOrder.ARTIST_Z_A)) {
            return LocalizedStore.SortParameter.Artist;
        }

        return null;
    }
    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the artist query.
     */
    public static final Cursor makeArtistCursor(final Context context) {
        // requested artist ordering
        final String artistSortOrder = PreferenceUtils.getInstance(context).getArtistSortOrder();

        Cursor cursor = context.getContentResolver().query(Artists.EXTERNAL_CONTENT_URI,
                new String[] {
                        /* 0 */
                        Artists._ID,
                        /* 1 */
                        Artists.ARTIST,
                        /* 2 */
                        Artists.NUMBER_OF_ALBUMS,
                        /* 3 */
                        Artists.NUMBER_OF_TRACKS
                }, null, null, artistSortOrder);

        // if our sort is a localized-based sort, grab localized data from the store
        final SortParameter sortParameter = getSortParameter(artistSortOrder);
        if (sortParameter != null && cursor != null) {
            final boolean descending = MusicUtils.isSortOrderDesending(artistSortOrder);
            return LocalizedStore.getInstance(context).getLocalizedSort(cursor, Artists._ID,
                    SortParameter.Artist, sortParameter, descending, true);
        }

        return cursor;
    }
}
