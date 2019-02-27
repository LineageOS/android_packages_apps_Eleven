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

package org.lineageos.eleven.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.MusicPlaybackService;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.SongAdapter;
import org.lineageos.eleven.dragdrop.DragSortListView;
import org.lineageos.eleven.dragdrop.DragSortListView.DragScrollProfile;
import org.lineageos.eleven.dragdrop.DragSortListView.DropListener;
import org.lineageos.eleven.dragdrop.DragSortListView.RemoveListener;
import org.lineageos.eleven.loaders.NowPlayingCursor;
import org.lineageos.eleven.loaders.QueueLoader;
import org.lineageos.eleven.menu.FragmentMenuItems;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.recycler.RecycleHolder;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.ui.activities.SlidingPanelActivity;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;
import org.lineageos.eleven.widgets.NoResultsContainer;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.TreeSet;

/**
 * This class is used to display all of the songs in the queue.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class QueueFragment extends Fragment implements LoaderCallbacks<List<Song>>,
        OnItemClickListener, DropListener, RemoveListener, DragScrollProfile, ServiceConnection {

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    /**
     * Service token for binding to the music service
     */
    private MusicUtils.ServiceToken mToken;

    /**
     * The listener to the playback service that will trigger updates to the ui
     */
    private QueueUpdateListener mQueueUpdateListener;

    /**
     * The adapter for the list
     */
    private SongAdapter mAdapter;

    /**
     * The list view
     */
    private DragSortListView mListView;

    /**
     * Pop up menu helper
     */
    private PopupMenuHelper mPopupMenuHelper;

    /**
     * Root view
     */
    private ViewGroup mRootView;

    /**
     * This holds the loading progress bar as well as the no results message
     */
    private LoadingEmptyContainer mLoadingEmptyContainer;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public QueueFragment() {
        // empty
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPopupMenuHelper = new PopupMenuHelper(getActivity(), getFragmentManager()) {
            private Song mSong;
            private int mSelectedPosition;
            private MusicPlaybackTrack mSelectedTrack;

            @Override
            public PopupMenuType onPreparePopupMenu(int position) {
                mSelectedPosition = position;
                mSong = mAdapter.getItem(mSelectedPosition);
                mSelectedTrack = MusicUtils.getTrack(mSelectedPosition);

                return PopupMenuType.Queue;
            }

            @Override
            protected long[] getIdList() {
                return new long[] { mSong.mSongId };
            }

            @Override
            protected long getSourceId() {
                if (mSelectedTrack == null) {
                    return -1;
                }

                return mSelectedTrack.mSourceId;
            }

            @Override
            protected Config.IdType getSourceType() {
                if (mSelectedTrack == null) {
                    return Config.IdType.NA;
                }

                return mSelectedTrack.mSourceType;
            }

            @Override
            protected String getArtistName() {
                return mSong.mArtistName;
            }

            @Override
            protected void playNext() {
                NowPlayingCursor queue = (NowPlayingCursor)QueueLoader
                        .makeQueueCursor(getActivity());
                queue.removeItem(mSelectedPosition);
                queue.close();
                queue = null;
                MusicUtils.playNext(getIdList(), getSourceId(), getSourceType());
                refreshQueue();
            }

            @Override
            protected void removeFromQueue() {
                MusicUtils.removeTrackAtPosition(getId(), mSelectedPosition);
                refreshQueue();
            }

            @Override
            protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
                super.updateMenuIds(type, set);

                // Don't show more by artist if it is an unknown artist
                if (MediaStore.UNKNOWN_STRING.equals(mSong.mArtistName)) {
                    set.remove(FragmentMenuItems.MORE_BY_ARTIST);
                }
            }
        };

        // Create the adapter
        mAdapter = new SongAdapter(getActivity(), R.layout.edit_queue_list_item,
                -1, Config.IdType.NA);
        mAdapter.setPopupMenuClickedListener((v, position) -> mPopupMenuHelper.showPopupMenu(v, position));
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup) inflater.inflate(R.layout.list_base, container, false);
        // Initialize the list
        mListView = mRootView.findViewById(R.id.list_base);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        // Release any references to the recycled Views
        mListView.setRecyclerListener(new RecycleHolder());
        // Play the selected song
        mListView.setOnItemClickListener(this);
        // Set the drop listener
        mListView.setDropListener(this);
        // Set the swipe to remove listener
        mListView.setRemoveListener(this);
        // Quick scroll while dragging
        mListView.setDragScrollProfile(this);
        // Enable fast scroll bars
        mListView.setFastScrollEnabled(true);
        // Setup the loading and empty state
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        // Setup the container strings
        setupNoResultsContainer(mLoadingEmptyContainer.getNoResultsContainer());
        mListView.setEmptyView(mLoadingEmptyContainer);
        return mRootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the broadcast receiver
        mQueueUpdateListener = new QueueUpdateListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mToken = MusicUtils.bindToService(getActivity(), this);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        refreshQueue();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        refreshQueue();
    }

    @Override
    public void onStart() {
        super.onStart();

        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Queue changes
        filter.addAction(MusicPlaybackService.QUEUE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);

        getActivity().registerReceiver(mQueueUpdateListener, filter);
    }

    @Override
    public void onDestroy() {
        try {
            getActivity().unregisterReceiver(mQueueUpdateListener);
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }

        super.onDestroy();
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        // When selecting a track from the queue, just jump there instead of
        // reloading the queue. This is both faster, and prevents accidentally
        // dropping out of party shuffle.
        MusicUtils.setQueuePosition(position);
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        mLoadingEmptyContainer.showLoading();
        return new QueueLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<List<Song>> loader, final List<Song> data) {
        // pause notifying the adapter and make changes before re-enabling it so that the list
        // view doesn't reset to the top of the list
        mAdapter.setNotifyOnChange(false);
        mAdapter.unload(); // Start fresh

        if (data.isEmpty()) {
            mLoadingEmptyContainer.showNoResults();
            mAdapter.setCurrentQueuePosition(SongAdapter.NOTHING_PLAYING);
            ((SlidingPanelActivity)getActivity()).clearMetaInfo();
        } else {
            // Add the songs found to the adapter
            for (final Song song : data) { mAdapter.add(song); }
            // Build the cache
            mAdapter.buildCache();
            // Set the currently playing audio
            mAdapter.setCurrentQueuePosition(MusicUtils.getQueuePosition());
        }
        // re-enable the notify by calling notify dataset changes
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(final Loader<List<Song>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    @Override
    public float getSpeed(final float w, final long t) {
        if (w > 0.8f) {
            return mAdapter.getCount() / 0.001f;
        } else {
            return 10.0f * w;
        }
    }

    @Override
    public void remove(final int which) {
        Song song = mAdapter.getItem(which);
        mAdapter.remove(song);
        mAdapter.notifyDataSetChanged();
        MusicUtils.removeTrackAtPosition(song.mSongId, which);
        // Build the cache
        mAdapter.buildCache();
    }

    @Override
    public void drop(final int from, final int to) {
        Song song = mAdapter.getItem(from);
        mAdapter.remove(song);
        mAdapter.insert(song, to);
        mAdapter.notifyDataSetChanged();
        MusicUtils.moveQueueItem(from, to);
        // Build the cache
        mAdapter.buildCache();
    }

    public void refreshQueue() {
        if (isAdded()) {
            getLoaderManager().restartLoader(LOADER, null, this);
        }
    }

    private void setupNoResultsContainer(NoResultsContainer empty) {
        int color = ContextCompat.getColor(getContext(), R.color.no_results_light);
        empty.setTextColor(color);
        empty.setMainText(R.string.empty_queue_main);
        empty.setSecondaryText(R.string.empty_queue_secondary);
    }

    /**
     * Used to monitor the state of playback
     */
    private static final class QueueUpdateListener extends BroadcastReceiver {

        private final WeakReference<QueueFragment> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public QueueUpdateListener(final QueueFragment fragment) {
            mReference = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            // TODO: Invalid options menu if opened?
            final String action = intent.getAction();
            if (action == null || action.isEmpty()) {
                return;
            }

            switch (action) {
                case MusicPlaybackService.META_CHANGED:
                    mReference.get().mAdapter.setCurrentQueuePosition(MusicUtils.getQueuePosition());
                    break;
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    mReference.get().mAdapter.notifyDataSetChanged();
                    break;
                case MusicPlaybackService.QUEUE_CHANGED:
                    mReference.get().refreshQueue();
                    break;
            }
        }
    }
}
