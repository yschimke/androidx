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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontStyle

/*
 * Platform-agnostic paint state for the embedded player's canvas draw path. This carries no
 * remote-core or platform dependency (pure Compose), so it lives in commonMain; the PaintBundle
 * decoding that fills it (updatePaintFromBundle) and the platform shader/text draws are layered on
 * top in the jvmAndAndroid / platform source sets.
 */

internal class ComposeLocalPaint {
    var color: Int = 0
    var isColorSet: Boolean = false
    var strokeWidth: Float = 1f
    var isStrokeWidthSet: Boolean = false
    var isStroke: Boolean = false
    var isStyleSet: Boolean = false
    var strokeCap: Int = 0
    var isStrokeCapSet: Boolean = false
    var strokeJoin: Int = 0
    var isStrokeJoinSet: Boolean = false
    var textSize: Float = Float.NaN
    var isTextSizeSet: Boolean = false
    var fontFamily: Int = 0
    var isTypefaceSet: Boolean = false
    var fontWeight: Int = 400
    var fontStyle: FontStyle = FontStyle.Normal
    var brush: Brush? = null
    // The platform shader object backing [brush] (SHADER/TEXTURE), kept so SHADER_MATRIX can set a
    // local matrix on it. Holds an android.graphics.Shader on Android; null on platforms without a
    // native shader (desktop falls back to the solid color). Opaque to the shared code.
    var platformShader: Any? = null
    var colorFilter: androidx.compose.ui.graphics.ColorFilter? = null
    var blendMode: androidx.compose.ui.graphics.BlendMode =
        androidx.compose.ui.graphics.BlendMode.SrcOver
    var isBlendModeSet: Boolean = false

    /** Paint alpha in [0,1] from the PaintBundle ALPHA op; multiplies the draw color's own alpha. */
    var alpha: Float = 1f

    /** The fill color with the paint's [alpha] folded into its alpha channel. */
    fun effectiveColor(): Color = Color(color).let { it.copy(alpha = it.alpha * alpha) }
}

internal fun mapStrokeCap(cap: Int): StrokeCap =
    when (cap) {
        1 -> StrokeCap.Round
        2 -> StrokeCap.Square
        else -> StrokeCap.Butt
    }

internal fun mapStrokeJoin(join: Int): StrokeJoin =
    when (join) {
        1 -> StrokeJoin.Round
        2 -> StrokeJoin.Bevel
        else -> StrokeJoin.Miter
    }

internal fun mapTileMode(mode: Int): TileMode =
    when (mode) {
        1 -> TileMode.Repeated
        2 -> TileMode.Mirror
        else -> TileMode.Clamp
    }

internal fun mapBlendMode(mode: Int): androidx.compose.ui.graphics.BlendMode =
    when (mode) {
        0 -> androidx.compose.ui.graphics.BlendMode.Clear
        1 -> androidx.compose.ui.graphics.BlendMode.Src
        2 -> androidx.compose.ui.graphics.BlendMode.Dst
        3 -> androidx.compose.ui.graphics.BlendMode.SrcOver
        4 -> androidx.compose.ui.graphics.BlendMode.DstOver
        5 -> androidx.compose.ui.graphics.BlendMode.SrcIn
        6 -> androidx.compose.ui.graphics.BlendMode.DstIn
        7 -> androidx.compose.ui.graphics.BlendMode.SrcOut
        8 -> androidx.compose.ui.graphics.BlendMode.DstOut
        9 -> androidx.compose.ui.graphics.BlendMode.SrcAtop
        10 -> androidx.compose.ui.graphics.BlendMode.DstAtop
        11 -> androidx.compose.ui.graphics.BlendMode.Xor
        12 -> androidx.compose.ui.graphics.BlendMode.Plus
        13 -> androidx.compose.ui.graphics.BlendMode.Modulate
        14 -> androidx.compose.ui.graphics.BlendMode.Screen
        15 -> androidx.compose.ui.graphics.BlendMode.Overlay
        16 -> androidx.compose.ui.graphics.BlendMode.Darken
        17 -> androidx.compose.ui.graphics.BlendMode.Lighten
        18 -> androidx.compose.ui.graphics.BlendMode.ColorDodge
        19 -> androidx.compose.ui.graphics.BlendMode.ColorBurn
        20 -> androidx.compose.ui.graphics.BlendMode.Hardlight
        21 -> androidx.compose.ui.graphics.BlendMode.Softlight
        22 -> androidx.compose.ui.graphics.BlendMode.Difference
        23 -> androidx.compose.ui.graphics.BlendMode.Exclusion
        24 -> androidx.compose.ui.graphics.BlendMode.Multiply
        25 -> androidx.compose.ui.graphics.BlendMode.Hue
        26 -> androidx.compose.ui.graphics.BlendMode.Saturation
        27 -> androidx.compose.ui.graphics.BlendMode.Color
        28 -> androidx.compose.ui.graphics.BlendMode.Luminosity
        else -> androidx.compose.ui.graphics.BlendMode.SrcOver
    }
