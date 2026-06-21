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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf

internal val LocalCoreDocument: ProvidableCompositionLocal<CoreDocument> = compositionLocalOf {
    throw IllegalStateException("No document")
}

/**
 * The pure-Compose [GraphContext] that resolves *computed* operations (color/text/float/int
 * expressions, attributes, lookups) via `derivedStateOf`. Resolvers route a computed id through it;
 * leaf variables read the snapshot store directly. Null when no graph is installed (shouldn't happen
 * inside [RcPlayer]).
 */
internal val LocalGraphContext: ProvidableCompositionLocal<GraphContext?> = compositionLocalOf {
    null
}

internal val LocalRemoteContext: ProvidableCompositionLocal<RemoteContext> = compositionLocalOf {
    throw IllegalStateException("No remote context")
}

internal val LocalComponentValueMap: ProvidableCompositionLocal<Map<Int, List<ComponentValue>>> =
    compositionLocalOf {
        emptyMap()
    }

internal val LocalComponentValueStateMap:
    ProvidableCompositionLocal<Map<Int, MutableState<Float>>> =
    compositionLocalOf {
        emptyMap()
    }

internal val LocalCurrentTimeMillis: ProvidableCompositionLocal<State<Float>> = compositionLocalOf {
    androidx.compose.runtime.mutableStateOf(0f)
}

/** Host-action callback (id, value) for `HostAction`/`RunAction` clicks. Default no-op. */
internal val LocalRemoteActionHandler: ProvidableCompositionLocal<(Int, String?) -> Unit> =
    compositionLocalOf {
        { _, _ -> }
    }

/** Host named-action callback (name, resolved value) for `HostNamedAction` clicks. Default no-op. */
internal val LocalRemoteNamedActionHandler: ProvidableCompositionLocal<(String, Any?) -> Unit> =
    compositionLocalOf {
        { _, _ -> }
    }
