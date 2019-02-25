/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.lineageos.eleven.ui.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PreferenceUtils;

/**
 * Settings.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // UP
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Add the preferences
        addPreferencesFromResource(R.xml.settings);

        final Preference deleteCache = findPreference("delete_cache");
        deleteCache.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(SettingsActivity.this).setMessage(R.string.delete_warning)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            ImageFetcher.getInstance(SettingsActivity.this).clearCaches())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .create().show();
            return true;
        });

        PreferenceUtils.getInstance(this).setOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferenceUtils.SHOW_VISUALIZER: {
                final boolean showVisualizer = sharedPreferences.getBoolean(key, false);
                if (showVisualizer && !PreferenceUtils.canRecordAudio(this)) {
                    PreferenceUtils.requestRecordAudio(this);
                }
                break;
            }
            case PreferenceUtils.USE_BLUR: {
                final boolean useBlur = sharedPreferences.getBoolean(key, false);
                ImageFetcher.getInstance(SettingsActivity.this).setUseBlur(useBlur);
                ImageFetcher.getInstance(SettingsActivity.this).clearCaches();
                break;
            }
            case PreferenceUtils.SHAKE_TO_PLAY: {
                final boolean enableShakeToPlay = sharedPreferences.getBoolean(key, false);
                MusicUtils.setShakeToPlayEnabled(enableShakeToPlay);
                break;
            }
            case PreferenceUtils.SHOW_ALBUM_ART_ON_LOCKSCREEN: {
                final boolean showAlbumArtOnLockscreen = sharedPreferences.getBoolean(key, true);
                MusicUtils.setShowAlbumArtOnLockscreen(showAlbumArtOnLockscreen);
                break;
            }
        }
    }
}
