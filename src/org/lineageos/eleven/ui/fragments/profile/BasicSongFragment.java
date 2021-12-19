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
package org.lineageos.eleven.ui.fragments.profile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.MusicStateListener;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.SongListAdapter;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.sectionadapter.SectionListContainer;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.ui.activities.BaseActivity;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.utils.SectionCreatorUtils;
import org.lineageos.eleven.utils.SongPopupMenuHelper;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;
import org.lineageos.eleven.widgets.NoResultsContainer;
import org.lineageos.eleven.widgets.SectionSeparatorItemDecoration;

import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class is used to display all of the songs
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class BasicSongFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<SectionListContainer<Song>>, MusicStateListener {

    /**
     * Fragment UI
     */
    protected ViewGroup mRootView;

    /**
     * The adapter for the list
     */
    protected SongListAdapter mAdapter;

    /**
     * The list view
     */
    protected RecyclerView mListView;

    /**
     * Pop up menu helper
     */
    protected PopupMenuHelper mPopupMenuHelper;

    /**
     * This holds the loading progress bar as well as the no results message
     */
    protected LoadingEmptyContainer mLoadingEmptyContainer;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public BasicSongFragment() {
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPopupMenuHelper = new SongPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Song getSong(int position) {
                return mAdapter.getItem(position);
            }

            @Override
            protected long getSourceId() {
                return getFragmentSourceId();
            }

            @Override
            protected Config.IdType getSourceType() {
                return getFragmentSourceType();
            }

            @Override
            protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
                super.updateMenuIds(type, set);
                BasicSongFragment.this.updateMenuIds(set);
            }
        };

        // Create the adapter
        mAdapter = createAdapter();
        mAdapter.setPopupMenuClickedListener((v, position) ->
                mPopupMenuHelper.showPopupMenu(v, position));
    }

    protected long getFragmentSourceId() {
        return -1;
    }

    protected Config.IdType getFragmentSourceType() {
        return Config.IdType.NA;
    }

    protected void updateMenuIds(TreeSet<Integer> set) {
        // do nothing - let subclasses override
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_list, container, false);
        // set the background on the root view
        final Context context = getContext();
        if (context != null) {
            mRootView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_color));
        }
        // Initialize the list
        mListView = mRootView.findViewById(R.id.list_base);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mListView.setItemAnimator(new DefaultItemAnimator());

        // Show progress bar
        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        // Setup the container strings
        setupNoResultsContainer(mLoadingEmptyContainer.getNoResultsContainer());
        mLoadingEmptyContainer.setVisibility(View.VISIBLE);

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

    /**
     * This allows subclasses to customize the look and feel of the no results container
     *
     * @param empty NoResultsContainer class
     */
    public void setupNoResultsContainer(final NoResultsContainer empty) {
        // do nothing
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Start the loader
        getFragmentLoaderManager().initLoader(getLoaderId(), null, this);
    }

    protected void onItemClick(final int position) {
        playAll(position);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<SectionListContainer<Song>> loader,
                               final SectionListContainer<Song> data) {
        Handler handler = new Handler(requireActivity().getMainLooper());
        if (data.mListResults.isEmpty()) {
            handler.post(() -> mAdapter.unload());
            mLoadingEmptyContainer.showNoResults();
            return;
        }

        mLoadingEmptyContainer.setVisibility(View.GONE);

        handler.post(() -> {
            mAdapter.setData(data.mListResults);
            setHeaders(data.mSections);
        });
    }

    /**
     * @return Gets the list of song ids from the adapter, or null if none
     */
    protected long[] getSongIdsFromAdapter() {
        if (mAdapter != null) {
            return mAdapter.getSongIds();
        }

        return null;
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
        getFragmentLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<SectionListContainer<Song>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    /**
     * If the subclasses want to use a customized SongAdapter they can override this method
     *
     * @return the Song adapter
     */
    protected SongListAdapter createAdapter() {
        return new SongListAdapter(
                requireActivity(),
                R.layout.list_item_normal,
                getFragmentSourceId(),
                getFragmentSourceType(),
                this::onItemClick
        );
    }

    /**
     * Allow subclasses to specify a different loader manager
     *
     * @return Loader Manager to use
     */
    public LoaderManager getFragmentLoaderManager() {
        return LoaderManager.getInstance(this);
    }

    @Override
    public void onMetaChanged() {
        MusicPlaybackTrack currentTrack = MusicUtils.getCurrentTrack();
        mAdapter.setCurrentlyPlayingTrack(currentTrack);
    }

    @Override
    public void onPlaylistChanged() {
        // Nothing to do
    }

    /**
     * LoaderCallbacks identifier
     */
    public abstract int getLoaderId();

    /**
     * If the user clicks play all
     *
     * @param position the position of the item clicked or -1 if shuffle all
     */
    public abstract void playAll(int position);

    protected abstract boolean hasHeaders();

    private void setHeaders(TreeMap<Integer, SectionCreatorUtils.Section> sections) {
        if (!hasHeaders() || sections == null) {
            return;
        }

        for (int i = 0; i < mListView.getItemDecorationCount(); i++) {
            mListView.removeItemDecorationAt(i);
        }
        mListView.addItemDecoration(new SectionSeparatorItemDecoration(requireContext(), sections));
    }
}
