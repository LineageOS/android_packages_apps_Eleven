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
package org.lineageos.eleven.debug;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * Base tree for use with Timber.<br></br>
 * Allows to add and remove {@link BaseTreeComponent} dynamically
 */
public class BaseTree extends Timber.DebugTree {
    private final Context applicationContext;
    private final HashSet<Integer> priorityFilterSet;

    private final ArrayList<BaseTreeComponent> baseTreeComponents = new ArrayList<>();

    private boolean isEnabled;

    /**
     * Maps a {@link Log} priority such as {@link Log#INFO} to a string.
     *
     * @param priority The log priority to map
     * @return A string representation of the priority
     */
    public static String mapPriorityToString(final int priority) {
        switch (priority) {
            case Log.ASSERT: {
                return "WTF";
            }
            case Log.DEBUG: {
                return "D";
            }
            case Log.ERROR: {
                return "E";
            }
            case Log.INFO: {
                return "I";
            }
            case Log.VERBOSE: {
                return "V";
            }
            case Log.WARN: {
                return "W";
            }
            default: {
                return "WTF";
            }
        }
    }

    public BaseTree(final Context context, final Set<Integer> priorityFilterList) {
        this.applicationContext = context.getApplicationContext();
        this.priorityFilterSet = new HashSet<>(priorityFilterList);
        this.isEnabled = true;
    }

    public List<BaseTreeComponent> getComponents() {
        return baseTreeComponents;
    }

    /**
     * When disabled, no events will get forwarded.<br></br>
     * This can be used to prevent the overhead of {@link #shouldLog(int)} calls.
     *
     * @param isEnabled Whether we should forward events or not
     * @return The same {@link BaseTree} instance to allow chained calls
     */
    public BaseTree setEnabled(final boolean isEnabled) {
        this.isEnabled = isEnabled;
        return this;
    }

    /**
     * Adds a {@link BaseTreeComponent} which will receive events and be able to react with custom logic.
     *
     * @param baseTreeComponent The {@link BaseTreeComponent} to add
     * @return The same {@link BaseTree} instance to allow chained calls
     */
    public BaseTree addComponent(final BaseTreeComponent baseTreeComponent) {
        baseTreeComponents.add(baseTreeComponent);
        return this;
    }

    /**
     * Removes a {@link BaseTreeComponent} which then will not receive any events anymore.
     *
     * @param baseTreeComponent The {@link BaseTreeComponent} to remove
     * @return The same {@link BaseTree} instance to allow chained calls
     */
    public BaseTree removeComponent(final BaseTreeComponent baseTreeComponent) {
        baseTreeComponents.remove(baseTreeComponent);
        return this;
    }

    /**
     * @see #removeComponent(BaseTreeComponent)
     */
    public BaseTree removeComponent(final Class clazz) {
        final Iterator<BaseTreeComponent> iterator = baseTreeComponents.iterator();
        while (iterator.hasNext()) {
            final BaseTreeComponent component = iterator.next();
            if (clazz.isInstance(component)) {
                iterator.remove();
            }
        }
        return this;
    }

    /**
     * DOES NOT ACTUALLY LOG!<br></br>
     * All log calls are getting forwarded to the added {@link BaseTreeComponent}s.<br></br>
     *
     * @see timber.log.Timber.Tree#log(int, String, String, Throwable)
     */
    @Override
    public void log(final int priority, final String tag, @NonNull final String message, final Throwable t) {
        // when we are not enabled, do not emit any events
        if (!isEnabled) {
            return;
        }

        for (final BaseTreeComponent baseTreeComponent : baseTreeComponents) {
            baseTreeComponent.log(priority, tag, message, t);
        }
    }

    /**
     * Calls the DebugTree&#39;s log method.<br></br>
     *
     * @see timber.log.Timber.DebugTree.log
     */
    public void reallyDoLog(final int priority, final String tag, @NonNull final String message, final Throwable t) {
        super.log(priority, tag, message, t);
    }

    /**
     * @param priority The priority, which should be logged
     * @return True, if we should log
     */
    public boolean shouldLog(final int priority) {
        if (!priorityFilterSet.isEmpty()) {
            for (final int priorityFilter : priorityFilterSet) {
                // if our priority is filtered, get out of here
                if (priority == priorityFilter) {
                    return false;
                }
            }
        }
        return true;
    }
}
