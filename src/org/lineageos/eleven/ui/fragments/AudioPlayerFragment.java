/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package org.lineageos.eleven.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Outline;
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
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.lineageos.eleven.MusicPlaybackService;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.AlbumArtPagerAdapter;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.loaders.NowPlayingCursor;
import org.lineageos.eleven.loaders.QueueLoader;
import org.lineageos.eleven.menu.CreateNewPlaylist;
import org.lineageos.eleven.menu.DeleteDialog;
import org.lineageos.eleven.menu.FragmentMenuItems;
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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

public class AudioPlayerFragment extends Fragment implements ServiceConnection {
    private static final String TAG = AudioPlayerFragment.class.getSimpleName();

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 15;

    // fragment view
    private ViewGroup mRootView;

    // Header views
    private TextView mSongTitle;
    private TextView mArtistName;

    // Playlist Button
    private ImageView mAddToPlaylistButton;

    // Menu Button
    private ImageView mMenuButton;

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

    // popup menu for pressing the menu icon
    private PopupMenu mPopupMenu;

    // Lyrics text view
    private TextView mLyricsText;

    private long mSelectedId = -1;

    private boolean mIsPaused = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Control the media volume
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Bind Eleven's service
        mToken = MusicUtils.bindToService(getActivity(), this);

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
        mRootView = (ViewGroup) inflater.inflate(R.layout.activity_player_fragment, container, false);

        // Header title values
        initHeaderBar();

        initPlaybackControls();

        mVisualizerView = mRootView.findViewById(R.id.visualizerView);
        mVisualizerView.initialize(getActivity());
        updateVisualizerPowerSaveMode();

        mLyricsText = mRootView.findViewById(R.id.audio_player_lyrics);

        return mRootView;
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

        mImageFetcher.flush();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH_TIME);
        // Unbind from the service
        if (MusicUtils.isPlaybackServiceConnected()) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }

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
        final View headerBar = mRootView.findViewById(R.id.audio_player_header);
        final int bottomActionBarHeight =
                getResources().getDimensionPixelSize(R.dimen.bottom_action_bar_height);

        headerBar.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // since we only want the top and bottom shadows, pad the horizontal width
                // to hide the shadows. Can't seem to find a better way to do this
                int padWidth = (int)(0.2f * view.getWidth());
                outline.setRect(-padWidth, -bottomActionBarHeight, view.getWidth() + padWidth,
                        view.getHeight());
            }
        });

        // Title text
        mSongTitle = mRootView.findViewById(R.id.header_bar_song_title);
        mArtistName = mRootView.findViewById(R.id.header_bar_artist_title);

        // Setup the playlist button - add a click listener to show the context
        mAddToPlaylistButton = mRootView.findViewById(R.id.header_bar_add_button);

        // Create the context menu when requested
        mAddToPlaylistButton.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            MusicUtils.makePlaylistMenu(getActivity(), GROUP_ID, menu);
            menu.setHeaderTitle(R.string.add_to_playlist);
        });

        // add a click listener to show the context
        mAddToPlaylistButton.setOnClickListener(v -> {
            // save the current track id
            mSelectedId = MusicUtils.getCurrentAudioId();
            mAddToPlaylistButton.showContextMenu();
        });

        // Add the menu button
        mMenuButton = mRootView.findViewById(R.id.header_bar_menu_button);
        mMenuButton.setOnClickListener(v -> showPopupMenu());
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
            mAddToPlaylistButton.setVisibility(View.GONE);
        } else {
            mAlbumArtViewPager.setVisibility(View.VISIBLE);
            mQueueEmpty.hideAll();
            mAddToPlaylistButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);
            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
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

    public void showPopupMenu() {
        // create the popup menu
        if (mPopupMenu == null) {
            mPopupMenu = new PopupMenu(getActivity(), mMenuButton);
            mPopupMenu.setOnMenuItemClickListener(this::onPopupMenuItemClick);
        }

        final Menu menu = mPopupMenu.getMenu();
        final MenuInflater inflater = mPopupMenu.getMenuInflater();
        menu.clear();

        // Shuffle all
        inflater.inflate(R.menu.shuffle_all, menu);
        if (MusicUtils.getQueueSize() > 0) {
            // ringtone, and equalizer
            inflater.inflate(R.menu.audio_player, menu);

            if (!NavUtils.hasEffectsPanel(getActivity())) {
                menu.removeItem(R.id.menu_audio_player_equalizer);
            }

            // save queue/clear queue
            inflater.inflate(R.menu.queue, menu);
        }
        // Settings
        inflater.inflate(R.menu.activity_base, menu);

        // show the popup
        mPopupMenu.show();
    }

    public boolean onPopupMenuItemClick(final MenuItem item) {
        switch (item.getItemId()) {
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

        return false;
    }

    public void dismissPopupMenu() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            switch (item.getItemId()) {
                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(new long[]{
                            mSelectedId
                    }).show(getFragmentManager(), "CreatePlaylist");
                    return true;
                case FragmentMenuItems.PLAYLIST_SELECTED:
                    final long mPlaylistId = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(getActivity(), new long[]{
                            mSelectedId
                    }, mPlaylistId);
                    return true;
                default:
                    break;
            }
        }

        return super.onContextItemSelected(item);
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
                    audioPlayerFragment.dismissPopupMenu();
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
