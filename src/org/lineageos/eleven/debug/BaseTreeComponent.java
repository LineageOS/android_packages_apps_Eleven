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

import java.util.HashSet;

/**
 * A TreeComponent can be attached and removed to the tree at runtime.
 */
abstract class BaseTreeComponent {
    protected BaseTree baseTree;
    private HashSet<Integer> priorityFilterSet = null;

    public BaseTreeComponent(final BaseTree baseTree) {
        this.baseTree = baseTree;
    }

    /**
     * Called whenever the BaseTree&#39;s log method gets called.
     *
     * @see timber.log.Timber.Tree#log(int, String, String, Throwable)
     */
    protected abstract void doLog(int priority, String tag, String message, Throwable t);

    public void log(int priority, String tag, String message, Throwable t) {
        if (shouldLog(priority)) {
            doLog(priority, tag, message, t);
        }
    }

    /**
     * If a priority filter set is set, it will be used to decide whether #doLog gets
     * called.<br></br>
     * If no filter is set, it will call {@link BaseTree#shouldLog(int)}.
     *
     * @see BaseTree#shouldLog(int)
     */
    protected boolean shouldLog(final int priority) {
        if (priorityFilterSet == null) {
            return baseTree.shouldLog(priority);
        }

        if (!priorityFilterSet.isEmpty()) {
            for (Integer priorityFilter : priorityFilterSet) {
                // if our priority is filtered, get out of here
                if (priority == priorityFilter) {
                    return false;
                }
            }
        }
        return true;
    }
}
