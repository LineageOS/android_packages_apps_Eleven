/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2019-2021 The LineageOS Project
 * Copyright (C) 2021 SHIFT GmbH
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
package org.lineageos.eleven.ui.activities;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import org.lineageos.eleven.R;
import org.lineageos.eleven.slidinguppanel.SlidingUpPanelLayout;
import org.lineageos.eleven.slidinguppanel.SlidingUpPanelLayout.SimplePanelSlideListener;
import org.lineageos.eleven.ui.HeaderBar;
import org.lineageos.eleven.ui.fragments.AudioPlayerFragment;
import org.lineageos.eleven.ui.fragments.QueueFragment;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PreferenceUtils;
import org.lineageos.eleven.utils.colors.ColorExtractor;
import org.lineageos.eleven.widgets.AlbumScrimImage;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class SlidingPanelActivity extends BaseActivity {

    public enum Panel {
        Browse,
        MusicPlayer,
        Queue,
        None,
    }

    private static final String STATE_KEY_CURRENT_PANEL = "CurrentPanel";

    private SlidingUpPanelLayout mFirstPanel;
    private SlidingUpPanelLayout mSecondPanel;
    protected Panel mTargetNavigatePanel;

    private final ShowPanelClickListener mShowMusicPlayer =
            new ShowPanelClickListener(Panel.MusicPlayer);

    // this is the blurred image that goes behind the now playing and queue fragments
    private AlbumScrimImage mAlbumScrimImage;

    private boolean mUseBlur;

    /**
     * Opens the now playing screen
     */
    private final View.OnClickListener mOpenNowPlaying = v -> {
        if (MusicUtils.getCurrentAudioId() != -1) {
            openAudioPlayer();
        } else {
            MusicUtils.shuffleAll(SlidingPanelActivity.this);
        }
    };

    @Override
    protected void initBottomActionBar() {
        super.initBottomActionBar();
        // Bottom action bar
        final LinearLayout bottomActionBar = findViewById(R.id.bottom_action_bar);
        // Display the now playing screen or shuffle if this isn't anything
        // playing
        bottomActionBar.setOnClickListener(mOpenNowPlaying);
    }

    @Override
    protected void init(final Bundle savedInstanceState) {
        super.init(savedInstanceState);

        mUseBlur = PreferenceUtils.getInstance(this).getUseBlur();

        mTargetNavigatePanel = Panel.None;

        setupFirstPanel();
        setupSecondPanel();

        // get the album scrim image
        mAlbumScrimImage = findViewById(R.id.albumScrimImage);

        if (savedInstanceState != null) {
            int panelIndex = savedInstanceState.getInt(STATE_KEY_CURRENT_PANEL,
                    Panel.Browse.ordinal());
            Panel targetPanel = Panel.values()[panelIndex];

            showPanel(targetPanel);
            mTargetNavigatePanel = Panel.None;

            if (targetPanel == Panel.Queue) {
                mFirstPanel.setSlidingEnabled(false);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_KEY_CURRENT_PANEL, getCurrentPanel().ordinal());
    }

    private void setupFirstPanel() {
        mFirstPanel = findViewById(R.id.sliding_layout);
        mFirstPanel.setPanelSlideListener(new SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                onSlide(slideOffset);
            }

            @Override
            public void onPanelExpanded(View panel) {
                checkTargetNavigation();
            }

            @Override
            public void onPanelCollapsed(View panel) {
                checkTargetNavigation();
            }
        });
    }

    private void setupSecondPanel() {
        mSecondPanel = findViewById(R.id.sliding_layout2);
        mSecondPanel.setPanelSlideListener(new SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                // if we are not going to a specific panel, then disable sliding to prevent
                // the two sliding panels from fighting for touch input
                if (mTargetNavigatePanel == Panel.None) {
                    mFirstPanel.setSlidingEnabled(false);
                }

                onSlide(slideOffset);
            }

            @Override
            public void onPanelExpanded(View panel) {
                checkTargetNavigation();
            }

            @Override
            public void onPanelCollapsed(View panel) {
                // re-enable sliding when the second panel is collapsed
                mFirstPanel.setSlidingEnabled(true);
                checkTargetNavigation();
            }
        });

        // setup the header bar
        setupQueueHeaderBar(mShowMusicPlayer);

        // set the drag view offset to allow the panel to go past the top of the viewport
        // since the previous view's is hiding the slide offset, we need to subtract that
        // from action bat height
        int slideOffset = getResources().getDimensionPixelOffset(
                R.dimen.sliding_panel_indicator_height);
        slideOffset -= ElevenUtils.getActionBarHeight(this);
        mSecondPanel.setSlidePanelOffset(slideOffset);
    }

    @Override protected void onResume() {
        super.onResume();

        // recreate activity if blur preference has changed to apply changes
        final boolean useBlur = PreferenceUtils.getInstance(this).getUseBlur();
        if (mUseBlur != useBlur) {
            recreate();
        }
    }

    @Override
    public void onBackPressed() {
        Panel panel = getCurrentPanel();
        switch (panel) {
            case Browse:
                super.onBackPressed();
                break;
            default:
            case MusicPlayer:
                showPanel(Panel.Browse);
                break;
            case Queue:
                showPanel(Panel.MusicPlayer);
                break;
        }
    }

    public void openAudioPlayer() {
        showPanel(Panel.MusicPlayer);
    }

    public void showPanel(Panel panel) {
        // if we are already at our target panel, then don't do anything
        if (panel == getCurrentPanel()) {
            return;
        }

        // TODO: Add ability to do this instantaneously as opposed to animate
        switch (panel) {
            case Browse:
                // if we are two panels over, we need special logic to jump twice
                mTargetNavigatePanel = panel;
                mSecondPanel.collapsePanel();
                // re-enable sliding on first panel so we can collapse it
                mFirstPanel.setSlidingEnabled(true);
                mFirstPanel.collapsePanel();
                break;
            case MusicPlayer:
                mSecondPanel.collapsePanel();
                mFirstPanel.expandPanel();
                break;
            case Queue:
                // if we are two panels over, we need special logic to jump twice
                mTargetNavigatePanel = panel;
                mSecondPanel.expandPanel();
                mFirstPanel.expandPanel();
                break;
        }
    }

    protected void onSlide(float slideOffset) {
    }

    /**
     * This checks if we are at our target panel and resets our flag if we are there
     */
    protected void checkTargetNavigation() {
        if (mTargetNavigatePanel == getCurrentPanel()) {
            mTargetNavigatePanel = Panel.None;
        }

        getAudioPlayerFragment().setVisualizerVisible(getCurrentPanel() == Panel.MusicPlayer);
    }

    public Panel getCurrentPanel() {
        if (!isInitialized()) {
            return Panel.None;
        }

        if (mSecondPanel.isPanelExpanded()) {
            return Panel.Queue;
        } else if (mFirstPanel.isPanelExpanded()) {
            return Panel.MusicPlayer;
        } else {
            return Panel.Browse;
        }
    }

    public void clearMetaInfo() {
        super.clearMetaInfo();
        mAlbumScrimImage.transitionToDefaultState();
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();

        updateScrimImage();
    }

    @Override
    public void onCacheResumed() {
        super.onCacheResumed();

        updateScrimImage();
    }

    private void updateScrimImage() {
        ElevenUtils.getImageFetcher(this).updateScrimImage(mAlbumScrimImage,
                mColorExtractorCallback);
    }

    protected AudioPlayerFragment getAudioPlayerFragment() {
        return (AudioPlayerFragment) getSupportFragmentManager().findFragmentById(
                R.id.audioPlayerFragment);
    }

    protected QueueFragment getQueueFragment() {
        return (QueueFragment) getSupportFragmentManager().findFragmentById(R.id.queueFragment);
    }

    private final ColorExtractor.Callback mColorExtractorCallback = (bitmapWithColors) -> {
        if (bitmapWithColors == null) {
            return;
        }

        // update scrim image
        final int[] gradientColors = new int[]{
                bitmapWithColors.getVibrantColor(), bitmapWithColors.getVibrantDarkColor()
        };

        final GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColors(gradientColors);
        mAlbumScrimImage.setGradientDrawable(gradientDrawable);
    };

    protected void setupQueueHeaderBar(final View.OnClickListener headerClickListener) {
        final HeaderBar headerBar = findViewById(R.id.secondHeaderBar);
        headerBar.setFragment(getQueueFragment());
        headerBar.setTitleText(R.string.page_play_queue);
        headerBar.setBackgroundColor(Color.TRANSPARENT);
        headerBar.setHeaderClickListener(headerClickListener);

        headerBar.hideBackButton();
    }

    private class ShowPanelClickListener implements View.OnClickListener {

        private final Panel mTargetPanel;

        public ShowPanelClickListener(Panel targetPanel) {
            mTargetPanel = targetPanel;
        }

        @Override
        public void onClick(View v) {
            showPanel(mTargetPanel);
        }
    }
}
