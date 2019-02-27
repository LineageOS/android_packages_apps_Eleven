/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2019 The LineageOS Project
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

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.NavUtils;

import androidx.fragment.app.Fragment;

/**
 * Simple Header bar wrapper class that also has its own menu bar button.
 * It can collect a list of popup menu creators and create a pop up menu
 * from the list
 */
public class HeaderBar extends LinearLayout {

    private ImageView mMenuButton;
    private ImageView mSearchButton;
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

        mSearchButton = findViewById(R.id.header_bar_search_button);
        mSearchButton.setOnClickListener(v -> NavUtils.openSearch(mFragment.getActivity(), ""));

        mBackButton = findViewById(R.id.header_bar_up);

        mTitleText = findViewById(R.id.header_bar_title);
    }

    public void hideBackButton() {
        mBackButton.setVisibility(View.GONE);
    }

    public void showBackButton() {
        mBackButton.setVisibility(View.VISIBLE);
    }

    public void hideSearchButton() {
        mSearchButton.setVisibility(View.GONE);
    }

    public void showSearchButton() {
        mSearchButton.setVisibility(View.VISIBLE);
    }

    /**
     * @param resId set the title text
     */
    public void setTitleText(int resId) {
        mTitleText.setText(resId);
    }

    /**
     * @param text set the title text
     */
    public void setTitleText(String text) {
        mTitleText.setText(text);
    }

    /**
     * Sets the back button listener
     * @param listener listener
     */
    public void setBackListener(final OnClickListener listener) {
        mBackButton.setOnClickListener(listener);
        setOnClickListener(listener);
    }

    /**
     * Sets the header bar listener
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

        // Settings
        inflater.inflate(R.menu.activity_base, menu);

        // show the popup
        mPopupMenu.show();
    }

    public boolean onPopupMenuItemClick(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                // Settings
                NavUtils.openSettings(mFragment.getActivity());
                return true;
            default:
                break;
        }

        return false;
    }
}
