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

import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.ArtistDetailAlbumAdapter;
import org.lineageos.eleven.adapters.ArtistDetailSongAdapter;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.loaders.AlbumLoader;
import org.lineageos.eleven.loaders.SongLoader;
import org.lineageos.eleven.menu.FragmentMenuItems;
import org.lineageos.eleven.model.Album;
import org.lineageos.eleven.model.Artist;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.utils.AlbumPopupMenuHelper;
import org.lineageos.eleven.utils.ArtistPopupMenuHelper;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.utils.SongPopupMenuHelper;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;

import java.util.List;
import java.util.TreeSet;

public class ArtistDetailFragment extends DetailFragment implements IChildFragment {
    private final int ALBUM_LOADER_ID = 0;
    private final int SONG_LOADER_ID = 1;

    private long mArtistId;
    private String mArtistName;

    private ImageView mHero;

    private ArtistDetailSongAdapter mSongAdapter;

    private ArtistDetailAlbumAdapter mAlbumAdapter;

    private PopupMenuHelper mSongPopupMenuHelper;
    private PopupMenuHelper mAlbumPopupMenuHelper;

    private LoadingEmptyContainer mLoadingEmptyContainer;

    private final LoaderManager.LoaderCallbacks<List<Song>> mSongsLoader =
            new LoaderManager.LoaderCallbacks<List<Song>>() {
                @NonNull
                @Override
                public Loader<List<Song>> onCreateLoader(int id, @Nullable Bundle args) {
                    mLoadingEmptyContainer.showLoading();
                    long sourceId = args == null ? -1 : args.getLong(Config.ID);
                    final String selection =
                            MediaStore.Audio.AudioColumns.ARTIST_ID + "=" + sourceId;
                    return new SongLoader(getContext(), selection);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
                    Handler handler = new Handler(requireActivity().getMainLooper());

                    if (data.isEmpty()) {
                        // no results - because the user deleted the last item - pop our fragment
                        // from the stack
                        getContainingActivity().postRemoveFragment(ArtistDetailFragment.this);
                        return;
                    }

                    mLoadingEmptyContainer.setVisibility(View.GONE);
                    // Do on UI thread: https://issuetracker.google.com/issues/37030377
                    handler.post(() -> mSongAdapter.setData(data));
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
                    mSongAdapter.unload();
                }
            };

    private final LoaderManager.LoaderCallbacks<List<Album>> mAlbumLoader =
            new LoaderManager.LoaderCallbacks<List<Album>>() {
                @NonNull
                @Override
                public Loader<List<Album>> onCreateLoader(int id, @Nullable Bundle args) {
                    return args == null
                            ? new Loader<>(requireContext())
                            : new AlbumLoader(requireContext(), args.getLong(Config.ID));
                }

                @Override
                public void onLoadFinished(@NonNull Loader<List<Album>> loader, List<Album> data) {
                    Handler handler = new Handler(requireActivity().getMainLooper());
                    if (data.isEmpty()) {
                        return;
                    }

                    // Do on UI thread: https://issuetracker.google.com/issues/37030377
                    handler.post(() -> mAlbumAdapter.setData(data));
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<Album>> loader) {
                    mAlbumAdapter.unload();
                }
            };

    @Override
    protected int getLayoutToInflate() {
        return R.layout.activity_artist_detail;
    }

    @Override
    protected String getTitle() {
        final Bundle args = getArguments();
        return args == null ? "" : args.getString(Config.ARTIST_NAME);
    }

    protected long getArtistId() {
        final Bundle args = getArguments();
        return args == null ? -1 : getArguments().getLong(Config.ID);
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();

        getContainingActivity().setFragmentPadding(false);

        Bundle args = getArguments();
        mArtistName = args == null ? "" : args.getString(Config.ARTIST_NAME);
        mArtistId = args == null ? -1 : args.getLong(Config.ID);

        setupPopupMenuHelpers();
        setupSongList();
        setupAlbumList();
        setupHero(mArtistName);

        LoaderManager lm = LoaderManager.getInstance(this);
        lm.initLoader(ALBUM_LOADER_ID, args, mAlbumLoader);
        lm.initLoader(SONG_LOADER_ID, args, mSongsLoader);
    }

    @Override
    protected PopupMenuHelper createActionMenuHelper() {
        return new ArtistPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            public Artist getArtist(int position) {
                return new Artist(mArtistId, mArtistName, 0, 0);
            }
        };
    }

    @Override
    protected int getShuffleTitleId() {
        return R.string.menu_shuffle_artist;
    }

    @Override
    protected void playShuffled() {
        MusicUtils.playArtist(getActivity(), mArtistId, -1, true);
    }

    private void setupHero(String artistName) {
        mHero = mRootView.findViewById(R.id.hero);
        mHero.setContentDescription(artistName);
        // initiate loading the artist image
        // since the artist image needs to be scaled to the image view bounds,
        // we need to wait till the first layout traversal to be able to get the image view
        // dimensions in the helper method that scales the image
        mHero.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mHero.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        ImageFetcher.getInstance(getActivity())
                                .loadArtistImage(mArtistName, mHero, true);
                    }
                });
    }

    private void setupAlbumList() {
        RecyclerView albumsList = mRootView.findViewById(R.id.albums);
        mAlbumAdapter = new ArtistDetailAlbumAdapter(getActivity());
        mAlbumAdapter.setPopupMenuClickedListener((v, position) ->
                mAlbumPopupMenuHelper.showPopupMenu(v, position));
        albumsList.setLayoutManager(new LinearLayoutManager(getActivity(),
                LinearLayoutManager.HORIZONTAL, false));
        albumsList.setItemAnimator(new DefaultItemAnimator());
        albumsList.setAdapter(mAlbumAdapter);
    }

    private void setupSongList() {
        RecyclerView songsList = mRootView.findViewById(R.id.songs);
        mSongAdapter = new ArtistDetailSongAdapter(getActivity());
        mSongAdapter.setPopupMenuClickedListener((v, position) ->
                mSongPopupMenuHelper.showPopupMenu(v, position));
        songsList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        songsList.setItemAnimator(new DefaultItemAnimator());
        songsList.setAdapter(mSongAdapter);
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        mLoadingEmptyContainer.showLoading();
    }

    private void setupPopupMenuHelpers() {
        mSongPopupMenuHelper = new SongPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Song getSong(int position) {
                return mSongAdapter.getItem(position);
            }

            @Override
            protected long getSourceId() {
                return getArtistId();
            }

            @Override
            protected Config.IdType getSourceType() {
                return Config.IdType.Artist;
            }

            @Override
            protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
                super.updateMenuIds(type, set);

                // since we are already on the artist page, this item doesn't make sense
                set.remove(FragmentMenuItems.MORE_BY_ARTIST);
            }
        };

        mAlbumPopupMenuHelper = new AlbumPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Album getAlbum(int position) {
                return mAlbumAdapter.getItem(position);
            }

            @Override
            protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
                super.updateMenuIds(type, set);

                // since we are already on the artist page, this item doesn't make sense
                set.remove(FragmentMenuItems.MORE_BY_ARTIST);
            }
        };
    }

    @Override
    public void restartLoader() {
        Bundle arguments = getArguments();
        LoaderManager lm = LoaderManager.getInstance(this);
        lm.restartLoader(ALBUM_LOADER_ID, arguments, mAlbumLoader);
        lm.restartLoader(SONG_LOADER_ID, arguments, mSongsLoader);

        ImageFetcher.getInstance(getActivity()).loadArtistImage(mArtistName, mHero, true);
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();

        mSongAdapter.setCurrentlyPlayingTrack(MusicUtils.getCurrentTrack());
    }

    @Override
    public PagerAdapter.MusicFragments getMusicFragmentParent() {
        return PagerAdapter.MusicFragments.ARTIST;
    }
}
