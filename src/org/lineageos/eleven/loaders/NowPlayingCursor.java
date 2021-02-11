/*
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import org.lineageos.eleven.utils.MusicUtils;

import java.util.Arrays;

/**
 * A custom {@link Cursor} used to return the queue and allow for easy dragging
 * and dropping of the items in it.
 */
@SuppressLint("NewApi")
public class NowPlayingCursor extends AbstractCursor {

    private static final String[] PROJECTION = new String[] {
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
    };

    private final Context mContext;

    private long[] mNowPlaying;

    private long[] mCursorIndexes;

    private int mSize;

    private int mCurPos;

    private Cursor mQueueCursor;

    /**
     * Constructor of <code>NowPlayingCursor</code>
     *
     * @param context The {@link Context} to use
     */
    public NowPlayingCursor(final Context context) {
        mContext = context;
        makeNowPlayingCursor();
    }

    @Override
    public int getCount() {
        return mSize;
    }

    @Override
    public boolean onMove(final int oldPosition, final int newPosition) {
        if (oldPosition == newPosition) {
            return true;
        }

        if (mNowPlaying == null || mCursorIndexes == null || newPosition >= mNowPlaying.length) {
            return false;
        }

        final long id = mNowPlaying[newPosition];
        final int cursorIndex = Arrays.binarySearch(mCursorIndexes, id);
        mQueueCursor.moveToPosition(cursorIndex);
        mCurPos = newPosition;
        return true;
    }

    @Override
    public String getString(final int column) {
        try {
            return mQueueCursor.getString(column);
        } catch (final Exception ignored) {
            onChange(true);
            return "";
        }
    }

    @Override
    public short getShort(final int column) {
        return mQueueCursor.getShort(column);
    }

    @Override
    public int getInt(final int column) {
        try {
            return mQueueCursor.getInt(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    @Override
    public long getLong(final int column) {
        try {
            return mQueueCursor.getLong(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    @Override
    public float getFloat(final int column) {
        return mQueueCursor.getFloat(column);
    }

    @Override
    public double getDouble(final int column) {
        return mQueueCursor.getDouble(column);
    }

    @Override
    public int getType(final int column) {
        return mQueueCursor.getType(column);
    }

    @Override
    public boolean isNull(final int column) {
        return mQueueCursor.isNull(column);
    }

    @Override
    public String[] getColumnNames() {
        return PROJECTION;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void deactivate() {
        if (mQueueCursor != null) {
            mQueueCursor.deactivate();
        }
    }

    @Override
    public boolean requery() {
        makeNowPlayingCursor();
        return true;
    }

    @Override
    public void close() {
        try {
            if (mQueueCursor != null) {
                mQueueCursor.close();
                mQueueCursor = null;
            }
        } catch (final Exception ignored) {
        }
        super.close();
    }

    /**
     * Actually makes the queue
     */
    private void makeNowPlayingCursor() {
        mQueueCursor = null;
        mNowPlaying = MusicUtils.getQueue();
        mSize = mNowPlaying.length;
        if (mSize == 0) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Media._ID + " IN (");
        for (int i = 0; i < mSize; i++) {
            selection.append(mNowPlaying[i]);
            if (i < mSize - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        mQueueCursor = mContext.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, PROJECTION, selection.toString(),
                null, MediaStore.Audio.Media._ID);

        if (mQueueCursor == null) {
            mSize = 0;
            return;
        }

        final int playlistSize = mQueueCursor.getCount();
        mCursorIndexes = new long[playlistSize];
        mQueueCursor.moveToFirst();
        final int columnIndex = mQueueCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        for (int i = 0; i < playlistSize; i++) {
            mCursorIndexes[i] = mQueueCursor.getLong(columnIndex);
            mQueueCursor.moveToNext();
        }
        mQueueCursor.moveToFirst();
        mCurPos = -1;

        int removed = 0;
        for (int i = mNowPlaying.length - 1; i >= 0; i--) {
            final long trackId = mNowPlaying[i];
            final int cursorIndex = Arrays.binarySearch(mCursorIndexes, trackId);
            if (cursorIndex < 0) {
                removed += MusicUtils.removeTrack(trackId);
            }
        }
        if (removed > 0) {
            mNowPlaying = MusicUtils.getQueue();
            mSize = mNowPlaying.length;
            if (mSize == 0) {
                mCursorIndexes = null;
            }
        }
    }

    /**
     * @param which The position to remove
     */
    public void removeItem(final int which) {
        if (!MusicUtils.removeTrackAtPosition(mNowPlaying[which], which)) {
            return;
        }
        int i = which;
        mSize--;
        while (i < mSize) {
            mNowPlaying[i] = mNowPlaying[i + 1];
            i++;
        }
        onMove(-1, mCurPos);
    }
}
