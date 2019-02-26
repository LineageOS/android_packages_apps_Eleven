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
import android.util.Log;

import java.util.HashSet;

import static java.util.Collections.emptySet;

public class Planter {
    public static BaseTree buildDefaultDebugTree(Context applicationContext) {
        final BaseTree baseTree = new BaseTree(applicationContext, emptySet());
        baseTree.addComponent(new LogComponent(baseTree));
        return baseTree;
    }

    public static BaseTree buildDefaultProductionTree(Context applicationContext) {
        final HashSet<Integer> priorityFilter = new HashSet<>(3);
        priorityFilter.add(Log.DEBUG);
        priorityFilter.add(Log.INFO);
        priorityFilter.add(Log.VERBOSE);

        final BaseTree baseTree = new BaseTree(applicationContext, priorityFilter);
        baseTree.addComponent(new LogComponent(baseTree));
        return baseTree;
    }
}
