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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.RemoteReadContext

/*
 * Desktop shader handling. AGSL RuntimeShader (the SHADER op) is Android-only, and texture/
 * shader-matrix support is not yet ported to Skia, so these clear the brush and the player falls back
 * to the paint's solid color — the same graceful degradation the Android player uses below API 33.
 */

internal actual fun ComposeLocalPaint.applyPlatformShader(
    shaderId: Int,
    remoteContext: RemoteContext,
) {
    platformShader = null
    brush = null
}

internal actual fun ComposeLocalPaint.applyPlatformTexture(
    bitmapId: Int,
    tileModes: Int,
    remoteContext: RemoteContext,
) {
    platformShader = null
    brush = null
}

internal actual fun ComposeLocalPaint.applyPlatformShaderMatrix(
    matrixWord: Int,
    read: RemoteReadContext,
) {
    // No native shader on desktop; nothing to transform.
}
