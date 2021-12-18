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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.Loader;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.Config.SmartPlaylistType;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.SongAdapter;
import org.lineageos.eleven.loaders.TopTracksLoader;
import org.lineageos.eleven.menu.FragmentMenuItems;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.sectionadapter.SectionCreator;
import org.lineageos.eleven.sectionadapter.SectionListContainer;
import org.lineageos.eleven.ui.activities.BaseActivity;
import org.lineageos.eleven.ui.fragments.profile.SmartPlaylistFragment;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.NoResultsContainer;

import java.util.TreeSet;

/**
 * This class is used to display all of the recently listened to songs by the
 * user.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentFragment extends SmartPlaylistFragment implements ISetupActionBar {

    @Override
    protected SmartPlaylistType getSmartPlaylistType() {
        return Config.SmartPlaylistType.RecentlyPlayed;
    }

    @Override
    protected void updateMenuIds(TreeSet<Integer> set) {
        set.add(FragmentMenuItems.REMOVE_FROM_RECENT);
    }

    @NonNull
    @Override
    public Loader<SectionListContainer<Song>> onCreateLoader(final int id, final Bundle args) {
        // show the loading progress bar
        mLoadingEmptyContainer.showLoading();

        TopTracksLoader loader = new TopTracksLoader(getActivity(),
                TopTracksLoader.QueryType.RecentSongs);
        return new SectionCreator<>(getActivity(), loader, null);
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();

        // refresh the list since a track playing means it should be recently played
        restartLoader();
    }

    @Override
    public void setupNoResultsContainer(NoResultsContainer empty) {
        super.setupNoResultsContainer(empty);

        empty.setMainText(R.string.empty_recent_main);
        empty.setSecondaryText(R.string.empty_recent);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
                                   Bundle savedInstanceState) {
        setupActionBar();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void setupActionBar() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof BaseActivity) {
            final BaseActivity baseActivity = (BaseActivity) activity;
            baseActivity.setupActionBar(R.string.playlist_recently_played);
            baseActivity.setActionBarElevation(true);
        }
    }

    @Override
    protected long getFragmentSourceId() {
        return Config.SmartPlaylistType.RecentlyPlayed.mId;
    }

    @Override
    protected SongAdapter createAdapter() {
        return new RecentAdapter(
                getActivity(),
                R.layout.list_item_normal,
                getFragmentSourceId(),
                getFragmentSourceType()
        );
    }

    private static class RecentAdapter extends SongAdapter {
        public RecentAdapter(Activity context, int layoutId, long sourceId,
                             Config.IdType sourceType) {
            super(context, layoutId, sourceId, sourceType);
        }

        @Override
        protected boolean showNowPlayingIndicator(Song song, int position) {
            return position == 0 && super.showNowPlayingIndicator(song, position);
        }
    }

    @Override
    protected int getShuffleTitleId() {
        return R.string.menu_shuffle_recent;
    }

    @Override
    protected int getClearTitleId() {
        return R.string.clear_recent_title;
    }

    @Override
    protected void clearList() {
        MusicUtils.clearRecent(getActivity());
    }
}
