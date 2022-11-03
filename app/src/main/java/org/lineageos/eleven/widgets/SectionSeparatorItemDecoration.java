/*
 * Copyright 2019 Google LLC
 * Copyright (C) 2021 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.eleven.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.R;
import org.lineageos.eleven.utils.SectionCreatorUtils;

import java.util.TreeMap;

public class SectionSeparatorItemDecoration extends RecyclerView.ItemDecoration {

    private final SparseArray<StaticLayout> mLabels;
    private final TextPaint mPaint;
    private final int mTextWidth;
    private final int mDecorHeight;
    private final int mHorizontalPadding;
    private final int mVerticalPadding;
    private final float mVerticalBias;

    public SectionSeparatorItemDecoration(Context context,
                                          TreeMap<Integer, SectionCreatorUtils.Section> items) {
        mPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);

        TypedArray attrs = context.obtainStyledAttributes(
                R.style.SectionSeparator,
                R.styleable.SectionSeparator);
        float textSize = attrs.getDimension(R.styleable.SectionSeparator_android_textSize,
                mPaint.getTextSize());
        mPaint.setTextSize(textSize);

        int textColor = attrs.getColor(R.styleable.SectionSeparator_android_textColor,
                Color.BLACK);
        mPaint.setColor(textColor);

        mTextWidth = attrs.getDimensionPixelSize(R.styleable.SectionSeparator_android_width, 0);
        int height = attrs.getDimensionPixelSize(R.styleable.SectionSeparator_android_height, 0);
        int minHeight = (int) Math.ceil(textSize);
        mDecorHeight = Math.max(minHeight, height);

        mHorizontalPadding = attrs.getDimensionPixelSize(
                R.styleable.SectionSeparator_android_paddingHorizontal, 0);
        mVerticalPadding = attrs.getDimensionPixelSize(
                R.styleable.SectionSeparator_android_paddingVertical, 0);

        float bias = attrs.getFloat(R.styleable.SectionSeparator_verticalBias, 0.5f);
        if (bias > 1f) {
            bias = 1f;
        } else if (bias < 0f) {
            bias = 0f;
        }
        mVerticalBias = bias;

        attrs.recycle();
        mLabels = buildLabels(items);
    }

    private SparseArray<StaticLayout> buildLabels(TreeMap<Integer,
            SectionCreatorUtils.Section> items) {
        SparseArray<StaticLayout> sparseArray = new SparseArray<>();
        items.forEach((index, section) -> {
            String text = section.mIdentifier;
            StaticLayout label = newStaticLayout(text);
            sparseArray.put(index, label);
        });
        return sparseArray;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View child,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(child);
        outRect.top = hasLabel(position) ? mDecorHeight : 0;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child != null && child.getTop() < parent.getHeight() && child.getBottom() > 0) {
                // Child is visible
                StaticLayout layout = mLabels.get(parent.getChildAdapterPosition(child), null);
                if (layout != null) {
                    float dy = mVerticalPadding +
                            layoutManager.getDecoratedTop(child) +
                            child.getTranslationY() +
                            // offset vertically within the space according to the bias
                            (mDecorHeight - layout.getHeight()) * mVerticalBias;
                    c.translate(mHorizontalPadding, dy);
                    layout.draw(c);
                    c.translate(-mHorizontalPadding, -dy);
                }
            }
        }
    }

    private StaticLayout newStaticLayout(CharSequence source) {
        return StaticLayout.Builder.obtain(source, 0, source.length(), mPaint, mTextWidth)
                .setLineSpacing(1f, 0f)
                .setIncludePad(false)
                .build();
    }

    private boolean hasLabel(int position) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return mLabels.contains(position);
        } else {
            return mLabels.indexOfKey(position) > -1;
        }
    }
}
