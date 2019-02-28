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
import androidx.lifecycle.LiveData
import org.lineageos.eleven.BuildConfig
import org.lineageos.eleven.utils.kotlin.SingletonHolder

class ElevenRepository private constructor(private val context: Context) : IElevenRepository {
    companion object : SingletonHolder<ElevenRepository, Context>({
        ElevenRepository(it.applicationContext)
    }) {
        private const val TAG = "ElevenRepository"
    }

    // region History

    override fun addSearchHistory(searchHistory: SearchHistory) {
        if (searchHistory.searchString.isNotBlank()) {
            ElevenDatabase.getInstance(context).searchHistoryDao().insert(searchHistory)
        } else {
            Log.w(TAG, "Trying to add empty search history entry")
        }
    }

    override fun addSearchString(searchString: String) {
        val searchHistory = SearchHistory(searchString, System.currentTimeMillis())
        addSearchHistory(searchHistory)
    }

    override fun getSearches(): LiveData<List<SearchHistory>> {
        return ElevenDatabase.getInstance(context).searchHistoryDao().getHistory()
    }

    // endregion History

    // region Playback State

    override fun getHistory(): List<PlaybackHistory> {
        return ElevenDatabase.getInstance(context).playbackHistoryDao().getHistory()
    }

    override fun getQueue(): List<PlaybackQueue> {
        return ElevenDatabase.getInstance(context).playbackQueueDao().getQueue()
    }

    override fun saveState(historyList: List<PlaybackHistory>, queueList: List<PlaybackQueue>) {
        // clear history and queue tables to not append to an old state
        ElevenDatabase.getInstance(context).playbackHistoryDao().clearHistory()
        ElevenDatabase.getInstance(context).playbackQueueDao().clearQueue()

        // only save last MAX_HISTORY_SIZE count of history entries
        if (historyList.size > PlaybackHistory.MAX_HISTORY_SIZE) {
            historyList.takeLast(PlaybackHistory.MAX_HISTORY_SIZE)
        } else {
            historyList
        }.let {
            if (it.isNotEmpty()) {
                ElevenDatabase.getInstance(context).playbackHistoryDao().insertAll(historyList)
            }
        }

        // only save last MAX_QUEUE_SIZE count of queue entries
        if (queueList.size > PlaybackQueue.MAX_QUEUE_SIZE) {
            queueList.takeLast(PlaybackQueue.MAX_QUEUE_SIZE)
        } else {
            queueList
        }.let {
            if (it.isNotEmpty()) {
                ElevenDatabase.getInstance(context).playbackQueueDao().insertAll(queueList)
            }
        }
    }

    // endregion Playback State

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
