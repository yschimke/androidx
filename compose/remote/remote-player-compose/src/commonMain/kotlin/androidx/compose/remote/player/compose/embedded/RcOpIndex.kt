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

import androidx.compose.remote.player.compose.embedded.rc.Operation
import androidx.compose.remote.player.compose.embedded.rc.childOperations
import androidx.compose.remote.player.compose.embedded.rc.computedOpId

/**
 * Index of computed-value operations by the id they produce — `VariableSupport`+`VariableProvider`
 * ops that compute from other variables. Animation/spring-bearing `FloatExpression`s are excluded
 * (those are displayed via `rememberAnimatedRemoteFloat`); everything else, including plain
 * `FloatExpression`/`IntegerExpression`, is included so the graph can resolve them when a derived op
 * reads them as an input (chains).
 *
 * Platform-agnostic: it walks the [Operation] tree through the common operations layer, so it lives in
 * commonMain.
 */
internal fun buildComputedOpIndex(operations: Collection<Operation>): Map<Int, Operation> {
    val map = HashMap<Int, Operation>()
    fun walk(ops: Collection<Operation>) {
        for (op in ops) {
            val id = computedOpId(op)
            if (id > 0 && !map.containsKey(id)) map[id] = op
            childOperations(op)?.let { walk(it) }
        }
    }
    walk(operations)
    return map
}
