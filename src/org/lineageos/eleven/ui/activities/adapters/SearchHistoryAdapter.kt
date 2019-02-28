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
package org.lineageos.eleven.ui.activities.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.eleven.R
import org.lineageos.eleven.ui.activities.SearchActivity
import java.lang.ref.WeakReference
import java.util.*

class SearchHistoryAdapter(searchActivity: SearchActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val searchActivityReference: WeakReference<SearchActivity> = WeakReference(searchActivity)
    private var searchHistoryList: List<String> = ArrayList()

    private inner class SearchHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemTextView: TextView = itemView.findViewById(R.id.line_one)

        init {
            itemView.setOnClickListener {
                searchActivityReference.get()?.let { searchActivity ->
                    val searchItem = searchHistoryList[layoutPosition]
                    searchActivity.setQuery(searchItem)
                }
            }
        }

        internal fun bindData(searchQuery: String) {
            itemTextView.text = searchQuery
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.list_item_search_history, viewGroup, false)
        return SearchHistoryViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        (viewHolder as SearchHistoryViewHolder).bindData(searchHistoryList[i])
    }

    override fun getItemCount(): Int {
        return searchHistoryList.size
    }

    fun setData(searchHistoryList: List<String>) {
        this.searchHistoryList = searchHistoryList
        notifyDataSetChanged()
    }
}
