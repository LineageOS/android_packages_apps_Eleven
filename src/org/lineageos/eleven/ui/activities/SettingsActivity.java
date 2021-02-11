/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2018-2021 The LineageOS Project
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

import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.lineageos.eleven.IElevenService;
import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PreferenceUtils;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            ServiceConnection, SharedPreferences.OnSharedPreferenceChangeListener {

        private SwitchPreference mShowVisualizer;

        private MusicUtils.ServiceToken mToken;

        private IElevenService mService;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final Preference deleteCache = findPreference("delete_cache");
            if (deleteCache != null) {
                deleteCache.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(getContext())
                            .setMessage(R.string.delete_warning)
                            .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                    ImageFetcher.getInstance(getContext()).clearCaches())
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .show();
                    return true;
                });
            }

            PreferenceUtils prefUtils = PreferenceUtils.getInstance(getContext());
            prefUtils.setOnSharedPreferenceChangeListener(this);

            mShowVisualizer = findPreference(PreferenceUtils.SHOW_VISUALIZER);
            if (mShowVisualizer != null) {
                final Activity activity = getActivity();
                // Otherwise we wouldn't notice if the permission has been denied via the Settings
                // app since the last time
                mShowVisualizer.setChecked(prefUtils.getShowVisualizer() && activity != null &&
                        PreferenceUtils.canRecordAudio(activity));
            }
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
        public void onStart() {
            super.onStart();

            // Bind to Eleven's service
            mToken = MusicUtils.bindToService(getActivity(), this);
        }

        @Override
        public void onStop() {
            super.onStop();

            // Unbind from the service
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mService = IElevenService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            final Activity activity = getActivity();
            switch (key) {
                case PreferenceUtils.SHOW_VISUALIZER:
                    final boolean showVisualizer = sharedPreferences.getBoolean(key, false);
                    if (showVisualizer && activity != null &&
                            !PreferenceUtils.canRecordAudio(activity)) {
                        requestRecordAudio();
                    }
                    break;

                case PreferenceUtils.USE_BLUR:
                    if (activity != null) {
                        final boolean useBlur = sharedPreferences.getBoolean(key, false);
                        final ImageFetcher fetcher = ImageFetcher.getInstance(activity);
                        fetcher.setUseBlur(useBlur);
                        fetcher.clearCaches();
                    }
                    break;

                case PreferenceUtils.SHAKE_TO_PLAY:
                    final boolean enableShakeToPlay = sharedPreferences.getBoolean(key, false);
                    try {
                        mService.setShakeToPlayEnabled(enableShakeToPlay);
                    } catch (final RemoteException exc) {
                        // do nothing
                    }
                    break;
            }
        }

        /* We can't call PreferenceUtils.requestRecordAudio since it's called from activity context
           and we need requestPermissions to be called for the fragment so
           onRequestPermissionsResult gets called */
        private void requestRecordAudio() {
            requestPermissions(new String[]{permission.RECORD_AUDIO},
                    PreferenceUtils.PERMISSION_REQUEST_RECORD_AUDIO);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (requestCode == PreferenceUtils.PERMISSION_REQUEST_RECORD_AUDIO) {
                boolean showRationale = shouldShowRequestPermissionRationale(permissions[0]);
                if (grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED) {
                    mShowVisualizer.setChecked(false);
                    if (!showRationale) {
                        new AlertDialog.Builder(getContext())
                                .setMessage(R.string.visualizer_perm_denied)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
