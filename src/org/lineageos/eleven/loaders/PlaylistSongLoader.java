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
package org.lineageos.eleven.loaders;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;

import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.utils.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI and
 * return the songs for a particular playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
    private static final String TAG = PlaylistSongLoader.class.getSimpleName();

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * The Id of the playlist the songs belong to.
     */
    private final long mPlaylistID;

    /**
     * Constructor of <code>SongLoader</code>
     *
     * @param context    The {@link Context} to use
     * @param playlistId The Id of the playlist the songs belong to.
     */
    public PlaylistSongLoader(final Context context, final long playlistId) {
        super(context);
        mPlaylistID = playlistId;
    }

    @Override
    public List<Song> loadInBackground() {
        final int playlistCount = countPlaylist(getContext(), mPlaylistID);

        // Create the Cursor
        Cursor cursor = makePlaylistSongCursor(getContext(), mPlaylistID);

        if (cursor != null) {
            boolean runCleanup = false;

            // if the raw playlist count differs from the mapped playlist count (ie the raw mapping
            // table vs the mapping table join the audio table) that means the playlist mapping
            // table is messed up
            if (cursor.getCount() != playlistCount) {
                Log.w(TAG, "Count Differs - raw is: " + playlistCount + " while cursor is " +
                        cursor.getCount());

                runCleanup = true;
            }

            // check if the play order is already messed up by duplicates
            if (!runCleanup && cursor.moveToFirst()) {
                final int playOrderCol = cursor.getColumnIndexOrThrow(Playlists.Members.PLAY_ORDER);

                int lastPlayOrder = -1;
                do {
                    int playOrder = cursor.getInt(playOrderCol);
                    // if we have duplicate play orders, we need to recreate the playlist
                    if (playOrder == lastPlayOrder) {
                        runCleanup = true;
                        break;
                    }
                    lastPlayOrder = playOrder;
                } while (cursor.moveToNext());
            }

            if (runCleanup) {
                Log.w(TAG, "Playlist order has flaws - recreating playlist");

                // cleanup the playlist
                cleanupPlaylist(getContext(), mPlaylistID, cursor);

                // create a new cursor
                cursor.close();
                cursor = makePlaylistSongCursor(getContext(), mPlaylistID);
                if (cursor != null) {
                    Log.d(TAG, "New Count is: " + cursor.getCount());
                }
            }
        }

        // Gather the data
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Copy the song Id
                final long id = cursor.getLong(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID));

                // Copy the song name
                final String songName = cursor.getString(cursor
                        .getColumnIndexOrThrow(AudioColumns.TITLE));

                // Copy the artist name
                final String artist = cursor.getString(cursor
                        .getColumnIndexOrThrow(AudioColumns.ARTIST));

                // Copy the album id
                final long albumId = cursor.getLong(cursor
                        .getColumnIndexOrThrow(AudioColumns.ALBUM_ID));

                // Copy the album name
                final String album = cursor.getString(cursor
                        .getColumnIndexOrThrow(AudioColumns.ALBUM));

                // Copy the duration
                final long duration = cursor.getLong(cursor
                        .getColumnIndexOrThrow(AudioColumns.DURATION));

                // Convert the duration into seconds
                final int durationInSecs = (int) duration / 1000;

                // Grab the Song Year
                final int year = cursor.getInt(cursor
                        .getColumnIndexOrThrow(AudioColumns.YEAR));

                // Create a new song
                final Song song = new Song(id, songName, artist, album, albumId, durationInSecs,
                        year);

                // Add everything up
                mSongList.add(song);
            } while (cursor.moveToNext());
        }
        // Close the cursor
        if (cursor != null) {
            cursor.close();
        }
        return mSongList;
    }

    /**
     * Cleans up the playlist based on the passed in cursor's data
     *
     * @param context    The {@link Context} to use
     * @param playlistId playlistId to clean up
     * @param cursor     data to repopulate the playlist with
     */
    private static void cleanupPlaylist(final Context context, final long playlistId,
                                        final Cursor cursor) {
        Log.w(TAG, "Cleaning up playlist: " + playlistId);

        final int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL,
                playlistId);

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // Delete all results in the playlist
        ops.add(ContentProviderOperation.newDelete(uri).build());

        // yield the db every 100 records to prevent ANRs
        final int YIELD_FREQUENCY = 100;

        // for each item, reset the play order position
        if (cursor.moveToFirst() && cursor.getCount() > 0) {
            do {
                final ContentProviderOperation.Builder builder =
                        ContentProviderOperation.newInsert(uri)
                                .withValue(Playlists.Members.PLAY_ORDER, cursor.getPosition())
                                .withValue(Playlists.Members.AUDIO_ID, cursor.getLong(idCol));

                // yield at the end and not at 0 by incrementing by 1
                if ((cursor.getPosition() + 1) % YIELD_FREQUENCY == 0) {
                    builder.withYieldAllowed(true);
                }
                ops.add(builder.build());
            } while (cursor.moveToNext());
        }

        try {
            // run the batch operation
            context.getContentResolver().applyBatch(MediaStore.AUTHORITY, ops);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e + " while cleaning up playlist " + playlistId);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "OperationApplicationException " + e + " while cleaning up playlist "
                    + playlistId);
        }
    }

    /**
     * Returns the playlist count for the raw playlist mapping table
     *
     * @param context    The {@link Context} to use
     * @param playlistId playlistId to count
     * @return the number of tracks in the raw playlist mapping table
     */
    private static int countPlaylist(final Context context, final long playlistId) {
        try (Cursor c = context.getContentResolver().query(
                Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId),
                new String[]{Playlists.Members.AUDIO_ID,}, null, null,
                Playlists.Members.DEFAULT_SORT_ORDER)) {
            // when we query using only the audio_id column we will get the raw mapping table
            // results - which will tell us if the table has rows that don't exist in the normal
            // table
            if (c != null) {
                return c.getCount();
            }
        }

        return 0;
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @param context    The {@link Context} to use.
     * @param playlistID The playlist the songs belong to.
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makePlaylistSongCursor(final Context context, final Long playlistID) {
        String mSelection = (AudioColumns.IS_MUSIC + "=1") +
                " AND " + AudioColumns.TITLE + " != ''";
        return context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistID),
                new String[]{
                        /* 0 */
                        MediaStore.Audio.Playlists.Members._ID,
                        /* 1 */
                        MediaStore.Audio.Playlists.Members.AUDIO_ID,
                        /* 2 */
                        AudioColumns.TITLE,
                        /* 3 */
                        AudioColumns.ARTIST,
                        /* 4 */
                        AudioColumns.ALBUM_ID,
                        /* 5 */
                        AudioColumns.ALBUM,
                        /* 6 */
                        AudioColumns.DURATION,
                        /* 7 */
                        AudioColumns.YEAR,
                        /* 8 */
                        Playlists.Members.PLAY_ORDER,
                }, mSelection, null,
                MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
    }
}
