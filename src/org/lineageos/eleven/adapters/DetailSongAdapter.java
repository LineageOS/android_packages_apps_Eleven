/*
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;
import org.lineageos.eleven.widgets.PopupMenuButton;

import java.util.Collections;
import java.util.List;

public abstract class DetailSongAdapter extends RecyclerView.Adapter<DetailSongAdapter.Holder>
        implements IPopupMenuCallback {

    public static final int NOTHING_PLAYING = -1;

    /**
     * Image cache and image fetcher.
     */
    private final ImageFetcher mImageFetcher;

    /**
     * Source id.
     */
    private long mSourceId = -1;

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IListener mListener;

    /**
     * Current music track.
     */
    private MusicPlaybackTrack mCurrentlyPlayingTrack;

    /**
     * Used to cache the song info.
     */
    private List<Song> mSongs = Collections.emptyList();

    protected final Context mContext;

    /**
     * Constructor of <code>DetailSongAdapter</code>
     *
     * @param context    The {@link Context} to use.
     */
    public DetailSongAdapter(final FragmentActivity context) {
        mContext = context;
        mImageFetcher = ElevenUtils.getImageFetcher(context);
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    public Song getItem(int pos) {
        return mSongs.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    protected long getSourceId() {
        return mSourceId;
    }

    public void setSourceId(long id) {
        mSourceId = id;
    }

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

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return newHolder(LayoutInflater.from(parent.getContext())
                .inflate(rowLayoutId(), parent, false), mImageFetcher);
    }


    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Song song = getItem(position);
        holder.update(song);
        holder.popupMenuButton.setPopupMenuClickedListener(mListener);
        holder.popupMenuButton.setPosition(position);

        if (mCurrentlyPlayingTrack != null
                && mCurrentlyPlayingTrack.mSourceId == getSourceId()
                && mCurrentlyPlayingTrack.mSourceType == getSourceType()
                && mCurrentlyPlayingTrack.mId == song.mSongId) {
            holder.playIcon.setVisibility(View.VISIBLE);
        } else {
            holder.playIcon.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> onItemClick(position));
    }

    protected abstract int rowLayoutId();

    protected abstract Config.IdType getSourceType();

    private void onItemClick(int id) {
        // id is in this case the index in the underlying collection,
        // which is what we are interested in here -- so use as position
        // ignore clicks on the header
        if (id < 0) {
            return;
        }
        // play clicked song and enqueue the rest of the songs in the Adapter
        int songCount = getItemCount();
        long[] toPlay = new long[songCount];
        // add all songs to list
        for (int i = 0; i < songCount; i++) {
            toPlay[i] = getItem(i).mSongId;
        }
        // specify the song position to start playing
        MusicUtils.playAll(toPlay, id, getSourceId(), getSourceType(), false);
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
            }
        }
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
    public void setPopupMenuClickedListener(IListener listener) {
        mListener = listener;
    }

    protected abstract Holder newHolder(View root, ImageFetcher fetcher);

    protected static abstract class Holder extends RecyclerView.ViewHolder {
        protected ImageFetcher fetcher;
        protected TextView title;
        protected PopupMenuButton popupMenuButton;
        protected ImageView playIcon;

        protected Holder(View root, ImageFetcher fetcher) {
            super(root);
            this.fetcher = fetcher;
            title = (TextView) root.findViewById(R.id.title);
            popupMenuButton = (PopupMenuButton) root.findViewById(R.id.overflow);
            playIcon = (ImageView) root.findViewById(R.id.now_playing);
        }

        protected abstract void update(Song song);
    }
}
