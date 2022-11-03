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
import org.lineageos.eleven.ui.fragments.QueueFragment;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;
import org.lineageos.eleven.widgets.PlayPauseButtonContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This {@link RecyclerView.Adapter} is used to show the queue in
 * {@link QueueFragment}.
 */
public class QueueSongAdapter extends RecyclerView.Adapter<MusicHolder> implements
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
     * Current music track
     */
    private MusicPlaybackTrack mCurrentlyPlayingTrack;

    private List<Song> mSongs;

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IPopupMenuCallback.IListener mListener;

    /**
     * Source id and type
     */
    protected final long mSourceId;
    protected final Config.IdType mSourceType;

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
    public QueueSongAdapter(final FragmentActivity context, final int layoutId, final long sourceId,
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
        mSongs = new ArrayList<>();
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

        holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
        // Sets the position each time because of recycling
        holder.mPopupMenuButton.get().setPosition(position);
        // Set each song name (line one)
        holder.mLineOne.get().setText(item.mSongName);
        // Set the album name (line two)
        holder.mLineTwo.get().setText(MusicUtils.makeCombinedString(mContext, item.mArtistName,
                item.mAlbumName));

        holder.itemView.setOnClickListener(v -> mOnItemClickListener.accept(position));

        // Asynchronously load the artist image into the adapter
        if (item.mAlbumId >= 0) {
            mImageFetcher.loadAlbumImage(item.mArtistName, item.mAlbumName, item.mAlbumId,
                    holder.mImage.get());
        }

        // padding doesn't apply to included layouts, so we need
        // to wrap it in a container and show/hide with the container
        PlayPauseButtonContainer buttonContainer = holder.mPlayPauseProgressButton.get();
        if (buttonContainer != null) {
            View playPauseContainer = holder.mPlayPauseProgressContainer.get();

            if (showNowPlayingIndicator(item)) {
                // make it visible
                buttonContainer.enableAndShow();
                playPauseContainer.setVisibility(View.VISIBLE);
            } else {
                // hide it
                buttonContainer.disableAndHide();
                playPauseContainer.setVisibility(View.GONE);
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

    @Override
    public void setPopupMenuClickedListener(IListener listener) {
        mListener = listener;
    }

    public Song getItem(int position) {
        return mSongs.get(position);
    }

    public void setData(List<Song> song) {
        int oldSize = mSongs == null ? 0 : mSongs.size();
        int newSize = song.size();

        mSongs = song;
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

    public void remove(int position) {
        mSongs.remove(position);
        notifyItemRemoved(position);
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
