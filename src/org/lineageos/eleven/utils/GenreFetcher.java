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
package org.lineageos.eleven.utils;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class GenreFetcher implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String[] GENRE_PROJECTION = new String[]{MediaStore.Audio.Genres.NAME};

    private final Context mContext;
    private final int mSongId;
    private final TextView mTextView;

    public static void fetch(FragmentActivity activity, int songId, TextView textView) {
        LoaderManager lm = LoaderManager.getInstance(activity);
        lm.restartLoader(0, null, new GenreFetcher(activity, songId, textView));
    }

    private GenreFetcher(Context context, int songId, TextView textView) {
        mContext = context;
        mSongId = songId;
        mTextView = textView;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext,
                MediaStore.Audio.Genres.getContentUriForAudioId("external", mSongId),
                GENRE_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (mTextView == null) {
            return;
        }
        if (!cursor.moveToFirst()) {
            // no displayable genre found
            mTextView.setVisibility(View.GONE);
        }
        String genre = cursor.getString(0);
        if (MusicUtils.isBlank(genre)) {
            mTextView.setText(genre);
            mTextView.setVisibility(View.VISIBLE);
        } else {
            mTextView.setText(genre);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}
