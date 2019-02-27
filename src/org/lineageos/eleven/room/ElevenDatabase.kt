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
package org.lineageos.eleven.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.lineageos.eleven.utils.kotlin.SingletonHolder

@Database(entities = [Property::class], version = 1)
abstract class ElevenDatabase : RoomDatabase() {
    companion object : SingletonHolder<ElevenDatabase, Context>({
        Room.databaseBuilder(it.applicationContext, ElevenDatabase::class.java, ElevenDatabase.DATABASE_NAME)
                .build()
    }) {
        private const val DATABASE_NAME = "eleven"
    }

    abstract fun propertyDao(): PropertyDao
}
