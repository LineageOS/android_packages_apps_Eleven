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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.AlbumDetailSongAdapter;
import org.lineageos.eleven.adapters.DetailSongAdapter;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.loaders.AlbumSongLoader;
import org.lineageos.eleven.model.Album;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.utils.AlbumPopupMenuHelper;
import org.lineageos.eleven.utils.GenreFetcher;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.utils.SongPopupMenuHelper;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;

import java.util.List;

public class AlbumDetailFragment extends DetailFragment implements IChildFragment,
        LoaderManager.LoaderCallbacks<List<Song>> {
    private static final int LOADER_ID = 1;

    private DetailSongAdapter mSongAdapter;
    private TextView mAlbumDuration;
    private TextView mGenre;
    private ImageView mAlbumArt;
    private PopupMenuHelper mSongMenuHelper;
    private long mAlbumId;
    private String mArtistName;
    private String mAlbumName;
    private LoadingEmptyContainer mLoadingEmptyContainer;

    @Override
    protected int getLayoutToInflate() {
        return R.layout.activity_album_detail;
    }

    @Override
    protected String getTitle() {
        final Bundle args = getArguments();
        return args == null ? "" : args.getString(Config.ARTIST_NAME);
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();

        Bundle args = getArguments();
        String artistName = args == null ? "" : args.getString(Config.ARTIST_NAME);

        setupPopupMenuHelper();
        setupHeader(artistName, args);
        setupSongList();

        LoaderManager.getInstance(this).initLoader(LOADER_ID, args, this);
    }

    @Override // DetailFragment
    protected PopupMenuHelper createActionMenuHelper() {
        return new AlbumPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            public Album getAlbum(int position) {
                return new Album(mAlbumId, mAlbumName, mArtistName, -1, null);
            }
        };
    }

    @Override // DetailFragment
    protected int getShuffleTitleId() {
        return R.string.menu_shuffle_album;
    }

    @Override // DetailFragment
    protected void playShuffled() {
        MusicUtils.playAlbum(getActivity(), mAlbumId, -1, true);
    }

    private void setupHeader(String artist, Bundle arguments) {
        if (arguments == null) {
            return;
        }
        mAlbumId = arguments.getLong(Config.ID);
        mArtistName = artist;
        mAlbumName = arguments.getString(Config.NAME);
        String year = arguments.getString(Config.ALBUM_YEAR);
        int songCount = arguments.getInt(Config.SONG_COUNT);

        mAlbumArt = mRootView.findViewById(R.id.album_art);
        mAlbumArt.setContentDescription(mAlbumName);
        ImageFetcher.getInstance(getActivity()).loadAlbumImage(artist,
                mAlbumName, mAlbumId, mAlbumArt);

        TextView title = mRootView.findViewById(R.id.title);
        title.setText(mAlbumName);

        setupCountAndYear(mRootView, year, songCount);

        // will be updated once we have song data
        mAlbumDuration = mRootView.findViewById(R.id.duration);
        mGenre = mRootView.findViewById(R.id.genre);
    }

    private void setupCountAndYear(View root, String year, int songCount) {
        TextView songCountAndYear = root.findViewById(R.id.song_count_and_year);
        if (songCount > 0) {
            String countText = getResources().
                    getQuantityString(R.plurals.Nsongs, songCount, songCount);
            if (year == null) {
                songCountAndYear.setText(countText);
            } else {
                songCountAndYear.setText(getString(R.string.combine_two_strings, countText, year));
            }
        } else if (year != null) {
            songCountAndYear.setText(year);
        }
    }

    private void setupPopupMenuHelper() {
        mSongMenuHelper = new SongPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Song getSong(int position) {
                return mSongAdapter.getItem(position);
            }

            @Override
            protected long getSourceId() {
                return mAlbumId;
            }

            @Override
            protected Config.IdType getSourceType() {
                return Config.IdType.Album;
            }
        };
    }

    private void setupSongList() {
        RecyclerView songsList = mRootView.findViewById(R.id.songs);
        mSongAdapter = new AlbumDetailSongAdapter(getActivity());
        mSongAdapter.setPopupMenuClickedListener((v, position) ->
                mSongMenuHelper.showPopupMenu(v, position));
        songsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        songsList.setItemAnimator(new DefaultItemAnimator());
        songsList.setAdapter(mSongAdapter);
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        mLoadingEmptyContainer.setVisibility(View.VISIBLE);
    }

    /**
     * called back by song loader
     */
    public void update(List<Song> songs) {
        final FragmentActivity activity = getActivity();
        // compute total run time for album
        int duration = 0;
        for (Song s : songs) {
            duration += s.mDuration;
        }
        mAlbumDuration.setText(MusicUtils.makeLongTimeString(activity, duration));

        // use the first song on the album to get a genre
        if (!songs.isEmpty() && activity != null) {
            GenreFetcher.fetch(activity , (int) songs.get(0).mSongId, mGenre);
        }
    }

    @Override
    public void restartLoader() {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, getArguments(), this);
        ImageFetcher.getInstance(getActivity()).loadAlbumImage(mArtistName, mAlbumName, mAlbumId,
                mAlbumArt);
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();

        mSongAdapter.setCurrentlyPlayingTrack(MusicUtils.getCurrentTrack());
    }

    @Override
    public PagerAdapter.MusicFragments getMusicFragmentParent() {
        return PagerAdapter.MusicFragments.ALBUM;
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, @Nullable Bundle args) {
        mLoadingEmptyContainer.showLoading();
        long sourceId = args == null ? -1 : args.getLong(Config.ID);
        mSongAdapter.setSourceId(sourceId);
        return new AlbumSongLoader(getContext(), sourceId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        Handler handler = new Handler(requireActivity().getMainLooper());

        if (data.isEmpty()) {
            getContainingActivity().postRemoveFragment(AlbumDetailFragment.this);
            return;
        }

        mLoadingEmptyContainer.setVisibility(View.GONE);
        // Do on UI thread: https://issuetracker.google.com/issues/37030377
        handler.post(() -> mSongAdapter.setData(data));
        update(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        // Clear the data in the adapter
        mSongAdapter.unload();
    }
}
