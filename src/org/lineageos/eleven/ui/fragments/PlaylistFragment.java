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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config.SmartPlaylistType;
import org.lineageos.eleven.MusicStateListener;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.adapters.PlaylistAdapter;
import org.lineageos.eleven.loaders.PlaylistLoader;
import org.lineageos.eleven.model.Playlist;
import org.lineageos.eleven.ui.activities.BaseActivity;
import org.lineageos.eleven.ui.fragments.phone.MusicBrowserFragment;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.utils.PlaylistPopupMenuHelper;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;

import java.util.Iterator;
import java.util.List;

/**
 * This class is used to display all of the playlists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistFragment extends MusicBrowserFragment implements
        LoaderManager.LoaderCallbacks<List<Playlist>>, MusicStateListener {

    /**
     * The adapter for the list
     */
    private PlaylistAdapter mAdapter;

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
    public PlaylistFragment() {
        // empty
    }

    @Override
    public int getLoaderId() {
        return PagerAdapter.MusicFragments.PLAYLIST.ordinal();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPopupMenuHelper = new PlaylistPopupMenuHelper(getActivity(), getChildFragmentManager(),
                null) {
            @Override
            public Playlist getPlaylist(int position) {
                return mAdapter.getItem(position);
            }
        };

        // Create the adapter
        mAdapter = new PlaylistAdapter(requireActivity(), this::onItemClick);
        mAdapter.setPopupMenuClickedListener((v, position) ->
                mPopupMenuHelper.showPopupMenu(v, position));
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_list,
                container, false);
        // Initialize the list
        // The list view
        RecyclerView listView = rootView.findViewById(R.id.list_base);
        listView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(mAdapter);

        // Setup the loading and empty state
        mLoadingEmptyContainer = rootView.findViewById(R.id.loading_empty_container);
        mLoadingEmptyContainer.setVisibility(View.VISIBLE);

        // Register the music status listener
        final FragmentActivity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setMusicStateListenerListener(this);
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        final FragmentActivity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).removeMusicStateListenerListener(this);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        initLoader(null, this);
    }

    private void onItemClick(int position) {
        Playlist playlist = mAdapter.getItem(position);

        SmartPlaylistType playlistType = SmartPlaylistType.getTypeById(playlist.mPlaylistId);
        if (playlistType != null) {
            NavUtils.openSmartPlaylist(getActivity(), playlistType);
        } else {
            NavUtils.openPlaylist(getActivity(), playlist.mPlaylistId, playlist.mPlaylistName);
        }
    }

    @NonNull
    @Override
    public Loader<List<Playlist>> onCreateLoader(final int id, final Bundle args) {
        // show the loading progress bar
        mLoadingEmptyContainer.showLoading();
        return new PlaylistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<List<Playlist>> loader,
                               final List<Playlist> data) {
        if (data.isEmpty()) {
            mLoadingEmptyContainer.showNoResults();
            return;
        }

        mLoadingEmptyContainer.setVisibility(View.GONE);

        // Start fresh, fill adapter with new data and create cache
        mAdapter.unload();

        // iterate through playlist list and add "smart playlists" first
        final Iterator<Playlist> playlistIterator = data.listIterator();
        while (playlistIterator.hasNext()) {
            final Playlist playlist = playlistIterator.next();
            if (playlist.mSongCount < 0) {
                mAdapter.add(playlist);
                playlistIterator.remove();
            }
        }

        // after the "smart playlists" are added, sort and add remaining playlists
        data.sort(new Playlist.IgnoreCaseComparator());
        for (final Playlist playlist : data) {
            mAdapter.add(playlist);
        }

        mAdapter.buildCache();
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<List<Playlist>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    @Override
    public void restartLoader() {
        restartLoader(this);
    }

    @Override
    public void onPlaylistChanged() {
        restartLoader();
    }

    @Override
    public void onMetaChanged() {
        // Nothing to do
    }
}
