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
import androidx.compose.remote.player.compose.embedded.rc.isParticleOp
import androidx.compose.remote.player.compose.embedded.rc.isWakeInOp

/** True if the op tree contains a particle loop (drives the frame-loop keepalive). */
internal fun containsParticles(operations: Collection<Operation>): Boolean =
    operations.any { op ->
        isParticleOp(op) || childOperations(op)?.let { containsParticles(it) } == true
    }

/**
 * True if the op tree contains a `WakeIn`, which asks the runtime to repaint after a delay. The
 * embedded player has no one-shot scheduler, so we approximate by keeping the frame loop alive — the
 * content re-evaluates and redraws continuously, a superset of the requested single wake.
 */
internal fun containsWakeIn(operations: Collection<Operation>): Boolean =
    operations.any { op ->
        isWakeInOp(op) || childOperations(op)?.let { containsWakeIn(it) } == true
    }
