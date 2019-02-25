/*
 * Copyright (C) 2019 The LineageOS Project
 * Copyright (C) 2019 SHIFT GmbH
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
package org.lineageos.eleven.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.MusicUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MainPlaybackControls extends FrameLayout {
    public static final String TIME_WHICH_LOOKS_LIKE_UNAMUSED_FACE = "--:--";

    private final TextView mCurrentTime;
    private final TextView mTotalTime;
    private final SeekBar mSeeker;

    private final ShuffleButton mShuffleButton;
    private final RepeatingImageButton mPreviousButton;
    private final PlayPauseButtonContainer mPlayPauseButtonContainer;
    private final RepeatingImageButton mNextButton;
    private final RepeatButton mRepeatButton;

    public MainPlaybackControls(@NonNull Context context) {
        this(context, null);
    }

    public MainPlaybackControls(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainPlaybackControls(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MainPlaybackControls(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.main_playback_controls, this, true);

        mCurrentTime = findViewById(R.id.audio_player_current_time);
        mTotalTime = findViewById(R.id.audio_player_total_time);
        mSeeker = findViewById(R.id.audio_player_seeker);

        mPlayPauseButtonContainer = findViewById(R.id.playPauseProgressButton);
        mShuffleButton = findViewById(R.id.action_button_shuffle);
        mRepeatButton = findViewById(R.id.action_button_repeat);
        mPreviousButton = findViewById(R.id.action_button_previous);
        mNextButton = findViewById(R.id.action_button_next);

        mSeeker.setOnSeekBarChangeListener(mSeekerListener);

        mPreviousButton.setRepeatListener(
                (v, delta, repeatCount) -> seekRelative(repeatCount, delta, false));
        mNextButton.setRepeatListener(
                (v, delta, repeatCount) -> seekRelative(repeatCount, delta, true));

        mPlayPauseButtonContainer.enableAndShow();
    }

    public void updateNowPlayingInfo() {
        final int durationInSeconds = MusicUtils.durationInSeconds();
        mSeeker.setMax(durationInSeconds);

        final String totalTime = MusicUtils.makeShortTimeString(getContext(), durationInSeconds);
        if (!totalTime.contentEquals(mTotalTime.getText())) {
            mTotalTime.setText(totalTime);
        }
    }

    public void updatePlaybackControls() {
        updatePlayPauseState();
        updateShuffleState();
        updateRepeatState();
    }

    public void updatePlayPauseState() {
        mPlayPauseButtonContainer.updateState();
    }

    public void updateShuffleState() {
        mShuffleButton.updateShuffleState();
    }

    public void updateRepeatState() {
        mRepeatButton.updateRepeatState();
    }

    // region refresh time
    public void refreshCurrentTime() {
        final long position = MusicUtils.position();
        final long duration = MusicUtils.duration();
        final boolean isPlaying = MusicUtils.isPlaying();
        refreshCurrentTime(position, duration, isPlaying);
    }

    public void refreshCurrentTime(long position, long duration, boolean isPlaying) {
        if (position >= 0 && duration > 0) {
            final int posInSeconds = ((int) (position / 1000));
            refreshCurrentTimeText(posInSeconds);
            mSeeker.setProgress(posInSeconds);

            if (isPlaying) {
                mCurrentTime.setVisibility(View.VISIBLE);
            }
        } else {
            mCurrentTime.setText(TIME_WHICH_LOOKS_LIKE_UNAMUSED_FACE);
        }
    }

    private void refreshCurrentTimeText(final long posInSeconds) {
        final String currentDuration = MusicUtils.makeShortTimeString(getContext(), posInSeconds);
        mCurrentTime.setText(currentDuration);
    }
    // endregion refresh time

    // region seeking
    private final SeekBar.OnSeekBarChangeListener mSeekerListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                refreshCurrentTimeText(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // ignore
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            final long wantedDurationInMs = seekBar.getProgress() * 1000L;
            MusicUtils.seek(wantedDurationInMs);
        }
    };

    private void seekRelative(final int repeatCount, long delta, boolean forwards) {
        if (!MusicUtils.isPlaybackServiceConnected()) {
            return;
        }

        if (repeatCount > 0) {
            final long EXTRA_FAST_CUTOFF = 10000;
            if (delta < EXTRA_FAST_CUTOFF) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = EXTRA_FAST_CUTOFF * 10 + (delta - EXTRA_FAST_CUTOFF) * 40;
            }

            MusicUtils.seekRelative(forwards ? delta : -delta);
            refreshCurrentTime();
        }
    }
    // endregion seeking
}
