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
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle

/*
 * PaintBundle decoding for the embedded player's canvas draw path. Depends on remote-core
 * (PaintBundle/RemoteContext), so it lives in jvmAndAndroidMain; the paint *state* it fills
 * (ComposeLocalPaint) and the stroke/blend/tile mappers are platform-agnostic and live in commonMain.
 * The platform shader builders (AGSL/texture/matrix) are expect/actual.
 */

/**
 * Build the AGSL/native shader for a PaintBundle `SHADER` op and store it on the paint (sets
 * [ComposeLocalPaint.brush] + [ComposeLocalPaint.platformShader]); a missing/invalid shader clears
 * them so the caller falls back to the solid color. Android uses an AGSL `RuntimeShader` (API 33+);
 * desktop has no AGSL, so it is a no-op (solid color).
 */
internal expect fun ComposeLocalPaint.applyPlatformShader(shaderId: Int, remoteContext: RemoteContext)

/** Build a bitmap-texture shader brush for a PaintBundle `TEXTURE` op (see [applyPlatformShader]). */
internal expect fun ComposeLocalPaint.applyPlatformTexture(
    bitmapId: Int,
    tileModes: Int,
    remoteContext: RemoteContext,
)

/** Apply a PaintBundle `SHADER_MATRIX` op: set a local matrix on the current platform shader. */
internal expect fun ComposeLocalPaint.applyPlatformShaderMatrix(
    matrixWord: Int,
    read: RemoteReadContext,
)

internal fun updatePaintFromBundle(
    bundle: PaintBundle,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteReadContext = remoteContext,
) {
    val array = bundle.mArray
    var i = 0
    while (i < bundle.mPos) {
        val cmd = array[i++]
        when (cmd and 0xFFFF) {
            PaintBundle.TEXT_SIZE -> {
                paintState.textSize = Float.fromBits(array[i++])
                paintState.isTextSizeSet = true
            }
            PaintBundle.TYPEFACE -> {
                val style = (cmd shr 16)
                val weight = style and 0x3ff
                val italic = (style shr 10) > 0
                paintState.fontFamily = array[i++]
                paintState.fontWeight = weight
                paintState.fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
                paintState.isTypefaceSet = true
            }
            PaintBundle.COLOR -> {
                paintState.color = array[i++]
                paintState.isColorSet = true
            }
            PaintBundle.COLOR_ID -> {
                val colorId = array[i++]
                // Reactive read: an animated/variable color re-runs the draw when it changes.
                paintState.color = read.getColor(colorId)
                paintState.isColorSet = true
            }
            PaintBundle.STROKE_WIDTH -> {
                paintState.strokeWidth = Float.fromBits(array[i++])
                paintState.isStrokeWidthSet = true
            }
            PaintBundle.STYLE -> {
                paintState.isStroke = (cmd shr 16) == PaintBundle.STYLE_STROKE
                paintState.isStyleSet = true
            }
            PaintBundle.STROKE_CAP -> {
                paintState.strokeCap = (cmd shr 16)
                paintState.isStrokeCapSet = true
            }
            PaintBundle.STROKE_JOIN -> {
                paintState.strokeJoin = (cmd shr 16)
                paintState.isStrokeJoinSet = true
            }
            PaintBundle.FONT_AXIS -> {
                val count = cmd shr 16
                i += 2 * count
            }
            PaintBundle.BLEND_MODE -> {
                val mode = (cmd shr 16)
                paintState.blendMode = mapBlendMode(mode)
                paintState.isBlendModeSet = true
            }
            androidx.compose.remote.core.operations.paint.PaintBundle.COLOR_FILTER -> {
                val mode = (cmd shr 16)
                val color = array[i++]
                paintState.colorFilter =
                    androidx.compose.ui.graphics.ColorFilter.tint(Color(color), mapBlendMode(mode))
            }
            PaintBundle.COLOR_FILTER_ID -> {
                val mode = (cmd shr 16)
                val colorId = array[i++]
                val color = read.getColor(colorId)
                paintState.colorFilter =
                    androidx.compose.ui.graphics.ColorFilter.tint(Color(color), mapBlendMode(mode))
            }
            PaintBundle.CLEAR_COLOR_FILTER -> {
                paintState.colorFilter = null
            }
            PaintBundle.SHADER -> {
                // AGSL shader brush (mirrors the View player's AndroidPaintContext.setShader); a
                // null/missing/unsupported shader clears it so the solid color is used.
                paintState.applyPlatformShader(array[i++], remoteContext)
            }
            PaintBundle.TEXTURE -> {
                // Bitmap texture shader. Layout (PaintBundle): bitmapId, tileModes (tileX=&0xF,
                // tileY=>>16), filter (unused here).
                val bitmapId = array[i++]
                val tileModes = array[i++]
                i++ // filter/maxAnisotropy word (filtering managed by Compose; consumed to stay synced)
                paintState.applyPlatformTexture(bitmapId, tileModes, remoteContext)
            }
            PaintBundle.ALPHA -> {
                // 1 float word (see PaintBundle.resolveIds). Folded into the draw color via
                // ComposeLocalPaint.effectiveColor().
                paintState.alpha = Float.fromBits(array[i++]).coerceIn(0f, 1f)
            }
            PaintBundle.ANTI_ALIAS,
            PaintBundle.IMAGE_FILTER_QUALITY,
            PaintBundle.FILTER_BITMAP -> {
                // Value is packed in the high bits of `cmd`; no extra words. Compose's DrawScope is
                // anti-aliased and manages filtering itself, so these are consumed and ignored.
            }
            PaintBundle.SHADER_MATRIX -> {
                // Local matrix on the current shader (1 word: NaN-encoded MatrixAccess id).
                paintState.applyPlatformShaderMatrix(array[i++], read)
            }
            PaintBundle.STROKE_MITER,
            PaintBundle.FALLBACK_TYPEFACE -> {
                i++ // 1 word each (PaintBundle.resolveIds); not applied yet, consumed to stay in sync.
            }
            PaintBundle.PATH_EFFECT -> {
                i += (cmd shr 16) // `count` float words (PaintBundle.resolveIds); not applied yet.
            }
            PaintBundle.GRADIENT -> {
                val gradientType = (cmd shr 16)
                var len = array[i++] and 0xFF // colors count
                val colors = IntArray(len)
                for (j in 0 until len) {
                    colors[j] = array[i++]
                }
                len = array[i++] // stops count
                val stops = FloatArray(len)
                for (j in 0 until len) {
                    stops[j] = Float.fromBits(array[i++])
                }

                val colorsList = colors.map { Color(it) }
                // Use explicit color stops only when well-formed: one per color, ascending, within
                // [0,1]. Compose's colorStops overloads throw otherwise, so fall back to even spacing.
                val colorStops: Array<Pair<Float, Color>>? =
                    if (
                        stops.size == colorsList.size &&
                            colorsList.isNotEmpty() &&
                            stops.all { it in 0f..1f } &&
                            stops.asList().zipWithNext().all { (lo, hi) -> lo <= hi }
                    ) {
                        Array(colorsList.size) { stops[it] to colorsList[it] }
                    } else {
                        null
                    }

                when (gradientType) {
                    0 -> { // LINEAR_GRADIENT
                        val startX = Float.fromBits(array[i++])
                        val startY = Float.fromBits(array[i++])
                        val endX = Float.fromBits(array[i++])
                        val endY = Float.fromBits(array[i++])
                        val tileMode = array[i++]
                        val start = Offset(startX, startY)
                        val end = Offset(endX, endY)
                        val tm = mapTileMode(tileMode)
                        if (
                            colorsList.size >= 2 &&
                                startX.isFinite() &&
                                startY.isFinite() &&
                                endX.isFinite() &&
                                endY.isFinite() &&
                                (startX != endX || startY != endY)
                        ) {
                            paintState.brush =
                                if (colorStops != null)
                                    Brush.linearGradient(
                                        colorStops = colorStops,
                                        start = start,
                                        end = end,
                                        tileMode = tm,
                                    )
                                else
                                    Brush.linearGradient(
                                        colors = colorsList,
                                        start = start,
                                        end = end,
                                        tileMode = tm,
                                    )
                        } else if (colorsList.isNotEmpty()) {
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                    1 -> { // RADIAL_GRADIENT
                        val centerX = Float.fromBits(array[i++])
                        val centerY = Float.fromBits(array[i++])
                        val radius = Float.fromBits(array[i++])
                        val tileMode = array[i++]
                        val center = Offset(centerX, centerY)
                        val tm = mapTileMode(tileMode)
                        if (
                            colorsList.size >= 2 &&
                                centerX.isFinite() &&
                                centerY.isFinite() &&
                                radius.isFinite() &&
                                radius > 0
                        ) {
                            paintState.brush =
                                if (colorStops != null)
                                    Brush.radialGradient(
                                        colorStops = colorStops,
                                        center = center,
                                        radius = radius,
                                        tileMode = tm,
                                    )
                                else
                                    Brush.radialGradient(
                                        colors = colorsList,
                                        center = center,
                                        radius = radius,
                                        tileMode = tm,
                                    )
                        } else if (colorsList.isNotEmpty()) {
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                    2 -> { // SWEEP_GRADIENT
                        val centerX = Float.fromBits(array[i++])
                        val centerY = Float.fromBits(array[i++])
                        val center = Offset(centerX, centerY)
                        if (colorsList.size >= 2 && centerX.isFinite() && centerY.isFinite()) {
                            paintState.brush =
                                if (colorStops != null)
                                    Brush.sweepGradient(colorStops = colorStops, center = center)
                                else Brush.sweepGradient(colors = colorsList, center = center)
                        } else if (colorsList.isNotEmpty()) {
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                }
            }
            else -> {
                // Unknown/variable-width sub-op whose word count we can't determine, so consuming a
                // guessed number would desync the rest of the bundle. Stop processing the remaining
                // sub-ops rather than crash; what was parsed so far still applies.
                println(
                    "Warning: unsupported PaintBundle sub-op ${cmd and 0xFFFF}; " +
                        "skipping remainder of bundle"
                )
                return
            }
        }
    }
}
