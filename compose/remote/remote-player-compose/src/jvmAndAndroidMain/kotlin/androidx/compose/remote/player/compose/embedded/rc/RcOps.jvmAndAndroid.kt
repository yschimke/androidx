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

package androidx.compose.remote.player.compose.embedded.rc

import androidx.compose.remote.core.VariableProvider
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.WakeIn
import androidx.compose.remote.core.operations.layout.Container

// On JVM/Android the operations layer is the Java remote-core model verbatim (zero-cost).
internal actual typealias Operation = androidx.compose.remote.core.Operation

internal actual fun computedOpId(op: Operation): Int {
    if (op is VariableSupport && op is VariableProvider) {
        val animated = op is FloatExpression && op.mFloatAnimation != null
        if (!animated) return op.id
    }
    return -1
}

internal actual fun childOperations(op: Operation): List<Operation>? =
    if (op is Container) op.list else null

internal actual fun isParticleOp(op: Operation): Boolean =
    op is ParticlesLoop || op is ParticlesCompare

internal actual fun isWakeInOp(op: Operation): Boolean = op is WakeIn

internal actual typealias RemoteReadContext = androidx.compose.remote.core.RemoteReadContext

internal actual fun rcReadFloat(read: RemoteReadContext, id: Int): Float = read.getFloat(id)

internal actual fun rcIdFromNan(value: Float): Int =
    androidx.compose.remote.core.operations.Utils.idFromNan(value)
