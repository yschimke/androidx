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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/*
 * Non-JVM (web/native) platform-draw actuals. Placeholders for now — these targets don't yet host the
 * player (its dispatch still lives in jvmAndAndroidMain and depends on remote-core), so the canvas
 * text/path draws are no-ops. They make the shared draw seam compile for wasmJs/native and are the
 * extension points a real non-JVM renderer fills (Skia/skiko is available via Compose Multiplatform).
 */

internal actual fun DrawScope.drawPlatformText(
    text: String,
    x: Float,
    y: Float,
    paint: ComposeLocalPaint,
) {}

internal actual fun DrawScope.drawPlatformTextOnPath(
    text: String,
    path: Path,
    hOffset: Float,
    vOffset: Float,
    paint: ComposeLocalPaint,
) {}

internal actual fun ComposeLocalPaint.measurePlatformTextBounds(text: String): Rect = Rect.Zero

internal actual fun ComposeLocalPaint.measurePlatformTextWidth(text: String): Float = 0f

internal actual fun platformPathLength(path: Path): Float = 0f

internal actual fun DrawScope.drawImageOnPath(
    image: ImageBitmap,
    path: Path,
    distance: Float,
    halfWidth: Float,
    top: Float,
    bottom: Float,
    alpha: Float,
) {}

internal actual fun mutablePlayerCanvasBitmap(stored: ImageBitmap, eraseColor: Int?): ImageBitmap =
    ImageBitmap(stored.width, stored.height)

internal actual fun platformConicTo(
    path: Path,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    weight: Float,
) {}
