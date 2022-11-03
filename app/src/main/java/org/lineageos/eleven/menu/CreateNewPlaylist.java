/*
 * Copyright (C) 2012 Andrew Neal
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
package org.lineageos.eleven.menu;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.MusicUtils;

public class CreateNewPlaylist extends BasePlaylistDialog {
    private long[] mPlaylistList = new long[]{};

    /**
     * @param list The list of tracks to add to the playlist
     * @return A new instance of this dialog.
     */
    public static CreateNewPlaylist getInstance(final long[] list) {
        final CreateNewPlaylist frag = new CreateNewPlaylist();
        final Bundle args = new Bundle();
        args.putLongArray(EXTRA_PLAYLIST_LIST, list);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outcicle) {
        outcicle.putString(EXTRA_DEFAULT_NAME, mPlaylist.getText().toString());
    }

    @Override
    public void initialize(final Bundle savedInstanceState) {
        mDefaultName = savedInstanceState != null
                ? savedInstanceState.getString(EXTRA_DEFAULT_NAME)
                : makePlaylistName();
        if (mDefaultName == null) {
            final Dialog dialog = getDialog();
            if (dialog != null) {
                dialog.dismiss();
            }
            return;
        }
        final Bundle args = getArguments();
        mPlaylistList = args == null ? new long[]{} : args.getLongArray(EXTRA_PLAYLIST_LIST);
        mPrompt = getString(R.string.create_playlist_prompt);
    }

    @Override
    public void onSaveClick() {
        final String playlistName = mPlaylist.getText().toString();
        final Activity activity = getActivity();
        if (activity == null || TextUtils.isEmpty(playlistName)) {
            return;
        }

        final int playlistId = (int) MusicUtils.getIdForPlaylist(getActivity(), playlistName);
        if (playlistId >= 0) {
            MusicUtils.clearPlaylist(activity, playlistId);
            MusicUtils.addToPlaylist(activity, mPlaylistList, playlistId);
        } else {
            final long newId = MusicUtils.createPlaylist(getActivity(), playlistName);
            MusicUtils.addToPlaylist(activity, mPlaylistList, newId);
        }
        final Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        mSaveButton = mPlaylistDialog.getButton(Dialog.BUTTON_POSITIVE);
        if (mSaveButton == null) {
            return;
        }

        final String playlistName = (editable == null ? "" : editable.toString().trim());
        if (playlistName.length() == 0) {
            mSaveButton.setEnabled(false);
        } else {
            mSaveButton.setEnabled(true);
            if (MusicUtils.getIdForPlaylist(getActivity(), playlistName) >= 0) {
                mSaveButton.setText(R.string.overwrite);
            } else {
                mSaveButton.setText(R.string.save);
            }
        }
    }

    private String makePlaylistName() {
        final Activity activity = getActivity();
        if (activity == null) {
            return null;
        }
        final ContentResolver resolver = activity.getContentResolver();
        final String[] projection = new String[]{MediaStore.Audio.Playlists.NAME};
        final String selection = MediaStore.Audio.Playlists.NAME + " != ''";
        try (final Cursor cursor = resolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, projection,
                selection, null, MediaStore.Audio.Playlists.NAME)) {
            if (cursor == null) {
                return null;
            }

            final String template = getString(R.string.new_playlist_name_template);
            int num = 1;
            String suggestedname = String.format(template, num++);
            boolean done = false;
            while (!done) {
                done = true;
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    final String playlistName = cursor.getString(0);
                    if (playlistName.compareToIgnoreCase(suggestedname) == 0) {
                        suggestedname = String.format(template, num++);
                        done = false;
                    }
                    cursor.moveToNext();
                }
            }
            return suggestedname;
        }
    }
}
