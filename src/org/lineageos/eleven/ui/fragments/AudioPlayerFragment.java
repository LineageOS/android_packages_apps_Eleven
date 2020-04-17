/*
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
package org.lineageos.eleven.ui.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import org.lineageos.eleven.MusicPlaybackService;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.AlbumArtPagerAdapter;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.loaders.NowPlayingCursor;
import org.lineageos.eleven.loaders.QueueLoader;
import org.lineageos.eleven.menu.CreateNewPlaylist;
import org.lineageos.eleven.menu.DeleteDialog;
import org.lineageos.eleven.ui.activities.HomeActivity;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.utils.PreferenceUtils;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;
import org.lineageos.eleven.widgets.MainPlaybackControls;
import org.lineageos.eleven.widgets.NoResultsContainer;
import org.lineageos.eleven.widgets.VisualizerView;

import java.lang.ref.WeakReference;
import java.util.List;

public class AudioPlayerFragment extends Fragment implements ServiceConnection {
    private static final String TAG = AudioPlayerFragment.class.getSimpleName();

    private AlertDialog mAlertDialog;

    // fragment view
    private ViewGroup mRootView;

    private Toolbar mPlayerToolBar;

    // Header views
    private TextView mSongTitle;
    private TextView mArtistName;

    // Message to refresh the time
    private static final int REFRESH_TIME = 1;

    // The service token
    private MusicUtils.ServiceToken mToken;

    // Album art ListView
    private ViewPager mAlbumArtViewPager;
    private LoadingEmptyContainer mQueueEmpty;

    // Visualizer View
    private VisualizerView mVisualizerView;

    private MainPlaybackControls mMainPlaybackControls;

    // Broadcast receiver
    private PlaybackStatus mPlaybackStatus;

    // Handler used to update the current time
    private TimeHandler mTimeHandler;

    // Image cache
    private ImageFetcher mImageFetcher;

    // Lyrics text view
    private TextView mLyricsText;

    private long mSelectedId = -1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Control the media volume
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Initialize the image fetcher/cache
        mImageFetcher = ElevenUtils.getImageFetcher(getActivity());

        // Initialize the handler used to update the current time
        mTimeHandler = new TimeHandler(this);

        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus(this);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                final Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup) inflater.inflate(R.layout.activity_player_fragment, container,
                false);

        initHeaderBar();
        initPlaybackControls();

        mVisualizerView = mRootView.findViewById(R.id.visualizerView);
        mVisualizerView.initialize(getActivity());
        updateVisualizerPowerSaveMode();

        mLyricsText = mRootView.findViewById(R.id.audio_player_lyrics);

        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        final Menu playerMenu = mPlayerToolBar.getMenu();
        playerMenu.clear();

        // Shuffle all
        inflater.inflate(R.menu.shuffle_all, playerMenu);
        if (MusicUtils.getQueueSize() > 0) {
            // ringtone, and equalizer
            inflater.inflate(R.menu.audio_player, playerMenu);

            if (!NavUtils.hasEffectsPanel(getActivity())) {
                playerMenu.removeItem(R.id.menu_audio_player_equalizer);
            }

            // save queue/clear queue
            inflater.inflate(R.menu.queue, playerMenu);
        }
        // Settings
        inflater.inflate(R.menu.activity_base, playerMenu);

        final int playerMenuSize = playerMenu.size();
        for (int i = 0; i < playerMenuSize; i++) {
            playerMenu.getItem(i).setOnMenuItemClickListener(this::onOptionsItemSelected);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_audio_player_add_to_playlist: {
                // save the current track id
                mSelectedId = MusicUtils.getCurrentAudioId();
                final List<String> menuItemList = MusicUtils.makePlaylist(getActivity());

                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.add_to_playlist)
                        .setItems(menuItemList.toArray(new String[0]), (dialog, which) -> {
                            final long playListId = MusicUtils.getIdForPlaylist(getActivity(),
                                    menuItemList.get(which));
                            MusicUtils.addToPlaylist(getActivity(), new long[]{mSelectedId},
                                    playListId);
                        })
                        .setPositiveButton(R.string.new_playlist, (dialog, which) -> {
                            dialog.dismiss();
                            CreateNewPlaylist.getInstance(new long[]{mSelectedId})
                                    .show(getFragmentManager(), "CreatePlaylist");
                        });
                mAlertDialog = builder.show();
                return true;
            }
            case R.id.menu_shuffle_all:
                // Shuffle all the songs
                MusicUtils.shuffleAll(getActivity());
                return true;
            case R.id.menu_audio_player_ringtone:
                // Set the current track as a ringtone
                MusicUtils.setRingtone(getActivity(), MusicUtils.getCurrentAudioId());
                return true;
            case R.id.menu_audio_player_equalizer:
                // Sound effects
                NavUtils.openEffectsPanel(getActivity(), HomeActivity.EQUALIZER);
                return true;
            case R.id.menu_settings:
                // Settings
                NavUtils.openSettings(getActivity());
                return true;
            case R.id.menu_audio_player_more_by_artist:
                NavUtils.openArtistProfile(getActivity(), MusicUtils.getArtistName());
                return true;
            case R.id.menu_audio_player_delete:
                // Delete current song
                DeleteDialog.newInstance(MusicUtils.getTrackName(), new long[]{
                        MusicUtils.getCurrentAudioId()
                }, null).show(getActivity().getSupportFragmentManager(), "DeleteDialog");
                return true;
            case R.id.menu_save_queue:
                NowPlayingCursor queue = (NowPlayingCursor) QueueLoader
                        .makeQueueCursor(getActivity());
                CreateNewPlaylist.getInstance(MusicUtils.getSongListForCursor(queue)).show(
                        getFragmentManager(), "CreatePlaylist");
                queue.close();
                return true;
            case R.id.menu_clear_queue:
                MusicUtils.clearQueue();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        // Set the playback drawables
        mMainPlaybackControls.updatePlaybackControls();
        // Setup the adapter
        createAndSetAdapter();
        // Current info
        updateNowPlayingInfo();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // empty
    }

    @Override
    public void onStart() {
        super.onStart();

        // Bind Eleven's service
        mToken = MusicUtils.bindToService(getActivity(), this);

        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        // Listen to changes to the entire queue
        filter.addAction(MusicPlaybackService.QUEUE_CHANGED);
        // Listen for lyrics text for the audio track
        filter.addAction(MusicPlaybackService.NEW_LYRICS);
        // Listen for power save mode changed
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        // Register the intent filters
        getActivity().registerReceiver(mPlaybackStatus, filter);
        // Refresh the current time
        final long next = refreshCurrentTime();
        queueNextRefresh(next);
    }

    @Override
    public void onStop() {
        super.onStop();

        // pause the update callback for the play pause progress button
        mTimeHandler.removeMessages(REFRESH_TIME);

        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }

        mImageFetcher.flush();

        // Unbind from the service
        MusicUtils.unbindFromService(mToken);
        mToken = null;

        // Unregister the receiver
        try {
            getActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }
    }

    /**
     * Initializes the header bar
     */
    private void initHeaderBar() {
        mPlayerToolBar = mRootView.findViewById(R.id.audio_player_header);

        // Title text
        mSongTitle = mRootView.findViewById(R.id.header_bar_song_title);
        mArtistName = mRootView.findViewById(R.id.header_bar_artist_title);
    }

    /**
     * Initializes the items in the now playing screen
     */
    private void initPlaybackControls() {
        mMainPlaybackControls = mRootView.findViewById(R.id.main_playback_controls);

        // Album art view pager
        mAlbumArtViewPager = mRootView.findViewById(R.id.audio_player_album_art_viewpager);
        mAlbumArtViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                int currentPosition = 0;
                if (MusicUtils.getShuffleMode() == MusicPlaybackService.SHUFFLE_NONE) {
                    // if we aren't shuffling, base the position on the queue position
                    currentPosition = MusicUtils.getQueuePosition();
                } else {
                    // if we are shuffling, use the history size as the position
                    currentPosition = MusicUtils.getQueueHistorySize();
                }

                // check if we are going to next or previous track
                if (position - currentPosition == 1) {
                    MusicUtils.asyncNext(getActivity());
                } else if (position - currentPosition == -1) {
                    MusicUtils.previous(getActivity(), true);
                } else if (currentPosition != position) {
                    Log.w(TAG, "Unexpected page position of " + position
                            + " when current is: " + currentPosition);
                }
            }
        });
        // view to show in place of album art if queue is empty
        mQueueEmpty = mRootView.findViewById(R.id.loading_empty_container);
        setupNoResultsContainer(mQueueEmpty.getNoResultsContainer());
    }

    private void setupNoResultsContainer(NoResultsContainer empty) {
        int color = ContextCompat.getColor(getContext(), R.color.no_results_light);
        empty.setTextColor(color);
        empty.setMainText(R.string.empty_queue_main);
        empty.setSecondaryText(R.string.empty_queue_secondary);
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private void updateNowPlayingInfo() {
        // Set the track name
        mSongTitle.setText(MusicUtils.getTrackName());
        mArtistName.setText(MusicUtils.getArtistName());

        mMainPlaybackControls.updateNowPlayingInfo();

        if (MusicUtils.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
            // we are repeating 1 so just jump to the 1st and only item
            mAlbumArtViewPager.setCurrentItem(0, false);
        } else if (MusicUtils.getShuffleMode() == MusicPlaybackService.SHUFFLE_NONE) {
            // we are playing in-order, base the position on the queue position
            mAlbumArtViewPager.setCurrentItem(MusicUtils.getQueuePosition(), true);
        } else {
            // if we are shuffling, just based our index based on the history
            mAlbumArtViewPager.setCurrentItem(MusicUtils.getQueueHistorySize(), true);
        }

        // Update the current time
        queueNextRefresh(1);
    }

    /**
     * This creates the adapter based on the repeat and shuffle configuration and sets it into the
     * page adapter
     */
    private void createAndSetAdapter() {
        final AlbumArtPagerAdapter albumArtPagerAdapter =
                new AlbumArtPagerAdapter(getChildFragmentManager());

        final int repeatMode = MusicUtils.getRepeatMode();
        final int queueSize = MusicUtils.getQueueSize();
        final int targetSize;
        final int targetIndex;

        if (repeatMode == MusicPlaybackService.REPEAT_CURRENT) {
            targetSize = 1;
            targetIndex = 0;
        } else if (MusicUtils.getShuffleMode() == MusicPlaybackService.SHUFFLE_NONE) {
            // if we aren't shuffling, use the queue to determine where we are
            targetSize = queueSize;
            targetIndex = MusicUtils.getQueuePosition();
        } else {
            // otherwise, set it to the max history size
            targetSize = MusicPlaybackService.MAX_HISTORY_SIZE;
            targetIndex = MusicUtils.getQueueHistorySize();
        }

        albumArtPagerAdapter.setPlaylistLength(targetSize);
        mAlbumArtViewPager.setAdapter(albumArtPagerAdapter);
        mAlbumArtViewPager.setCurrentItem(targetIndex);

        if(queueSize == 0) {
            mAlbumArtViewPager.setVisibility(View.GONE);
            mQueueEmpty.showNoResults();
        } else {
            mAlbumArtViewPager.setVisibility(View.VISIBLE);
            mQueueEmpty.hideAll();
        }
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);
        mTimeHandler.removeMessages(REFRESH_TIME);
        mTimeHandler.sendMessageDelayed(message, delay);
    }

    /* Used to update the current time string */
    private long refreshCurrentTime() {
        if (!MusicUtils.isPlaybackServiceConnected()) {
            return MusicUtils.UPDATE_FREQUENCY_MS;
        }

        final long position = MusicUtils.position();
        final long duration = MusicUtils.duration();
        final boolean isPlaying = MusicUtils.isPlaying();
        mMainPlaybackControls.refreshCurrentTime(position, duration, isPlaying);

        if (position >= 0 && duration > 0 && isPlaying) {
            // calculate the number of milliseconds until the next full second,
            // so the counter can be updated at just the right time
            return Math.max(20, 1000 - position % 1000);
        }
        return MusicUtils.UPDATE_FREQUENCY_MS;
    }

    public void onLyrics(String lyrics) {
        if (TextUtils.isEmpty(lyrics)
                || !PreferenceUtils.getInstance(getActivity()).getShowLyrics()) {
            mLyricsText.animate().alpha(0).setDuration(200);
        } else {
            lyrics = lyrics.replace("\n", "<br/>");
            Spanned span = Html.fromHtml(lyrics);
            mLyricsText.setText(span);

            mLyricsText.animate().alpha(1).setDuration(200);
        }
    }

    public void setVisualizerVisible(boolean visible) {
        if (visible && PreferenceUtils.getInstance(getActivity()).getShowVisualizer()) {
            if (PreferenceUtils.canRecordAudio(getActivity())) {
                mVisualizerView.setVisible(true);
            } else {
                PreferenceUtils.requestRecordAudio(getActivity());
            }
        } else {
            mVisualizerView.setVisible(false);
        }
    }

    public void updateVisualizerPowerSaveMode() {
        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mVisualizerView.setPowerSaveMode(pm.isPowerSaveMode());
    }

    public void setVisualizerColor(int color) {
        mVisualizerView.setColor(color);
    }

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {

        private final WeakReference<AudioPlayerFragment> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        TimeHandler(final AudioPlayerFragment player) {
            mAudioPlayer = new WeakReference<>(player);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REFRESH_TIME:
                    final long next = mAudioPlayer.get().refreshCurrentTime();
                    mAudioPlayer.get().queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Used to monitor the state of playback
     */
    private static final class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<AudioPlayerFragment> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final AudioPlayerFragment fragment) {
            mReference = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null || action.isEmpty()) {
                return;
            }

            final AudioPlayerFragment audioPlayerFragment = mReference.get();
            switch (action) {
                case MusicPlaybackService.META_CHANGED:
                    // if we are repeating current and the track has changed, re-create the adapter
                    if (MusicUtils.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                        mReference.get().createAndSetAdapter();
                    }

                    // Current info
                    audioPlayerFragment.updateNowPlayingInfo();
                    break;
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    audioPlayerFragment.mMainPlaybackControls.updatePlayPauseState();
                    audioPlayerFragment.mVisualizerView.setPlaying(MusicUtils.isPlaying());
                    break;
                case MusicPlaybackService.REPEATMODE_CHANGED:
                case MusicPlaybackService.SHUFFLEMODE_CHANGED:
                    // Set the repeat image
                    audioPlayerFragment.mMainPlaybackControls.updateRepeatState();
                    // Set the shuffle image
                    audioPlayerFragment.mMainPlaybackControls.updateShuffleState();

                    // Update the queue
                    audioPlayerFragment.createAndSetAdapter();
                    break;
                case MusicPlaybackService.QUEUE_CHANGED:
                    audioPlayerFragment.createAndSetAdapter();
                    break;
                case MusicPlaybackService.NEW_LYRICS:
                    audioPlayerFragment.onLyrics(intent.getStringExtra("lyrics"));
                    break;
                case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED:
                    audioPlayerFragment.updateVisualizerPowerSaveMode();
                    break;
            }
        }
    }
}
