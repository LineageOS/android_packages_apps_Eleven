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
package org.lineageos.eleven.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.fragment.app.FragmentActivity;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.sectionadapter.SectionAdapter;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.ui.MusicHolder;
import org.lineageos.eleven.ui.MusicHolder.DataHolder;
import org.lineageos.eleven.ui.fragments.QueueFragment;
import org.lineageos.eleven.ui.fragments.SongFragment;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;
import org.lineageos.eleven.widgets.PlayPauseButtonContainer;

/**
 * This {@link ArrayAdapter} is used to display all of the songs on a user's
 * device for {@link SongFragment}. It is also used to show the queue in
 * {@link QueueFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongAdapter extends ArrayAdapter<Song>
        implements SectionAdapter.BasicAdapter, IPopupMenuCallback {

    public static final int NOTHING_PLAYING = -1;

    /**
     * Number of views (TextView)
     */
    private static final int VIEW_TYPE_COUNT = 1;

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Image cache and image fetcher
     */
    private final ImageFetcher mImageFetcher;

    /**
     * The index of the item that is currently playing
     */
    private long mCurrentQueuePosition = NOTHING_PLAYING;

    /**
     * Used to cache the song info
     */
    private DataHolder[] mData;

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IPopupMenuCallback.IListener mListener;

    /**
     * Current music track
     */
    protected MusicPlaybackTrack mCurrentlyPlayingTrack;

    /**
     * Source id and type
     */
    protected long mSourceId;
    protected Config.IdType mSourceType;

    /**
     * Constructor of <code>SongAdapter</code>
     *
     * @param context    The {@link Context} to use.
     * @param layoutId   The resource Id of the view to inflate.
     * @param sourceId   The source id that the adapter is created from
     * @param sourceType The source type that the adapter is created from
     */
    public SongAdapter(final FragmentActivity context, final int layoutId, final long sourceId,
                       final Config.IdType sourceType) {
        super(context, 0);
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ElevenUtils.getImageFetcher(context);
        // set the source id and type
        mSourceId = sourceId;
        mSourceType = sourceType;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        // Recycle ViewHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            convertView.setTag(holder);

            holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        // Retrieve the data holder
        final DataHolder dataHolder = mData[position];

        // Sets the position each time because of recycling
        holder.mPopupMenuButton.get().setPosition(position);
        // Set each song name (line one)
        holder.mLineOne.get().setText(dataHolder.lineOne);
        // Set the album name (line two)
        holder.mLineTwo.get().setText(dataHolder.lineTwo);

        // Asynchronously load the artist image into the adapter
        Song item = getItem(position);
        if (item.mAlbumId >= 0) {
            mImageFetcher.loadAlbumImage(item.mArtistName, item.mAlbumName, item.mAlbumId,
                    holder.mImage.get());
        }

        // padding doesn't apply to included layouts, so we need
        // to wrap it in a container and show/hide with the container
        PlayPauseButtonContainer playPauseButtonContainer = holder.mPlayPauseProgressButton.get();
        if (playPauseButtonContainer != null) {
            View playPauseContainer = holder.mPlayPauseProgressContainer.get();

            if (mCurrentQueuePosition == position) {
                // make it visible
                playPauseButtonContainer.enableAndShow();
                playPauseContainer.setVisibility(View.VISIBLE);
            } else {
                // hide it
                playPauseButtonContainer.disableAndHide();
                playPauseContainer.setVisibility(View.GONE);
            }
        }

        View nowPlayingIndicator = holder.mNowPlayingIndicator.get();
        if (nowPlayingIndicator != null) {
            if (showNowPlayingIndicator(item, position)) {
                nowPlayingIndicator.setVisibility(View.VISIBLE);
            } else {
                nowPlayingIndicator.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    /**
     * Determines whether the song at the position should show the currently playing indicator
     *
     * @param song     the song in question
     * @param position the position of the song
     * @return true if we want to show the indicator
     */
    protected boolean showNowPlayingIndicator(final Song song, final int position) {
        return mCurrentlyPlayingTrack != null
                && mCurrentlyPlayingTrack.mSourceId == mSourceId
                && mCurrentlyPlayingTrack.mSourceType == mSourceType
                && mCurrentlyPlayingTrack.mId == song.mSongId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    @Override
    public void buildCache() {
        mData = new DataHolder[getCount()];
        for (int i = 0; i < getCount(); i++) {
            // Build the song
            final Song song = getItem(i);

            // skip special placeholders
            if (song.mSongId == -1) {
                continue;
            }

            // Build the data holder
            mData[i] = new DataHolder();
            // Song Id
            mData[i].itemId = song.mSongId;
            // Song names (line one)
            mData[i].lineOne = song.mSongName;
            // Song duration (line one, right)
            mData[i].lineOneRight = MusicUtils.makeShortTimeString(getContext(), song.mDuration);

            // Artist Name | Album Name (line two)
            mData[i].lineTwo = MusicUtils.makeCombinedString(getContext(), song.mArtistName,
                    song.mAlbumName);
        }
    }

    /**
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(final boolean pause) {
        if (mImageFetcher != null) {
            mImageFetcher.setPauseDiskCache(pause);
        }
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    @Override
    public void unload() {
        clear();
        mData = null;
    }

    /**
     * Do nothing.
     */
    @Override
    public void flush() {
    }

    /**
     * Gets the item position for a given id
     *
     * @param id identifies the object
     * @return the position if found, -1 otherwise
     */
    @Override
    public int getItemPosition(long id) {
        for (int i = 0; i < getCount(); i++) {
            if (getItem(i).mSongId == id) {
                return i;
            }
        }

        return -1;
    }

    public void setCurrentQueuePosition(long queuePosition) {
        if (mCurrentQueuePosition != queuePosition) {
            mCurrentQueuePosition = queuePosition;

            notifyDataSetChanged();
        }
    }

    @Override
    public void setPopupMenuClickedListener(IListener listener) {
        mListener = listener;
    }

    /**
     * Sets the currently playing track for the adapter to know when to show indicators
     *
     * @param currentTrack the currently playing track
     * @return true if the current track is different
     */
    public boolean setCurrentlyPlayingTrack(MusicPlaybackTrack currentTrack) {
        if (mCurrentlyPlayingTrack == null || !mCurrentlyPlayingTrack.equals(currentTrack)) {
            mCurrentlyPlayingTrack = currentTrack;

            notifyDataSetChanged();
            return true;
        }

        return false;
    }

    /**
     * @return Gets the list of song ids from the adapter
     */
    public long[] getSongIds() {
        long[] ret = new long[getCount()];
        for (int i = 0; i < getCount(); i++) {
            ret[i] = getItem(i).mSongId;
        }

        return ret;
    }
}
