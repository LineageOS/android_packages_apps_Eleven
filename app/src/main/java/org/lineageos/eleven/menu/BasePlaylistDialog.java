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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.MusicUtils;

/**
 * A simple base class for the playlist dialogs.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class BasePlaylistDialog extends DialogFragment implements TextWatcher {
    public static final String EXTRA_DEFAULT_NAME = "default_name";
    public static final String EXTRA_PLAYLIST_LIST = "playlist_list";
    public static final String EXTRA_RENAME = "rename";

    /* The actual dialog */
    protected AlertDialog mPlaylistDialog;

    /* Used to make new playlist names */
    protected EditText mPlaylist;

    /* The dialog save button */
    protected Button mSaveButton;

    /* The dialog prompt */
    protected String mPrompt;

    /* The default edit text text */
    protected String mDefaultName;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        mPlaylistDialog = new AlertDialog.Builder(getActivity()).create();
        mPlaylistDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.save),
                (dialog, which) -> {
                    onSaveClick();
                    MusicUtils.refresh();
                    dialog.dismiss();
                });
        mPlaylistDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                (dialog, which) -> {
                    MusicUtils.refresh();
                    dialog.dismiss();
                });

        mPlaylist = new EditText(getActivity());
        mPlaylist.setSingleLine(true);
        mPlaylist.setInputType(mPlaylist.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mPlaylist.post(() -> {
            // Request focus to the edit text
            mPlaylist.requestFocus();
            // Select the playlist name
            mPlaylist.selectAll();
        });

        initialize(savedInstanceState);

        mPlaylist.setText(mDefaultName);
        mPlaylist.setSelection(mDefaultName.length());
        mPlaylist.addTextChangedListener(this);

        mPlaylistDialog.setTitle(mPrompt);
        mPlaylistDialog.setView(mPlaylist);
        mPlaylistDialog.getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mPlaylistDialog.show();
        return mPlaylistDialog;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        /* Nothing to do */
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count,
                                  final int after) {
        /* Nothing to do */
    }

    /**
     * Initializes the prompt and default name
     */
    public abstract void initialize(Bundle savedInstanceState);

    /**
     * Called when the save button of our {@link AlertDialog} is pressed
     */
    public abstract void onSaveClick();

}
