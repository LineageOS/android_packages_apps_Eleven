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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Album;
import org.lineageos.eleven.ui.MusicHolder;
import org.lineageos.eleven.ui.MusicHolder.DataHolder;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * This {@link ArrayAdapter} is used to display all of the albums on a user's
 * device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumAdapter extends RecyclerView.Adapter<MusicHolder> implements IPopupMenuCallback {
    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Image cache and image fetcher
     */
    private final ImageFetcher mImageFetcher;

    /**
     * Used to cache the album info
     */
    private DataHolder[] mData = new DataHolder[0];
    private List<Album> mAlbums = Collections.emptyList();

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IPopupMenuCallback.IListener mListener;

    private final Context mContext;
    private final Consumer<Album> mOnItemClickedListener;

    /**
     * Constructor of <code>AlbumAdapter</code>
     *
     * @param context  The {@link Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public AlbumAdapter(final FragmentActivity context, final int layoutId,
                        final Consumer<Album> onItemClickedListener) {
        mContext = context;
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ElevenUtils.getImageFetcher(context);
        mOnItemClickedListener = onItemClickedListener;
    }

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicHolder(LayoutInflater.from(mContext).inflate(mLayoutId, parent, false));
    }

    @Override
    public int getItemCount() {
        return mAlbums.size();
    }

    public Album getItem(int pos) {
        return mAlbums.get(pos);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        // Retrieve the data holder
        final DataHolder dataHolder = mData[position];

        // set the pop up menu listener
        holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
        // Sets the position each time because of recycling
        holder.mPopupMenuButton.get().setPosition(position);
        // Set each album name (line one)
        holder.mLineOne.get().setText(dataHolder.lineOne);
        // Set the artist name (line two)
        holder.mLineTwo.get().setText(dataHolder.lineTwo);
        // Set click listener
        holder.itemView.setOnClickListener(v -> mOnItemClickedListener.accept(getItem(position)));
        // Asynchronously load the album images into the adapter
        mImageFetcher.loadAlbumImage(
                dataHolder.lineTwo, dataHolder.lineOne,
                dataHolder.itemId, holder.mImage.get());
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    /**
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    public void buildCache() {
        mData = new DataHolder[mAlbums.size()];
        int i = 0;
        for (Album album : mAlbums) {
            mData[i] = new DataHolder();
            mData[i].itemId = album.mAlbumId;
            mData[i].lineOne = album.mAlbumName;
            mData[i].lineTwo = album.mArtistName;
            i++;
        }
    }

    public void setData(List<Album> albums) {
        int oldSize = mAlbums == null ? 0 : mAlbums.size();
        int newSize = albums.size();

        mAlbums = albums;
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

    public void unload() {
        int size = mAlbums.size();
        mAlbums.clear();
        notifyItemRangeRemoved(0, size);
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
     * Flushes the disk cache.
     */
    public void flush() {
        mImageFetcher.flush();
    }

    @Override
    public void setPopupMenuClickedListener(IPopupMenuCallback.IListener listener) {
        mListener = listener;
    }
}