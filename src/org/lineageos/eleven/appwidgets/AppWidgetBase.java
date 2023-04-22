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
package org.lineageos.eleven.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import org.lineageos.eleven.R;
import org.lineageos.eleven.cache.ImageCache;
import org.lineageos.eleven.cache.ImageFetcher;

public abstract class AppWidgetBase extends AppWidgetProvider {

    protected PendingIntent buildPendingIntent(Context context, final String action,
                                               final ComponentName serviceName) {
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    public void showDefaults(Context context, RemoteViews appWidgetView) {
        ImageFetcher imageFetcher = ImageFetcher.getInstance(context);
        imageFetcher.setImageCache(ImageCache.getInstance(context));
        Resources resources = context.getResources();
        final CharSequence trackName = resources.getString(R.string.widget_track_name);
        final CharSequence artistName = resources.getString(R.string.widget_artist_name);
        final CharSequence albumName = resources.getString(R.string.widget_album_name);
        final Bitmap bitmap = imageFetcher.getArtwork("", 0, true).getBitmap();

        // Set the titles and artwork
        appWidgetView.setTextViewText(R.id.app_widget_line_one, trackName);
        appWidgetView.setTextViewText(R.id.app_widget_line_two, artistName);
        appWidgetView.setTextViewText(R.id.app_widget_line_three, albumName);
        appWidgetView.setImageViewBitmap(R.id.app_widget_image, bitmap);
    }
}
