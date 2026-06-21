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

import androidx.compose.foundation.layout.Row
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.executeOperations
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun RcPlayerRow(layout: RowLayout, modifier: Modifier) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    // Resolve spacedBy (may be a NaN-encoded variable/expression) before scaling.
    val spacedBy = rememberRemoteFloatAsState(rowSpacedBy(layout)).value
    val drawModifier =
        if (layout.mDrawContentOperations != null) {
            Modifier.drawWithContent {
                executeOperations(
                    operations = layout.mDrawContentOperations!!.mList,
                    remoteContext = remoteContext,
                    onDrawContent = { drawContent() },
                    graph = graph,
                )
            }
        } else Modifier

    Row(
        modifier = modifier.then(drawModifier),
        horizontalArrangement =
            rowHorizontalArrangement(layout.horizontalPositioning, spacedBy, behavior, density),
        verticalAlignment = rowVerticalAlignment(layout.verticalPositioning),
    ) {
        RcPlayerChildren(layout) { child ->
            val mods =
                (child as? androidx.compose.remote.core.operations.layout.LayoutComponent)
                    ?.componentModifiers
                    ?.getList()
            var childModifier: Modifier = Modifier

            val weightOp =
                mods?.find {
                    it is
                        androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation &&
                        it.getType() ==
                            androidx.compose.remote.core.operations.layout.modifiers
                                .DimensionModifierOperation
                                .Type
                                .WEIGHT
                }
                    as?
                    androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
            if (weightOp != null) {
                childModifier = childModifier.weight(weightOp.getValue())
            }

            // AlignBy: align this child to the row's text baseline. `alignByBaseline()` is a RowScope
            // extension, available here because this lambda is lexically inside the Row content.
            val alignByOp =
                mods?.find {
                    it is
                        androidx.compose.remote.core.operations.layout.modifiers
                            .AlignByModifierOperation
                }
                    as?
                    androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
            if (alignByOp != null) {
                val line = alignByOp.getLine()
                if (androidx.compose.remote.core.operations.Utils.isVariable(line)) {
                    val id = androidx.compose.remote.core.operations.Utils.idFromNan(line)
                    if (
                        id ==
                            androidx.compose.remote.core.operations.layout.modifiers
                                .AlignByModifierOperation
                                .ID_FIRST_BASELINE ||
                            id ==
                                androidx.compose.remote.core.operations.layout.modifiers
                                    .AlignByModifierOperation
                                    .ID_LAST_BASELINE
                    ) {
                        childModifier = childModifier.alignByBaseline()
                    }
                }
            }
            childModifier
        }
    }
}
