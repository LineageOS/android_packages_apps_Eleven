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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

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

public abstract class DetailSongAdapter extends BaseAdapter implements
        LoaderManager.LoaderCallbacks<List<Song>>, OnItemClickListener, IPopupMenuCallback {
    protected final Activity mActivity;
    private final ImageFetcher mImageFetcher;
    private final LayoutInflater mInflater;
    private List<Song> mSongs = Collections.emptyList();
    private IListener mListener;
    private long mSourceId = -1;
    private MusicPlaybackTrack mCurrentlyPlayingTrack;

    public DetailSongAdapter(final Activity activity) {
        mActivity = activity;
        mImageFetcher = ElevenUtils.getImageFetcher(activity);
        mInflater = LayoutInflater.from(activity);
    }

    @Override
    public int getCount() {
        return mSongs.size();
    }

    @Override
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

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(rowLayoutId(), parent, false);
            convertView.setTag(newHolder(convertView, mImageFetcher));
        }

        Holder holder = (Holder) convertView.getTag();

        Song song = getItem(pos);
        holder.update(song);
        holder.popupMenuButton.setPopupMenuClickedListener(mListener);
        holder.popupMenuButton.setPosition(pos);

        if (mCurrentlyPlayingTrack != null
                && mCurrentlyPlayingTrack.mSourceId == getSourceId()
                && mCurrentlyPlayingTrack.mSourceType == getSourceType()
                && mCurrentlyPlayingTrack.mId == song.mSongId) {
            holder.playIcon.setVisibility(View.VISIBLE);
        } else {
            holder.playIcon.setVisibility(View.GONE);
        }

        return convertView;
    }

    protected abstract int rowLayoutId();

    protected abstract void onLoading();

    protected abstract void onNoResults();

    protected abstract Config.IdType getSourceType();

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        // id is in this case the index in the underlying collection,
        // which is what we are interested in here -- so use as position
        int position = (int) id;
        // ignore clicks on the header
        if (id < 0) {
            return;
        }
        // play clicked song and enqueue the rest of the songs in the Adapter
        int songCount = getCount();
        long[] toPlay = new long[songCount];
        // add all songs to list
        for (int i = 0; i < songCount; i++) {
            toPlay[i] = getItem(i).mSongId;
        }
        // specify the song position to start playing
        MusicUtils.playAll(toPlay, position, getSourceId(), getSourceType(), false);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> songs) {
        if (songs.isEmpty()) {
            onNoResults();
            return;
        }
        mSongs = songs;
        notifyDataSetChanged();
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

    protected static abstract class Holder {
        protected ImageFetcher fetcher;
        protected TextView title;
        protected PopupMenuButton popupMenuButton;
        protected ImageView playIcon;

        protected Holder(View root, ImageFetcher fetcher) {
            this.fetcher = fetcher;
            title = root.findViewById(R.id.title);
            popupMenuButton = root.findViewById(R.id.overflow);
            playIcon = root.findViewById(R.id.now_playing);
        }

        protected abstract void update(Song song);
    }
}
