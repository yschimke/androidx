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
 * The embedded player's operations layer: a common abstraction over the RemoteCompose operation
 * model. On JVM/Android [Operation] is a zero-cost typealias to the Java `remote-core` `Operation`;
 * on native/web it is reimplemented. Shared player *logic* (algorithms, Compose) depends only on this
 * package, so it can live in commonMain.
 *
 * Rather than mirror each remote-core type's members as expect classes (huge, and brittle across
 * Java-interop edges), remote-core introspection is exposed through small expect *helper functions*.
 * This keeps the common surface minimal and the algorithms — which are the actual shared logic —
 * platform-agnostic.
 */

/** A single RemoteCompose operation (remote-core `Operation`). Opaque in common code. */
internal expect abstract class Operation

/**
 * If [op] is a computed-value operation (`VariableSupport` + `VariableProvider`) that is *not* an
 * animated/spring `FloatExpression`, returns the id it produces (> 0); otherwise returns -1. Animated
 * floats are excluded because they're displayed via the animated-float resolver, not the graph.
 */
internal expect fun computedOpId(op: Operation): Int

/** The child operations of [op] if it is a layout `Container`, else null. */
internal expect fun childOperations(op: Operation): List<Operation>?
