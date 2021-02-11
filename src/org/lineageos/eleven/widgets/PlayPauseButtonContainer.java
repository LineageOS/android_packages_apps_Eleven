/*
 * Copyright (C) 2019-2021 The LineageOS Project
 * Copyright (C) 2019 SHIFT GmbH
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

import org.lineageos.eleven.R;

public class PlayPauseButtonContainer extends FrameLayout {
    private PlayPauseButton mPlayPauseButton;

    public PlayPauseButtonContainer(Context context, AttributeSet attrs) {
        super(context, attrs);

        // set enabled to false as default so that calling enableAndShow will execute
        setEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mPlayPauseButton = findViewById(R.id.action_button_play);
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
    }

    /**
     * Enables and shows the container
     */
    public void enableAndShow() {
        setEnabled(true);
        setVisibility(VISIBLE);
    }

    /**
     * Disables and sets the visibility to gone for the container
     */
    public void disableAndHide() {
        setEnabled(false);
        setVisibility(GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled()) return;
        super.setEnabled(enabled);

        if (isEnabled()) {
            updateState();
        }
    }

    public void updateState() {
        mPlayPauseButton.updateState();
    }
}
