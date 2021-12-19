/*
 * Copyright (C) 2012 Andrew Neal
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.MusicStateListener;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.AlbumAdapter;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.loaders.AlbumLoader;
import org.lineageos.eleven.model.Album;
import org.lineageos.eleven.sectionadapter.SectionCreator;
import org.lineageos.eleven.sectionadapter.SectionListContainer;
import org.lineageos.eleven.ui.activities.BaseActivity;
import org.lineageos.eleven.ui.fragments.phone.MusicBrowserFragment;
import org.lineageos.eleven.utils.AlbumPopupMenuHelper;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;

/**
 * This class is used to display all of the albums on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumFragment extends MusicBrowserFragment implements
        LoaderManager.LoaderCallbacks<SectionListContainer<Album>>, MusicStateListener {

    /**
     * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
     */
    private static final int TWO = 2, FOUR = 4;

    /**
     * Fragment UI
     */
    private ViewGroup mRootView;

    /**
     * The adapter for the grid
     */
    private AlbumAdapter mAdapter;

    /**
     * Pop up menu helper
     */
    private PopupMenuHelper mPopupMenuHelper;

    /**
     * This holds the loading progress bar as well as the no results message
     */
    private LoadingEmptyContainer mLoadingEmptyContainer;

    @Override
    public int getLoaderId() {
        return PagerAdapter.MusicFragments.ALBUM.ordinal();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPopupMenuHelper = new AlbumPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            public Album getAlbum(int position) {
                return mAdapter.getItem(position);
            }
        };

        int layout = R.layout.grid_items_normal;

        mAdapter = new AlbumAdapter(requireActivity(), layout, this::onItemClick);
        mAdapter.setPopupMenuClickedListener((v, position) ->
                mPopupMenuHelper.showPopupMenu(v, position));
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.grid_base, container, false);
        initGridView();

        // Register the music status listener
        final Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setMusicStateListenerListener(this);
        }

        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        initLoader(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        final Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).removeMusicStateListenerListener(this);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mAdapter.flush();
    }

    @Override
    @NonNull
    public Loader<SectionListContainer<Album>> onCreateLoader(final int id, final Bundle args) {
        mLoadingEmptyContainer.showLoading();
        // if we ever decide to add section headers for grid items, we can pass a comparator
        // instead of null
        return new SectionCreator<>(getActivity(), new AlbumLoader(getActivity()), null);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<SectionListContainer<Album>> loader,
                               final SectionListContainer<Album> data) {
        Handler handler = new Handler(requireActivity().getMainLooper());

        if (data.mListResults.isEmpty()) {
            // Do on UI thread: https://issuetracker.google.com/issues/37030377
            handler.post(() -> mAdapter.unload());
            mLoadingEmptyContainer.showNoResults();
            return;
        }

        mLoadingEmptyContainer.setVisibility(View.GONE);
        // Do on UI thread: https://issuetracker.google.com/issues/37030377
        handler.post(() -> mAdapter.setData(data.mListResults));
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<SectionListContainer<Album>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    /**
     * Restarts the loader.
     */
    public void refresh() {
        // Wait a moment for the preference to change.
        SystemClock.sleep(10);
        restartLoader();
    }

    @Override
    public void restartLoader() {
        // Update the list when the user deletes any items
        restartLoader(this);
    }

    @Override
    public void onMetaChanged() {
        // Nothing to do
    }

    @Override
    public void onPlaylistChanged() {
        // Nothing to do
    }

    /**
     * Sets up the grid view
     */
    private void initGridView() {
        final Activity activity = getActivity();
        int columns = (activity != null && ElevenUtils.isLandscape(activity)) ? FOUR : TWO;
        final GridLayoutManager layoutManager = new GridLayoutManager(activity, columns);
        // Initialize the grid
        RecyclerView gridView = mRootView.findViewById(R.id.grid_base);
        // Set up the helpers
        gridView.setLayoutManager(layoutManager);
        // Set up the animator
        gridView.setItemAnimator(new DefaultItemAnimator());
        // Set the data behind the grid
        gridView.setAdapter(mAdapter);

        // Show progress bar
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        mLoadingEmptyContainer.setVisibility(View.VISIBLE);
    }

    private void onItemClick(Album album) {
        NavUtils.openAlbumProfile(getActivity(), album.mAlbumName, album.mArtistName,
                album.mAlbumId);
    }
}
