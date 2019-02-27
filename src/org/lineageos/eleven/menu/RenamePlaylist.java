/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2019 The LineageOS Project
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

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.text.Editable;
import android.text.TextUtils;

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.MusicUtils;

import androidx.annotation.NonNull;

/**
 * Alert dialog used to rename playlists.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RenamePlaylist extends BasePlaylistDialog {
    private long mRenameId;

    /**
     * @param id The Id of the playlist to rename
     * @return A new instance of this dialog.
     */
    public static RenamePlaylist getInstance(final Long id) {
        final RenamePlaylist frag = new RenamePlaylist();
        final Bundle args = new Bundle();
        args.putLong(EXTRA_RENAME, id);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outcicle) {
        outcicle.putString(EXTRA_DEFAULT_NAME, mPlaylist.getText().toString());
        outcicle.putLong(EXTRA_RENAME, mRenameId);
    }

    @Override
    public void initialize(final Bundle savedInstanceState) {
        mRenameId = savedInstanceState != null
                ? savedInstanceState.getLong(EXTRA_RENAME)
                : getArguments().getLong(EXTRA_RENAME, -1);
        final String originalName = MusicUtils.getNameForPlaylist(getContext(), mRenameId);
        mDefaultName = savedInstanceState != null
                ? savedInstanceState.getString(EXTRA_DEFAULT_NAME)
                : originalName;
        if (mRenameId < 0 || originalName == null || mDefaultName == null) {
            getDialog().dismiss();
            return;
        }
        mPrompt = getString(R.string.create_playlist_prompt);
    }

    @Override
    public void onSaveClick() {
        final String playlistName = mPlaylist.getText().toString();
        if (!TextUtils.isEmpty(playlistName)) {
            final ContentResolver resolver = getActivity().getContentResolver();
            final ContentValues values = new ContentValues(1);
            values.put(Audio.Playlists.NAME, playlistName);
            resolver.update(Audio.Playlists.EXTERNAL_CONTENT_URI, values,
                    MediaStore.Audio.Playlists._ID + "=?", new String[]{
                            String.valueOf(mRenameId)
                    });
            getDialog().dismiss();
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
}
