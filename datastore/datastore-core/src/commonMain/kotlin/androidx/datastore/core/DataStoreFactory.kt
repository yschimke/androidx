/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.core

import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.CoroutineScope

public object DataStoreFactory {

    @JvmOverloads
    public fun <T> create(
        codec: Codec<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
        migrations: List<DataMigration<T>> = listOf(),
        scope: CoroutineScope,
        producePath: () -> Path
    ): DataStore<T> {
        return SingleProcessDataStore(
            codec,
            corruptionHandler = corruptionHandler ?: NoOpCorruptionHandler(),
            initTasksList = listOf(DataMigrationInitializer.getInitializer(migrations)),
            scope = scope,
            producePath = producePath,
        )
    }
}