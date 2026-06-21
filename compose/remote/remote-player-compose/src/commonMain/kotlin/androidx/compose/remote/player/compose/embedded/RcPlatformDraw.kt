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
 * Platform draw seam for the embedded player's canvas path. Canvas *text* and *path-measure* glyph
 * placement have no Compose-multiplatform DrawScope API, so they go through expect/actual: Android
 * uses the framework Canvas/Paint/PathMeasure; desktop uses Skia. Everything else in the draw path
 * (shapes, images, clips, transforms, gradients) uses the multiplatform DrawScope directly.
 */

/** Draw a single text run at baseline ([x], [y]) using [paint]'s size/typeface/color. */
internal expect fun DrawScope.drawPlatformText(
    text: String,
    x: Float,
    y: Float,
    paint: ComposeLocalPaint,
)

/** Lay [text] along [path] (Compose path), offset by [hOffset]/[vOffset], using [paint]. */
internal expect fun DrawScope.drawPlatformTextOnPath(
    text: String,
    path: Path,
    hOffset: Float,
    vOffset: Float,
    paint: ComposeLocalPaint,
)

/** Measured tight bounds (left/top/right/bottom) of [text] under [paint]. */
internal expect fun ComposeLocalPaint.measurePlatformTextBounds(text: String): Rect

/** Advance width of [text] under [paint]. */
internal expect fun ComposeLocalPaint.measurePlatformTextWidth(text: String): Float

/** Total length of [path], for distributing glyphs along it. */
internal expect fun platformPathLength(path: Path): Float

/**
 * Draw [image] centered on [path] at [distance] along it, rotated to the path tangent, scaled to the
 * dst box [-halfWidth..halfWidth] x [top..bottom], at [alpha]. Used by bitmap-font-on-path text.
 */
internal expect fun DrawScope.drawImageOnPath(
    image: ImageBitmap,
    path: Path,
    distance: Float,
    halfWidth: Float,
    top: Float,
    bottom: Float,
    alpha: Float,
)

/**
 * A mutable image the size of [stored] for off-screen rendering (DrawToBitmap): the document draws
 * into the returned image via a `Canvas`, then draws it back later. [eraseColor] (if non-null) fills
 * it first. Android returns a mutable Bitmap copy; desktop a Skia-backed bitmap.
 */
internal expect fun mutablePlayerCanvasBitmap(stored: ImageBitmap, eraseColor: Int?): ImageBitmap

/** Append a conic segment to [path] (Compose's common Path has no conicTo). */
internal expect fun platformConicTo(
    path: Path,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    weight: Float,
)
