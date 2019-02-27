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
package org.lineageos.eleven.room

import android.content.Context
import android.util.Log
import org.lineageos.eleven.BuildConfig
import org.lineageos.eleven.utils.kotlin.SingletonHolder

class ElevenRepository private constructor(private val context: Context) : IElevenRepository {
    companion object : SingletonHolder<ElevenRepository, Context>({
        ElevenRepository(it.applicationContext)
    }) {
        private const val TAG = "ElevenRepository"
    }

    // region Property

    override fun storeProperty(key: String, value: String) {
        storeProperty(Property(key, value))
    }

    override fun storeProperty(property: Property) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "storeProperty($property)")
        }
        ElevenDatabase.getInstance(context).propertyDao().storeProperty(property)
    }

    override fun getProperty(key: String): Property? {
        return ElevenDatabase.getInstance(context).propertyDao().getProperty(key)
    }

    // endregion Property
}
