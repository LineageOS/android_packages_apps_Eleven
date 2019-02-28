/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
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

package org.lineageos.eleven.ui.activities.adapters

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.text.TextUtils
import org.lineageos.eleven.Config
import org.lineageos.eleven.model.SearchResult
import org.lineageos.eleven.sectionadapter.SectionCreator
import org.lineageos.eleven.utils.ElevenUtils
import org.lineageos.eleven.utils.MusicUtils
import java.util.*

class SummarySearchLoader(
        context: Context,
        private val mQuery: String,
        private val mSearchType: SearchResult.ResultType?
) : SectionCreator.SimpleListLoader<SearchResult>(context) {
    companion object {
        fun makePlaylistSearchCursor(context: Context,
                                     searchTerms: String): Cursor? {
            if (TextUtils.isEmpty(searchTerms)) {
                return null
            }

            // trim out special characters like % or \ as well as things like "a" "and" etc
            val trimmedSearchTerms = MusicUtils.getTrimmedName(searchTerms)

            if (TextUtils.isEmpty(trimmedSearchTerms)) {
                return null
            }

            val keywords = trimmedSearchTerms!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            // prep the keyword for like search
            for (i in keywords.indices) {
                keywords[i] = "%" + keywords[i] + "%"
            }

            val where = StringBuilder()
            for (i in keywords.indices) {
                if (i == 0) {
                    where.append("name LIKE ?")
                } else {
                    where.append(" AND name LIKE ?")
                }
            }

            return context.contentResolver.query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    arrayOf(
                            /* 0 */
                            BaseColumns._ID,
                            /* 1 */
                            MediaStore.Audio.PlaylistsColumns.NAME), where.toString(), keywords, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER)
        }
    }

    override fun loadInBackground(): List<SearchResult>? {
        // if we are doing a specific type search, run that one
        return if (mSearchType != null && mSearchType != SearchResult.ResultType.Unknown) {
            runSearchForType()
        } else {
            runGenericSearch()
        }
    }

    /**
     * This creates a search result given the data at the cursor position
     * @param cursor at the position for the item
     * @param type the type of item to create
     * @return the search result
     */
    private fun createSearchResult(cursor: Cursor, type: SearchResult.ResultType?): SearchResult? {
        when (type) {
            SearchResult.ResultType.Playlist -> {
                val item = SearchResult.createPlaylistResult(cursor)
                if (item != null) {
                    item.mSongCount = MusicUtils.getSongCountForPlaylist(context, item.mId)
                }
                return item
            }
            SearchResult.ResultType.Song -> {
                val item = SearchResult.createSearchResult(cursor)
                if (item != null) {
                    val details = MusicUtils.getAlbumArtDetails(context, item.mId)
                    if (details != null) {
                        item.mArtist = details.mArtistName
                        item.mAlbum = details.mAlbumName
                        item.mAlbumId = details.mAlbumId
                    }
                }
                return item
            }
            else -> return SearchResult.createSearchResult(cursor)
        }
    }

    /**
     * This creates a search for a specific type given a filter string.  This will return the
     * full list of results that matches those two requirements
     * @return the results for that search
     */
    private fun runSearchForType(): List<SearchResult> {
        val results = ArrayList<SearchResult>()
        if (mSearchType == SearchResult.ResultType.Playlist) {
            makePlaylistSearchCursor(context, mQuery)
        } else {
            ElevenUtils.createSearchQueryCursor(context, mQuery)
        }?.use { cursor ->
            if (cursor.moveToFirst()) {
                val mimeTypeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                do {
                    var addResult = true
                    if (mSearchType != SearchResult.ResultType.Playlist) {
                        val type = SearchResult.ResultType.getResultType(cursor, mimeTypeIndex)
                        if (type != mSearchType) {
                            addResult = false
                        }
                    }

                    if (addResult) {
                        createSearchResult(cursor, mSearchType)?.let {
                            results.add(it)
                        }
                    }
                } while (cursor.moveToNext())
            }
        }

        return results
    }

    /**
     * This will run a search given a filter string and return the top NUM_RESULTS_TO_GET per
     * type
     * @return the results for that search
     */
    private fun runGenericSearch(): List<SearchResult> {
        val results = ArrayList<SearchResult>()
        // number of types to query for
        val numTypes = SearchResult.ResultType.getNumTypes()

        // number of results we want
        val numResultsNeeded = Config.SEARCH_NUM_RESULTS_TO_GET * numTypes

        // current number of results we have
        var numResultsAdded = 0

        // count for each result type
        val numOfEachType = IntArray(numTypes)

        // search playlists first
        makePlaylistSearchCursor(context, mQuery)?.use { playlistCursor ->
            if (playlistCursor.moveToFirst()) {
                do {
                    createSearchResult(playlistCursor, SearchResult.ResultType.Playlist)?.let {
                        numResultsAdded++
                        results.add(it)
                    }
                } while (playlistCursor.moveToNext() && numResultsAdded < Config.SEARCH_NUM_RESULTS_TO_GET)

                // because we deal with playlists separately,
                // just mark that we have the full # of playlists
                // so that logic later can quit out early if full
                numResultsAdded = Config.SEARCH_NUM_RESULTS_TO_GET
            }
        }

        // do fancy audio search
        ElevenUtils.createSearchQueryCursor(context, mQuery)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // pre-cache this index
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.MIME_TYPE)

                do {
                    // get the result type
                    val type = SearchResult.ResultType.getResultType(cursor, mimeTypeIndex)

                    // if we still need this type
                    if (numOfEachType[type.ordinal] < Config.SEARCH_NUM_RESULTS_TO_GET) {
                        // get the search result
                        val item = createSearchResult(cursor, type)

                        if (item != null) {
                            // add it
                            results.add(item)
                            numOfEachType[type.ordinal]++
                            numResultsAdded++

                            // if we have enough then quit
                            if (numResultsAdded >= numResultsNeeded) {
                                break
                            }
                        }
                    }
                } while (cursor.moveToNext())
            }
        }

        // sort our results
        Collections.sort(results, SearchResult.COMPARATOR)

        return results
    }
}
