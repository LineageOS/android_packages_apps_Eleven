/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
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

package org.lineageos.eleven.ui.activities

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.list_base.*
import kotlinx.android.synthetic.main.loading_empty_container.*
import org.lineageos.eleven.Config
import org.lineageos.eleven.R
import org.lineageos.eleven.adapters.SummarySearchAdapter
import org.lineageos.eleven.menu.FragmentMenuItems
import org.lineageos.eleven.model.SearchResult
import org.lineageos.eleven.model.SearchResult.ResultType
import org.lineageos.eleven.recycler.RecycleHolder
import org.lineageos.eleven.room.SearchHistory
import org.lineageos.eleven.sectionadapter.SectionAdapter
import org.lineageos.eleven.sectionadapter.SectionCreator
import org.lineageos.eleven.sectionadapter.SectionListContainer
import org.lineageos.eleven.ui.activities.adapters.SearchHistoryAdapter
import org.lineageos.eleven.ui.activities.adapters.SummarySearchLoader
import org.lineageos.eleven.ui.models.SearchViewModel
import org.lineageos.eleven.utils.MusicUtils
import org.lineageos.eleven.utils.MusicUtils.ServiceToken
import org.lineageos.eleven.utils.NavUtils
import org.lineageos.eleven.utils.PopupMenuHelper
import org.lineageos.eleven.utils.SectionCreatorUtils
import org.lineageos.eleven.utils.SectionCreatorUtils.IItemCompare
import java.util.*

class SearchActivity : AppCompatActivity() {
    companion object {
        /**
         * Intent extra for identifying the search type to filter for
         */
        const val EXTRA_SEARCH_MODE = "search_mode"

        private const val LOADING_DELAY = 500L

        private const val SEARCH_LOADER = 0
    }

    private var mToken: ServiceToken? = null

    private var mFilterString: String? = null

    private var mSearchView: SearchView? = null

    private val inputMethodManager: InputMethodManager by lazy {
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val mLoadingRunnable = Runnable { setState(VisibleState.Loading) }

    /**
     * boolean tracking whether this is the search level when the user first enters search
     * or if the user has clicked show all
     */
    private var mTopLevelSearch: Boolean = false

    /**
     * If the user has clicked show all, this tells us what type (Artist, Album, etc)
     */
    private var mSearchType: ResultType? = null

    private var mCurrentState: VisibleState? = null

    private var mHandler = Handler()

    private var mQuitting = false

    private var mHandledOnTouch: Boolean = false

    private var mLastSearchQuery: String? = null

    private lateinit var sectionAdapter: SectionAdapter<SearchResult, SummarySearchAdapter>

    private lateinit var searchViewModel: SearchViewModel
    private var mHasHistoryLoadedOnce: Boolean = false

    private val mSearchHistoryObserver = Observer<List<SearchHistory>> { searchHistories ->
        if (!mHasHistoryLoadedOnce) {
            setState(VisibleState.SearchHistory)
            mHasHistoryLoadedOnce = true
        }

        val strings = ArrayList<String>()
        if (searchHistories != null) {
            strings.addAll(SearchHistory.getRecentSearchesAsString(searchHistories))
        }
        (list_search_history.adapter as SearchHistoryAdapter).setData(strings)
    }

    private val mPopupMenuHelper: PopupMenuHelper = object : PopupMenuHelper(this, supportFragmentManager) {
        private var mSelectedItem: SearchResult? = null

        override fun onPreparePopupMenu(position: Int): PopupMenuHelper.PopupMenuType {
            mSelectedItem = sectionAdapter.getTItem(position)

            return PopupMenuHelper.PopupMenuType.SearchResult
        }

        override fun getIdList(): LongArray? {
            return when (mSelectedItem!!.mType) {
                SearchResult.ResultType.Artist -> {
                    MusicUtils.getSongListForArtist(this@SearchActivity, mSelectedItem!!.mId)
                }
                SearchResult.ResultType.Album -> {
                    MusicUtils.getSongListForAlbum(this@SearchActivity, mSelectedItem!!.mId)
                }
                SearchResult.ResultType.Song -> {
                    longArrayOf(mSelectedItem!!.mId)
                }
                SearchResult.ResultType.Playlist -> {
                    MusicUtils.getSongListForPlaylist(this@SearchActivity, mSelectedItem!!.mId)
                }
                else -> {
                    null
                }
            }
        }

        override fun getSourceId(): Long {
            return mSelectedItem!!.mId
        }

        override fun getSourceType(): Config.IdType {
            return mSelectedItem!!.mType.sourceType
        }

        override fun updateMenuIds(type: PopupMenuHelper.PopupMenuType, set: TreeSet<Int>) {
            super.updateMenuIds(type, set)

            if (mSelectedItem!!.mType == ResultType.Album) {
                set.add(FragmentMenuItems.MORE_BY_ARTIST)
            }
        }

        override fun getArtistName(): String {
            return mSelectedItem!!.mArtist
        }
    }

    // region listeners

    private val mLoaderCallbacks = object : LoaderCallbacks<SectionListContainer<SearchResult>> {
        override fun onCreateLoader(id: Int, b: Bundle?): Loader<SectionListContainer<SearchResult>> {
            var comparator: IItemCompare<SearchResult>? = null

            // prep the loader in case the query takes a long time
            setLoading()

            // if we are at the top level, create a comparator to separate the different types into
            // their own sections (artists, albums, etc)
            if (mTopLevelSearch) {
                comparator = SectionCreatorUtils.createSearchResultComparison(this@SearchActivity)
            }

            val loader = SummarySearchLoader(this@SearchActivity, mFilterString ?: "", mSearchType)
            return SectionCreator(this@SearchActivity, loader, comparator)
        }

        override fun onLoadFinished(loader: Loader<SectionListContainer<SearchResult>>,
                                    data: SectionListContainer<SearchResult>) {
            if (data.mListResults.isEmpty()) {
                sectionAdapter.clear()
                setState(VisibleState.Empty)
            } else {
                sectionAdapter.setData(data)
                setState(VisibleState.SearchResults)
            }
        }

        override fun onLoaderReset(loader: Loader<SectionListContainer<SearchResult>>) {
            sectionAdapter.unload()
        }
    }

    private val mOnTouchListener = object : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val action = event.action
            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (!mHandledOnTouch) {
                        hideInputManager()
                        mHandledOnTouch = true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mHandledOnTouch = false
                }
            }
            return false
        }
    }

    private val mOnItemClickListener = object : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val context = this@SearchActivity
            if (sectionAdapter.isSectionFooter(position)) {
                // since a footer should be after a list item by definition, let's look up the type
                // of the previous item
                val item = sectionAdapter.getTItem(position - 1) ?: return
                val intent = Intent(context, SearchActivity::class.java)
                intent.putExtra(SearchManager.QUERY, mFilterString)
                intent.putExtra(SearchActivity.EXTRA_SEARCH_MODE, item.mType.ordinal)
                startActivity(intent)
            } else {
                val item = sectionAdapter.getTItem(position) ?: return
                when (item.mType) {
                    SearchResult.ResultType.Artist -> {
                        NavUtils.openArtistProfile(context, item.mArtist)
                    }
                    SearchResult.ResultType.Album -> {
                        NavUtils.openAlbumProfile(context, item.mAlbum, item.mArtist, item.mId)
                    }
                    SearchResult.ResultType.Playlist -> {
                        NavUtils.openPlaylist(context, item.mId, item.mTitle)
                    }
                    SearchResult.ResultType.Song -> {
                        // If it's a song, play it and leave
                        val list = longArrayOf(item.mId)
                        MusicUtils.playAll(list, 0, -1, Config.IdType.NA, false)
                    }
                    else -> {
                        /* Do nothing */
                    }
                }
            }
        }
    }
    // endregion listeners

    private enum class VisibleState {
        SearchHistory,
        Empty,
        SearchResults,
        Loading
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel::class.java)

        volumeControlStream = AudioManager.STREAM_MUSIC

        setContentView(R.layout.activity_search)

        val adapter = SummarySearchAdapter(this)
        sectionAdapter = SectionAdapter(this, adapter)
        // Set the prefix
        sectionAdapter.underlyingAdapter.setPrefix(mFilterString)
        sectionAdapter.setupHeaderParameters(R.layout.list_search_header, false)
        sectionAdapter.setupFooterParameters(R.layout.list_search_footer, true)
        sectionAdapter.setPopupMenuClickedListener { v, position ->
            mPopupMenuHelper.showPopupMenu(v, position)
        }

        // setup the no results container
        val noResults = loading_empty_container.noResultsContainer
        noResults.setMainText(R.string.empty_search)
        noResults.setSecondaryText(R.string.empty_search_check)

        initListView()

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        mFilterString = intent.getStringExtra(SearchManager.QUERY)
        // if we have a non-empty search string, this is a 2nd lvl search
        if (!TextUtils.isEmpty(mFilterString)) {
            mTopLevelSearch = false

            // get the search type to filter by
            val type = intent.getIntExtra(SearchActivity.EXTRA_SEARCH_MODE, -1)
            if (type >= 0 && type < ResultType.values().size) {
                mSearchType = ResultType.values()[type]
            }

            var resourceId = 0
            when (mSearchType) {
                SearchResult.ResultType.Artist -> resourceId = R.string.search_title_artists
                SearchResult.ResultType.Album -> resourceId = R.string.search_title_albums
                SearchResult.ResultType.Playlist -> resourceId = R.string.search_title_playlists
                SearchResult.ResultType.Song -> resourceId = R.string.search_title_songs
                else -> {
                    /* Do nothing */
                }
            }
            actionBar?.title = getString(resourceId, mFilterString)

            // Set the prefix
            sectionAdapter.underlyingAdapter.setPrefix(mFilterString)

            // Start the loader for the query
            supportLoaderManager.initLoader(SEARCH_LOADER, null, mLoaderCallbacks)
        } else {
            mTopLevelSearch = true
        }

        setLoading()
        searchViewModel.getSearches().observe(this, mSearchHistoryObserver)
    }

    override fun onResume() {
        super.onResume()

        mToken = MusicUtils.bindToService(this, null)
    }

    override fun onPause() {
        super.onPause()

        if (mToken != null) {
            MusicUtils.unbindFromService(mToken)
            mToken = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // if we are not a top level search view, we do not need to create the search fields
        if (!mTopLevelSearch) {
            return super.onCreateOptionsMenu(menu)
        }

        // Search view
        menuInflater.inflate(R.menu.search, menu)

        // Filter the list the user is looking it via SearchView
        val searchItem = menu.findItem(R.id.menu_search)
        mSearchView = searchItem.actionView as SearchView
        mSearchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // simulate an on query text change
                onQueryTextChange(query)
                // hide the input manager
                hideInputManager()

                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (mQuitting) {
                    return true
                }

                if (TextUtils.isEmpty(newText)) {
                    if (!TextUtils.isEmpty(mFilterString)) {
                        mFilterString = ""
                        setState(VisibleState.SearchHistory)
                        supportLoaderManager.destroyLoader(SEARCH_LOADER)
                    }

                    return true
                }

                // if the strings are the same, return
                if (newText == mFilterString) {
                    return true
                }

                // Called when the action bar search text has changed. Update
                // the search filter, and restart the loader to do a new query
                // with this filter.
                mFilterString = newText
                // Set the prefix
                sectionAdapter.underlyingAdapter.setPrefix(mFilterString)
                supportLoaderManager.restartLoader(SEARCH_LOADER, null, mLoaderCallbacks)
                return true
            }
        })
        mSearchView?.queryHint = getString(R.string.searchHint)

        // The SearchView has no way for you to customize or get access to the search icon in a
        // normal fashion, so we need to manually look for the icon and change the
        // layout params to hide it
        mSearchView?.setIconifiedByDefault(false)
        mSearchView?.isIconified = false
        mSearchView?.findViewById<ImageView>(R.id.search_mag_icon)?.let { searchIcon ->
            searchIcon.layoutParams = LinearLayout.LayoutParams(0, 0)
        }

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                quit()
                return false
            }
        })

        searchItem.expandActionView()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                quit()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun setQuery(searchItem: String) {
        mSearchView?.setQuery(searchItem, true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListView() {
        // Set the data behind the list
        list_base.adapter = sectionAdapter
        list_base.setRecyclerListener(RecycleHolder())
        list_base.onItemClickListener = mOnItemClickListener
        list_base.setOnScrollListener(object : OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                // Pause disk cache access to ensure smoother scrolling
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    sectionAdapter.underlyingAdapter.setPauseDiskCache(true)
                } else {
                    sectionAdapter.underlyingAdapter.setPauseDiskCache(false)
                    sectionAdapter.notifyDataSetChanged()
                }
            }

            override fun onScroll(v: AbsListView, item: Int, visibleItemCount: Int, totalItemCount: Int) {
                // Nothing to do
            }
        })
        list_base.setOnTouchListener(mOnTouchListener)
        // If we setEmptyView with mLoadingEmptyContainer it causes a crash in DragSortListView
        // when updating the search.  For now let's manually toggle visibility and come back
        // to this later
        //mListView.setEmptyView(mLoadingEmptyContainer);

        // load the search history list view
        list_search_history.setHasFixedSize(true)
        list_search_history.layoutManager = LinearLayoutManager(this)
        list_search_history.adapter = SearchHistoryAdapter(this@SearchActivity)
        list_search_history.setOnTouchListener(mOnTouchListener)
    }

    private fun quit() {
        mQuitting = true
        finish()
    }

    private fun hideInputManager() {
        // When the search is "committed" by the user, then hide the keyboard so
        // the user can more easily browse the list of results.
        val searchView = mSearchView ?: return
        if (searchView.hasFocus()) {
            inputMethodManager.hideSoftInputFromWindow(searchView.windowToken, 0)
            searchView.clearFocus()

            // add our search string if it is not empty and has not already been added
            val filterString = mFilterString
            if (!filterString.isNullOrBlank() && filterString == mLastSearchQuery) {
                searchViewModel.addSearchString(filterString)
                mLastSearchQuery = filterString
            }
        }
    }

    private fun setLoading() {
        if (mCurrentState != VisibleState.Loading) {
            mHandler.removeCallbacks(mLoadingRunnable)
            mHandler.postDelayed(mLoadingRunnable, LOADING_DELAY)
        }
    }

    private fun setState(state: VisibleState) {
        // remove any delayed runnables.  This has to be before mCurrentState == state
        // in case the state doesn't change but we've created a loading runnable
        mHandler.removeCallbacks(mLoadingRunnable)

        // if we are already looking at view already, just quit
        if (mCurrentState == state) {
            return
        }
        mCurrentState = state

        list_search_history.visibility = View.INVISIBLE
        list_base.visibility = View.INVISIBLE
        loading_empty_container.visibility = View.INVISIBLE

        when (mCurrentState) {
            SearchActivity.VisibleState.SearchHistory -> {
                list_search_history.visibility = View.VISIBLE
            }
            SearchActivity.VisibleState.SearchResults -> {
                list_base.visibility = View.VISIBLE
            }
            SearchActivity.VisibleState.Empty -> {
                loading_empty_container.visibility = View.VISIBLE
                loading_empty_container.showNoResults()
            }
            SearchActivity.VisibleState.Loading -> {
                loading_empty_container.visibility = View.VISIBLE
                loading_empty_container.showLoading()
            }
        }
    }
}
