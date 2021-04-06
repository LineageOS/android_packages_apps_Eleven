/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.lineageos.eleven;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.legacy.content.WakefulBroadcastReceiver;

@SuppressWarnings("deprecation")
public class MediaButtonIntentReceiver extends WakefulBroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaButtonIntentReceiver";

    private static final SparseArray<String> KEY_CODE_MAPPING = new SparseArray<>(7);

    static {
        KEY_CODE_MAPPING.put(KeyEvent.KEYCODE_HEADSETHOOK, MusicPlaybackService.CMDHEADSETHOOK);
        KEY_CODE_MAPPING.put(KeyEvent.KEYCODE_MEDIA_STOP, MusicPlaybackService.CMDSTOP);
        KEY_CODE_MAPPING.put(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                MusicPlaybackService.CMDTOGGLEPAUSE);
        KEY_CODE_MAPPING.put(KeyEvent.KEYCODE_MEDIA_NEXT, MusicPlaybackService.CMDNEXT);
        KEY_CODE_MAPPING.put(KeyEvent.KEYCODE_MEDIA_PREVIOUS, MusicPlaybackService.CMDPREVIOUS);
        KEY_CODE_MAPPING.put(KeyEvent.KEYCODE_MEDIA_PAUSE, MusicPlaybackService.CMDPAUSE);
        KEY_CODE_MAPPING.put(KeyEvent.KEYCODE_MEDIA_PLAY, MusicPlaybackService.CMDPLAY);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (DEBUG) Log.v(TAG, "Received intent: " + intent);
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }
        final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
            return;
        }

        String command = KEY_CODE_MAPPING.get(event.getKeyCode());
        if (command == null) {
            return;
        }

        final Intent i = new Intent(context, MusicPlaybackService.class);
        i.setAction(MusicPlaybackService.SERVICECMD);
        i.putExtra(MusicPlaybackService.CMDNAME, command);
        i.putExtra(MusicPlaybackService.FROM_MEDIA_BUTTON, true);
        i.putExtra(MusicPlaybackService.TIMESTAMP, event.getEventTime());
        context.startForegroundService(i);

        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
    }
}
