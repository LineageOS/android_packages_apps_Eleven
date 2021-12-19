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
package org.lineageos.eleven.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.MusicPlaybackService;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.QueueSongAdapter;
import org.lineageos.eleven.loaders.NowPlayingCursor;
import org.lineageos.eleven.loaders.QueueLoader;
import org.lineageos.eleven.menu.DeleteDialog;
import org.lineageos.eleven.menu.FragmentMenuItems;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.ui.activities.SlidingPanelActivity;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.widgets.DragSortItemTouchHelperCallback;
import org.lineageos.eleven.widgets.DragSortListener;
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
public class QueueFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Song>>,
        ServiceConnection, DragSortListener {

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
    private QueueSongAdapter mAdapter;

    /**
     * Drag sort item helper.
     */
    private ItemTouchHelper mDragSortHelper;

    /**
     * Pop up menu helper
     */
    private PopupMenuHelper mPopupMenuHelper;

    /**
     * This holds the loading progress bar as well as the no results message
     */
    private LoadingEmptyContainer mLoadingEmptyContainer;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public QueueFragment() {
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPopupMenuHelper = new PopupMenuHelper(getActivity(), getChildFragmentManager()) {
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
                return new long[]{mSong.mSongId};
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
            protected void onDeleteClicked() {
                DeleteDialog.newInstance(mSong.mSongName, new long[]{getId()}, null)
                        .show(getChildFragmentManager(), "DeleteDialog");
            }

            @Override
            protected void playNext() {
                NowPlayingCursor queue = (NowPlayingCursor) QueueLoader
                        .makeQueueCursor(getActivity());
                queue.removeItem(mSelectedPosition);
                queue.close();
                MusicUtils.playNext(getIdList(), getSourceId(), getSourceType());
                refreshQueue();
            }

            @Override
            protected void removeFromQueue() {
                MusicUtils.removeTrackAtPosition(getId(), mSelectedPosition);
                remove(mSelectedPosition);
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
        mAdapter = new QueueSongAdapter(requireActivity(), R.layout.edit_queue_list_item,
                -1, Config.IdType.NA, this::onItemClick);
        mAdapter.setPopupMenuClickedListener((v, position) ->
                mPopupMenuHelper.showPopupMenu(v, position));
        mDragSortHelper = new ItemTouchHelper(new DragSortItemTouchHelperCallback(this));

        // Initialize the broadcast receiver
        mQueueUpdateListener = new QueueUpdateListener(this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_list,
                container, false);
        // Initialize the list
        RecyclerView listView = rootView.findViewById(R.id.list_base);
        listView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        // Set the data behind the list
        listView.setAdapter(mAdapter);
        mDragSortHelper.attachToRecyclerView(listView);
        // Setup the loading and empty state
        mLoadingEmptyContainer = rootView.findViewById(R.id.loading_empty_container);
        // Setup the container strings
        setupNoResultsContainer(mLoadingEmptyContainer.getNoResultsContainer());
        mLoadingEmptyContainer.setVisibility(View.VISIBLE);
        return rootView;
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        refreshQueue();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onStart() {
        super.onStart();

        // Bind Eleven's service
        mToken = MusicUtils.bindToService(getActivity(), this);

        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Queue changes
        filter.addAction(MusicPlaybackService.QUEUE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.registerReceiver(mQueueUpdateListener, filter);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            final FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.unregisterReceiver(mQueueUpdateListener);
            }
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }

        MusicUtils.unbindFromService(mToken);
        mToken = null;
    }

    public void onItemClick(final int position) {
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
        Handler handler = new Handler(requireActivity().getMainLooper());
        handler.post(() -> mAdapter.unload()); // Start fresh

        if (data.isEmpty()) {
            mLoadingEmptyContainer.showNoResults();
            mAdapter.setCurrentlyPlayingTrack(null);
            final FragmentActivity activity = getActivity();
            if (activity instanceof SlidingPanelActivity) {
                ((SlidingPanelActivity) activity).clearMetaInfo();
            }
        } else {
            mLoadingEmptyContainer.setVisibility(View.GONE);

            // Add the songs found to the adapter
            handler.post(() -> {
                mAdapter.setData(data);

                // Set the currently playing audio
                mAdapter.setCurrentlyPlayingTrack(MusicUtils.getCurrentTrack());
            });
        }
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<List<Song>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    @Override
    public void onItemMove(int startPosition, int endPosition) {
        Handler handler = new Handler(requireActivity().getMainLooper());
        handler.post(() -> mAdapter.move(startPosition, endPosition));
        MusicUtils.moveQueueItem(startPosition, endPosition);
    }

    public void remove(final int which) {
        Song song = mAdapter.getItem(which);
        Handler handler = new Handler(requireActivity().getMainLooper());
        handler.post(() -> mAdapter.remove(which));
        MusicUtils.removeTrackAtPosition(song.mSongId, which);
    }

    /**
     * Called to restart the loader callbacks
     */
    public void refreshQueue() {
        if (isAdded()) {
            LoaderManager.getInstance(this)
                    .restartLoader(LOADER, null, this);
        }
    }

    private void setupNoResultsContainer(NoResultsContainer empty) {
        final Context context = getContext();
        if (context != null) {
            int color = ContextCompat.getColor(context, R.color.no_results_light);
            empty.setTextColor(color);
        }
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
            final String action = intent.getAction();
            if (MusicPlaybackService.META_CHANGED.equals(action)
                    || MusicPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
                mReference.get().mAdapter.setCurrentlyPlayingTrack(MusicUtils.getCurrentTrack());
            } else if (MusicPlaybackService.QUEUE_CHANGED.equals(action)) {
                mReference.get().refreshQueue();

            }

        }
    }
}
