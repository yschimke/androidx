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

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.AndroidPath
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle

/**
 * Build a framework [android.graphics.Paint] for canvas text from the current paint state:
 * anti-aliased, the effective color, the text size, and a bold/italic Typeface from weight/style.
 */
private fun ComposeLocalPaint.toNativeTextPaint(): android.graphics.Paint {
    val style =
        when {
            fontStyle == FontStyle.Italic && fontWeight >= 600 ->
                android.graphics.Typeface.BOLD_ITALIC
            fontStyle == FontStyle.Italic -> android.graphics.Typeface.ITALIC
            fontWeight >= 600 -> android.graphics.Typeface.BOLD
            else -> android.graphics.Typeface.NORMAL
        }
    return android.graphics.Paint().apply {
        isAntiAlias = true
        color = effectiveColor().toArgb()
        textSize = this@toNativeTextPaint.textSize
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, style)
    }
}

internal actual fun DrawScope.drawPlatformText(
    text: String,
    x: Float,
    y: Float,
    paint: ComposeLocalPaint,
) {
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint.toNativeTextPaint())
}

internal actual fun DrawScope.drawPlatformTextOnPath(
    text: String,
    path: Path,
    hOffset: Float,
    vOffset: Float,
    paint: ComposeLocalPaint,
) {
    drawContext.canvas.nativeCanvas.drawTextOnPath(
        text,
        path.asAndroidPath(),
        hOffset,
        vOffset,
        paint.toNativeTextPaint(),
    )
}

internal actual fun ComposeLocalPaint.measurePlatformTextBounds(text: String): Rect {
    val bounds = android.graphics.Rect()
    toNativeTextPaint().getTextBounds(text, 0, text.length, bounds)
    return Rect(
        bounds.left.toFloat(),
        bounds.top.toFloat(),
        bounds.right.toFloat(),
        bounds.bottom.toFloat(),
    )
}

internal actual fun ComposeLocalPaint.measurePlatformTextWidth(text: String): Float =
    toNativeTextPaint().measureText(text)

internal actual fun platformPathLength(path: Path): Float =
    android.graphics.PathMeasure(path.asAndroidPath(), false).length

internal actual fun DrawScope.drawImageOnPath(
    image: ImageBitmap,
    path: Path,
    distance: Float,
    halfWidth: Float,
    top: Float,
    bottom: Float,
    alpha: Float,
) {
    val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val matrix = android.graphics.Matrix()
    pathMeasure.getMatrix(
        distance,
        matrix,
        android.graphics.PathMeasure.POSITION_MATRIX_FLAG or
            android.graphics.PathMeasure.TANGENT_MATRIX_FLAG,
    )
    val bitmap = image.asAndroidBitmap()
    val nativePaint =
        android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            this.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        }
    val canvas = drawContext.canvas.nativeCanvas
    canvas.save()
    canvas.concat(matrix)
    canvas.drawBitmap(
        bitmap,
        android.graphics.Rect(0, 0, bitmap.width, bitmap.height),
        android.graphics.RectF(-halfWidth, top, halfWidth, bottom),
        nativePaint,
    )
    canvas.restore()
}

internal actual fun mutablePlayerCanvasBitmap(stored: ImageBitmap, eraseColor: Int?): ImageBitmap {
    val src = stored.asAndroidBitmap()
    val target = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
    if (eraseColor != null) target.eraseColor(eraseColor)
    return target.asImageBitmap()
}

internal actual fun platformConicTo(
    path: Path,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    weight: Float,
) {
    // conicTo on the framework path is API 34+; below that the conic is skipped (rare op).
    if (android.os.Build.VERSION.SDK_INT >= 34) {
        (path as AndroidPath).internalPath.conicTo(x1, y1, x2, y2, weight)
    }
}
