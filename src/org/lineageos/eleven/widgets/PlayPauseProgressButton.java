/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2021 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.eleven.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.MusicUtils;

/**
 * This class handles the play-pause button as well as the circular progress bar
 * it self-updates the progress bar but the containing activity/fragment
 * needs to add code to pause/resume this button to prevent unnecessary
 * updates while the activity/fragment is not visible
 */
public class PlayPauseProgressButton extends FrameLayout {
    private ProgressBar mProgressBar;
    private PlayPauseButton mPlayPauseButton;
    private Runnable mUpdateProgress;
    private boolean mPaused;

    public PlayPauseProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        // set enabled to false as default so that calling enableAndShow will execute
        setEnabled(false);

        // set paused to false since we shouldn't be typically created while not visible
        mPaused = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mPlayPauseButton = (PlayPauseButton) findViewById(R.id.action_button_play);
        mProgressBar = (ProgressBar) findViewById(R.id.circularProgressBarAlt);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Make the play pause button size dependent on the container size
        int horizontalPadding = getMeasuredWidth() / 4;
        int verticalPadding = getMeasuredHeight() / 4;
        mPlayPauseButton.setPadding(
                horizontalPadding, horizontalPadding,
                verticalPadding, verticalPadding);

        // rotate the progress bar 90 degrees counter clockwise so that the
        // starting position is at the top
        mProgressBar.setPivotX(mProgressBar.getMeasuredWidth() / 2f);
        mProgressBar.setPivotY(mProgressBar.getMeasuredHeight() / 2f);
        mProgressBar.setRotation(-90);
    }

    /**
     * Enable and shows the container
     */
    public void enableAndShow() {
        // enable
        setEnabled(true);

        // make our view visible
        setVisibility(VISIBLE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        // if the enabled state isn't changed, quit
        if (enabled == isEnabled()) {
            return;
        }

        super.setEnabled(enabled);
        // signal our state has changed
        onStateChanged();
    }

    /**
     * Pauses the progress bar periodic update logic
     */
    public void pause() {
        if (mPaused) {
            return;
        }

        mPaused = true;
        // signal our state has changed
        onStateChanged();
    }

    /**
     * Signaled if the state has changed (either the enabled or paused flag)
     * When the state changes, we either kick off the updates or remove them
     * based on those flags
     */
    private void onStateChanged() {
        // if we are enabled and not paused
        if (isEnabled() && !mPaused) {
            // update the state of the progress bar and play/pause button
            updateState();

            // kick off update states
            postUpdate();
        } else {
            // otherwise remove our update
            removeUpdate();
        }
    }

    /**
     * Updates the state of the progress bar and the play pause button
     */
    public void updateState() {
        long currentSongDuration = MusicUtils.duration();
        long currentSongProgress = MusicUtils.position();

        int progress = 0;
        if (currentSongDuration > 0) {
            progress = (int) (mProgressBar.getMax() * currentSongProgress / currentSongDuration);
        }

        mProgressBar.setProgress(progress);
        mPlayPauseButton.updateState();
    }

    /**
     * Creates and posts the update runnable to the handler
     */
    private void postUpdate() {
        if (mUpdateProgress == null) {
            mUpdateProgress = () -> {
                updateState();
                postDelayed(mUpdateProgress, MusicUtils.UPDATE_FREQUENCY_MS);
            };
        }

        // remove any existing callbacks
        removeCallbacks(mUpdateProgress);

        // post ourselves as a delayed
        post(mUpdateProgress);
    }

    /**
     * Removes the runnable from the handler
     */
    private void removeUpdate() {
        if (mUpdateProgress != null) {
            removeCallbacks(mUpdateProgress);
        }
    }
}
