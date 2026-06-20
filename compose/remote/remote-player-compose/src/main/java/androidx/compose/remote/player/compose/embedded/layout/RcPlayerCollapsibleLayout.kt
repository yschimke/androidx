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

import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.roundToInt

// CollapsiblePriorityModifierOperation.orientation values (mirrors CollapsiblePriority).
private const val ORIENTATION_HORIZONTAL = 0
private const val ORIENTATION_VERTICAL = 1

@Composable
internal fun RcPlayerCollapsibleColumn(layout: CollapsibleColumnLayout, modifier: Modifier) =
    // CollapsibleColumnLayout inherits ColumnLayout's `mSpacedBy`. Resolve it (it may be a NaN-encoded
    // variable/expression) to the pixel gap the custom Layout consumes.
    RcPlayerCollapsible(
        layout = layout,
        modifier = modifier,
        vertical = true,
        spacing = rememberRemoteFloatAsState(columnSpacedBy(layout)).value,
        orientation = ORIENTATION_VERTICAL,
    )

@Composable
internal fun RcPlayerCollapsibleRow(layout: CollapsibleRowLayout, modifier: Modifier) =
    RcPlayerCollapsible(
        layout = layout,
        modifier = modifier,
        vertical = false,
        spacing = rememberRemoteFloatAsState(rowSpacedBy(layout)).value,
        orientation = ORIENTATION_HORIZONTAL,
    )

/**
 * Renders a collapsible row/column: lay children out along the main axis and, when they don't all
 * fit in the available main-axis space, drop ("collapse") children — lowest [collapsible priority]
 * first — until the rest fit. Children without a priority modifier (for this orientation) default to
 * `Float.MAX_VALUE`, i.e. they're never the first to go and the highest-priority one is the "last
 * standing", matching `CollapsiblePriority` in remote-core. Survivors are placed in document order.
 *
 * Implemented as a custom [Layout] because Compose owns measure/layout in the embedded player —
 * there is no core layout pass to consult for child visibility. This is an approximation of the
 * core's collapse algorithm (greedy lowest-priority drop against the measured main-axis budget); the
 * exact intrinsic-size / last-standing tie-breaking edge cases are not all reproduced.
 *
 * [CollapsiblePriorityModifierOperation]: see remote-core.
 */
@Composable
private fun RcPlayerCollapsible(
    layout: LayoutComponent,
    modifier: Modifier,
    vertical: Boolean,
    spacing: Float,
    orientation: Int,
) {
    Layout(content = { RcPlayerChildren(layout) }, modifier = modifier) { measurables, constraints ->
        // `spacing` is the resolved pixel gap (remote-core's spacedBy resolves to px); use directly.
        val spacingPx = spacing.roundToInt().coerceAtLeast(0)
        // Measure each child with the cross axis bounded by the container and the main axis free.
        val childConstraints =
            if (vertical) constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
            else constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        val placeables = measurables.map { it.measure(childConstraints) }
        val n = placeables.size

        fun mainSize(p: Placeable) = if (vertical) p.height else p.width
        fun crossSize(p: Placeable) = if (vertical) p.width else p.height

        val available =
            if (vertical) {
                if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
            } else {
                if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
            }

        // Priority per child (measurables align 1:1 with children in document order). Default is
        // Float.MAX_VALUE — never collapsed before an explicitly-prioritized sibling.
        val children = (layout as? LayoutComponent)?.childrenComponents
        val priorities =
            FloatArray(n) { i ->
                val child = children?.getOrNull(i) as? LayoutComponent
                val priority =
                    child?.selfOrModifier(CollapsiblePriorityModifierOperation::class.java)
                if (priority != null && priority.orientation == orientation) priority.priority
                else Float.MAX_VALUE
            }

        val kept = BooleanArray(n) { true }
        fun usedMain(): Int {
            var total = 0
            var visible = 0
            for (i in 0 until n) {
                if (kept[i]) {
                    total += mainSize(placeables[i])
                    visible++
                }
            }
            if (visible > 1) total += spacingPx * (visible - 1)
            return total
        }
        fun keptCount() = kept.count { it }

        // Greedily collapse the lowest-priority visible child until the rest fit (always keep >= 1).
        // Ties resolve to the later child, so earlier (document-order) children survive.
        while (usedMain() > available && keptCount() > 1) {
            var dropIndex = -1
            var lowest = Float.POSITIVE_INFINITY
            for (i in 0 until n) {
                if (kept[i] && priorities[i] <= lowest) {
                    lowest = priorities[i]
                    dropIndex = i
                }
            }
            if (dropIndex < 0) break
            kept[dropIndex] = false
        }

        val mainExtent = usedMain().coerceAtMost(available)
        var crossExtent = 0
        for (i in 0 until n) if (kept[i]) crossExtent = maxOf(crossExtent, crossSize(placeables[i]))

        val width =
            if (vertical) {
                if (constraints.hasBoundedWidth) constraints.maxWidth else crossExtent
            } else mainExtent
        val height =
            if (vertical) mainExtent
            else {
                if (constraints.hasBoundedHeight) constraints.maxHeight else crossExtent
            }

        layout(constraints.constrainWidth(width), constraints.constrainHeight(height)) {
            var pos = 0
            for (i in 0 until n) {
                if (!kept[i]) continue
                if (vertical) placeables[i].placeRelative(0, pos)
                else placeables[i].placeRelative(pos, 0)
                pos += mainSize(placeables[i]) + spacingPx
            }
        }
    }
}
