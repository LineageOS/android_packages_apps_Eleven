/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2020-2021 The LineageOS Project
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
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.ui.activities.HomeActivity;
import org.lineageos.eleven.utils.MusicUtils;

/**
 * Used when the user requests to modify Album art or Artist image
 * It provides an easy interface for them to choose a new image or use the old
 * image.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PhotoSelectionDialog extends DialogFragment {

    private static final int NEW_PHOTO = 0;

    private static final int DEFAULT_PHOTO = 1;

    private String mKey;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public PhotoSelectionDialog() {
    }

    /**
     * @param title The dialog title.
     * @param key   key to query ImageFetcher
     * @return A new instance of the dialog.
     */
    public static PhotoSelectionDialog newInstance(final String title, String key) {
        final PhotoSelectionDialog frag = new PhotoSelectionDialog();
        final Bundle args = new Bundle();
        args.putString(Config.NAME, title);
        frag.setArguments(args);
        frag.mKey = key;
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final String title = args == null ? "" : args.getString(Config.NAME);
        final String[] choices = new String[2];
        // Select a photo from the gallery
        choices[NEW_PHOTO] = getString(R.string.new_photo);
        // Default photo
        choices[DEFAULT_PHOTO] = getString(R.string.use_default);

        // Dialog item Adapter
        final HomeActivity activity = (HomeActivity) getActivity();
        final ListAdapter adapter = new ArrayAdapter<>(activity,
                android.R.layout.select_dialog_item, choices);
        return new AlertDialog.Builder(activity).setTitle(title)
                .setAdapter(adapter, (dialog, which) -> {
                    switch (which) {
                        case NEW_PHOTO:
                            if (activity != null) {
                                activity.selectNewPhoto(mKey);
                            }
                            break;
                        case DEFAULT_PHOTO:
                            MusicUtils.selectOldPhoto(activity, mKey);
                            break;
                        default:
                            break;
                    }
                })
                .create();
    }
}
