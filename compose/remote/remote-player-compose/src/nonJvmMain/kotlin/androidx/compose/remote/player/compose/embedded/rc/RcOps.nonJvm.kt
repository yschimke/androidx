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

/*
 * Non-JVM (web/native) operations-layer actuals. These targets can't use the Java `remote-core`, so
 * the operation model has no implementation yet — the actuals are placeholders that make the shared
 * code compile. A real non-JVM player needs `remote-core`'s operation model (document byte-parser +
 * operations + expression engine) reimplemented here; see STATUS.md.
 */

/** Placeholder operation type for non-JVM targets (no remote-core). */
internal actual abstract class Operation

internal actual fun computedOpId(op: Operation): Int = -1

internal actual fun childOperations(op: Operation): List<Operation>? = null

internal actual fun isParticleOp(op: Operation): Boolean = false

internal actual fun isWakeInOp(op: Operation): Boolean = false
