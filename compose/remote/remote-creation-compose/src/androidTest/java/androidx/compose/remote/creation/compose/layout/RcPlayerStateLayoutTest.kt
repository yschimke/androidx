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

package androidx.compose.remote.creation.compose.layout

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteEnum
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerStateLayoutTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun captureDocument(content: @Composable @RemoteComposable () -> Unit): CoreDocument {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return runBlocking {
            val captured = captureSingleRemoteDocument(context, content = content)
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(captured.bytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        }
    }

    @Test
    fun update() {
        // We need to capture the document with the INITIAL state!
        // And then we can interact with it in the player!
        // Wait! In the original test, `rememberMutableRemoteEnum` was called INSIDE
        // `testRule.setContent`!
        // And that means it was part of the composition!
        // If I capture it, I only capture the INITIAL state!
        // And I need to simulate the click in the player!
        // And `ValueChange` action should update the state in the player!
        // And that should trigger recomposition in the player!
        // Yes! That should work!

        val document = captureDocument {
            val currentState = rememberMutableRemoteEnum(LayoutState.First)
            NestedStateLayout("one".rs, currentState = currentState) { state ->
                RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                    RemoteText("State $state".rs, color = Color.Black.rc)
                    RemoteText(
                        "Switch".rs,
                        color = Color.Black.rc,
                        modifier =
                            RemoteModifier.clickable(
                                ValueChange(
                                    currentState,
                                    RemoteEnum(
                                        if (state == LayoutState.First) LayoutState.Second
                                        else LayoutState.First
                                    ),
                                )
                            ),
                    )
                }
            }
        }

        composeTestRule.setContent { RcPlayer(document, autoUpdate = false) }

        // Assert initial state
        composeTestRule.onNodeWithText("State First").assertExists()

        // Click to switch state
        composeTestRule.onNodeWithText("Switch").performClick()

        // Assert updated state
        composeTestRule.onNodeWithText("State Second").assertExists()
    }

    @Composable
    @RemoteComposable
    private fun NestedStateLayout(
        label: RemoteString,
        currentState: RemoteEnum<LayoutState> = rememberMutableRemoteEnum(LayoutState.First),
        content: @Composable @RemoteComposable (LayoutState) -> Unit,
    ) {
        RemoteStateLayout(
            state = currentState,
            modifier = RemoteModifier.fillMaxSize().background(Color.LightGray.rc),
        ) { layoutState ->
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color.Blue.rc).padding(10.rdp)
            ) {
                RemoteText(label + " / " + layoutState.toString(), color = Color.Black.rc)
                when (layoutState) {
                    LayoutState.First -> {
                        content(layoutState)
                    }

                    LayoutState.Second -> {
                        content(layoutState)
                    }
                }
            }
        }
    }

    private enum class LayoutState {
        First,
        Second,
    }
}
