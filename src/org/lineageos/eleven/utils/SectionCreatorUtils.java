/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2021 The LineageOS Project
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
package org.lineageos.eleven.utils;

import android.content.Context;
import android.text.TextUtils;

import org.lineageos.eleven.R;
import org.lineageos.eleven.model.Artist;
import org.lineageos.eleven.model.Song;

import java.util.List;
import java.util.TreeMap;

/**
 * This Utils class contains code that compares two different items and determines whether
 * a section should be created
 */
public class SectionCreatorUtils {
    public enum SectionType {
        Header,
        Footer
    }

    public static class Section {
        public SectionType mType;
        public String mIdentifier;

        public Section(final SectionType type, final String identifier) {
            mType = type;
            mIdentifier = identifier;
        }
    }

    /**
     * Interface to compare two items and create labels
     *
     * @param <T> type of item to compare
     */
    @SuppressWarnings("unused")
    public static class IItemCompare<T> {
        /**
         * Compares to items and returns a section divider T if there should
         * be a section divider between first and second
         *
         * @param first      the first element in the list.  If null, it is checking to see
         *                   if we need a divider at the beginning of the list
         * @param second     the second element in the list.
         * @param items      the source list of items that we are creating headers from
         * @param firstIndex index of the first item we are looking at
         * @return String the expected separator label or null if none
         */
        public String createSectionHeader(T first, T second, List<T> items, int firstIndex) {
            return createSectionHeader(first, second);
        }

        public String createSectionHeader(T first, T second) {
            return null;
        }

        /**
         * Compares to items and returns a section divider T if there should
         * be a section divider between first and second
         *
         * @param first      the first element in the list.
         * @param second     the second element in the list. If null, it is checking to see if we
         *                   need a divider at the end of the list
         * @param items      the source list of items that we are creating footers from
         * @param firstIndex index of the first item we are looking at
         * @return String the expected separator label or null if none
         */
        public String createSectionFooter(T first, T second, List<T> items, int firstIndex) {
            return createSectionFooter(first, second);
        }

        public String createSectionFooter(T first, T second) {
            return null;
        }

        /**
         * Returns the section label that corresponds to this item
         *
         * @param item the item
         * @return the section label that this label falls under
         */
        public String createHeaderLabel(T item) {
            return null;
        }

        /**
         * Returns the section label that corresponds to this item
         *
         * @param item the item
         * @return the section label that this label falls under
         */
        public String createFooterLabel(T item) {
            return null;
        }

        // partial sectioning helper functions

        public boolean shouldStopSectionCreation() {
            return false;
        }
    }

    /**
     * A localized String comparison implementation of IItemCompare
     *
     * @param <T> the type of item to compare
     */
    public static abstract class LocalizedCompare<T> extends IItemCompare<T> {
        protected Context mContext;
        private boolean mStopSectionCreation;

        public LocalizedCompare(Context context) {
            mContext = context;
            mStopSectionCreation = false;
        }

        @Override
        public String createSectionHeader(T first, T second) {
            String secondLabel = createHeaderLabel(second);
            // if we can't determine a good label then don't bother creating a section
            if (secondLabel == null) {
                // stop section creation as the items further down the list
                mStopSectionCreation = true;
                return null;
            }

            if (first == null || !secondLabel.equals(createHeaderLabel(first))) {
                return secondLabel;
            }

            return null;
        }

        @Override
        public String createHeaderLabel(T item) {
            final String label = MusicUtils.getLocalizedBucketLetter(getString(item));
            return createHeaderLabel(label);
        }

        protected String createHeaderLabel(final String label) {
            if (TextUtils.isEmpty(label)) {
                return mContext.getString(R.string.header_other);
            }
            return label;
        }

        public abstract String getString(T item);

        @Override
        public boolean shouldStopSectionCreation() {
            return mStopSectionCreation;
        }
    }

    /**
     * A simple int comparison implementation of IItemCompare
     *
     * @param <T> the type of item to compare
     */
    public static abstract class IntCompare<T> extends IItemCompare<T> {
        @Override
        public String createSectionHeader(T first, T second) {
            if (first == null || getInt(first) != getInt(second)) {
                return createHeaderLabel(second);
            }

            return null;
        }

        @Override
        public String createHeaderLabel(T item) {
            return String.valueOf(getInt(item));
        }

        public abstract int getInt(T item);
    }

    /**
     * A Bounded int comparison implementation of IntCompare
     * Basically this will take ints and determine what bounds it falls into
     * For example, 1-5 mintes, 5-10 minutes, 10+ minutes
     *
     * @param <T> the type of item to compare
     */
    public static abstract class BoundedIntCompare<T> extends IntCompare<T> {
        protected Context mContext;

        public BoundedIntCompare(Context context) {
            mContext = context;
        }

        protected abstract int getStringId(int value);

        @Override
        public String createSectionHeader(T first, T second) {
            int secondStringId = getStringId(getInt(second));
            if (first == null || getStringId(getInt(first)) != secondStringId) {
                return createLabel(secondStringId, second);
            }

            return null;
        }

        protected String createLabel(int stringId, T item) {
            return mContext.getString(stringId);
        }

        @Override
        public String createHeaderLabel(T item) {
            return createLabel(getStringId(getInt(item)), item);
        }
    }

    /**
     * This implements BoundedIntCompare and gives duration buckets
     *
     * @param <T> the type of item to compare
     */
    public static abstract class DurationCompare<T> extends BoundedIntCompare<T> {
        private static final int SECONDS_PER_MINUTE = 60;

        public DurationCompare(Context context) {
            super(context);
        }

        @Override
        protected int getStringId(int value) {
            if (value < 30) {
                return R.string.header_less_than_30s;
            } else if (value < SECONDS_PER_MINUTE) {
                return R.string.header_30_to_60_seconds;
            } else if (value < 2 * SECONDS_PER_MINUTE) {
                return R.string.header_1_to_2_minutes;
            } else if (value < 3 * SECONDS_PER_MINUTE) {
                return R.string.header_2_to_3_minutes;
            } else if (value < 4 * SECONDS_PER_MINUTE) {
                return R.string.header_3_to_4_minutes;
            } else if (value < 5 * SECONDS_PER_MINUTE) {
                return R.string.header_4_to_5_minutes;
            } else if (value < 10 * SECONDS_PER_MINUTE) {
                return R.string.header_5_to_10_minutes;
            } else if (value < 30 * SECONDS_PER_MINUTE) {
                return R.string.header_10_to_30_minutes;
            } else if (value < 60 * SECONDS_PER_MINUTE) {
                return R.string.header_30_to_60_minutes;
            }

            return R.string.header_greater_than_60_minutes;
        }
    }

    /**
     * This implements BoundedIntCompare and gives number of songs buckets
     *
     * @param <T> the type of item to compare
     */
    public static abstract class NumberOfSongsCompare<T> extends BoundedIntCompare<T> {
        public NumberOfSongsCompare(Context context) {
            super(context);
        }

        @Override
        protected int getStringId(int value) {
            if (value <= 1) {
                return R.string.header_1_song;
            } else if (value <= 4) {
                return R.string.header_2_to_4_songs;
            } else if (value <= 9) {
                return R.string.header_5_to_9_songs;
            }

            return R.string.header_10_plus_songs;
        }
    }

    /**
     * This implements BoundedIntCompare and gives number of albums buckets
     *
     * @param <T> the type of item to compare
     */
    public static abstract class NumberOfAlbumsCompare<T> extends BoundedIntCompare<T> {
        public NumberOfAlbumsCompare(Context context) {
            super(context);
        }

        @Override
        protected int getStringId(int value) {
            if (value <= 4) {
                return R.plurals.Nalbums;
            }

            return R.string.header_5_plus_albums;
        }

        @Override
        public String createSectionHeader(T first, T second) {
            boolean returnSeparator = false;
            if (first == null) {
                returnSeparator = true;
            } else {
                // create a separator if both album counts are different and they are
                // not greater than 5 albums
                int firstInt = getInt(first);
                int secondInt = getInt(second);
                if (firstInt != secondInt &&
                        !(firstInt >= 5 && secondInt >= 5)) {
                    returnSeparator = true;
                }
            }

            if (returnSeparator) {
                return createHeaderLabel(second);
            }

            return null;
        }

        @Override
        protected String createLabel(int stringId, T item) {
            if (stringId == R.plurals.Nalbums) {
                final int numItems = getInt(item);
                return mContext.getResources().getQuantityString(stringId, numItems, numItems);
            }

            return super.createLabel(stringId, item);
        }
    }

    /**
     * This creates the sections given a list of items and the comparison algorithm
     *
     * @param list       The list of items to analyze
     * @param comparator The comparison function to use
     * @param <T>        the type of item to compare
     * @return Creates a TreeMap of indices (if the headers were part of the list) to section labels
     */
    public static <T> TreeMap<Integer, Section> createSections(final List<T> list,
                                                               final IItemCompare<T> comparator) {
        if (list != null && list.size() > 0) {
            TreeMap<Integer, Section> sections = new TreeMap<>();
            for (int i = 0; i < list.size() + 1; i++) {
                T first = (i == 0 ? null : list.get(i - 1));
                T second = (i == list.size() ? null : list.get(i));

                // create the footer first because if we need both it should be footer,header,item
                // not header,footer,item
                if (first != null) {
                    String footer = comparator.createSectionFooter(first, second, list, i - 1);
                    if (footer != null) {
                        // add sectionHeaders.size() to store the indices of the combined list
                        sections.put(i, new Section(SectionType.Footer, footer));
                    }
                }

                if (second != null) {
                    String header = comparator.createSectionHeader(first, second, list, i - 1);
                    if (header != null) {
                        // add sectionHeaders.size() to store the indices of the combined list
                        sections.put(i, new Section(SectionType.Header, header));
                        // stop section creation
                        if (comparator.shouldStopSectionCreation()) {
                            break;
                        }
                    }
                }
            }

            return sections;
        }

        return null;
    }

    /**
     * Returns an artist comparison based on the current sort
     *
     * @param context Context for string generation
     * @return the artist comparison method
     */
    public static IItemCompare<Artist> createArtistComparison(final Context context) {
        IItemCompare<Artist> sectionCreator = null;

        final String sortOrder = PreferenceUtils.getInstance(context).getArtistSortOrder();
        switch (sortOrder) {
            case SortOrder.ArtistSortOrder.ARTIST_A_Z:
            case SortOrder.ArtistSortOrder.ARTIST_Z_A:
                sectionCreator = new LocalizedCompare<Artist>(context) {
                    @Override
                    public String getString(Artist item) {
                        return item.mArtistName;
                    }

                    @Override
                    public String createHeaderLabel(Artist item) {
                        if (item.mBucketLabel != null) {
                            return super.createHeaderLabel(item.mBucketLabel);
                        }

                        return super.createHeaderLabel(item);
                    }
                };
                break;
            case SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS:
                sectionCreator = new NumberOfAlbumsCompare<Artist>(context) {
                    @Override
                    public int getInt(Artist item) {
                        return item.mAlbumNumber;
                    }
                };
                break;
            case SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS:
                sectionCreator = new NumberOfSongsCompare<Artist>(context) {
                    @Override
                    public int getInt(Artist item) {
                        return item.mSongNumber;
                    }
                };
                break;
        }

        return sectionCreator;
    }

    /**
     * Returns an song comparison based on the current sort
     *
     * @param context Context for string generation
     * @return the song comparison method
     */
    public static IItemCompare<Song> createSongComparison(final Context context) {
        IItemCompare<Song> sectionCreator = null;

        final String sortOrder = PreferenceUtils.getInstance(context).getSongSortOrder();

        // doesn't make sense to have headers for SONG_FILENAME
        // so we will not return a sectionCreator for that one
        switch (sortOrder) {
            case SortOrder.SongSortOrder.SONG_A_Z:
            case SortOrder.SongSortOrder.SONG_Z_A:
                sectionCreator = new LocalizedCompare<Song>(context) {
                    @Override
                    public String getString(Song item) {
                        return item.mSongName;
                    }

                    @Override
                    public String createHeaderLabel(Song item) {
                        if (item.mBucketLabel != null) {
                            return super.createHeaderLabel(item.mBucketLabel);
                        }

                        return super.createHeaderLabel(item);
                    }
                };
                break;
            case SortOrder.SongSortOrder.SONG_ALBUM:
                sectionCreator = new LocalizedCompare<Song>(context) {
                    @Override
                    public String getString(Song item) {
                        return item.mAlbumName;
                    }

                    @Override
                    public String createHeaderLabel(Song item) {
                        if (item.mBucketLabel != null) {
                            return super.createHeaderLabel(item.mBucketLabel);
                        }

                        return super.createHeaderLabel(item);
                    }
                };
                break;
            case SortOrder.SongSortOrder.SONG_ARTIST:
                sectionCreator = new LocalizedCompare<Song>(context) {
                    @Override
                    public String getString(Song item) {
                        return item.mArtistName;
                    }

                    @Override
                    public String createHeaderLabel(Song item) {
                        if (item.mBucketLabel != null) {
                            return super.createHeaderLabel(item.mBucketLabel);
                        }

                        return super.createHeaderLabel(item);
                    }
                };
                break;
            case SortOrder.SongSortOrder.SONG_DURATION:
                sectionCreator = new DurationCompare<Song>(context) {
                    @Override
                    public int getInt(Song item) {
                        return item.mDuration;
                    }
                };
                break;
            case SortOrder.SongSortOrder.SONG_YEAR:
                sectionCreator = new IntCompare<Song>() {
                    @Override
                    public int getInt(Song item) {
                        return item.mYear;
                    }

                    @Override
                    public String createHeaderLabel(Song item) {
                        // I have seen tracks in my library where it would return 0 or 2
                        // so have this check to return a more friendly label in that case
                        if (MusicUtils.isInvalidYear(item.mYear)) {
                            return context.getString(R.string.header_unknown_year);
                        }

                        return super.createHeaderLabel(item);
                    }
                };
                break;
        }

        return sectionCreator;
    }
}
