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

import androidx.compose.material3.Text
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
internal fun RcPlayerText(layout: CoreText, modifier: Modifier) {
    val textId = layout.textId ?: return
    val text by rememberRemoteStringAsState(textId)
    val paintState = ComposeLocalPaint()
    updatePaintFromBundle(layout.mPaint, paintState, LocalRemoteContext.current)

    val color = if (paintState.isColorSet) Color(paintState.color) else Color(layout.mColorValue)
    val fontSize = if (paintState.isTextSizeSet) paintState.textSize else layout.mFontSizeValue
    val fontSizeSp = with(LocalDensity.current) { fontSize.toSp() }

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else layout.mType
    val fontFamily =
        when (fontFamilyType) {
            1 -> FontFamily.SansSerif
            2 -> FontFamily.Serif
            3 -> FontFamily.Monospace
            else -> FontFamily.Default
        }
    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(layout.mFontWeightValue.toInt())
    val fontStyle =
        if (paintState.isTypefaceSet) paintState.fontStyle
        else {
            if (layout.mFontStyle == 1) FontStyle.Italic else FontStyle.Normal
        }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSizeSp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        textAlign =
            when (layout.mTextAlignValue) {
                CoreText.TEXT_ALIGN_LEFT -> TextAlign.Left
                CoreText.TEXT_ALIGN_RIGHT -> TextAlign.Right
                CoreText.TEXT_ALIGN_CENTER -> TextAlign.Center
                CoreText.TEXT_ALIGN_JUSTIFY -> TextAlign.Justify
                CoreText.TEXT_ALIGN_START -> TextAlign.Start
                CoreText.TEXT_ALIGN_END -> TextAlign.End
                else -> TextAlign.Start
            },
        overflow =
            when (layout.mOverflow) {
                CoreText.OVERFLOW_CLIP -> TextOverflow.Clip
                CoreText.OVERFLOW_ELLIPSIS -> TextOverflow.Ellipsis
                CoreText.OVERFLOW_VISIBLE -> TextOverflow.Visible
                else -> TextOverflow.Clip
            },
        maxLines = layout.mMaxLines,
        letterSpacing = layout.mLetterSpacing.em,
        lineHeight =
            (layout.mFontSizeValue * layout.mLineHeightMultiplier + layout.mLineHeightAdd).sp,
    )
}

@Composable
internal fun RcPlayerText(layout: TextLayout, modifier: Modifier) {
    val textId = layout.textId ?: return
    val text by rememberRemoteStringAsState(textId)
    val paintState = ComposeLocalPaint()
    updatePaintFromBundle(layout.mPaint, paintState, LocalRemoteContext.current)

    val color = if (paintState.isColorSet) Color(paintState.color) else Color(layout.mColorValue)
    val fontSize = if (paintState.isTextSizeSet) paintState.textSize else layout.mFontSizeValue
    val fontSizeSp = with(LocalDensity.current) { fontSize.toSp() }

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else layout.mType
    val fontFamily =
        when (fontFamilyType) {
            1 -> FontFamily.SansSerif
            2 -> FontFamily.Serif
            3 -> FontFamily.Monospace
            else -> FontFamily.Default
        }
    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(layout.mFontWeight.toInt())
    val fontStyle = if (paintState.isTypefaceSet) paintState.fontStyle else FontStyle.Normal

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSizeSp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        textAlign =
            when (layout.mTextAlignValue) {
                TextLayout.TEXT_ALIGN_LEFT -> TextAlign.Left
                TextLayout.TEXT_ALIGN_RIGHT -> TextAlign.Right
                TextLayout.TEXT_ALIGN_CENTER -> TextAlign.Center
                TextLayout.TEXT_ALIGN_JUSTIFY -> TextAlign.Justify
                TextLayout.TEXT_ALIGN_START -> TextAlign.Start
                TextLayout.TEXT_ALIGN_END -> TextAlign.End
                else -> TextAlign.Start
            },
        overflow =
            when (layout.mOverflow) {
                TextLayout.OVERFLOW_CLIP -> TextOverflow.Clip
                TextLayout.OVERFLOW_ELLIPSIS -> TextOverflow.Ellipsis
                TextLayout.OVERFLOW_VISIBLE -> TextOverflow.Visible
                else -> TextOverflow.Clip
            },
        maxLines = layout.mMaxLines,
    )
}
