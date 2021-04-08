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

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Artist;
import org.lineageos.eleven.ui.MusicHolder;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This {@link ArrayAdapter} is used to display all of the artists on a user's
 * device
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistAdapter extends RecyclerView.Adapter<MusicHolder> implements IPopupMenuCallback {

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Image cache and image fetcher
     */
    private final ImageFetcher mImageFetcher;

    /**
     * Used to cache the artist info
     */
    private List<Artist> mArtists;

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IListener mListener;

    private final Context mContext;
    private final Handler mHandler;
    private final Consumer<Integer> mOnItemClickListener;

    /**
     * Constructor of <code>ArtistAdapter</code>
     *
     * @param context  The {@link Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public ArtistAdapter(final FragmentActivity context, final int layoutId,
                         final Consumer<Integer> onItemClickListener) {
        mContext = context;
        mHandler = new Handler(context.getMainLooper());
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ElevenUtils.getImageFetcher(context);
        mOnItemClickListener = onItemClickListener;
        mArtists = new ArrayList<>(0);
    }

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicHolder(LayoutInflater.from(parent.getContext())
                .inflate(mLayoutId, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        Artist artist = getItem(position);
        String albumNumber = MusicUtils.makeLabel(mContext,
                R.plurals.Nalbums, artist.mAlbumNumber);
        String songNumber = MusicUtils.makeLabel(mContext,
                R.plurals.Nsongs, artist.mSongNumber);

        holder.itemView.setOnClickListener(v -> mOnItemClickListener.accept(position));
        // set the pop up menu listener
        holder.mPopupMenuButton.get().setPopupMenuClickedListener(mListener);
        // Set each artist name (line one)
        holder.mLineOne.get().setText(artist.mArtistName);
        // Set the number of albums (line two)
        holder.mLineTwo.get().setText(MusicUtils.makeCombinedString(mContext,
                albumNumber, songNumber));
        // Asynchronously load the artist image into the adapter
        mImageFetcher.loadArtistImage(artist.mArtistName, holder.mImage.get());
        // because of recycling, we need to set the position each time
        holder.mPopupMenuButton.get().setPosition(position);
    }

    @Override
    public int getItemCount() {
        return mArtists.size();
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        mHandler.post(() -> {
            int size = mArtists.size();
            mArtists.clear();
            notifyItemRangeRemoved(0, size);
        });
    }

    /**
     * Flushes the disk cache.
     */
    public void flush() {
        mImageFetcher.flush();
    }

    @Override
    public void setPopupMenuClickedListener(IListener listener) {
        mListener = listener;
    }

    public Artist getItem(int position) {
        return mArtists.get(position);
    }

    public void setData(List<Artist> artists) {
        mHandler.post(() -> {
            int oldSize = mArtists == null ? 0 : mArtists.size();
            int newSize = artists.size();

            mArtists = artists;

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
        });
    }
}
