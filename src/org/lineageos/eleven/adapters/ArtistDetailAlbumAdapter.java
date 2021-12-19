/*
 * Copyright (C) 2014 The CyanogenMod Project
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.model.Album;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.widgets.IPopupMenuCallback;
import org.lineageos.eleven.widgets.PopupMenuButton;

import java.util.Collections;
import java.util.List;

public class ArtistDetailAlbumAdapter
        extends RecyclerView.Adapter<ArtistDetailAlbumAdapter.ViewHolder>
        implements IPopupMenuCallback {
    private static final int TYPE_FIRST = 1;
    private static final int TYPE_MIDDLE = 2;
    private static final int TYPE_LAST = 3;

    /**
     * Image cache and image fetcher.
     */
    private final ImageFetcher mImageFetcher;

    /**
     * Used to listen to the pop up menu callbacks
     */
    private IListener mListener;

    /**
     * Used to cache the album info.
     */
    private List<Album> mAlbums = Collections.emptyList();

    private final Activity mActivity;

    private final int mListMargin;

    /**
     * Constructor of <code>ArtistDetailAlbumAdapter</code>
     *
     * @param activity    The {@link FragmentActivity} to use.
     */
    public ArtistDetailAlbumAdapter(final FragmentActivity activity) {
        mActivity = activity;
        mImageFetcher = ElevenUtils.getImageFetcher(activity);
        mListMargin = activity.getResources().
                getDimensionPixelSize(R.dimen.list_item_general_margin);
    }

    @Override
    public int getItemViewType(int position) {
        // use view types to distinguish first and last elements
        // so they can be given special treatment for layout
        if (position == 0) {
            return TYPE_FIRST;
        } else if (position == getItemCount() - 1) {
            return TYPE_LAST;
        } else return TYPE_MIDDLE;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.artist_detail_album, parent, false);
        // add extra margin to the first and last elements
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        if (viewType == TYPE_FIRST) {
            params.leftMargin = mListMargin;
        } else if (viewType == TYPE_LAST) {
            params.rightMargin = mListMargin;
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album a = mAlbums.get(position);
        StringBuilder sb = new StringBuilder();
        sb.append(a.mAlbumName);
        if (!TextUtils.isEmpty(a.mYear)) {
            sb.append('\n').append(a.mYear);
        }
        holder.description.setText(sb.toString());
        mImageFetcher.loadAlbumImage(
                a.mArtistName, a.mAlbumName, a.mAlbumId, holder.art);
        holder.popupButton.setPopupMenuClickedListener(mListener);
        holder.popupButton.setPosition(position);
        addAction(holder.itemView, a);
    }

    private void addAction(View view, final Album album) {
        view.setOnClickListener(v -> NavUtils.openAlbumProfile(
                mActivity, album.mAlbumName, album.mArtistName, album.mAlbumId));
    }

    @Override
    public int getItemCount() {
        return mAlbums.size();
    }

    public Album getItem(int position) {
        return mAlbums.get(position);
    }

    @Override
    public void setPopupMenuClickedListener(IListener listener) {
        mListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView art;
        public final TextView description;
        public final PopupMenuButton popupButton;

        public ViewHolder(View root) {
            super(root);
            art = root.findViewById(R.id.image);
            description = root.findViewById(R.id.description);
            popupButton = root.findViewById(R.id.popup_menu_button);
        }
    }

    public void setData(List<Album> albums) {
        int oldSize = mAlbums == null ? 0 : mAlbums.size();
        int newSize = albums.size();

        mAlbums = albums;

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
        int size = mAlbums.size();
        mAlbums.clear();
        mImageFetcher.flush();
        notifyItemRangeRemoved(0, size);
    }
}
