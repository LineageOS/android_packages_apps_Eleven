/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2018-2020 The LineageOS Project
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
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PreferenceUtils;

public class SettingsActivity extends AppCompatActivity
        implements ServiceConnection {

    // The service token
    private MusicUtils.ServiceToken mToken;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Bind Eleven's service
        mToken = MusicUtils.bindToService(this, this);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        MusicUtils.unbindFromService(mToken);
        mToken = null;
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        // empty
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // empty
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final Preference deleteCache = findPreference("delete_cache");
            deleteCache.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.delete_warning)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                ImageFetcher.getInstance(getContext()).clearCaches())
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            });

            PreferenceUtils.getInstance(getContext()).setOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            PreferenceUtils.getInstance(getContext()).removeOnSharedPreferenceChangeListener(this);
            super.onDestroy();
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
            }
        }
    }
}
