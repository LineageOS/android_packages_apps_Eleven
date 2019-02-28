/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.lineageos.eleven.ui.fragments;

import android.content.Context;
import android.os.Bundle;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.loaders.SongLoader;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.sectionadapter.SectionCreator;
import org.lineageos.eleven.sectionadapter.SectionListContainer;
import org.lineageos.eleven.ui.fragments.profile.BasicSongFragment;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.SectionCreatorUtils;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * This class is used to display all of the songs on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongFragment extends BasicSongFragment {

    /**
     * {@inheritDoc}
     */
    public void playAll(int position) {
        int internalPosition = mAdapter.getInternalPosition(position);
        final long[] list = mAdapter.getUnderlyingAdapter().getSongIds();
        if (list != null) {
            MusicUtils.playAll(list, internalPosition, -1, Config.IdType.NA, false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<SectionListContainer<Song>> onCreateLoader(final int id, final Bundle args) {
        // show the loading progress bar
        mLoadingEmptyContainer.showLoading();

        // get the context
        Context context = getActivity();

        // create the underlying song loader
        SongLoader songLoader = new SongLoader(context);

        // get the song comparison method to create the headers with
        SectionCreatorUtils.IItemCompare<Song> songComparison = SectionCreatorUtils.createSongComparison(context);

        // return the wrapped section creator
        return new SectionCreator<>(context, songLoader, songComparison);
    }


    @Override
    public int getLoaderId() {
        return PagerAdapter.MusicFragments.SONG.ordinal();
    }

    /**
     * Scrolls the list to the currently playing song when the user touches the
     * header in the {@link TitlePageIndicator}.
     */
    public void scrollToCurrentSong() {
        final int currentSongPosition = getItemPositionBySong();

        if (currentSongPosition != 0) {
            mListView.setSelection(currentSongPosition);
        }
    }

    /**
     * @return The position of an item in the list based on the name of the
     * currently playing song.
     */
    private int getItemPositionBySong() {
        final long trackId = MusicUtils.getCurrentAudioId();
        if (mAdapter == null) {
            return 0;
        }

        int position = mAdapter.getItemPosition(trackId);

        // if for some reason we don't find the item, just jump to the top
        if (position < 0) {
            return 0;
        }

        return position;
    }

    @Override
    public LoaderManager getFragmentLoaderManager() {
        return getParentFragment().getLoaderManager();
    }
}
