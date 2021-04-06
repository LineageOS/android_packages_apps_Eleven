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
import android.widget.BaseAdapter;

import androidx.fragment.app.FragmentActivity;

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Album;
import org.lineageos.eleven.ui.MusicHolder;
import org.lineageos.eleven.ui.MusicHolder.DataHolder;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;

import java.util.Collections;
import java.util.List;

/**
 * This {@link ArrayAdapter} is used to display all of the albums on a user's
 * device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumAdapter extends BaseAdapter implements IPopupMenuCallback {
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

    /**
     * number of columns of containing grid view,
     * used to determine which views to pad
     */
    private int mColumns;
    private final int mPadding;

    private final Context mContext;

    /**
     * Constructor of <code>AlbumAdapter</code>
     *
     * @param context  The {@link Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public AlbumAdapter(final FragmentActivity context, final int layoutId) {
        mContext = context;
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ElevenUtils.getImageFetcher(context);
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.list_item_general_margin);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        // Recycle ViewHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            convertView.setTag(holder);
            // set the pop up menu listener
            holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        adjustPadding(position, convertView);

        // Retrieve the data holder
        final DataHolder dataHolder = mData[position];

        // Sets the position each time because of recycling
        holder.mPopupMenuButton.get().setPosition(position);
        // Set each album name (line one)
        holder.mLineOne.get().setText(dataHolder.lineOne);
        // Set the artist name (line two)
        holder.mLineTwo.get().setText(dataHolder.lineTwo);
        // Asynchronously load the album images into the adapter
        mImageFetcher.loadAlbumImage(
                dataHolder.lineTwo, dataHolder.lineOne,
                dataHolder.itemId, holder.mImage.get());

        return convertView;
    }

    private void adjustPadding(final int position, View convertView) {
        if (position < mColumns) {
            // first row
            convertView.setPadding(0, mPadding, 0, 0);
            return;
        }
        int count = getCount();
        int footers = count % mColumns;
        if (footers == 0) {
            footers = mColumns;
        }
        if (position >= (count - footers)) {
            // last row
            convertView.setPadding(0, 0, 0, mPadding);
        } else {
            // middle rows
            convertView.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return mAlbums.size();
    }

    @Override
    public Album getItem(int pos) {
        return mAlbums.get(pos);
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
        mAlbums = albums;
        buildCache();
        notifyDataSetChanged();
    }

    public void setNumColumns(int columns) {
        mColumns = columns;
    }

    public void unload() {
        setData(Collections.emptyList());
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
