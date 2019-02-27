/*
 * Copyright (C) 2012 Andrew Neal
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

package org.lineageos.eleven

import android.app.Application
import android.os.StrictMode
import android.util.Log

import org.lineageos.eleven.cache.ImageCache

class ElevenApplication : Application() {
    companion object {
        private const val TAG = "ElevenApplication"
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    override fun onLowMemory() {
        ImageCache.getInstance(this).evictAll()
        super.onLowMemory()
    }

    private fun enableStrictMode() {
        Log.d(TAG, "Enabling StrictMode")

        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyFlashScreen()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
