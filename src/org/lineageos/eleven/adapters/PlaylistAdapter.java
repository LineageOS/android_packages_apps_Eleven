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

import org.lineageos.eleven.Config.SmartPlaylistType;
import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Playlist;
import org.lineageos.eleven.ui.MusicHolder;
import org.lineageos.eleven.ui.MusicHolder.DataHolder;
import org.lineageos.eleven.ui.fragments.PlaylistFragment;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This {@link ArrayAdapter} is used to display all of the playlists on a user's
 * device for {@link PlaylistFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistAdapter extends RecyclerView.Adapter<MusicHolder> implements IPopupMenuCallback {

    /**
     * Used to identify the view type
     */
    private static final int USER_PLAYLIST_VIEW_TYPE = 0;

    /**
     * Used to identify the view type
     */
    private static final int SMART_PLAYLIST_VIEW_TYPE = 1;

    /**
     * Used to cache the playlist info
     */
    private DataHolder[] mData;
    private final List<Playlist> mPlaylists;

    /**
     * Used to listen to the pop up menu callbacks
     */
    protected IListener mListener;

    /**
     * Used to listen to item clicks.
     */
    private final Consumer<Integer> mOnItemClickListener;

    private final Context mContext;

    /**
     * Constructor of <code>PlaylistAdapter</code>
     *
     * @param activity The {@link FragmentActivity} to use.
     */
    public PlaylistAdapter(final FragmentActivity activity,
                           final Consumer<Integer> onItemClickListener) {
        mContext = activity;
        mOnItemClickListener = onItemClickListener;
        mPlaylists = new ArrayList<>();
    }

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final int layout;
        switch (viewType) {
            case USER_PLAYLIST_VIEW_TYPE:
                layout = R.layout.list_item_normal;
                break;
            case SMART_PLAYLIST_VIEW_TYPE:
                layout = R.layout.list_item_smart_playlist;
                break;
            default:
                throw new IllegalArgumentException("Unknown view type " + viewType);
        }
        return new MusicHolder(LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        // Retrieve the data holder
        final DataHolder dataHolder = mData[position];

        // set the pop up menu listener
        holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
        // because of recycling, we need to set the position each time
        holder.mPopupMenuButton.get().setPosition(position);
        // Set each playlist name (line one)
        holder.mLineOne.get().setText(dataHolder.lineOne);

        if (dataHolder.lineTwo == null) {
            holder.mLineTwo.get().setVisibility(View.GONE);
        } else {
            holder.mLineTwo.get().setVisibility(View.VISIBLE);
            holder.mLineTwo.get().setText(dataHolder.lineTwo);
        }

        holder.itemView.setOnClickListener(v -> mOnItemClickListener.accept(position));

        SmartPlaylistType type = SmartPlaylistType.getTypeById(dataHolder.itemId);
        if (type != null) {
            // Set the image resource based on the icon
            switch (type) {
                case LastAdded:
                    holder.mImage.get().setImageResource(R.drawable.recently_added);
                    break;
                case RecentlyPlayed:
                    holder.mImage.get().setImageResource(R.drawable.recent_icon);
                    break;
                case TopTracks:
                default:
                    holder.mImage.get().setImageResource(R.drawable.top_tracks_icon);
                    break;
            }
        } else {
            // load the image
            ImageFetcher.getInstance(mContext).loadPlaylistCoverArtImage(
                    dataHolder.itemId, holder.mImage.get());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).isSmartPlaylist()) {
            return SMART_PLAYLIST_VIEW_TYPE;
        } else {
            return USER_PLAYLIST_VIEW_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        return mPlaylists.size();
    }

    public Playlist getItem(int position) {
        return mPlaylists.get(position);
    }

    public void add(Playlist playlist) {
        mPlaylists.add(playlist);
        notifyItemInserted(mPlaylists.size() - 1);
    }

    /**
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    public void buildCache() {
        mData = new DataHolder[mPlaylists.size()];
        for (int i = 0; i < mPlaylists.size(); i++) {
            // Build the artist
            final Playlist playlist = getItem(i);

            // Build the data holder
            mData[i] = new DataHolder();
            // Playlist Id
            mData[i].itemId = playlist.mPlaylistId;
            // Playlist names (line one)
            mData[i].lineOne = playlist.mPlaylistName;
            // # of songs
            if (playlist.mSongCount >= 0) {
                mData[i].lineTwo = MusicUtils.makeLabel(mContext,
                        R.plurals.Nsongs, playlist.mSongCount);
            }
        }
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        int size = mPlaylists.size();
        mPlaylists.clear();
        mData = null;
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public void setPopupMenuClickedListener(IListener listener) {
        mListener = listener;
    }
}
