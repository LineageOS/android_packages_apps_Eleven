/*
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
package org.lineageos.eleven.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.ElevenUtils;

public abstract class AudioButton extends AppCompatImageButton implements OnClickListener,
        OnLongClickListener {
    public static float ACTIVE_ALPHA = 1.0f;
    public static float INACTIVE_ALPHA = 0.4f;

    public AudioButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setPadding(0, 0, 0, 0);
        setBackground(ContextCompat.getDrawable(context, R.drawable.selectable_background));
        // Control playback (cycle shuffle)
        setOnClickListener(this);
        // Show the cheat sheet
        setOnLongClickListener(this);
    }

    @Override
    public boolean onLongClick(final View view) {
        if (TextUtils.isEmpty(view.getContentDescription())) {
            return false;
        } else {
            ElevenUtils.showCheatSheet(view);
            return true;
        }
    }
}
