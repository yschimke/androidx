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

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RcPlayerFlowLayout(layout: FlowLayout, modifier: Modifier) {
    // FlowLayout extends RowLayout, so it carries the same positioning + spacedBy. Honor them:
    // main-axis (within a row) via horizontalArrangement, cross-axis (between wrapped rows) via
    // verticalArrangement, plus the wrap limit. Previously this was a bare FlowRow that dropped them.
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    // Resolve spacedBy (may be a NaN-encoded variable/expression) before scaling.
    val spacedBy = rememberRemoteFloatAsState(rowSpacedBy(layout)).value
    val gap = rawDimensionDp(spacedBy, behavior, density)
    val maxItems = if (layout.mMaxItemsInEachRow > 0) layout.mMaxItemsInEachRow else Int.MAX_VALUE
    FlowRow(
        modifier = modifier,
        horizontalArrangement =
            rowHorizontalArrangement(layout.horizontalPositioning, spacedBy, behavior, density),
        verticalArrangement = if (spacedBy > 0f) Arrangement.spacedBy(gap) else Arrangement.Top,
        maxItemsInEachRow = maxItems,
    ) {
        RcPlayerChildren(layout)
    }
}
