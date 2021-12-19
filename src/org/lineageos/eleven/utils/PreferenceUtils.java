/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2018-2021 The LineageOS Project
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

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import org.lineageos.eleven.ui.fragments.AlbumFragment;
import org.lineageos.eleven.ui.fragments.ArtistFragment;
import org.lineageos.eleven.ui.fragments.SongFragment;
import org.lineageos.eleven.ui.fragments.phone.MusicBrowserPhoneFragment;

/**
 * A collection of helpers designed to get and set various preferences across
 * Eleven.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class PreferenceUtils {

    // Default start page (Artist page)
    public static final int DEFAULT_PAGE = 2;

    // Saves the last page the pager was on in {@link MusicBrowserPhoneFragment}
    public static final String START_PAGE = "start_page";

    // Sort order for the artist list
    public static final String ARTIST_SORT_ORDER = "artist_sort_order";

    // Sort order for the album list
    public static final String ALBUM_SORT_ORDER = "album_sort_order";

    // Sort order for the album song list
    public static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";

    // Sort order for the song list
    public static final String SONG_SORT_ORDER = "song_sort_order";

    // datetime cutoff for determining which songs go in last added playlist
    public static final String LAST_ADDED_CUTOFF = "last_added_cutoff";

    // show lyrics option
    public static final String SHOW_LYRICS = "show_lyrics";

    // show visualizer flag
    public static final String SHOW_VISUALIZER = "music_visualization";

    // shake to play flag
    public static final String SHAKE_TO_PLAY = "shake_to_play";

    public static final int PERMISSION_REQUEST_STORAGE = 1;
    public static final int PERMISSION_REQUEST_RECORD_AUDIO = 2;

    private static PreferenceUtils sInstance;

    private final SharedPreferences mPreferences;

    /**
     * Constructor for <code>PreferenceUtils</code>
     *
     * @param context The {@link Context} to use.
     */
    public PreferenceUtils(final Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @param context The {@link Context} to use.
     * @return A singleton of this class
     */
    public static PreferenceUtils getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new PreferenceUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Saves the current page the user is on when they close the app.
     *
     * @param value The last page the pager was on when the onDestroy is called
     *              in {@link MusicBrowserPhoneFragment}.
     */
    public void setStartPage(final int value) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(START_PAGE, value);
        editor.apply();
    }

    /**
     * Set the listener for preference change
     */
    public void setOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Set the listener for preference change
     */
    public void removeOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Returns the last page the user was on when the app was exited.
     *
     * @return The page to start on when the app is opened.
     */
    public final int getStartPage() {
        return mPreferences.getInt(START_PAGE, DEFAULT_PAGE);
    }

    /**
     * Saves the sort order for a list.
     *
     * @param key   Which sort order to change
     * @param value The new sort order
     */
    private void setSortOrder(final String key, final String value) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Sets the sort order for the artist list.
     *
     * @param value The new sort order
     */
    public void setArtistSortOrder(final String value) {
        setSortOrder(ARTIST_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the artist list in {@link ArtistFragment}
     */
    public final String getArtistSortOrder() {
        return mPreferences.getString(ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z);
    }

    /**
     * Sets the sort order for the album list.
     *
     * @param value The new sort order
     */
    public void setAlbumSortOrder(final String value) {
        setSortOrder(ALBUM_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the album list in {@link AlbumFragment}
     */
    public final String getAlbumSortOrder() {
        return mPreferences.getString(ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z);
    }

    /**
     * @return The sort order used for the album song in
     * AlbumSongFragment
     */
    public final String getAlbumSongSortOrder() {
        return mPreferences.getString(ALBUM_SONG_SORT_ORDER,
                SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
    }

    /**
     * Sets the sort order for the song list.
     *
     * @param value The new sort order
     */
    public void setSongSortOrder(final String value) {
        setSortOrder(SONG_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the song list in {@link SongFragment}
     */
    public final String getSongSortOrder() {
        return mPreferences.getString(SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
    }

    /**
     * @param lastAddedMillis timestamp in millis used as a cutoff for last added playlist
     */
    public void setLastAddedCutoff(long lastAddedMillis) {
        mPreferences.edit().putLong(LAST_ADDED_CUTOFF, lastAddedMillis).apply();
    }

    public long getLastAddedCutoff() {
        return mPreferences.getLong(LAST_ADDED_CUTOFF, 0L);
    }

    /**
     * @return Whether we want to show lyrics
     */
    public final boolean getShowLyrics() {
        return mPreferences.getBoolean(SHOW_LYRICS, true);
    }

    public static boolean canRecordAudio(Activity activity) {
        return activity.checkSelfPermission(permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public static void requestRecordAudio(Activity activity) {
        activity.requestPermissions(
                new String[]{permission.RECORD_AUDIO},
                PERMISSION_REQUEST_RECORD_AUDIO);
    }

    public boolean getShowVisualizer() {
        return mPreferences.getBoolean(SHOW_VISUALIZER, false);
    }

    public boolean getShakeToPlay() {
        return mPreferences.getBoolean(SHAKE_TO_PLAY, false);
    }
}
