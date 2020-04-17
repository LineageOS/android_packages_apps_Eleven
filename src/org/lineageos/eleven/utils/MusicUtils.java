/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2019 The LineageOS Project
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

package org.lineageos.eleven.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.lineageos.eleven.BuildConfig;
import org.lineageos.eleven.Config.IdType;
import org.lineageos.eleven.Config.SmartPlaylistType;
import org.lineageos.eleven.IElevenService;
import org.lineageos.eleven.MusicPlaybackService;
import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.loaders.LastAddedLoader;
import org.lineageos.eleven.loaders.PlaylistLoader;
import org.lineageos.eleven.loaders.PlaylistSongLoader;
import org.lineageos.eleven.loaders.SongLoader;
import org.lineageos.eleven.loaders.TopTracksLoader;
import org.lineageos.eleven.locale.LocaleUtils;
import org.lineageos.eleven.model.AlbumArtistDetails;
import org.lineageos.eleven.provider.RecentStore;
import org.lineageos.eleven.provider.SongPlayCount;
import org.lineageos.eleven.service.MusicPlaybackTrack;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A collection of helpers directly related to music or Eleven's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class MusicUtils {
    public static final String TAG = MusicUtils.class.getSimpleName();

    private static final long[] sEmptyList;
    private static Set<WeakReference<ServiceToken>> sKnownTokens = new HashSet<>();
    private static ContentValues[] mContentValuesCache = null;

    private static final int MIN_VALID_YEAR = 1900; // used to remove invalid years from metadata

    public static final String MUSIC_ONLY_SELECTION = MediaStore.Audio.AudioColumns.IS_MUSIC + "=1"
                    + " AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''"; //$NON-NLS-2$

    public static final long UPDATE_FREQUENCY_MS = 500;
    public static final long UPDATE_FREQUENCY_FAST_MS = 30;

    static {
        sEmptyList = new long[0];
    }

    /* This class is never initiated */
    public MusicUtils() {
    }

    /**
     * @param context The {@link Context} to use
     * @param callback The {@link ServiceConnection} to use
     * @return The new instance of {@link ServiceToken}
     */
    public static ServiceToken bindToService(final Context context,
            final ServiceConnection callback) {
        final ServiceBinder binder = new ServiceBinder(callback);
        final Intent intent = new Intent(context, MusicPlaybackService.class);
        final int flags = Context.BIND_ADJUST_WITH_ACTIVITY | Context.BIND_AUTO_CREATE;
        if (context.bindService(intent, binder, flags)) {
            ServiceToken token = new ServiceToken(context, binder);
            sKnownTokens.add(new WeakReference<>(token));
            return token;
        }
        return null;
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(final ServiceToken token) {
        if (token == null) {
            return;
        }
        final ServiceBinder binder = token.mBinder;
        if (binder == null) {
            return;
        }
        token.discard();
        for (WeakReference<ServiceToken> ref : sKnownTokens) {
            if (ref.get() == token) {
                sKnownTokens.remove(ref);
                break;
            }
        }
    }

    public static final class ServiceBinder implements ServiceConnection {
        private final ServiceConnection mCallback;
        private IElevenService mServiceConnection;

        /**
         * Constructor of <code>ServiceBinder</code>
         *
         * @param callback The {@link ServiceConnection} to use
         */
        public ServiceBinder(final ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            mServiceConnection = IElevenService.Stub.asInterface(service);
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            mServiceConnection = null;
        }
    }

    public static final class ServiceToken {
        private final WeakReference<Context> mContextRef;
        private final ServiceBinder mBinder;

        /**
         * Constructor of <code>ServiceToken</code>
         *
         * @param context The context for the bind operation
         * @param binder The {@link ServiceBinder} this token references
         */
        private ServiceToken(final Context context, final ServiceBinder binder) {
            mContextRef = new WeakReference<>(context);
            mBinder = binder;
        }

        private void discard() {
            Context context = mContextRef.get();
            if (context != null) {
                context.unbindService(mBinder);
            }
        }
    }

    private static IElevenService getService() {
        for (WeakReference<ServiceToken> ref : sKnownTokens) {
            ServiceToken token = ref.get();
            IElevenService service = token != null ? token.mBinder.mServiceConnection : null;
            if (service != null) {
                return service;
            }
        }
        return null;
    }
    public static boolean isPlaybackServiceConnected() {
        return getService() != null;
    }

    /**
     * Used to make number of labels for the number of artists, albums, songs,
     * genres, and playlists.
     *
     * @param context The {@link Context} to use.
     * @param pluralInt The ID of the plural string to use.
     * @param number The number of artists, albums, songs, genres, or playlists.
     * @return A {@link String} used as a label for the number of artists,
     *         albums, songs, genres, and playlists.
     */
    public static String makeLabel(final Context context, final int pluralInt,
            final int number) {
        return context.getResources().getQuantityString(pluralInt, number, number);
    }

    /**
     * * Used to create a formatted time string for the duration of tracks.
     *
     * @param context The {@link Context} to use.
     * @param secs The track in seconds.
     * @return Duration of a track that's properly formatted.
     */
    @NonNull
    public static String makeShortTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;
        secs %= 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    /**
     * Used to create a formatted time string in the format of #h #m or #m if there is only minutes
     *
     * @param context The {@link Context} to use.
     * @param secs The duration seconds.
     * @return Duration properly formatted in #h #m format
     */
    public static String makeLongTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;

        String hoursString = MusicUtils.makeLabel(context, R.plurals.Nhours, (int)hours);
        String minutesString = MusicUtils.makeLabel(context, R.plurals.Nminutes, (int)mins);

        if (hours == 0) {
            return minutesString;
        } else if (mins == 0) {
            return hoursString;
        }

        final String durationFormat = context.getResources().getString(R.string.duration_format);
        return String.format(durationFormat, hoursString, minutesString);
    }

    /**
     * Used to combine two strings with some kind of separator in between
     *
     * @param context The {@link Context} to use.
     * @param first string to combine
     * @param second string to combine
     * @return the combined string
     */
    public static String makeCombinedString(final Context context, final String first,
                                                  final String second) {
        final String formatter = context.getResources().getString(R.string.combine_two_strings);
        return String.format(formatter, first, second);
    }

    /**
     * Changes to the next track
     */
    public static void next() {
        try {
            IElevenService service = getService();
            if (service != null) {
                service.next();
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "next()", exc);
        }
    }

    /**
     * Set shake to play status
     */
    public static void setShakeToPlayEnabled(final boolean enabled) {
        try {
            IElevenService service = getService();
            if (service != null) {
                service.setShakeToPlayEnabled(enabled);
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "setShakeToPlayEnabled(" + enabled + ")", exc);
        }
    }

    /**
     * Set show album art on lockscreen
     */
    public static void setShowAlbumArtOnLockscreen(final boolean enabled) {
        try {
            IElevenService service = getService();
            if (service != null) {
                service.setLockscreenAlbumArt(enabled);
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "setLockscreenAlbumArt(" + enabled + ")", exc);
        }
    }

    /**
     * Changes to the next track asynchronously
     */
    public static void asyncNext(final Context context) {
        final Intent previous = new Intent(context, MusicPlaybackService.class);
        previous.setAction(MusicPlaybackService.NEXT_ACTION);
        context.startService(previous);
    }

    /**
     * Changes to the previous track.
     *
     * @NOTE The AIDL isn't used here in order to properly use the previous
     *       action. When the user is shuffling, because {@link
     *       MusicPlaybackService#openCurrentAndNext()} is used, the user won't
     *       be able to travel to the previously skipped track. To remedy this,
     *       {@link MusicPlaybackService#openCurrent()} is called in {@link
     *       MusicPlaybackService#prev(boolean)}. {@code #startService(Intent intent)}
     *       is called here to specifically invoke the onStartCommand used by
     *       {@link MusicPlaybackService}, which states if the current position
     *       less than 2000 ms, start the track over, otherwise move to the
     *       previously listened track.
     */
    public static void previous(final Context context, final boolean force) {
        final Intent previous = new Intent(context, MusicPlaybackService.class);
        if (force) {
            previous.setAction(MusicPlaybackService.PREVIOUS_FORCE_ACTION);
        } else {
            previous.setAction(MusicPlaybackService.PREVIOUS_ACTION);
        }
        context.startService(previous);
    }

    /**
     * Plays or pauses the music.
     */
    public static void playOrPause() {
        try {
            IElevenService service = getService();
            if (service != null) {
                if (service.isPlaying()) {
                    service.pause();
                } else {
                    service.play();
                }
            }
        } catch (final Exception exc) {
            Log.e(TAG, "playOrPause()", exc);
        }
    }

    /**
     * Cycles through the repeat options.
     */
    public static void cycleRepeat() {
        try {
            IElevenService service = getService();
            if (service != null) {
                switch (service.getRepeatMode()) {
                    case MusicPlaybackService.REPEAT_NONE:
                        service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        break;
                    case MusicPlaybackService.REPEAT_ALL:
                        service.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
                        if (service.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
                            service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        }
                        break;
                    default:
                        service.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
                        break;
                }
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "cycleRepeat()", exc);
        }
    }

    /**
     * Cycles through the shuffle options.
     */
    public static void cycleShuffle() {
        try {
            IElevenService service = getService();
            if (service != null) {
                switch (service.getShuffleMode()) {
                    case MusicPlaybackService.SHUFFLE_NONE:
                        service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
                        if (service.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                            service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        }
                        break;
                    case MusicPlaybackService.SHUFFLE_NORMAL:
                        service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    case MusicPlaybackService.SHUFFLE_AUTO:
                        service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    default:
                        break;
                }
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "cycleShuffle()", exc);
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static boolean isPlaying() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.isPlaying();
            } catch (final RemoteException exc) {
                Log.e(TAG, "isPlaying()", exc);
            }
        }
        return false;
    }

    /**
     * @return The current shuffle mode.
     */
    public static int getShuffleMode() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getShuffleMode();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getShuffleMode()", exc);
            }
        }
        return 0;
    }

    /**
     * @return The current repeat mode.
     */
    public static int getRepeatMode() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getRepeatMode();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getRepeatMode()", exc);
            }
        }
        return 0;
    }

    /**
     * @return The current track name.
     */
    public static String getTrackName() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getTrackName();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getTrackName()", exc);
            }
        }
        return null;
    }

    /**
     * @return The current artist name.
     */
    public static String getArtistName() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getArtistName();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getArtistName()", exc);
            }
        }
        return null;
    }

    /**
     * @return The current album name.
     */
    public static String getAlbumName() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getAlbumName();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getAlbumName()", exc);
            }
        }
        return null;
    }

    /**
     * @return The current album Id.
     */
    public static long getCurrentAlbumId() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getAlbumId();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getCurrentAlbumId()", exc);
            }
        }
        return -1;
    }

    /**
     * @return The current song Id.
     */
    public static long getCurrentAudioId() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getAudioId();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getCurrentAudioId()", exc);
            }
        }
        return -1;
    }

    /**
     * @return The current Music Playback Track
     */
    public static MusicPlaybackTrack getCurrentTrack() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getCurrentTrack();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getCurrentTrack()", exc);
            }
        }
        return null;
    }

    /**
     * @return The Music Playback Track at the specified index
     */
    public static MusicPlaybackTrack getTrack(int index) {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getTrack(index);
            } catch (final RemoteException exc) {
                Log.e(TAG, "getTrack(" + index + ")", exc);
            }
        }
        return null;
    }

    /**
     * @return The next song Id.
     */
    public static long getNextAudioId() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getNextAudioId();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getNextAudioId()", exc);
            }
        }
        return -1;
    }

    /**
     * @return The previous song Id.
     */
    public static long getPreviousAudioId() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getPreviousAudioId();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getPreviousAudioId()", exc);
            }
        }
        return -1;
    }

    /**
     * @return The current artist Id.
     */
    public static long getCurrentArtistId() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getArtistId();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getArtistId()", exc);
            }
        }
        return -1;
    }

    /**
     * @return The audio session Id.
     */
    public static int getAudioSessionId() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getAudioSessionId();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getAudioSessionId()", exc);
            }
        }
        return -1;
    }

    /**
     * @return The queue.
     */
    public static long[] getQueue() {
        try {
            IElevenService service = getService();
            if (service != null) {
                return service.getQueue();
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "getQueue()", exc);
        }
        return sEmptyList;
    }

    /**
     * @return the id of the track in the queue at the given position
     */
    public static long getQueueItemAtPosition(int position) {
        try {
            IElevenService service = getService();
            if (service != null) {
                return service.getQueueItemAtPosition(position);
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "getQueueItemAtPosition(" + position + ")", exc);
        }
        return -1;
    }

    /**
     * @return the current queue size
     */
    public static int getQueueSize() {
        try {
            IElevenService service = getService();
            if (service != null) {
                return service.getQueueSize();
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "getQueueSize()", exc);
        }
        return 0;
    }

    /**
     * @return The position of the current track in the queue.
     */
    public static int getQueuePosition() {
        try {
            IElevenService service = getService();
            if (service != null) {
                return service.getQueuePosition();
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "getQueuePosition()", exc);
        }
        return 0;
    }

    /**
     * @return The queue history size
     */
    public static int getQueueHistorySize() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getQueueHistorySize();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getQueueHistorySize()", exc);
            }
        }
        return 0;
    }

    /**
     * @return The queue history position at the position
     */
    public static int getQueueHistoryPosition(int position) {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getQueueHistoryPosition(position);
            } catch (final RemoteException exc) {
                Log.e(TAG, "getQueueHistoryPosition(" + position + ")", exc);
            }
        }
        return -1;
    }

    /**
     * @return The queue history
     */
    public static int[] getQueueHistoryList() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.getQueueHistoryList();
            } catch (final RemoteException exc) {
                Log.e(TAG, "getQueueHistoryList()", exc);
            }
        }
        return null;
    }

    /**
     * @param id The ID of the track to remove.
     * @return removes track from a playlist or the queue.
     */
    public static int removeTrack(final long id) {
        IElevenService service = getService();
        try {
            if (service != null) {
                return service.removeTrack(id);
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "removeTrack(" + id + ")", exc);
        }
        return 0;
    }

    /**
     * Remove song at a specified position in the list
     *
     * @param id The ID of the track to remove
     * @param position The position of the song
     *
     * @return true if successful, false otherwise
     */
    public static boolean removeTrackAtPosition(final long id, final int position) {
        try {
            IElevenService service = getService();
            if (service != null) {
                return service.removeTrackAtPosition(id, position);
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "removeTrackAtPosition(" + id + ", " + position + ")", exc);
        }
        return false;
    }

    /**
     * @param cursor The {@link Cursor} used to perform our query.
     * @return The song list for a MIME type.
     */
    public static long[] getSongListForCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        final int len = cursor.getCount();
        final long[] list = new long[len];
        cursor.moveToFirst();
        int columnIndex = -1;
        try {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        } catch (final IllegalArgumentException notaplaylist) {
            columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        }
        try {
            for (int i = 0; i < len; i++) {
                list[i] = cursor.getLong(columnIndex);
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The ID of the artist.
     * @return The song list for an artist.
     */
    public static long[] getSongListForArtist(final Context context, final long id) {
        final String[] projection = new String[] {
            BaseColumns._ID
        };
        final String selection = AudioColumns.ARTIST_ID + "=" + id + " AND "
                + AudioColumns.IS_MUSIC + "=1";
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK)) {
            if (cursor != null) {
                return getSongListForCursor(cursor);
            }
        }
        return sEmptyList;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The ID of the album.
     * @return The song list for an album.
     */
    public static long[] getSongListForAlbum(final Context context, final long id) {
        final String[] projection = new String[] {
            BaseColumns._ID
        };
        final String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC
                + "=1";
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER)) {
            if (cursor != null) {
                return getSongListForCursor(cursor);
            }
        }
        return sEmptyList;
    }

    /**
     * Plays songs by an artist.
     *
     * @param context The {@link Context} to use.
     * @param artistId The artist Id.
     * @param position Specify where to start.
     */
    public static void playArtist(final Context context, final long artistId, int position, boolean shuffle) {
        final long[] artistList = getSongListForArtist(context, artistId);
        if (artistList != null) {
            playAll(context, artistList, position, artistId, IdType.Artist, shuffle);
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The ID of the genre.
     * @return The song list for an genre.
     */
    public static long[] getSongListForGenre(final Context context, final long id) {
        final String[] projection = new String[] {
            BaseColumns._ID
        };
        String selection = (AudioColumns.IS_MUSIC + "=1") +
                " AND " + MediaColumns.TITLE + "!=''";
        final Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", id);
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection,
                null, null)) {
            if (cursor != null) {
                return getSongListForCursor(cursor);
            }
        }
        return sEmptyList;
    }

    /**
     * @param context The {@link Context} to use
     * @param uri The source of the file
     */
    public static void playFile(final Context context, final Uri uri) {
        IElevenService service = getService();
        if (uri == null || service == null) {
            return;
        }

        // If this is a file:// URI, just use the path directly instead
        // of going through the open-from-filedescriptor codepath.
        String filename;
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            filename = uri.getPath();
        } else {
            filename = uri.toString();
        }

        try {
            service.stop();
            service.openFile(filename);
            service.play();
        } catch (final RemoteException exc) {
            Log.e(TAG, "playFile(" + uri + ")", exc);
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param list The list of songs to play.
     * @param position Specify where to start.
     * @param forceShuffle True to force a shuffle, false otherwise.
     */
    public static void playAll(final Context context, final long[] list, int position,
                               final long sourceId, final IdType sourceType,
                               final boolean forceShuffle) {
        IElevenService service = getService();
        if (list == null || list.length == 0 || service == null) {
            return;
        }
        try {
            if (forceShuffle) {
                service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
            }
            if (position < 0) {
                position = 0;
            }
            service.open(list, forceShuffle ? -1 : position, sourceId, sourceType.mId);
            service.play();
        } catch (final RemoteException exc) {
            Log.e(TAG, "playAll(...)", exc);
        }
    }

    /**
     * @param list The list to enqueue.
     */
    public static void playNext(final long[] list, final long sourceId, final IdType sourceType) {
        IElevenService service = getService();
        if (service == null) {
            return;
        }
        try {
            service.enqueue(list, MusicPlaybackService.NEXT, sourceId, sourceType.mId);
        } catch (final RemoteException exc) {
            Log.e(TAG, "playNext(" + Arrays.asList(list) + ", " + sourceId + ", " + sourceType + ")", exc);
        }
    }

    /**
     * @param context The {@link Context} to use.
     */
    public static void shuffleAll(final Context context) {
        long[] mTrackListTmp;
        try (Cursor cursor = SongLoader.makeSongCursor(context, null)) {
            mTrackListTmp = getSongListForCursor(cursor);
        }

        final long[] mTrackList = mTrackListTmp;
        IElevenService service = getService();
        if (mTrackList.length == 0 || service == null) {
            return;
        }

        try {
            service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
            final long mCurrentId = service.getAudioId();
            final int mCurrentQueuePosition = getQueuePosition();
            if (mCurrentQueuePosition == 0 && mCurrentId == mTrackList[0]) {
                final long[] mPlaylist = getQueue();
                if (Arrays.equals(mTrackList, mPlaylist)) {
                    service.play();
                    return;
                }
            }
            service.open(mTrackList, -1, -1, IdType.NA.mId);
            service.play();
        } catch (final RemoteException exc) {
            Log.e(TAG, "shuffleAll()", exc);
        }
    }

    /**
     * Returns The ID for a playlist.
     *
     * @param context The {@link Context} to use.
     * @param name The name of the playlist.
     * @return The ID for a playlist.
     */
    public static long getIdForPlaylist(final Context context, final String name) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getIdForPlaylist(" + name + ")");
        }

        try (Cursor cursor = context.getContentResolver().query(Playlists.EXTERNAL_CONTENT_URI,
                new String[]{BaseColumns._ID}, PlaylistsColumns.NAME + "=?",
                new String[]{name}, PlaylistsColumns.NAME)) {
            if (cursor != null) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    return cursor.getInt(0);
                }
            }
        }
        return -1;
    }

    /** @param context The {@link Context} to use.
     *  @param id The id of the playlist.
     *  @return The name for a playlist. */
    public static String getNameForPlaylist(final Context context, final long id) {
        try (Cursor cursor = context.getContentResolver().query(
                Playlists.EXTERNAL_CONTENT_URI, new String[]{PlaylistsColumns.NAME},
                BaseColumns._ID + "=?", new String[]{Long.toString(id)}, null)) {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            }
        }
        // nothing found
        return null;
    }

    /**
     * Returns the Id for an artist.
     *
     * @param context The {@link Context} to use.
     * @param name The name of the artist.
     * @return The ID for an artist.
     */
    public static long getIdForArtist(final Context context, final String name) {
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{BaseColumns._ID},
                ArtistColumns.ARTIST + "=?", new String[]{name}, ArtistColumns.ARTIST)) {
            if (cursor != null) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    return cursor.getInt(0);
                }
            }
        }
        return -1;
    }

    /**
     * Returns the ID for an album.
     *
     * @param context The {@link Context} to use.
     * @param albumName The name of the album.
     * @param artistName The name of the artist
     * @return The ID for an album.
     */
    public static long getIdForAlbum(final Context context, final String albumName,
            final String artistName) {
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{BaseColumns._ID},
                AlbumColumns.ALBUM + "=? AND " + AlbumColumns.ARTIST + "=?", new String[]{
                        albumName, artistName
                }, AlbumColumns.ALBUM)) {
            if (cursor != null) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    return cursor.getInt(0);
                }
            }
        }
        return -1;
    }

    /**
     * Plays songs from an album.
     *
     * @param context The {@link Context} to use.
     * @param albumId The album Id.
     * @param position Specify where to start.
     */
    public static void playAlbum(final Context context, final long albumId, int position, boolean shuffle) {
        final long[] albumList = getSongListForAlbum(context, albumId);
        if (albumList != null) {
            playAll(context, albumList, position, albumId, IdType.Album, shuffle);
        }
    }

    public static void makeInsertItems(final long[] ids, final int offset, int len, final int base) {
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }

        if (mContentValuesCache == null || mContentValuesCache.length != len) {
            mContentValuesCache = new ContentValues[len];
        }
        for (int i = 0; i < len; i++) {
            if (mContentValuesCache[i] == null) {
                mContentValuesCache[i] = new ContentValues();
            }
            mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
            mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param name The name of the new playlist.
     * @return A new playlist ID.
     */
    public static long createPlaylist(final Context context, final String name) {
        if (name != null && name.length() > 0) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] projection = new String[] {
                PlaylistsColumns.NAME
            };
            final String selection = PlaylistsColumns.NAME + " = '" + name + "'";
            try (Cursor cursor = resolver.query(Playlists.EXTERNAL_CONTENT_URI,
                    projection, selection, null, null)) {
                if (cursor != null) {
                    if (cursor.getCount() <= 0) {
                        final ContentValues values = new ContentValues(1);
                        values.put(PlaylistsColumns.NAME, name);
                        final Uri uri = resolver.insert(
                                Playlists.EXTERNAL_CONTENT_URI, values);
                        if (uri != null && uri.getLastPathSegment() != null) {
                            return Long.parseLong(uri.getLastPathSegment());
                        }
                    }
                }
            }
            return -1;
        }
        return -1;
    }

    /**
     * @param context The {@link Context} to use.
     * @param playlistId The playlist ID.
     */
    public static void clearPlaylist(final Context context, final int playlistId) {
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        context.getContentResolver().delete(uri, null, null);
    }

    /** remove all backing data for top tracks playlist */
    public static void clearTopTracks(Context context) {
        SongPlayCount.getInstance(context).deleteAll();
    }

    /** remove all backing data for top tracks playlist */
    public static void clearRecent(Context context) {
        RecentStore.getInstance(context).deleteAll();
    }

    /** move up cutoff for last added songs so playlist will be cleared */
    public static void clearLastAdded(Context context) {
        PreferenceUtils.getInstance(context)
            .setLastAddedCutoff(System.currentTimeMillis());
    }

    /**
     * @param context The {@link Context} to use.
     * @param ids The id of the song(s) to add.
     * @param playlistid The id of the playlist being added to.
     */
    public static void addToPlaylist(final Context context, final long[] ids, final long playlistid) {
        final int size = ids.length;
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[] {
            "max(" + Playlists.Members.PLAY_ORDER + ")",
        };
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);

        int base = 0;
        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                base = cursor.getInt(0) + 1;
            }
        }

        int numinserted = 0;
        for (int offSet = 0; offSet < size; offSet += 1000) {
            makeInsertItems(ids, offSet, 1000, base);
            numinserted += resolver.bulkInsert(uri, mContentValuesCache);
        }
        final String message = context.getResources().getQuantityString(
                R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        playlistChanged();
    }

    /**
     * Removes a single track from a given playlist
     * @param context The {@link Context} to use.
     * @param id The id of the song to remove.
     * @param playlistId The id of the playlist being removed from.
     */
    public static void removeFromPlaylist(final Context context, final long id,
            final long playlistId) {
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        final ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, Playlists.Members.AUDIO_ID + " = ? ", new String[] {
            Long.toString(id)
        });
        final String message = context.getResources().getQuantityString(
                R.plurals.NNNtracksfromplaylist, 1, 1);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        playlistChanged();
    }

    /**
     * @param context The {@link Context} to use.
     * @param list The list to enqueue.
     */
    public static void addToQueue(final Context context, final long[] list, long sourceId,
                                  IdType sourceType) {
        IElevenService service = getService();
        if (service == null) {
            return;
        }
        try {
            service.enqueue(list, MusicPlaybackService.LAST, sourceId, sourceType.mId);
            final String message = makeLabel(context, R.plurals.NNNtrackstoqueue, list.length);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (final RemoteException exc) {
            Log.e(TAG, "addToQueue(...)", exc);
        }
    }

    /**
     * @param context The {@link Context} to use
     * @param id The song ID.
     */
    public static void setRingtone(final Context context, final long id) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            final ContentValues values = new ContentValues(2);
            values.put(AudioColumns.IS_RINGTONE, "1");
            values.put(AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (final UnsupportedOperationException ignored) {
            return;
        }

        final String[] projection = new String[] {
                BaseColumns._ID, MediaColumns.DATA, MediaColumns.TITLE
        };

        final String selection = BaseColumns._ID + "=" + id;
        try (Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, null)) {
            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
                final String message = context.getString(R.string.set_as_ringtone,
                        cursor.getString(2));
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The id of the album.
     * @return The song count for an album.
     */
    public static int getSongCountForAlbumInt(final Context context, final long id) {
        int songCount = 0;
        if (id == -1) { return songCount; }

        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{AlbumColumns.NUMBER_OF_SONGS}, null, null, null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    if (!cursor.isNull(0)) {
                        songCount = cursor.getInt(0);
                    }
                }
            }
        }

        return songCount;
    }

    /**
     * Gets the number of songs for a playlist
     * @param context The {@link Context} to use.
     * @param playlistId the id of the playlist
     * @return the # of songs in the playlist
     */
    public static int getSongCountForPlaylist(final Context context, final long playlistId) {
        try (Cursor cursor = context.getContentResolver().query(
                Playlists.Members.getContentUri("external", playlistId),
                new String[]{BaseColumns._ID}, MusicUtils.MUSIC_ONLY_SELECTION, null, null)) {
            if (cursor != null) {
                int count = 0;
                if (cursor.moveToFirst()) {
                    count = cursor.getCount();
                }
                return count;
            }
        }
        return 0;
    }

    public static AlbumArtistDetails getAlbumArtDetails(final Context context, final long trackId) {
        String selection = (AudioColumns.IS_MUSIC + "=1") +
                " AND " + BaseColumns._ID + " = '" + trackId + "'";

        final Cursor cursor = context.getContentResolver().query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            new String[] {
                    /* 0 */
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                    /* 1 */
                MediaStore.Audio.AudioColumns.ALBUM,
                    /* 2 */
                MediaStore.Audio.AlbumColumns.ARTIST,
            }, selection, null, null
        );

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            final AlbumArtistDetails result = new AlbumArtistDetails();
            result.mAudioId = trackId;
            result.mAlbumId = cursor.getLong(0);
            result.mAlbumName = cursor.getString(1);
            result.mArtistName = cursor.getString(2);
            return result;
        } finally {
            cursor.close();
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The id of the album.
     * @return The release date for an album.
     */
    public static String getReleaseDateForAlbum(final Context context, final long id) {
        if (id == -1) {
            return null;
        }
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
        String releaseDate = null;
        try (Cursor cursor = context.getContentResolver().query(uri, new String[] {
                AlbumColumns.FIRST_YEAR
        }, null, null, null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    releaseDate = cursor.getString(0);
                }
            }
        }
        return releaseDate;
    }

    /**
     * @return The path to the currently playing file as {@link String}
     */
    public static String getFilePath() {
        try {
            IElevenService service = getService();
            if (service != null) {
                return service.getPath();
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "getFilePath()", exc);
        }
        return null;
    }

    /**
     * @param from The index the item is currently at.
     * @param to The index the item is moving to.
     */
    public static void moveQueueItem(final int from, final int to) {
        try {
            IElevenService service = getService();
            if (service != null) {
                service.moveQueueItem(from, to);
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "moveQueueItem(" + from + ", " + to + ")", exc);
        }
    }

    /**
     * @param context The {@link Context} to sue
     * @param playlistId The playlist Id
     * @return The track list for a playlist
     */
    public static long[] getSongListForPlaylist(final Context context, final long playlistId) {
        try (final Cursor cursor = PlaylistSongLoader.makePlaylistSongCursor(context, playlistId)) {
            if (cursor != null) {
                return getSongListForCursor(cursor);
            }
        }
        return sEmptyList;
    }

    /**
     * Plays a user created playlist.
     *
     * @param context The {@link Context} to use.
     * @param playlistId The playlist Id.
     */
    public static void playPlaylist(final Context context, final long playlistId, boolean shuffle) {
        final long[] playlistList = getSongListForPlaylist(context, playlistId);
        if (playlistList != null) {
            playAll(context, playlistList, -1, playlistId, IdType.Playlist, shuffle);
        }
    }

    /**
     * @param context The {@link Context} to use
     * @param type The Smart Playlist Type
     * @return The song list for the last added playlist
     */
    public static long[] getSongListForSmartPlaylist(final Context context,
                                                           final SmartPlaylistType type) {
        Cursor cursor = null;
        try {
            switch (type) {
                case LastAdded:
                    cursor = LastAddedLoader.makeLastAddedCursor(context);
                    break;
                case RecentlyPlayed:
                    cursor = TopTracksLoader.makeRecentTracksCursor(context);
                    break;
                case TopTracks:
                    cursor = TopTracksLoader.makeTopTracksCursor(context);
                    break;
            }
            return MusicUtils.getSongListForCursor(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Plays the smart playlist
     * @param context The {@link Context} to use
     * @param position the position to start playing from
     * @param type The Smart Playlist Type
     */
    public static void playSmartPlaylist(final Context context, final int position,
                                         final SmartPlaylistType type, final boolean shuffle) {
        final long[] list = getSongListForSmartPlaylist(context, type);
        MusicUtils.playAll(context, list, position, type.mId, IdType.Playlist, shuffle);
    }

    /**
     * Creates a map used to add items to a new playlist or an existing one.
     *
     * @param context The {@link Context} to use.
     */
    public static List<String> makePlaylist(final Context context) {
        final List<String> menuItemMap = new ArrayList<>();

        try (final Cursor cursor = PlaylistLoader.makePlaylistCursor(context)) {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    final String name = cursor.getString(1);
                    if (name != null) {
                        menuItemMap.add(name);
                    }
                    cursor.moveToNext();
                }
            }
        }

        // sort the list but ignore case
        Collections.sort(menuItemMap, new IgnoreCaseComparator());
        // add new_playlist to the top of the sorted list
        menuItemMap.add(0, context.getString(R.string.new_playlist));

        return menuItemMap;
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public static void refresh() {
        try {
            IElevenService service = getService();
            if (service != null) {
                service.refresh();
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "refresh()", exc);
        }
    }

    /**
     * Called when one of playlists have changed
     */
    public static void playlistChanged() {
        try {
            IElevenService service = getService();
            if (service != null) {
                service.playlistChanged();
            }
        } catch (final RemoteException exc) {
            Log.e(TAG, "playlistChanged()", exc);
        }
    }

    /**
     * Seeks the current track to a desired position
     *
     * @param position The position to seek to
     */
    public static void seek(final long position) {
        IElevenService service = getService();
        if (service != null) {
            try {
                service.seek(position);
            } catch (final RemoteException exc) {
                Log.e(TAG, "seek(" + position + ")", exc);
            }
        }
    }

    /**
     * Seeks the current track to a desired relative position.  This can be used
     * to simulate fastforward and rewind
     *
     * @param deltaInMs The delta in ms to seek from the current position
     */
    public static void seekRelative(final long deltaInMs) {
        IElevenService service = getService();
        if (service != null) {
            try {
                service.seekRelative(deltaInMs);
            } catch (final RemoteException exc) {
                Log.e(TAG, "seekRelative(" + deltaInMs + ")", exc);
            } catch (final IllegalStateException exc) {
                Log.e(TAG, "seekRelative(" + deltaInMs + ")", exc);
            }
        }
    }

    /**
     * @return The current position time of the track
     */
    public static long position() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.position();
            } catch (final RemoteException exc) {
                Log.e(TAG, "position()", exc);
            } catch (final IllegalStateException exc) {
                Log.e(TAG, "position()", exc);
            }
        }
        return 0;
    }

    /**
     * @return The total length of the current track
     */
    public static long duration() {
        IElevenService service = getService();
        if (service != null) {
            try {
                return service.duration();
            } catch (final RemoteException exc) {
                Log.e(TAG, "duration()", exc);
            } catch (final IllegalStateException exc) {
                Log.e(TAG, "duration()", exc);
            }
        }
        return 0;
    }

    /**
     * @return The total length of the current track in seconds
     */
    public static int durationInSeconds() {
        return ((int) duration() / 1000);
    }

    /**
     * @param position The position to move the queue to
     */
    public static void setQueuePosition(final int position) {
        IElevenService service = getService();
        if (service != null) {
            try {
                service.setQueuePosition(position);
            } catch (final RemoteException exc) {
                Log.e(TAG, "setQueuePosition(" + position + ")", exc);
            }
        }
    }

    /**
     * Clears the queue
     */
    public static void clearQueue() {
        IElevenService service = getService();
        try {
            service.removeTracks(0, Integer.MAX_VALUE);
        } catch (final RemoteException exc) {
            Log.e(TAG, "clearQueue()", exc);
        }
    }

    /**
     * Perminately deletes item(s) from the user's device
     *
     * @param context The {@link Context} to use.
     * @param list The item(s) to delete.
     */
    public static void deleteTracks(final Context context, final long[] list) {
        final String[] projection = new String[] {
                BaseColumns._ID, MediaColumns.DATA, AudioColumns.ALBUM_ID
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            selection.append(list[i]);
            if (i < list.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        try (Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(),
                null, null)) {
            if (c != null) {
                // Step 1: Remove selected tracks from the current playlist, as well
                // as from the album art cache
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    // Remove from current playlist
                    final long id = c.getLong(0);
                    removeTrack(id);
                    // Remove the track from the play count
                    SongPlayCount.getInstance(context).removeItem(id);
                    // Remove any items in the recents database
                    RecentStore.getInstance(context).removeItem(id);
                    c.moveToNext();
                }

                // Step 2: Remove selected tracks from the database
                context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        selection.toString(), null);

                // Step 3: Remove files from card
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    final String name = c.getString(1);
                    final File f = new File(name);
                    try { // File.delete can throw a security exception
                        if (!f.delete()) {
                            // I'm not sure if we'd ever get here (deletion would
                            // have to fail, but no exception thrown)
                            Log.e("MusicUtils", "Failed to delete file " + name);
                        }
                        c.moveToNext();
                    } catch (final SecurityException ex) {
                        c.moveToNext();
                    }
                }
            }
        }

        final String message = makeLabel(context, R.plurals.NNNtracksdeleted, list.length);

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        // We deleted a number of tracks, which could affect any number of
        // things
        // in the media content domain, so update everything.
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        // Notify the lists to update
        refresh();
    }

    /**
     * Simple function used to determine if the song/album year is invalid
     * @param year value to test
     * @return true if the app considers it valid
     */
    public static boolean isInvalidYear(int year) {
        return year < MIN_VALID_YEAR;
    }

    /**
     * A snippet is taken from MediaStore.Audio.keyFor method
     * This will take a name, removes things like "the", "an", etc
     * as well as special characters and return it
     * @param name the string to trim
     * @return the trimmed name
     */
    public static String getTrimmedName(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }

        name = name.trim().toLowerCase();
        if (name.startsWith("the ")) {
            name = name.substring(4);
        }
        if (name.startsWith("an ")) {
            name = name.substring(3);
        }
        if (name.startsWith("a ")) {
            name = name.substring(2);
        }
        if (name.endsWith(", the") || name.endsWith(",the") ||
                name.endsWith(", an") || name.endsWith(",an") ||
                name.endsWith(", a") || name.endsWith(",a")) {
            name = name.substring(0, name.lastIndexOf(','));
        }
        name = name.replaceAll("[\\[\\]\\(\\)\"'.,?!]", "").trim();

        return name;
    }

    /**
     * A snippet is taken from MediaStore.Audio.keyFor method
     * This will take a name, removes things like "the", "an", etc
     * as well as special characters, then find the localized label
     * @param name Name to get the label of
     * @return the localized label of the bucket that the name falls into
     */
    public static String getLocalizedBucketLetter(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }

        name = getTrimmedName(name);

        if (name.length() > 0) {
            return LocaleUtils.getInstance().getLabel(name);
        }

        return null;
    }

    /** @return true if a string is null, empty, or contains only whitespace */
    public static boolean isBlank(String s) {
        if(s == null) { return true; }
        if(s.isEmpty()) { return true; }
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(!Character.isWhitespace(c)) { return false; }
        }
        return true;
    }

    /**
     * Removes the header image from the cache.
     */
    @WorkerThread
    public static void removeFromCache(Activity activity, String key) {
        ImageFetcher imageFetcher = ElevenUtils.getImageFetcher(activity);
        imageFetcher.removeFromCache(key);

        // Give the disk cache a little time before requesting a new image.
        // TODO: this is bad
        SystemClock.sleep(80);
    }

    /**
     * Removes image from cache so that the stock image is retrieved on reload
     */
    public static void selectOldPhoto(Activity activity, String key) {
        // First remove the old image
        removeFromCache(activity, key);
        MusicUtils.refresh();
    }

    /**
     *
     * @param sortOrder values are mostly derived from SortOrder.class or could also be any sql
     *                  order clause
     * @return
     */
    public static boolean isSortOrderDesending(String sortOrder) {
        return sortOrder.endsWith(" DESC");
    }

    /**
     * Takes a collection of items and builds a comma-separated list of them
     * @param items collection of items
     * @return comma-separted list of items
     */
    public static <E> String buildCollectionAsString(Collection<E> items) {
        Iterator<E> iterator = items.iterator();
        StringBuilder str = new StringBuilder();
        if (iterator.hasNext()) {
            str.append(iterator.next());
            while (iterator.hasNext()) {
                str.append(",");
                str.append(iterator.next());
            }
        }

        return str.toString();
    }

    public static class IgnoreCaseComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            return s1.compareToIgnoreCase(s2);
        }
    }
}
