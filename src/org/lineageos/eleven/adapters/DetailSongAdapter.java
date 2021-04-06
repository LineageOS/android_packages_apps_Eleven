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

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
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
        implements LoaderManager.LoaderCallbacks<List<Song>>, IPopupMenuCallback {
    protected final Activity mActivity;
    private final Handler mHandler;
    private final ImageFetcher mImageFetcher;
    private final LayoutInflater mInflater;
    private List<Song> mSongs = Collections.emptyList();
    private IListener mListener;
    private long mSourceId = -1;
    private MusicPlaybackTrack mCurrentlyPlayingTrack;

    public DetailSongAdapter(final FragmentActivity activity) {
        mActivity = activity;
        mHandler = new Handler(activity.getMainLooper());
        mImageFetcher = ElevenUtils.getImageFetcher(activity);
        mInflater = LayoutInflater.from(activity);
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

    protected void setSourceId(long id) {
        mSourceId = id;
    }

    public void setCurrentlyPlayingTrack(MusicPlaybackTrack currentTrack) {
        if (mCurrentlyPlayingTrack == null || !mCurrentlyPlayingTrack.equals(currentTrack)) {
            mCurrentlyPlayingTrack = currentTrack;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return newHolder(mInflater.inflate(rowLayoutId(), parent, false), mImageFetcher);
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

    protected abstract void onLoading();

    protected abstract void onNoResults();

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
        MusicUtils.playAll(mActivity, toPlay, id, getSourceId(), getSourceType(), false);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> songs) {
        // Do on UI thread: https://issuetracker.google.com/issues/37030377
        mHandler.post(() -> {
            if (songs.isEmpty()) {
                onNoResults();
                return;
            }

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
        });
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        mSongs = Collections.emptyList();
        notifyDataSetChanged();
        mImageFetcher.flush();
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
            title = root.findViewById(R.id.title);
            popupMenuButton = root.findViewById(R.id.overflow);
            playIcon = root.findViewById(R.id.now_playing);
        }

        protected abstract void update(Song song);
    }
}
