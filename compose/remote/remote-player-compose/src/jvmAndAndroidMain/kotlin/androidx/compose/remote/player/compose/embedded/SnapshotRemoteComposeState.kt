/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * A [RemoteComposeState] whose reactive scalar caches (float / integer / color) are backed by Compose
 * [SnapshotStateMap]s instead of plain maps.
 *
 * This makes Compose the single source of truth for those variables: a read through `getFloat` /
 * `getInteger` / `getColor` performed inside composition, layout, or draw is recorded as a snapshot
 * dependency, and a write (the core's `updateFloat`/`overrideFloat`/… as host actions, value
 * changes, or expression evaluation run) invalidates exactly those readers. The embedded player can
 * therefore resolve these variables with `derivedStateOf { context.getFloat(id) }` and drop the
 * per-id `mutableStateOf` mirror + `listenToVar` listener bridge that previously kept core state and
 * Compose state in sync.
 *
 * The scalar numeric caches (float/int/color) and the id->Object data cache (text, bitmaps) are
 * overridden; the latter's entries are *replaced* on update, so a snapshot map observes the change.
 * Longs are intentionally NOT covered: they live in the object map as `LongConstant`s mutated in
 * place, which a reference-keyed snapshot map can't detect — those keep the listener path. Paths,
 * collections, data maps, the reverse Object->id map, override flags, and listeners all use the base
 * in-memory storage unchanged. The base routes access through the `storeGet*`/`storePut*` seam, so
 * override flags and change-detection still work — only where the value lives changes.
 */
internal class SnapshotRemoteComposeState : RemoteComposeState() {
    private val floats: SnapshotStateMap<Int, Float> = mutableStateMapOf()
    private val integers: SnapshotStateMap<Int, Int> = mutableStateMapOf()
    private val colors: SnapshotStateMap<Int, Int> = mutableStateMapOf()
    private val data: SnapshotStateMap<Int, Any> = mutableStateMapOf()

    override fun storeGetFloat(id: Int): Float = floats[id] ?: 0f

    override fun storePutFloat(id: Int, value: Float) {
        floats[id] = value
    }

    override fun storeGetInteger(id: Int): Int = integers[id] ?: 0

    override fun storePutInteger(id: Int, value: Int) {
        integers[id] = value
    }

    override fun storeGetColor(id: Int): Int = colors[id] ?: 0

    override fun storePutColor(id: Int, color: Int) {
        colors[id] = color
    }

    override fun storeGetData(id: Int): Any? = data[id]

    override fun storePutData(id: Int, item: Any) {
        data[id] = item
    }
}
