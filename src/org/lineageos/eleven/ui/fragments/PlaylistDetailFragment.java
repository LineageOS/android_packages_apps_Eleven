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
import android.provider.MediaStore;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.adapters.ProfileSongAdapter;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.dragdrop.DragSortListView;
import org.lineageos.eleven.dragdrop.DragSortListView.DragScrollProfile;
import org.lineageos.eleven.dragdrop.DragSortListView.DropListener;
import org.lineageos.eleven.dragdrop.DragSortListView.RemoveListener;
import org.lineageos.eleven.loaders.PlaylistSongLoader;
import org.lineageos.eleven.menu.FragmentMenuItems;
import org.lineageos.eleven.model.Playlist;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.recycler.RecycleHolder;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PlaylistPopupMenuHelper;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.utils.PopupMenuHelper.PopupMenuType;
import org.lineageos.eleven.utils.SongPopupMenuHelper;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;
import org.lineageos.eleven.widgets.NoResultsContainer;

import java.util.List;
import java.util.TreeSet;

public class PlaylistDetailFragment extends FadingBarFragment implements
        LoaderManager.LoaderCallbacks<List<Song>>, OnItemClickListener, DropListener,
        RemoveListener, DragScrollProfile, IChildFragment {

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LoaderManager lm = LoaderManager.getInstance(this);
        lm.initLoader(0, getArguments(), this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPopupMenuHelper = new SongPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Song getSong(int position) {
                if (position == 0) {
                    return null;
                }

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
                mAdapter.remove(mSong);
                mAdapter.buildCache();
                mAdapter.notifyDataSetChanged();
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
        final DragSortListView listView = mRootView.findViewById(R.id.list_base);
        listView.setOnScrollListener(PlaylistDetailFragment.this);

        mAdapter = new ProfileSongAdapter(
                mPlaylistId,
                getActivity(),
                R.layout.edit_track_list_item,
                R.layout.faux_playlist_header
        );
        mAdapter.setPopupMenuClickedListener((v, position) ->
                mPopupMenuHelper.showPopupMenu(v, position));
        listView.setAdapter(mAdapter);
        // Release any references to the recycled Views
        listView.setRecyclerListener(new RecycleHolder());
        // Play the selected song
        listView.setOnItemClickListener(this);
        // Set the drop listener
        listView.setDropListener(this);
        // Set the swipe to remove listener
        listView.setRemoveListener(this);
        // Quick scroll while dragging
        listView.setDragScrollProfile(this);

        // Adjust the progress bar padding to account for the header
        int padTop = getResources().getDimensionPixelSize(R.dimen.playlist_detail_header_height);
        mRootView.findViewById(R.id.progressbar).setPadding(0, padTop, 0, 0);

        // set the loading and empty view container
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        setupNoResultsContainer(mLoadingEmptyContainer.getNoResultsContainer());
        listView.setEmptyView(mLoadingEmptyContainer);
    }

    private void setupNoResultsContainer(final NoResultsContainer container) {
        container.setMainText(R.string.empty_playlist_main);
        container.setSecondaryText(R.string.empty_playlist_secondary);
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
        if (which == 0) {
            return;
        }

        Song song = mAdapter.getItem(which);
        mAdapter.remove(song);
        mAdapter.buildCache();
        mAdapter.notifyDataSetChanged();
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistId);
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getContentResolver().delete(uri,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + song.mSongId,
                    null);
        }

        MusicUtils.refresh();
    }

    @Override
    public void drop(int from, int to) {
        from = Math.max(ProfileSongAdapter.NUM_HEADERS, from);
        to = Math.max(ProfileSongAdapter.NUM_HEADERS, to);

        Song song = mAdapter.getItem(from);
        mAdapter.remove(song);
        mAdapter.insert(song, to);
        mAdapter.buildCache();
        mAdapter.notifyDataSetChanged();

        final int realFrom = from - ProfileSongAdapter.NUM_HEADERS;
        final int realTo = to - ProfileSongAdapter.NUM_HEADERS;
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            MediaStore.Audio.Playlists.Members.moveItem(activity.getContentResolver(),
                    mPlaylistId, realFrom, realTo);
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        if (position == 0) {
            return;
        }
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        Cursor cursor = PlaylistSongLoader.makePlaylistSongCursor(activity,
                mPlaylistId);
        final long[] list = MusicUtils.getSongListForCursor(cursor);
        MusicUtils.playAll(activity, list, position - ProfileSongAdapter.NUM_HEADERS,
                mPlaylistId, Config.IdType.Playlist, false);
        cursor.close();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        super.onScrollStateChanged(view, scrollState);

        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            mAdapter.setPauseDiskCache(true);
        } else {
            mAdapter.setPauseDiskCache(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    protected int getHeaderHeight() {
        return mHeaderContainer.getHeight();
    }

    protected void setHeaderPosition(float y) {
        // Offset the header height to account for the faux header
        y = y - getResources().getDimension(R.dimen.header_bar_height);
        mHeaderContainer.setY(y);
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int i, Bundle bundle) {
        mLoadingEmptyContainer.showLoading();

        return new PlaylistSongLoader(getActivity(), mPlaylistId);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<List<Song>> loader, final List<Song> data) {
        if (data.isEmpty()) {
            mLoadingEmptyContainer.showNoResults();

            // hide the header container
            mHeaderContainer.setVisibility(View.INVISIBLE);

            // Start fresh
            mAdapter.unload();
        } else {
            // show the header container
            mHeaderContainer.setVisibility(View.VISIBLE);

            // pause notifying the adapter and make changes before re-enabling it so that the list
            // view doesn't reset to the top of the list
            mAdapter.setNotifyOnChange(false);
            // Start fresh
            mAdapter.unload();
            // Return the correct count
            mAdapter.addAll(data);
            // build the cache
            mAdapter.buildCache();
            // re-enable the notify by calling notify dataset changes
            mAdapter.notifyDataSetChanged();
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
        mAdapter.unload();
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
}
