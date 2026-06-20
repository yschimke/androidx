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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.runtime.Composable

internal class WriteToDocumentNode : RemoteComposeNode() {
    var onWrite: (RemoteComposeWriter) -> Unit = {}

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        // render() runs after composition (inside the capture's mutable snapshot), the one phase where
        // writing directly to the document is legal — composition-time writes are guarded against. Open
        // a canvas component (sized by [modifier]) so any draw ops the writer emits have a target, hand
        // the raw writer to [onWrite], then close it. Everything [onWrite] emits goes straight to the
        // document buffer in call order, so it interleaves correctly with the surrounding tree.
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startCanvas(recordingModifier)
        onWrite(creationState.document)
        creationState.document.endCanvas()
    }
}

/**
 * Escape hatch for authoring raw RemoteCompose operations from within the composable capture API.
 *
 * Implemented as an applied node (like [RemoteCanvas]/`RemoteColumn`): its content runs at document
 * *render* time — after composition, the only phase where writing directly to the document is allowed
 * — and receives the live [RemoteComposeWriter]. This lets you emit operations the typed DSL doesn't
 * surface (e.g. `wakeIn`, `createFloatFunction`/`callFloatFunction`, `createParticles`/
 * `particlesComparison`, the bitmap-font run/anchored/on-path draws, `startCustom`/`endCustom`),
 * positioned correctly in the document tree relative to surrounding composables.
 *
 * The writer is opened inside a `startCanvas`/`endCanvas` span sized by [modifier], so draw ops have a
 * target component. Use raw writer calls only inside [onWrite] (do not mix with the [RemoteCanvas]
 * DSL, whose ops are buffered and flushed separately — interleaving the two reorders them).
 *
 * @param modifier sizing/placement for the canvas component the ops draw into.
 * @param onWrite emits operations against the [RemoteComposeWriter] at render time.
 */
@RemoteComposable
@Composable
public fun WriteToDocument(
    modifier: RemoteModifier = RemoteModifier,
    onWrite: (RemoteComposeWriter) -> Unit,
) {
    RemoteComposeNode(
        factory = ::WriteToDocumentNode,
        update = {
            set(modifier) { this.modifier = it }
            set(onWrite) { this.onWrite = it }
        },
    )
}
