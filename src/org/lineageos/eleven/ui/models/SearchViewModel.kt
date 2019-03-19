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
package org.lineageos.eleven.ui.models

import android.app.Application
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Job
import org.lineageos.eleven.room.ElevenRepository
import org.lineageos.eleven.room.SearchHistory
import org.lineageos.eleven.utils.kotlin.Coroutines

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    @UiThread
    fun addSearchString(searchString: String): Job {
        return Coroutines.io {
            val searchHistory = SearchHistory(searchString, System.currentTimeMillis())
            ElevenRepository.getInstance(getApplication()).addSearchHistory(searchHistory)
        }
    }

    fun getSearches(): LiveData<List<SearchHistory>> {
        return ElevenRepository.getInstance(getApplication()).getSearches()
    }
}
