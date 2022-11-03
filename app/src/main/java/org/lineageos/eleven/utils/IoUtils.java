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
package org.lineageos.eleven.utils;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.net.Socket;

public class IoUtils {
    public static void closeQuietly(@Nullable final Object object) {
        try {
            if (object instanceof Socket) {
                ((Socket) object).close();
            } else if (object instanceof Closeable) {
                ((Closeable) object).close();
            }
        } catch (Exception ignored) {
            // ignored
        }
    }
}
