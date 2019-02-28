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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "playbackQueue")
class PlaybackQueue {
    companion object {
        const val MAX_QUEUE_SIZE = 1000
    }

    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0

    @ColumnInfo(name = "trackId")
    var trackId: Long = 0

    @ColumnInfo(name = "sourceId")
    var sourceId: Long = 0

    @ColumnInfo(name = "sourceType")
    var sourceType: Int = 0

    @ColumnInfo(name = "sourcePosition")
    var sourcePosition: Int = 0

    @Ignore
    constructor()

    @Ignore
    constructor(trackId: Long, sourceId: Long, sourceType: Int, sourcePosition: Int) : this(0, trackId, sourceId, sourceType, sourcePosition)

    constructor(uid: Long, trackId: Long, sourceId: Long, sourceType: Int, sourcePosition: Int) {
        this.uid = uid
        this.trackId = trackId
        this.sourceId = sourceId
        this.sourceType = sourceType
        this.sourcePosition = sourcePosition
    }
}
