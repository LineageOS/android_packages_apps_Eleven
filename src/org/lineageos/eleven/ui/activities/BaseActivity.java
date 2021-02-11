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
package org.lineageos.eleven.ui.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.lineageos.eleven.MusicPlaybackService;
import org.lineageos.eleven.MusicStateListener;
import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ICacheListener;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.Lists;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.MusicUtils.ServiceToken;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.widgets.PlayPauseButtonContainer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A base {@link FragmentActivity} used to update the bottom bar and
 * bind to Eleven's service.
 * <p>
 * {@link SlidingPanelActivity} extends from this skeleton.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class BaseActivity extends AppCompatActivity implements ServiceConnection,
        MusicStateListener, ICacheListener {

    /**
     * Play-State and meta change listener
     */
    private final ArrayList<MusicStateListener> mMusicStateListener = Lists.newArrayList();

    private Toolbar mToolBar;

    private int mActionBarHeight;

    /**
     * The service token
     */
    private ServiceToken mToken;

    /**
     * Play pause progress button
     */
    private PlayPauseButtonContainer mPlayPauseButtonContainer;

    /**
     * Track name (BAB)
     */
    private TextView mTrackName;

    /**
     * Artist name (BAB)
     */
    private TextView mArtistName;

    /**
     * Album art (BAB)
     */
    private ImageView mAlbumArt;

    /**
     * Broadcast receiver
     */
    private PlaybackStatus mPlaybackStatus;

    private Drawable mActionBarBackground;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus(this);

        // Calculate ActionBar height
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true)) {
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(value.data,
                    getResources().getDisplayMetrics());
        }

        // Set the layout
        setContentView(setContentView());

        mToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

        setActionBarTitle(getString(R.string.app_name));

        // set the background on the root view
        getWindow().getDecorView().getRootView().setBackgroundColor(
                ContextCompat.getColor(this, R.color.background_color));
        // Initialze the bottom action bar
        initBottomActionBar();

        // listen to changes to the cache status
        ImageFetcher.getInstance(this).addCacheListener(this);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        onMetaChanged();
        // if there were any pending intents while the service was started
        handlePendingPlaybackRequests();
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Settings
        getMenuInflater().inflate(R.menu.activity_base, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            // Settings
            NavUtils.openSettings(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        onMetaChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind Eleven's service
        mToken = MusicUtils.bindToService(this, this);

        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        // If a playlist has changed, notify us
        filter.addAction(MusicPlaybackService.PLAYLIST_CHANGED);
        // If there is an error playing a track
        filter.addAction(MusicPlaybackService.TRACK_ERROR);
        registerReceiver(mPlaybackStatus, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        MusicUtils.unbindFromService(mToken);
        mToken = null;

        // Unregister the receiver
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove any music status listeners
        mMusicStateListener.clear();

        // remove cache listeners
        ImageFetcher.getInstance(this).removeCacheListener(this);
    }

    public void setupActionBar(int resId) {
        setupActionBar(getString(resId));
    }

    public void setupActionBar(String title) {
        setActionBarTitle(title);

        if (mActionBarBackground == null) {
            final int actionBarColor = ContextCompat.getColor(this,
                    R.color.header_action_bar_color);
            mActionBarBackground = new ColorDrawable(actionBarColor);
            mToolBar.setBackground(mActionBarBackground);
        }
    }

    public void setActionBarTitle(String title) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    public void setActionBarAlpha(int alpha) {
        mActionBarBackground.setAlpha(alpha);
    }

    public void setActionBarElevation(boolean isElevated) {
        float targetElevation = isElevated
                ? getResources().getDimension(R.dimen.action_bar_elevation) : 0;
        mToolBar.setElevation(targetElevation);
    }

    public void setFragmentPadding(boolean enablePadding) {
        final int height = enablePadding ? mActionBarHeight : 0;
        findViewById(R.id.activity_base_content).setPadding(0, height, 0, 0);
    }

    /**
     * Initializes the items in the bottom action bar.
     */
    protected void initBottomActionBar() {
        // Play and pause button
        mPlayPauseButtonContainer = findViewById(R.id.playPauseProgressButton);
        mPlayPauseButtonContainer.enableAndShow();

        // Track name
        mTrackName = findViewById(R.id.bottom_action_bar_line_one);
        // Artist name
        mArtistName = findViewById(R.id.bottom_action_bar_line_two);
        // Album art
        mAlbumArt = findViewById(R.id.bottom_action_bar_album_art);
        // Open to the currently playing album profile
        mAlbumArt.setOnClickListener(mOpenCurrentAlbumProfile);
    }

    protected void clearMetaInfo() {
        mAlbumArt.setImageResource(R.drawable.default_artwork);
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private void updateBottomActionBarInfo() {
        // Set the track name
        mTrackName.setText(MusicUtils.getTrackName());
        // Set the artist name
        mArtistName.setText(MusicUtils.getArtistName());
        // Set the album art
        ElevenUtils.getImageFetcher(this).loadCurrentArtwork(mAlbumArt);
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private void updatePlaybackControls() {
        // Set the play and pause image
        mPlayPauseButtonContainer.updateState();
    }

    /**
     * Opens the album profile of the currently playing album
     */
    private final View.OnClickListener mOpenCurrentAlbumProfile = v -> {
        if (MusicUtils.getCurrentAudioId() != -1) {
            NavUtils.openAlbumProfile(BaseActivity.this, MusicUtils.getAlbumName(),
                    MusicUtils.getArtistName(), MusicUtils.getCurrentAlbumId());
        } else {
            MusicUtils.shuffleAll(BaseActivity.this);
        }
    };

    /**
     * Used to monitor the state of playback
     */
    private final static class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<BaseActivity> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final BaseActivity activity) {
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null || action.isEmpty()) {
                return;
            }

            final BaseActivity baseActivity = mReference.get();
            if (baseActivity == null) {
                return;
            }
            switch (action) {
                case MusicPlaybackService.META_CHANGED:
                    baseActivity.onMetaChanged();
                    break;
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    baseActivity.mPlayPauseButtonContainer.updateState();
                    break;
                case MusicPlaybackService.REFRESH:
                    baseActivity.restartLoader();
                    break;
                case MusicPlaybackService.PLAYLIST_CHANGED:
                    baseActivity.onPlaylistChanged();
                    break;
                case MusicPlaybackService.TRACK_ERROR:
                    final String errorMsg = context.getString(R.string.error_playing_track,
                            intent.getStringExtra(MusicPlaybackService.TrackErrorExtra.TRACK_NAME));
                    Toast.makeText(baseActivity, errorMsg, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void onMetaChanged() {
        // update action bar info
        updateBottomActionBarInfo();

        // Let the listener know to the meta changed
        for (final MusicStateListener listener : mMusicStateListener) {
            if (listener != null) {
                listener.onMetaChanged();
            }
        }
    }

    @Override
    public void restartLoader() {
        // Let the listener know to update a list
        for (final MusicStateListener listener : mMusicStateListener) {
            if (listener != null) {
                listener.restartLoader();
            }
        }
    }

    @Override
    public void onPlaylistChanged() {
        // Let the listener know to update a list
        for (final MusicStateListener listener : mMusicStateListener) {
            if (listener != null) {
                listener.onPlaylistChanged();
            }
        }
    }

    /**
     * @param status The {@link MusicStateListener} to use
     */
    public void setMusicStateListenerListener(final MusicStateListener status) {
        if (status == this) {
            throw new UnsupportedOperationException("Override the method, don't add a listener");
        }

        if (status != null) {
            mMusicStateListener.add(status);
        }
    }

    /**
     * @param status The {@link MusicStateListener} to use
     */
    public void removeMusicStateListenerListener(final MusicStateListener status) {
        if (status != null) {
            mMusicStateListener.remove(status);
        }
    }

    @Override
    public void onCacheResumed() {
        // Set the album art
        ElevenUtils.getImageFetcher(this).loadCurrentArtwork(mAlbumArt);
    }

    /**
     * @return The resource ID to be inflated.
     */
    public abstract int setContentView();

    /**
     * handle pending playback requests
     */
    public abstract void handlePendingPlaybackRequests();
}
