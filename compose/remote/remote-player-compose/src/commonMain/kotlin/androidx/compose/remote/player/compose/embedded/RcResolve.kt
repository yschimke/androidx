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

import androidx.compose.remote.player.compose.embedded.rc.RemoteReadContext
import androidx.compose.remote.player.compose.embedded.rc.rcIdFromNan
import androidx.compose.remote.player.compose.embedded.rc.rcReadFloat

/**
 * Resolve a draw operand to a float. A NaN-encoded [value] is a variable reference, read from
 * [context] (the draw read context — normally the GraphContext, so a time/variable-driven value
 * re-runs the draw when it changes); otherwise the literal [fallback] (the op's resolved `mOut*`) is
 * used. Platform-agnostic; the variable read + NaN decode go through the operations layer.
 */
internal fun resolveFloat(value: Float, fallback: Float, context: RemoteReadContext): Float =
    if (value.isNaN()) rcReadFloat(context, rcIdFromNan(value)) else fallback
