/*
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

package org.lineageos.eleven;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothDisconnectReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "BluetoothDisconnectReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.v(TAG, "Received intent: " + intent);

        String action = intent.getAction();
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            BluetoothDevice disconnectedDevice =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (disconnectedDevice != null) {
                if (disconnectedDevice.getBluetoothClass().getMajorDeviceClass()
                            == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                    Log.v(TAG, "audio device disconnected");
                    // stop playback
                    final Intent i = new Intent(context, MusicPlaybackService.class);
                    i.setAction(MusicPlaybackService.SERVICECMD);
                    i.putExtra(MusicPlaybackService.CMDNAME, MusicPlaybackService.CMDPAUSE);
                    context.startForegroundService(i);
                } else if (DEBUG) {
                    Log.v(TAG, "Ignoring disconnect of bluetooth device with class " +
                            disconnectedDevice.getBluetoothClass());
                }
            }
        }
    }
}
