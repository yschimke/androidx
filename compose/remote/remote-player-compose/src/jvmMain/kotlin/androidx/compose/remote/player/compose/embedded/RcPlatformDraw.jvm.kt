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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.atan2
import kotlin.math.roundToInt
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint as SkiaPaint

/*
 * Desktop (JVM) implementations of the embedded player's platform draw seam, backed by Skia (the
 * renderer JetBrains Compose Multiplatform uses). Text is drawn with a Skia [Font]; path/image and
 * the off-screen layer use the multiplatform Compose APIs. Text-on-path is a graceful no-op for now
 * (Skia has no direct draw-text-on-path).
 */

private fun ComposeLocalPaint.skiaFont(): Font =
    Font(null, if (textSize.isNaN()) 12f else textSize)

private fun ComposeLocalPaint.skiaPaint(): SkiaPaint =
    SkiaPaint().apply { color = effectiveColor().toArgb() }

internal actual fun DrawScope.drawPlatformText(
    text: String,
    x: Float,
    y: Float,
    paint: ComposeLocalPaint,
) {
    drawContext.canvas.nativeCanvas.drawString(text, x, y, paint.skiaFont(), paint.skiaPaint())
}

internal actual fun DrawScope.drawPlatformTextOnPath(
    text: String,
    path: Path,
    hOffset: Float,
    vOffset: Float,
    paint: ComposeLocalPaint,
) {
    // Skia has no built-in draw-text-on-path; approximate by laying the string along the path with
    // per-glyph positioning would require a glyph runner. Left as a graceful no-op for now.
}

internal actual fun ComposeLocalPaint.measurePlatformTextBounds(text: String): Rect {
    val b = skiaFont().measureText(text)
    return Rect(b.left, b.top, b.right, b.bottom)
}

internal actual fun ComposeLocalPaint.measurePlatformTextWidth(text: String): Float =
    skiaFont().measureTextWidth(text)

internal actual fun platformPathLength(path: Path): Float =
    PathMeasure().apply { setPath(path, false) }.length

internal actual fun DrawScope.drawImageOnPath(
    image: ImageBitmap,
    path: Path,
    distance: Float,
    halfWidth: Float,
    top: Float,
    bottom: Float,
    alpha: Float,
) {
    val measure = PathMeasure().apply { setPath(path, false) }
    val pos = measure.getPosition(distance)
    val tangent = measure.getTangent(distance)
    val degrees = Math.toDegrees(atan2(tangent.y, tangent.x).toDouble()).toFloat()
    withTransform({
        translate(pos.x, pos.y)
        rotate(degrees, pivot = Offset.Zero)
    }) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset((-halfWidth).roundToInt(), top.roundToInt()),
            dstSize = IntSize((2f * halfWidth).roundToInt(), (bottom - top).roundToInt()),
            alpha = alpha,
        )
    }
}

internal actual fun mutablePlayerCanvasBitmap(stored: ImageBitmap, eraseColor: Int?): ImageBitmap {
    val target = ImageBitmap(stored.width, stored.height)
    val canvas = Canvas(target)
    if (eraseColor != null) {
        canvas.drawRect(
            0f,
            0f,
            stored.width.toFloat(),
            stored.height.toFloat(),
            Paint().apply { color = Color(eraseColor) },
        )
    } else {
        canvas.drawImage(stored, Offset.Zero, Paint())
    }
    return target
}

internal actual fun platformConicTo(
    path: Path,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    weight: Float,
) {
    path.asSkiaPath().conicTo(x1, y1, x2, y2, weight)
}
