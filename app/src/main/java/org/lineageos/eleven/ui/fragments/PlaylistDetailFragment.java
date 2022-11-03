/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2021 The LineageOS Project
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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.adapters.ProfileSongAdapter;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.loaders.PlaylistSongLoader;
import org.lineageos.eleven.menu.FragmentMenuItems;
import org.lineageos.eleven.model.Playlist;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PlaylistPopupMenuHelper;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.utils.PopupMenuHelper.PopupMenuType;
import org.lineageos.eleven.utils.SongPopupMenuHelper;
import org.lineageos.eleven.widgets.DragSortItemTouchHelperCallback;
import org.lineageos.eleven.widgets.DragSortListener;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;
import org.lineageos.eleven.widgets.NoResultsContainer;

import java.util.List;
import java.util.TreeSet;

public class PlaylistDetailFragment extends DetailFragment implements
        LoaderManager.LoaderCallbacks<List<Song>>,
        IChildFragment, DragSortListener {

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    private ProfileSongAdapter mAdapter;

    private View mHeaderContainer;

    private LoadingEmptyContainer mLoadingEmptyContainer;

    private TextView mNumberOfSongs;
    private TextView mDurationOfPlaylist;

    /**
     * The Id of the playlist the songs belong to
     */
    private long mPlaylistId;
    private String mPlaylistName;

    /**
     * Drag sort item helper.
     */
    private ItemTouchHelper mDragSortHelper;

    /**
     * Pop up menu helper
     */
    private PopupMenuHelper mPopupMenuHelper;

    @Override
    protected String getTitle() {
        return mPlaylistName;
    }

    @Override
    protected int getLayoutToInflate() {
        return R.layout.playlist_detail;
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();
        setupHero();
        setupSongList();
        LoaderManager.getInstance(this).initLoader(0, getArguments(), this);
    }

    private void lookupName() {
        mPlaylistName = MusicUtils.getNameForPlaylist(getActivity(), mPlaylistId);
    }

    @Override
    protected PopupMenuHelper createActionMenuHelper() {
        return new PlaylistPopupMenuHelper(
                getActivity(), getChildFragmentManager(), PopupMenuType.Playlist) {
            public Playlist getPlaylist(int position) {
                return new Playlist(mPlaylistId, getTitle(), 0);
            }
        };
    }

    @Override
    protected int getShuffleTitleId() {
        return R.string.menu_shuffle_playlist;
    }

    @Override
    protected void playShuffled() {
        MusicUtils.playPlaylist(getActivity(), mPlaylistId, true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPopupMenuHelper = new SongPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Song getSong(int position) {
                return mAdapter.getItem(position);
            }

            @Override
            protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
                super.updateMenuIds(type, set);

                set.add(FragmentMenuItems.REMOVE_FROM_PLAYLIST);
                set.remove(FragmentMenuItems.DELETE);
            }

            @Override
            protected long getSourceId() {
                return mPlaylistId;
            }

            @Override
            protected Config.IdType getSourceType() {
                return Config.IdType.Playlist;
            }

            @Override
            protected void removeFromPlaylist() {
                remove(mSong);
                final FragmentActivity activity = getActivity();
                if (activity != null) {
                    MusicUtils.removeFromPlaylist(activity, mSong.mSongId, mPlaylistId);
                }
                LoaderManager.getInstance(PlaylistDetailFragment.this)
                        .restartLoader(LOADER, null, PlaylistDetailFragment.this);
            }
        };

        final Bundle args = getArguments();
        mPlaylistId = args == null ? -1 : args.getLong(Config.ID);
        lookupName();

        mAdapter = new ProfileSongAdapter(
                mPlaylistId,
                getActivity(),
                R.layout.edit_track_list_item,
                this::onItemClick
        );
        mAdapter.setPopupMenuClickedListener((v, position) ->
                mPopupMenuHelper.showPopupMenu(v, position));
        mDragSortHelper = new ItemTouchHelper(new DragSortItemTouchHelperCallback(this));
    }

    private void setupHero() {
        final ImageView playlistImageView = mRootView.findViewById(R.id.image);
        mHeaderContainer = mRootView.findViewById(R.id.playlist_header);
        mNumberOfSongs = mRootView.findViewById(R.id.number_of_songs_text);
        mDurationOfPlaylist = mRootView.findViewById(R.id.duration_text);

        final ImageFetcher imageFetcher = ImageFetcher.getInstance(getActivity());
        imageFetcher.loadPlaylistArtistImage(mPlaylistId, playlistImageView);
    }

    private void setupSongList() {
        final RecyclerView listView = mRootView.findViewById(R.id.list_base);

        listView.setAdapter(mAdapter);
        listView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        mDragSortHelper.attachToRecyclerView(listView);

        // Adjust the progress bar padding to account for the header
        int padTop = getResources().getDimensionPixelSize(R.dimen.playlist_detail_header_height);
        mRootView.findViewById(R.id.progressbar).setPadding(0, padTop, 0, 0);

        // set the loading and empty view container
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        setupNoResultsContainer(mLoadingEmptyContainer.getNoResultsContainer());
        mLoadingEmptyContainer.setVisibility(View.VISIBLE);
    }

    private void setupNoResultsContainer(final NoResultsContainer container) {
        container.setMainText(R.string.empty_playlist_main);
        container.setSecondaryText(R.string.empty_playlist_secondary);
    }

    private void remove(Song song) {
        Handler handler = new Handler(requireActivity().getMainLooper());
        handler.post(() -> {
            mAdapter.remove(song);

            final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
                    mPlaylistId);
            final FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.getContentResolver().delete(uri,
                        MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + song.mSongId,
                        null);
            }

            MusicUtils.refresh();
        });
    }

    public void onItemClick(final int position) {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        Cursor cursor = PlaylistSongLoader.makePlaylistSongCursor(activity,
                mPlaylistId);
        final long[] list = MusicUtils.getSongListForCursor(cursor);
        MusicUtils.playAll(activity, list, position, mPlaylistId, Config.IdType.Playlist, false);
        cursor.close();
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int i, Bundle bundle) {
        mLoadingEmptyContainer.showLoading();

        return new PlaylistSongLoader(getActivity(), mPlaylistId);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<List<Song>> loader, final List<Song> data) {
        Handler handler = new Handler(requireActivity().getMainLooper());
        if (data.isEmpty()) {
            mLoadingEmptyContainer.showNoResults();
            // need to call this after showNoResults, otherwise removing any would
            // clear the whole list (not only visibly but it's gone even when re-entering the
            // playlist)
            mLoadingEmptyContainer.setVisibility(View.VISIBLE);

            // hide the header container
            mHeaderContainer.setVisibility(View.INVISIBLE);

            // Start fresh
            handler.post(() -> mAdapter.unload());
        } else {
            mLoadingEmptyContainer.setVisibility(View.GONE);
            // show the header container
            mHeaderContainer.setVisibility(View.VISIBLE);

            // pause notifying the adapter and make changes before re-enabling it so that the list
            // view doesn't reset to the top of the list
            handler.post(() -> {
                // Start fresh
                mAdapter.unload();
                // Return the correct count
                mAdapter.setData(data);
            });

            // set the number of songs
            final FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            String numberOfSongs = MusicUtils.makeLabel(activity, R.plurals.Nsongs,
                    data.size());
            mNumberOfSongs.setText(numberOfSongs);

            long duration = 0;

            // Add the data to the adapter
            for (final Song song : data) {
                duration += song.mDuration;
            }

            // set the duration
            String durationString = MusicUtils.makeLongTimeString(activity, duration);
            mDurationOfPlaylist.setText(durationString);
        }
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<List<Song>> loader) {
        // Clear the data in the adapter
        Handler handler = new Handler(requireActivity().getMainLooper());
        handler.post(() -> mAdapter.unload());
    }

    @Override
    public void restartLoader() {
        lookupName(); // playlist name may have changed
        if (mPlaylistName == null) {
            // if name is null, we've been deleted, so close the this fragment
            getContainingActivity().postRemoveFragment(this);
            return;
        }

        // since onCreateOptionsMenu can be called after onCreate it is possible for
        // mActionMenuHelper to be null.  In this case, don't bother updating the Name since when
        // it does create it, it will use the updated name anyways
        if (mActionMenuHelper != null) {
            // update action bar title and popup menu handler
            ((PlaylistPopupMenuHelper) mActionMenuHelper).updateName(mPlaylistName);
        }

        getContainingActivity().setActionBarTitle(mPlaylistName);
        // and reload the song list
        LoaderManager.getInstance(this)
                .restartLoader(0, getArguments(), this);
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();

        mAdapter.setCurrentlyPlayingTrack(MusicUtils.getCurrentTrack());
    }

    @Override
    public void onPlaylistChanged() {
        super.onPlaylistChanged();

        restartLoader();
    }

    @Override
    public PagerAdapter.MusicFragments getMusicFragmentParent() {
        return PagerAdapter.MusicFragments.PLAYLIST;
    }

    @Override
    public void onItemMove(int startPosition, int endPosition) {
        Handler handler = new Handler(requireActivity().getMainLooper());
        handler.post(() -> mAdapter.move(startPosition, endPosition));

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            MediaStore.Audio.Playlists.Members.moveItem(activity.getContentResolver(),
                    mPlaylistId, startPosition, endPosition);
        }
    }
}
