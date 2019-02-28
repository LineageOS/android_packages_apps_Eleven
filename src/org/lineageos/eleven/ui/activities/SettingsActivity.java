/*
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

package org.lineageos.eleven.ui.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PreferenceUtils;

public class SettingsActivity extends AppCompatActivity {
    private Drawable mActionBarBackground;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (mActionBarBackground == null) {
            final int actionBarColor = ContextCompat.getColor(this,
                    R.color.header_action_bar_color);
            mActionBarBackground = new ColorDrawable(actionBarColor);
            toolbar.setBackground(mActionBarBackground);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_base_content, new SettingsFragment())
                .commit();
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

    public static class SettingsFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final Preference deleteCache = findPreference("delete_cache");
            deleteCache.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getContext()).setMessage(R.string.delete_warning)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                ImageFetcher.getInstance(getContext()).clearCaches())
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .create().show();
                return true;
            });

            PreferenceUtils.getInstance(getContext()).setOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.settings, rootKey);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case PreferenceUtils.SHOW_VISUALIZER: {
                    final boolean showVisualizer = sharedPreferences.getBoolean(key, false);
                    if (showVisualizer && !PreferenceUtils.canRecordAudio(getActivity())) {
                        PreferenceUtils.requestRecordAudio(getActivity());
                    }
                    break;
                }
                case PreferenceUtils.USE_BLUR: {
                    final boolean useBlur = sharedPreferences.getBoolean(key, false);
                    ImageFetcher.getInstance(getActivity()).setUseBlur(useBlur);
                    ImageFetcher.getInstance(getActivity()).clearCaches();
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
}
