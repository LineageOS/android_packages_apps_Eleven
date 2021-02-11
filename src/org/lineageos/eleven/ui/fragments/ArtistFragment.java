/*
 * Copyright (C) 2012 Andrew Neal
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
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.lineageos.eleven.MusicStateListener;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.ArtistAdapter;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.loaders.ArtistLoader;
import org.lineageos.eleven.model.Artist;
import org.lineageos.eleven.recycler.RecycleHolder;
import org.lineageos.eleven.sectionadapter.SectionAdapter;
import org.lineageos.eleven.sectionadapter.SectionCreator;
import org.lineageos.eleven.sectionadapter.SectionListContainer;
import org.lineageos.eleven.ui.activities.BaseActivity;
import org.lineageos.eleven.ui.fragments.phone.MusicBrowserFragment;
import org.lineageos.eleven.utils.ArtistPopupMenuHelper;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.utils.SectionCreatorUtils;
import org.lineageos.eleven.utils.SectionCreatorUtils.IItemCompare;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;

/**
 * This class is used to display all of the artists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistFragment extends MusicBrowserFragment implements
        LoaderManager.LoaderCallbacks<SectionListContainer<Artist>>,
        OnScrollListener, OnItemClickListener, MusicStateListener {

    /**
     * Fragment UI
     */
    private ViewGroup mRootView;

    /**
     * The adapter for the grid
     */
    private SectionAdapter<Artist, ArtistAdapter> mAdapter;

    /**
     * Pop up menu helper
     */
    private PopupMenuHelper mPopupMenuHelper;

    /**
     * Loading container and no results container
     */
    private LoadingEmptyContainer mLoadingEmptyContainer;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public ArtistFragment() {
    }

    @Override
    public int getLoaderId() {
        return PagerAdapter.MusicFragments.ARTIST.ordinal();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPopupMenuHelper = new ArtistPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Artist getArtist(int position) {
                return mAdapter.getTItem(position);
            }
        };

        // Create the adapter
        final int layout = R.layout.list_item_normal;
        ArtistAdapter adapter = new ArtistAdapter(getActivity(), layout);
        mAdapter = new SectionAdapter<>(getActivity(), adapter);
        mAdapter.setPopupMenuClickedListener((v, position) ->
                mPopupMenuHelper.showPopupMenu(v, position));
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup) inflater.inflate(R.layout.list_base, null);
        initListView();

        // Register the music status listener
        final Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setMusicStateListenerListener(this);
        }

        return mRootView;
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
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        initLoader(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.flush();
    }

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            mAdapter.getUnderlyingAdapter().setPauseDiskCache(true);
        } else {
            mAdapter.getUnderlyingAdapter().setPauseDiskCache(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        Artist artist = mAdapter.getTItem(position);
        NavUtils.openArtistProfile(getActivity(), artist.mArtistName);
    }

    @NonNull
    @Override
    public Loader<SectionListContainer<Artist>> onCreateLoader(final int id, final Bundle args) {
        mLoadingEmptyContainer.showLoading();
        final Context context = getActivity();
        IItemCompare<Artist> comparator = SectionCreatorUtils.createArtistComparison(context);
        return new SectionCreator<>(getActivity(), new ArtistLoader(context), comparator);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<SectionListContainer<Artist>> loader,
                               final SectionListContainer<Artist> data) {
        if (data.mListResults.isEmpty()) {
            mAdapter.unload();
            mLoadingEmptyContainer.showNoResults();
            return;
        }

        mAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<SectionListContainer<Artist>> loader) {
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
    public void onScroll(final AbsListView view, final int firstVisibleItem,
                         final int visibleItemCount, final int totalItemCount) {
        // Nothing to do
    }

    @Override
    public void restartLoader() {
        // Update the list when the user deletes any items
        restartLoader(null, this);
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
     * Sets up various helpers for both the list and grid
     *
     * @param list The list or grid
     */
    private void initAbsListView(final AbsListView list) {
        // Release any references to the recycled Views
        list.setRecyclerListener(new RecycleHolder());
        // Show the albums and songs from the selected artist
        list.setOnItemClickListener(this);
        // To help make scrolling smooth
        list.setOnScrollListener(this);
    }

    /**
     * Sets up the list view
     */
    private void initListView() {
        // Initialize the grid
        final ListView listView = mRootView.findViewById(R.id.list_base);
        // Set the data behind the list
        listView.setAdapter(mAdapter);
        // set the loading and empty view container
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        listView.setEmptyView(mLoadingEmptyContainer);
        // Set up the helpers
        initAbsListView(listView);
    }
}
