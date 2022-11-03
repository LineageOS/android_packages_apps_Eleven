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
package org.lineageos.eleven.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.ui.MusicHolder;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This {@link ArrayAdapter} is used to display the songs for a particular playlist
 * {@link org.lineageos.eleven.ui.fragments.PlaylistDetailFragment}
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileSongAdapter extends RecyclerView.Adapter<MusicHolder> implements
        IPopupMenuCallback {

    private static final int NOTHING_PLAYING = -1;

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
    private List<Song> mSongs;

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IPopupMenuCallback.IListener mListener;

    /**
     * Current music track
     */
    private MusicPlaybackTrack mCurrentlyPlayingTrack;

    /**
     * Source id and type
     */
    private final long mSourceId;
    private final Config.IdType mSourceType = Config.IdType.Playlist;

    private final Context mContext;
    private final Consumer<Integer> mOnItemClickListener;

    /**
     * Constructor of <code>ProfileSongAdapter</code>
     *
     * @param context The {@link FragmentActivity} to use
     * @param layoutId The resource Id of the view to inflate.
     */
    public ProfileSongAdapter(final long playlistId, final FragmentActivity context,
                              final int layoutId, final Consumer<Integer> onItemClickListener) {
        mContext = context;
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ElevenUtils.getImageFetcher(context);
        // set the source id and type
        mSourceId = playlistId;
        mOnItemClickListener = onItemClickListener;
        mSongs = new ArrayList<>(0);
    }

    /**
     * Determines whether the song at the position should show the currently playing indicator
     *
     * @param song     the song in question
     * @return true if we want to show the indicator
     */
    protected boolean showNowPlayingIndicator(final Song song) {
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
        Song item = getItem(position);

        holder.itemView.setOnClickListener(v -> mOnItemClickListener.accept(position));

        holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
        // Sets the position each time because of recycling
        holder.mPopupMenuButton.get().setPosition(position);
        // Set each song name (line one)
        holder.mLineOne.get().setText(item.mSongName);
        // Set the album name (line two)
        holder.mLineTwo.get().setText(MusicUtils.makeCombinedString(mContext, item.mArtistName,
                item.mAlbumName));

        // Asynchronously load the artist image into the adapter
        if (item.mAlbumId >= 0) {
            mImageFetcher.loadAlbumImage(item.mArtistName, item.mAlbumName, item.mAlbumId,
                    holder.mImage.get());
        }

        View nowPlayingIndicator = holder.mNowPlayingIndicator.get();
        if (nowPlayingIndicator != null) {
            if (showNowPlayingIndicator(item)) {
                nowPlayingIndicator.setVisibility(View.VISIBLE);
            } else {
                nowPlayingIndicator.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        int size = mSongs.size();
        mSongs.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public void setPopupMenuClickedListener(IPopupMenuCallback.IListener listener) {
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

    public Song getItem(int position) {
        return mSongs.get(position);
    }

    public void setData(List<Song> songs) {
        int oldSize = mSongs == null ? 0 : mSongs.size();
        int newSize = songs.size();

        mSongs = songs;

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

    public void remove(Song song) {
        final int index = mSongs.indexOf(song);
        if (index >= 0) {
            mSongs.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void move(int startPosition, int endPosition) {
        if (startPosition == endPosition) {
            return;
        }

        Song moving = mSongs.remove(startPosition);
        mSongs.add(endPosition, moving);
        notifyItemMoved(startPosition, endPosition);
    }
}
