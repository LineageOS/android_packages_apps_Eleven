/*
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
package org.lineageos.eleven.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.ui.MusicHolder;
import org.lineageos.eleven.ui.MusicHolder.DataHolder;
import org.lineageos.eleven.ui.fragments.SongFragment;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This {@link RecyclerView.Adapter} is used to display all of the songs on a user's
 * device for {@link SongFragment}.
 */
public class SongListAdapter extends RecyclerView.Adapter<MusicHolder> implements
        IPopupMenuCallback {

    public static final int NOTHING_PLAYING = -1;

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Image cache and image fetcher
     */
    private final ImageFetcher mImageFetcher;

    /**
     * Used to cache the song info
     */
    private DataHolder[] mData;
    private List<Song> mSongs;

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IListener mListener;

    /**
     * Current music track
     */
    private MusicPlaybackTrack mCurrentlyPlayingTrack;

    /**
     * Source id and type
     */
    private final long mSourceId;
    private final Config.IdType mSourceType;

    private final Context mContext;
    private final Consumer<Integer> mOnItemClickListener;

    /**
     * Constructor of <code>SongAdapter</code>
     *
     * @param context    The {@link Context} to use.
     * @param layoutId   The resource Id of the view to inflate.
     * @param sourceId   The source id that the adapter is created from
     * @param sourceType The source type that the adapter is created from
     */
    public SongListAdapter(final FragmentActivity context, final int layoutId, final long sourceId,
                           final Config.IdType sourceType,
                           final Consumer<Integer> onItemClickListener) {
        mContext = context;
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ElevenUtils.getImageFetcher(context);
        // set the source id and type
        mSourceId = sourceId;
        mSourceType = sourceType;
        mOnItemClickListener = onItemClickListener;
        mSongs = new ArrayList<>(0);
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

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicHolder(LayoutInflater.from(parent.getContext())
                .inflate(mLayoutId, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        // Retrieve the data holder
        final DataHolder dataHolder = mData[position];

        holder.itemView.setOnClickListener(v -> mOnItemClickListener.accept(position));

        holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
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

        View nowPlayingIndicator = holder.mNowPlayingIndicator.get();
        if (nowPlayingIndicator != null) {
            if (showNowPlayingIndicator(item, position)) {
                nowPlayingIndicator.setVisibility(View.VISIBLE);
            } else {
                nowPlayingIndicator.setVisibility(View.GONE);
            }
        }

        customizeBind(holder, position);
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    protected void customizeBind(@NonNull MusicHolder holder, int position) {
    }

    /**
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    public void buildCache() {
        mData = new DataHolder[getItemCount()];
        for (int i = 0; i < getItemCount(); i++) {
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
            // Song names
            mData[i].lineOne = song.mSongName;
            // Song duration
            mData[i].lineOneRight = MusicUtils.makeShortTimeString(mContext, song.mDuration);

            // Artist Name | Album Name
            mData[i].lineTwo = MusicUtils.makeCombinedString(mContext, song.mArtistName,
                    song.mAlbumName);
        }
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        int size = mSongs.size();
        mSongs.clear();
        mData = null;
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public void setPopupMenuClickedListener(IListener listener) {
        mListener = listener;
    }

    /**
     * Sets the currently playing track for the adapter to know when to show indicators
     *
     * @param currentTrack the currently playing track
     */
    public void setCurrentlyPlayingTrack(MusicPlaybackTrack currentTrack) {
        if (mCurrentlyPlayingTrack != null && mCurrentlyPlayingTrack.equals(currentTrack)) {
            return;
        }

        long previousPlayingId = mCurrentlyPlayingTrack == null
                ? NOTHING_PLAYING : mCurrentlyPlayingTrack.mId;
        mCurrentlyPlayingTrack = currentTrack;

        int toBeUpdated = (currentTrack == null || currentTrack.mId == NOTHING_PLAYING)
                ? 1 : 2;
        int updated = 0;

        for (int i = 0; i < mSongs.size() && updated < toBeUpdated; i++) {
            long id = mSongs.get(i).mSongId;
            if ((currentTrack != null && id == currentTrack.mId) || id == previousPlayingId) {
                notifyItemChanged(i);
                updated++;
            }
        }
    }

    /**
     * @return Gets the list of song ids from the adapter
     */
    public long[] getSongIds() {
        long[] ret = new long[mSongs.size()];
        for (int i = 0; i < mSongs.size(); i++) {
            ret[i] = getItem(i).mSongId;
        }

        return ret;
    }

    public Song getItem(int position) {
        return mSongs.get(position);
    }

    public void setData(List<Song> songs) {
        int oldSize = mSongs == null ? 0 : mSongs.size();
        int newSize = songs.size();

        mSongs = songs;
        buildCache();

        if (oldSize == 0) {
            notifyItemRangeInserted(0, newSize);
        } else {
            int diff = oldSize - newSize;
            if (diff > 0) {
                // Items were removed
                notifyItemRangeChanged(0, newSize);
                notifyItemRangeRemoved(newSize, diff);
            } else if (diff < 0) {
                // Items were added
                notifyItemRangeChanged(0, oldSize);
                notifyItemRangeInserted(oldSize, diff * -1);
            } else {
                notifyItemChanged(0, oldSize);
            }
        }
    }
}
