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

import androidx.lifecycle.LiveData

interface IElevenRepository {
    // region History

    fun addSearchHistory(searchHistory: SearchHistory)
    fun addSearchString(searchString: String)
    fun getSearches(): LiveData<List<SearchHistory>>

    // endregion History

    // region Playback State

    fun getHistory(): List<PlaybackHistory>
    fun getQueue(): List<PlaybackQueue>

    fun saveState(historyList: List<PlaybackHistory>, queueList: List<PlaybackQueue>)

    // endregion Playback State

    // region Property

    fun getProperty(key: String): Property?

    fun storeProperty(key: String, value: String)
    fun storeProperty(property: Property)

    // endregion Property
}
