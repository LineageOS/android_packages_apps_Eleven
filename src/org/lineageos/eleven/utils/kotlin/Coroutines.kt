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
package org.lineageos.eleven.utils.kotlin

import kotlinx.coroutines.*

object Coroutines {
    fun io(work: suspend (() -> Unit)): Job =
            CoroutineScope(Dispatchers.IO).launch {
                work()
            }

    fun <T : Any> ioThenMain(work: suspend (() -> T?), callback: ((T?) -> Unit)): Job =
            CoroutineScope(Dispatchers.Main).launch {
                val data = CoroutineScope(Dispatchers.IO).async rt@{
                    return@rt work()
                }.await()
                callback(data)
            }
}
