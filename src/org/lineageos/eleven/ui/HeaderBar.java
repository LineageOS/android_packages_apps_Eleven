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
package org.lineageos.eleven.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.lineageos.eleven.R;
import org.lineageos.eleven.loaders.NowPlayingCursor;
import org.lineageos.eleven.loaders.QueueLoader;
import org.lineageos.eleven.menu.CreateNewPlaylist;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.NavUtils;

/**
 * Simple Header bar wrapper class that also has its own menu bar button.
 * It can collect a list of popup menu creators and create a pop up menu
 * from the list
 */
public class HeaderBar extends LinearLayout {

    private ImageView mMenuButton;
    private ImageView mBackButton;
    private TextView mTitleText;
    private PopupMenu mPopupMenu;
    private Fragment mFragment;

    public HeaderBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFragment(Fragment activity) {
        mFragment = activity;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMenuButton = findViewById(R.id.header_bar_menu_button);
        mMenuButton.setOnClickListener(v -> showPopupMenu());

        mBackButton = findViewById(R.id.header_bar_up);

        mTitleText = findViewById(R.id.header_bar_title);
    }

    public void hideBackButton() {
        mBackButton.setVisibility(View.GONE);
    }

    /**
     * @param resId set the title text
     */
    public void setTitleText(int resId) {
        mTitleText.setText(resId);
    }

    /**
     * Sets the header bar listener
     *
     * @param listener listener
     */
    public void setHeaderClickListener(final OnClickListener listener) {
        setOnClickListener(listener);
    }

    public void showPopupMenu() {
        // create the popup menu
        if (mPopupMenu == null) {
            mPopupMenu = new PopupMenu(mFragment.getActivity(), mMenuButton);
            mPopupMenu.setOnMenuItemClickListener(this::onPopupMenuItemClick);
        }

        final Menu menu = mPopupMenu.getMenu();
        final MenuInflater inflater = mPopupMenu.getMenuInflater();

        menu.clear();

        // Shuffle all
        inflater.inflate(R.menu.shuffle_all, menu);
        if (MusicUtils.getQueueSize() > 0) {
            // save queue/clear queue
            inflater.inflate(R.menu.queue, menu);
        }
        // Settings
        inflater.inflate(R.menu.activity_base, menu);

        // show the popup
        mPopupMenu.show();
    }

    public boolean onPopupMenuItemClick(final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_shuffle_all) {
            // Shuffle all the songs
            MusicUtils.shuffleAll(mFragment.getActivity());
        } else if (id == R.id.menu_settings) {
            // Settings
            NavUtils.openSettings(mFragment.getActivity());
        } else if (id == R.id.menu_save_queue) {
            NowPlayingCursor queue = (NowPlayingCursor) QueueLoader
                    .makeQueueCursor(mFragment.getActivity());
            CreateNewPlaylist.getInstance(MusicUtils.getSongListForCursor(queue)).show(
                    mFragment.getChildFragmentManager(), "CreatePlaylist");
            queue.close();
        } else if (id == R.id.menu_clear_queue) {
            MusicUtils.clearQueue();
        } else {
            return false;
        }
        return true;
    }
}
