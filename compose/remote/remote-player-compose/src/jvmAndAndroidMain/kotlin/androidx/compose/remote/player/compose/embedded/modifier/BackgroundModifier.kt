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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteColorAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

@Composable
internal fun Modifier.background(op: BackgroundModifierOperation): Modifier {
    val color =
        if (op.mUseColorId) {
            rememberRemoteColorAsState(op.mColorId).value
        } else {
            val r = rememberRemoteFloatAsState(op.mRId).value
            val g = rememberRemoteFloatAsState(op.mGId).value
            val b = rememberRemoteFloatAsState(op.mBId).value
            val a = rememberRemoteFloatAsState(op.mAId).value
            Color(r, g, b, a)
        }

    val shape: Shape =
        when (op.mShapeType) {
            ShapeType.RECTANGLE -> RectangleShape
            ShapeType.CIRCLE -> CircleShape
            else -> RectangleShape // Default to rectangle for now
        }

    return this.background(color, shape)
}
