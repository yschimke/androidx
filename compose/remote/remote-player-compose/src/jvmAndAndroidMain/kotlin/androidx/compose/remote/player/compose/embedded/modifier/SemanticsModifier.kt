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

import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.AccessibleComponent.Role
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text

@Composable
internal fun Modifier.semantics(op: CoreSemantics): Modifier {
    val contentDescriptionString =
        op.getContentDescriptionId()?.let { rememberRemoteStringAsState(it).value }
    val textString = op.getTextId()?.let { rememberRemoteStringAsState(it).value }

    val properties: SemanticsPropertyReceiver.() -> Unit = {
        if (contentDescriptionString != null) {
            contentDescription = contentDescriptionString
        }

        if (textString != null) {
            this.text = androidx.compose.ui.text.AnnotatedString(textString)
        }

        if (op.role != null) {
            this.role = op.role!!.toComposeRole()
        }
    }
    return when (op.mMode) {
        AccessibleComponent.Mode.SET -> this.semantics(properties = properties)
        AccessibleComponent.Mode.CLEAR_AND_SET -> this.clearAndSetSemantics(properties = properties)
        AccessibleComponent.Mode.MERGE ->
            this.semantics(mergeDescendants = true, properties = properties)
    }
}

private fun Role.toComposeRole(): androidx.compose.ui.semantics.Role {
    return when (this) {
        Role.BUTTON -> androidx.compose.ui.semantics.Role.Button
        Role.CHECKBOX -> androidx.compose.ui.semantics.Role.Checkbox
        Role.SWITCH -> androidx.compose.ui.semantics.Role.Switch
        Role.RADIO_BUTTON -> androidx.compose.ui.semantics.Role.RadioButton
        Role.TAB -> androidx.compose.ui.semantics.Role.Tab
        Role.IMAGE -> androidx.compose.ui.semantics.Role.Image
        Role.DROPDOWN_LIST -> androidx.compose.ui.semantics.Role.DropdownList
        Role.PICKER -> androidx.compose.ui.semantics.Role.Button
        Role.CAROUSEL -> androidx.compose.ui.semantics.Role.Button
        else -> androidx.compose.ui.semantics.Role.Button
    }
}
