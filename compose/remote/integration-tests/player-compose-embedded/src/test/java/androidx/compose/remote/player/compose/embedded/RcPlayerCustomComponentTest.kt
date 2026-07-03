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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Custom (host-extension) components: the document declares a `Custom` component with a
 * config string + typed properties, and the host's `customContent` composable renders it (the
 * embedded equivalent of the View player's setCustomSupport / CustomContext).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCustomComponentTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun customComponentRendersHostContentWithResolvedProperties() {
        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "test:badge",
                            listOf(
                                Custom.CustomProperty(
                                    1.toShort(),
                                    Custom.CustomProperty.FLOAT_PROP,
                                    42f,
                                ),
                                Custom.CustomProperty(2.toShort(), Custom.CustomProperty.INT_PROP, 7),
                            ),
                        )
                        writer.endCustom()
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(
                    document = document,
                    autoUpdate = false,
                    customContent = { custom ->
                        BasicText(
                            "custom:${custom.config}:${custom.floats[1]?.toInt()}:${custom.ints[2]}"
                        )
                    },
                )
            }
        }
        rule.mainClock.advanceTimeBy(100)

        // The host content composed inside the Custom component, with the config name and both
        // properties resolved by type.
        rule.onNodeWithText("custom:test:badge:42:7").assertExists()
    }

    /**
     * Return channels flow host values back into the document: the host writes via
     * returnFloat/returnText and the bound document variable/text holds the value — available to
     * the rest of the document on the same recomposition, not a later frame.
     */
    @Test
    fun customReturnChannelsWriteBackIntoTheDocument() {
        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                val floatTargetId = writer.addNamedFloat("returnTarget", 0f)
                val textTargetId = writer.textCreateId("")
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "test:return",
                            listOf(
                                Custom.CustomProperty(
                                    1.toShort(),
                                    Custom.CustomProperty.FLOAT_RETURN,
                                    floatTargetId,
                                ),
                                Custom.CustomProperty(
                                    2.toShort(),
                                    Custom.CustomProperty.TEXT_RETURN,
                                    textTargetId,
                                ),
                            ),
                        )
                        writer.endCustom()
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(
                    document = document,
                    autoUpdate = false,
                    customContent = { custom ->
                        custom.returnFloat(1, 77f)
                        custom.returnText(2, "from-host")
                        BasicText("returned")
                    },
                )
            }
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("returned").assertExists()

        // The write-back landed in the document store, at the ids the Custom op declared.
        val custom = requireNotNull(findCustom(document.getOperationsReflection()))
        val props = custom.readData().properties as List<Custom.CustomProperty>
        val floatId =
            Utils.idFromNan(props.first { it.mDataType == Custom.CustomProperty.FLOAT_RETURN }.mFloatValue)
        val textId = props.first { it.mDataType == Custom.CustomProperty.TEXT_RETURN }.mIntValue
        assertThat(document.remoteComposeState.getFloat(floatId)).isEqualTo(77f)
        assertThat(document.remoteComposeState.getFromId(textId)).isEqualTo("from-host")
    }

    private fun findCustom(operations: Collection<Operation>): Custom? {
        for (op in operations) {
            if (op is Custom) return op
            if (op is Container) findCustom(op.list)?.let { return it }
        }
        return null
    }
}
