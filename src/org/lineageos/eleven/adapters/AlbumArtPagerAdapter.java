/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2018-2021 The LineageOS Project
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
package org.lineageos.eleven.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.lineageos.eleven.BuildConstants;
import org.lineageos.eleven.MusicPlaybackService;
import org.lineageos.eleven.R;
import org.lineageos.eleven.model.AlbumArtistDetails;
import org.lineageos.eleven.utils.ElevenUtils;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.SquareImageView;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A {@link androidx.fragment.app.FragmentStatePagerAdapter} class for swiping between album art
 */
public class AlbumArtPagerAdapter extends FragmentStatePagerAdapter {
    private static final boolean DEBUG = false;
    private static final String TAG = AlbumArtPagerAdapter.class.getSimpleName();

    public static final long NO_TRACK_ID = -1;
    private static final int MAX_ALBUM_ARTIST_SIZE = 10;

    // This helps with flickering and jumping and reloading the same tracks
    private final static LinkedList<AlbumArtistDetails> sCacheAlbumArtistDetails =
            new LinkedList<>();

    /**
     * Adds the album artist details to the cache
     *
     * @param details the AlbumArtistDetails to add
     */
    public static void addAlbumArtistDetails(AlbumArtistDetails details) {
        if (getAlbumArtistDetails(details.mAudioId) == null) {
            sCacheAlbumArtistDetails.add(details);
            if (sCacheAlbumArtistDetails.size() > MAX_ALBUM_ARTIST_SIZE) {
                sCacheAlbumArtistDetails.remove();
            }
        }
    }

    /**
     * Gets the album artist details for the audio track.  If it exists, it re-inserts the item
     * to the end of the queue so it is considered the 'freshest' and stays longer
     *
     * @param audioId the audio track to look for
     * @return the details of the album artist
     */
    public static AlbumArtistDetails getAlbumArtistDetails(long audioId) {
        for (Iterator<AlbumArtistDetails> i = sCacheAlbumArtistDetails.descendingIterator();
             i.hasNext(); ) {
            final AlbumArtistDetails entry = i.next();
            if (entry.mAudioId == audioId) {
                // remove it from the stack to re-add to the top
                i.remove();
                sCacheAlbumArtistDetails.add(entry);
                return entry;
            }
        }

        return null;
    }

    // the length of the playlist
    private int mPlaylistLen = 0;

    public AlbumArtPagerAdapter(FragmentManager fm) {
        super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    @NonNull
    public Fragment getItem(final int position) {
        long trackID = getTrackId(position);
        return AlbumArtFragment.newInstance(trackID);
    }

    @Override
    public int getCount() {
        return mPlaylistLen;
    }

    public void setPlaylistLength(final int len) {
        mPlaylistLen = len;
        notifyDataSetChanged();
    }

    /**
     * Gets the track id for the item at position
     *
     * @param position position of the item of the queue
     * @return track id of the item at position or NO_TRACK_ID if unknown
     */
    private long getTrackId(int position) {
        if (MusicUtils.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
            // if we are only playing one song, return the current audio id
            return MusicUtils.getCurrentAudioId();
        } else if (MusicUtils.getShuffleMode() == MusicPlaybackService.SHUFFLE_NONE) {
            // if we aren't shuffling, just return based on the queue position
            // add a check for empty queue
            return MusicUtils.getQueueItemAtPosition(position);
        } else {
            // if we are shuffling, there is no 'queue' going forward per say
            // because it is dynamically generated.  In that case we can only look
            // at the history and up to the very next track.  When we come back to this
            // after the demo, we should redo that queue logic to be able to give us
            // tracks going forward

            // how far into the history we are
            int positionOffset = MusicUtils.getQueueHistorySize();

            if (position - positionOffset == 0) { // current track
                return MusicUtils.getCurrentAudioId();
            } else if (position - positionOffset == 1) { // next track
                return MusicUtils.getNextAudioId();
            } else if (position < positionOffset) {
                int queuePosition = MusicUtils.getQueueHistoryPosition(position);
                if (position >= 0) {
                    return MusicUtils.getQueueItemAtPosition(queuePosition);
                }
            }
        }

        // fallback case
        return NO_TRACK_ID;
    }

    /**
     * The fragments to be displayed inside this adapter.  This wraps the album art
     * and handles loading the album art for a given audio id
     */
    public static class AlbumArtFragment extends Fragment {
        private static final String ID = BuildConstants.PACKAGE_NAME +
                ".adapters.AlbumArtPagerAdapter.AlbumArtFragment.ID";

        private AlbumArtistLoader mTask;
        private SquareImageView mImageView;
        private long mAudioId = NO_TRACK_ID;

        public static AlbumArtFragment newInstance(final long trackId) {
            AlbumArtFragment frag = new AlbumArtFragment();
            final Bundle args = new Bundle();
            args.putLong(ID, trackId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final Bundle args = getArguments();
            mAudioId = args == null ? NO_TRACK_ID : args.getLong(ID, NO_TRACK_ID);
        }

        @Override
        @SuppressLint("InflateParams")
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                 final Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.album_art_fragment, null);
            mImageView = rootView.findViewById(R.id.audio_player_album_art);
            return rootView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();

            // if we are destroying our view, cancel our task and null it
            if (mTask != null) {
                mTask.cancel();
                mTask = null;
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            loadImageAsync();
        }

        /**
         * Loads the image asynchronously
         */
        private void loadImageAsync() {
            // if we have no track id, quit
            if (mAudioId == NO_TRACK_ID) {
                return;
            }

            // try loading from the cache
            AlbumArtistDetails details = getAlbumArtistDetails(mAudioId);
            if (details != null) {
                loadImageAsync(details);
            } else {
                // Cancel any previous tasks
                if (mTask != null) {
                    mTask.cancel();
                    mTask = null;
                }

                mTask = new AlbumArtistLoader(this, getActivity());
                mTask.execute(mAudioId);
            }

        }

        /**
         * Loads the image asynchronously
         *
         * @param details details of the image to load
         */
        private void loadImageAsync(AlbumArtistDetails details) {
            // load the actual image
            ElevenUtils.getImageFetcher(getActivity()).loadAlbumImage(
                    details.mArtistName,
                    details.mAlbumName,
                    details.mAlbumId,
                    mImageView
            );
        }
    }

    /**
     * This looks up the album and artist details for a track
     */
    private static class AlbumArtistLoader {
        private final WeakReference<Context> mContext;
        private final AlbumArtFragment mFragment;

        private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        public AlbumArtistLoader(final AlbumArtFragment albumArtFragment, final Context context) {
            mContext = new WeakReference<>(context);
            mFragment = albumArtFragment;
        }

        public void execute(long albumId) {
            mExecutor.execute(() -> {
                AlbumArtistDetails result = MusicUtils.getAlbumArtDetails(mContext.get(), albumId);

                mHandler.post(() ->  {
                    if (result != null) {
                        if (DEBUG) {
                            Log.d(TAG, "[" + mFragment.mAudioId + "] Loading image: "
                                    + result.mAlbumId + ","
                                    + result.mAlbumName + ","
                                    + result.mArtistName);
                        }

                        AlbumArtPagerAdapter.addAlbumArtistDetails(result);
                        mFragment.loadImageAsync(result);
                    } else if (DEBUG) {
                        Log.d(TAG, "No Image found for audioId: " + mFragment.mAudioId);
                    }
                });
            });
        }

        public void cancel() {
            mExecutor.shutdownNow();
        }
    }
}
