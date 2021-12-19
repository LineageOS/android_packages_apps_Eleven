/*
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
package org.lineageos.eleven.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.lineageos.eleven.utils.MusicUtils;

/**
 * This db stores the details to generate the playlist artwork including when it was
 * last updated and the # of songs in the playlist when it last updated
 */
public class PlaylistArtworkStore {
    private static final long ONE_DAY_IN_MS = 1000 * 60 * 60 * 24;

    private static PlaylistArtworkStore sInstance = null;

    private final Context mContext;
    private final MusicDB mMusicDatabase;

    /**
     * @param context The {@link android.content.Context} to use
     * @return A new instance of this class.
     */
    public static synchronized PlaylistArtworkStore getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new PlaylistArtworkStore(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * @param playlistId playlist identifier
     * @return the key used for the imagae cache for the cover art
     */
    public static String getCoverCacheKey(final long playlistId) {
        return "playlist_cover_" + playlistId;
    }

    /**
     * @param playlistId playlist identifier
     * @return the key used for the imagae cache for the top artist image
     */
    public static String getArtistCacheKey(final long playlistId) {
        return "playlist_artist_" + playlistId;
    }

    /**
     * Constructor of <code>RecentStore</code>
     *
     * @param context The {@link android.content.Context} to use
     */
    public PlaylistArtworkStore(final Context context) {
        mContext = context;
        mMusicDatabase = MusicDB.getInstance(context);
    }

    public void onCreate(final SQLiteDatabase db) {
        // create the table
        String builder = "CREATE TABLE IF NOT EXISTS " +
                PlaylistArtworkStoreColumns.NAME +
                "(" +
                PlaylistArtworkStoreColumns.ID +
                " INT UNIQUE," +
                PlaylistArtworkStoreColumns.LAST_UPDATE_ARTIST +
                " LONG DEFAULT 0," +
                PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_ARTIST +
                " INT DEFAULT 0," +
                PlaylistArtworkStoreColumns.LAST_UPDATE_COVER +
                " LONG DEFAULT 0," +
                PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_COVER +
                " INT DEFAULT 0);";

        db.execSQL(builder);
    }

    public void onDowngrade(SQLiteDatabase db) {
        // If we ever have downgrade, drop the table to be safe
        db.execSQL("DROP TABLE IF EXISTS " + PlaylistArtworkStoreColumns.NAME);
        onCreate(db);
    }

    /**
     * @param playlistId playlist identifier
     * @return true if the artist artwork should be updated based on time since last update and
     * whether the # of songs for the playlist has changed
     */
    public boolean needsArtistArtUpdate(final long playlistId) {
        return needsUpdate(playlistId, PlaylistArtworkStoreColumns.LAST_UPDATE_ARTIST,
                PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_ARTIST);
    }

    /**
     * @param playlistId playlist identifier
     * @return true if the cover artwork should be updated based on time since last update and
     * whether the # of songs for the playlist has changed
     */
    public boolean needsCoverArtUpdate(final long playlistId) {
        return needsUpdate(playlistId, PlaylistArtworkStoreColumns.LAST_UPDATE_COVER,
                PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_COVER);
    }

    /**
     * Updates the time and the # of songs in the db for the artist section of the table
     *
     * @param playlistId playlist identifier
     */
    public void updateArtistArt(final long playlistId) {
        updateOrInsertTime(playlistId,
                PlaylistArtworkStoreColumns.LAST_UPDATE_ARTIST,
                PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_ARTIST);
    }

    /**
     * Updates the time and the # of songs in the db for the cover art of the table
     *
     * @param playlistId playlist identifier
     */
    public void updateCoverArt(final long playlistId) {
        updateOrInsertTime(playlistId,
                PlaylistArtworkStoreColumns.LAST_UPDATE_COVER,
                PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_COVER);
    }

    /**
     * Internal function to update the entry for the columns passed in
     *
     * @param playlistId      playlist identifier
     * @param columnName      the column to update to the current time
     * @param countColumnName the column to set the # of songs to based on the playlist
     */
    private void updateOrInsertTime(final long playlistId, final String columnName,
                                    final String countColumnName) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.beginTransaction();

        // gets the existing values for the entry if it exists
        ContentValues values = getExistingContentValues(database, playlistId);
        boolean existingEntry = values.size() > 0;
        // update the values
        values.put(PlaylistArtworkStoreColumns.ID, playlistId);
        values.put(columnName, System.currentTimeMillis());
        values.put(countColumnName, MusicUtils.getSongCountForPlaylist(mContext, playlistId));

        // if it is an existing entry, update, otherwise insert
        if (existingEntry) {
            database.update(PlaylistArtworkStoreColumns.NAME, values,
                    PlaylistArtworkStoreColumns.ID + "=" + playlistId, null);
        } else {
            database.insert(PlaylistArtworkStoreColumns.NAME, null, values);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /**
     * Internal function to get the existing values for a playlist entry
     *
     * @param playlistId playlist identifier
     * @return the content values
     */
    private ContentValues getExistingContentValues(final SQLiteDatabase database,
                                                   final long playlistId) {
        final ContentValues values = new ContentValues(5);
        try (final Cursor c = getEntry(database, playlistId)) {
            if (c != null && c.moveToFirst()) {
                values.put(PlaylistArtworkStoreColumns.ID, c.getLong(0));
                values.put(PlaylistArtworkStoreColumns.LAST_UPDATE_ARTIST, c.getLong(1));
                values.put(PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_ARTIST, c.getInt(2));
                values.put(PlaylistArtworkStoreColumns.LAST_UPDATE_COVER, c.getLong(3));
                values.put(PlaylistArtworkStoreColumns.NUM_SONGS_LAST_UPDATE_COVER, c.getInt(4));
            }
        }

        return values;
    }

    /**
     * Internal function to return whether the columns show that this needs an update
     *
     * @param playlistId      playlist identifier
     * @param columnName      the column to inspect
     * @param countColumnName the column count to inspect
     */
    private boolean needsUpdate(final long playlistId, final String columnName,
                                final String countColumnName) {
        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        try (final Cursor c = getEntry(database, playlistId)) {
            if (c != null && c.moveToFirst()) {
                final long lastUpdate = c.getLong(c.getColumnIndex(columnName));
                final long msSinceEpoch = System.currentTimeMillis();
                final int songCount = MusicUtils.getSongCountForPlaylist(mContext, playlistId);
                final int lastUpdatedSongCount = c.getInt(c.getColumnIndex(countColumnName));

                // if the elapsed time since our last update is less than a day and the
                // number of songs in the playlist hasn't changed, then don't update
                if (msSinceEpoch - lastUpdate < ONE_DAY_IN_MS &&
                        songCount == lastUpdatedSongCount) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Internal function to get the cursor entry for the playlist
     *
     * @param playlistId playlist identifier
     * @return cursor
     */
    private Cursor getEntry(final SQLiteDatabase database, final long playlistId) {
        return database.query(PlaylistArtworkStoreColumns.NAME, null,
                PlaylistArtworkStoreColumns.ID + "=" + playlistId, null, null, null, null);
    }

    public interface PlaylistArtworkStoreColumns {
        /* Table name */
        String NAME = "playlist_details";

        /* Playlist ID column */
        String ID = "playlistid";

        /* When the top artist was last updated */
        String LAST_UPDATE_ARTIST = "last_updated_artist";

        /* The number of songs when we last updated the artist */
        String NUM_SONGS_LAST_UPDATE_ARTIST = "num_songs_last_updated_artist";

        /* When the cover art was last updated */
        String LAST_UPDATE_COVER = "last_updated_cover";

        /* The number of songs when we last updated the cover */
        String NUM_SONGS_LAST_UPDATE_COVER = "num_songs_last_updated_cover";
    }
}
