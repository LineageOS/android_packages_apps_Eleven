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
package org.lineageos.eleven.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class RecentStore {
    /* Maximum # of items in the db */
    private static final int MAX_ITEMS_IN_DB = 100;

    private static RecentStore sInstance = null;

    private final MusicDB mMusicDatabase;

    /**
     * Constructor of <code>RecentStore</code>
     *
     * @param context The {@link Context} to use
     */
    public RecentStore(final Context context) {
        mMusicDatabase = MusicDB.getInstance(context);
    }

    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + RecentStoreColumns.NAME + " ("
                + RecentStoreColumns.ID + " LONG NOT NULL," + RecentStoreColumns.TIME_PLAYED
                + " LONG NOT NULL);");
    }

    public void onDowngrade(SQLiteDatabase db) {
        // If we ever have downgrade, drop the table to be safe
        db.execSQL("DROP TABLE IF EXISTS " + RecentStoreColumns.NAME);
        onCreate(db);
    }

    /**
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static synchronized RecentStore getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new RecentStore(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Used to store song IDs in the database.
     *
     * @param songId The song id to store
     */
    public void addSongId(final long songId) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.beginTransaction();

        try {
            // see if the most recent item is the same song id, if it is then don't insert
            try (Cursor mostRecentItem = queryRecentIds("1")) {
                if (mostRecentItem != null && mostRecentItem.moveToFirst()) {
                    if (songId == mostRecentItem.getLong(0)) {
                        return;
                    }
                }
            }

            // add the entry
            final ContentValues values = new ContentValues(2);
            values.put(RecentStoreColumns.ID, songId);
            values.put(RecentStoreColumns.TIME_PLAYED, System.currentTimeMillis());
            database.insert(RecentStoreColumns.NAME, null, values);

            // if our db is too large, delete the extra items
            try (Cursor oldest = database.query(RecentStoreColumns.NAME,
                    new String[]{RecentStoreColumns.TIME_PLAYED}, null, null, null, null,
                    RecentStoreColumns.TIME_PLAYED + " ASC")) {

                if (oldest != null && oldest.getCount() > MAX_ITEMS_IN_DB) {
                    oldest.moveToPosition(oldest.getCount() - MAX_ITEMS_IN_DB);
                    long timeOfRecordToKeep = oldest.getLong(0);

                    database.delete(RecentStoreColumns.NAME,
                            RecentStoreColumns.TIME_PLAYED + " < ?",
                            new String[]{String.valueOf(timeOfRecordToKeep)});
                }
            }
        } finally {
            database.setTransactionSuccessful();
            database.endTransaction();
        }
    }

    /**
     * @param songId to remove.
     */
    public void removeItem(final long songId) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.delete(RecentStoreColumns.NAME, RecentStoreColumns.ID + " = ?", new String[]{
                String.valueOf(songId)
        });

    }

    public void deleteAll() {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.delete(RecentStoreColumns.NAME, null, null);
    }

    /**
     * Gets a cursor to the list of recently played content
     *
     * @param limit # of songs to limit the result to
     * @return cursor
     */
    public Cursor queryRecentIds(final String limit) {
        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        return database.query(RecentStoreColumns.NAME,
                new String[]{RecentStoreColumns.ID}, null, null, null, null,
                RecentStoreColumns.TIME_PLAYED + " DESC", limit);
    }

    public interface RecentStoreColumns {
        /* Table name */
        String NAME = "recenthistory";

        /* Album IDs column */
        String ID = "songid";

        /* Time played column */
        String TIME_PLAYED = "timeplayed";
    }
}
