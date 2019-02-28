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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "searchHistory")
class SearchHistory {
    companion object {
        const val MAX_HISTORY_SIZE = 30

        fun getRecentSearches(searchHistoryList: List<SearchHistory>): List<SearchHistory> {
            return searchHistoryList.takeLast(SearchHistory.MAX_HISTORY_SIZE).reversed()
        }

        fun getRecentSearchesAsString(searchHistoryList: List<SearchHistory>): List<String> {
            return getRecentSearches(searchHistoryList).map {
                it.searchString
            }
        }
    }

    @PrimaryKey
    var searchString: String = ""

    @ColumnInfo(name = "searchTime")
    var searchTime: Long = 0

    @Ignore
    constructor()

    constructor(searchString: String, searchTime: Long) {
        this.searchString = searchString
        this.searchTime = searchTime
    }
}
