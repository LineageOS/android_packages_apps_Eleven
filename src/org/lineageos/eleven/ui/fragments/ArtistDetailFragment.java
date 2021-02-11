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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.ArtistDetailAlbumAdapter;
import org.lineageos.eleven.adapters.ArtistDetailSongAdapter;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.cache.ImageFetcher;
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

import java.util.TreeSet;

public class ArtistDetailFragment extends FadingBarFragment implements IChildFragment {
    private final int ALBUM_LOADER_ID = 0;
    private final int SONG_LOADER_ID = 1;

    private long mArtistId;
    private String mArtistName;

    private ImageView mHero;
    private View mHeader;

    private ArtistDetailSongAdapter mSongAdapter;

    private ArtistDetailAlbumAdapter mAlbumAdapter;

    private PopupMenuHelper mSongPopupMenuHelper;
    private PopupMenuHelper mAlbumPopupMenuHelper;

    private LoadingEmptyContainer mLoadingEmptyContainer;

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
        lm.initLoader(ALBUM_LOADER_ID, args, mAlbumAdapter);
        lm.initLoader(SONG_LOADER_ID, args, mSongAdapter);
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
        mHero = (ImageView) mHeader.findViewById(R.id.hero);
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
        RecyclerView albumsList = (RecyclerView) mHeader.findViewById(R.id.albums);
        albumsList.setHasFixedSize(true);
        albumsList.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        mAlbumAdapter = new ArtistDetailAlbumAdapter(getActivity());
        mAlbumAdapter.setPopupMenuClickedListener((v, position) ->
                mAlbumPopupMenuHelper.showPopupMenu(v, position));
        albumsList.setAdapter(mAlbumAdapter);
    }

    private void setupSongList() {
        ListView songsList = (ListView) mRootView.findViewById(R.id.songs);
        mHeader = LayoutInflater.from(getActivity()).
                inflate(R.layout.artist_detail_header, songsList, false);
        songsList.addHeaderView(mHeader);
        songsList.setOnScrollListener(this);
        mSongAdapter = new ArtistDetailSongAdapter(getActivity()) {
            @Override
            protected void onLoading() {
                mLoadingEmptyContainer.showLoading();
            }

            @Override
            protected void onNoResults() {
                // no results - because the user deleted the last item - pop our fragment
                // from the stack
                getContainingActivity().postRemoveFragment(ArtistDetailFragment.this);
            }
        };
        mSongAdapter.setPopupMenuClickedListener((v, position) ->
                mSongPopupMenuHelper.showPopupMenu(v, position));
        songsList.setAdapter(mSongAdapter);
        songsList.setOnItemClickListener(mSongAdapter);
        mLoadingEmptyContainer =
                (LoadingEmptyContainer) mRootView.findViewById(R.id.loading_empty_container);
        songsList.setEmptyView(mLoadingEmptyContainer);
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

    // TODO: change this class to use the same header strategy as PlaylistDetail
    protected int getHeaderHeight() {
        return mHero.getHeight();
    }

    protected void setHeaderPosition(float y) {
    }

    @Override
    public void restartLoader() {
        Bundle arguments = getArguments();
        LoaderManager lm = LoaderManager.getInstance(this);
        lm.restartLoader(ALBUM_LOADER_ID, arguments, mAlbumAdapter);
        lm.restartLoader(SONG_LOADER_ID, arguments, mSongAdapter);

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
