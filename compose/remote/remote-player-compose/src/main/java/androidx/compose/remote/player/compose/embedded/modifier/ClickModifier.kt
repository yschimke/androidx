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

import androidx.compose.foundation.clickable
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.RunActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalRemoteActionHandler
import androidx.compose.remote.player.compose.embedded.LocalRemoteNamedActionHandler
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach

// HostNamedActionOperation's name/type/value-id are package-private in remote-core (left
// unchanged); read them reflectively.
private val hnaTextIdField =
    HostNamedActionOperation::class.java.getDeclaredField("mTextId").apply { isAccessible = true }
private val hnaTypeField =
    HostNamedActionOperation::class.java.getDeclaredField("mType").apply { isAccessible = true }
private val hnaValueIdField =
    HostNamedActionOperation::class.java.getDeclaredField("mValueId").apply { isAccessible = true }

@Composable
internal fun Modifier.click(op: ClickModifierOperation): Modifier {
    val coreDocument = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val onAction = LocalRemoteActionHandler.current
    val onNamedAction = LocalRemoteNamedActionHandler.current
    val contentDescription = op.contentDescriptionId?.let { rememberRemoteStringAsState(it).value }

    fun applyAction(action: Operation) {
        when (action) {
            is ValueIntegerChangeActionOperation ->
                remoteContext.overrideInteger(action.targetValueId, action.value)
            is ValueFloatChangeActionOperation ->
                remoteContext.overrideFloat(action.targetValueId, action.value)
            is ValueStringChangeActionOperation ->
                remoteContext.overrideText(action.targetValueId, action.valueId)
            is ValueIntegerExpressionChangeActionOperation -> {
                val targetId = Utils.idFromLong(action.targetValueId).toInt()
                val expressionId = Utils.idFromLong(action.valueExpressionId)
                coreDocument.evaluateIntExpression(expressionId, targetId, remoteContext)
            }
            is ValueFloatExpressionChangeActionOperation -> {
                val targetId = action.targetValueId
                val expressionId = action.valueExpressionId
                coreDocument.evaluateFloatExpression(expressionId, targetId, remoteContext)
            }
            // Host callback (id only; HostActionOperation carries no value).
            is HostActionOperation -> onAction(action.actionId, null)
            // Named host action (what the public hostAction(name, value) authors): resolve the name
            // and the typed value, then notify the host.
            is HostNamedActionOperation -> {
                val name = remoteContext.getText(hnaTextIdField.getInt(action)) ?: ""
                val valueId = hnaValueIdField.getInt(action)
                val value: Any? =
                    if (valueId == -1) {
                        null
                    } else {
                        when (hnaTypeField.getInt(action)) {
                            HostNamedActionOperation.FLOAT_TYPE -> remoteContext.getFloat(valueId)
                            HostNamedActionOperation.INT_TYPE -> remoteContext.getInteger(valueId)
                            HostNamedActionOperation.STRING_TYPE -> remoteContext.getText(valueId)
                            else -> null
                        }
                    }
                onNamedAction(name, value)
            }
            // A container of nested actions — run each.
            is RunActionOperation -> action.getList().fastForEach { applyAction(it) }
        }
    }

    return this.clickable(onClickLabel = contentDescription) {
        op.mList.fastForEach { applyAction(it) }
        coreDocument.updateVariables(
            remoteContext,
            androidx.compose.remote.core.operations.Theme.SYSTEM,
            coreDocument.mOperations,
        )
    }
}
