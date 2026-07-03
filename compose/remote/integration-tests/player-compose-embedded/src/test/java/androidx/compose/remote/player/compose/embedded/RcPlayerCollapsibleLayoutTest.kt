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

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for collapsible layouts: when children overflow the main axis, the lowest
 * `CollapsiblePriority` child collapses first (the decision reuses core's priority ordering — see
 * RcPlayerCollapsibleLayout), not simply the last child in document order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCollapsibleLayoutTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun collapsesLowestPriorityChildFirst() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // A 100dp-high collapsible column with three 40dp children: A (priority 10),
            // B (priority 1), C (priority 5). Only two fit. Core semantics keep the two
            // highest-priority children (A and C) and collapse B — document-order dropping
            // would instead keep A and B.
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteCollapsibleColumn(
                                modifier = RemoteModifier.size(100.rdp, 100.rdp)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "A".rs }
                                            .priority(10f)
                                            .size(100.rdp, 40.rdp)
                                            .background(Color(0xFFAA0000))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "B".rs }
                                            .priority(1f)
                                            .size(100.rdp, 40.rdp)
                                            .background(Color(0xFF00AA00))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "C".rs }
                                            .priority(5f)
                                            .size(100.rdp, 40.rdp)
                                            .background(Color(0xFF0000AA))
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument().apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, autoUpdate = false)
                }
            }
            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithContentDescription("A").assertIsDisplayed()
            rule.onNodeWithContentDescription("C").assertIsDisplayed()
            // B has the lowest collapsible priority, so it is the one collapsed (measured but
            // never placed — it stays in the raw semantics tree, but is not displayed and not
            // hit-testable).
            rule.onNodeWithContentDescription("B").assertIsNotDisplayed()
        }
    }

    @Test
    fun keepsAllChildrenWhenTheyFit() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteCollapsibleColumn(
                                modifier = RemoteModifier.size(100.rdp, 100.rdp)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "A".rs }
                                            .priority(2f)
                                            .size(100.rdp, 40.rdp)
                                            .background(Color(0xFFAA0000))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "B".rs }
                                            .priority(1f)
                                            .size(100.rdp, 40.rdp)
                                            .background(Color(0xFF00AA00))
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument().apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, autoUpdate = false)
                }
            }
            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithContentDescription("A").assertIsDisplayed()
            rule.onNodeWithContentDescription("B").assertIsDisplayed()
        }
    }

    /**
     * Row variant of the priority collapse, with placement assertions: survivors compact in
     * document order — C sits where B would have been, on the very first frame (no settle time).
     */
    @Test
    fun collapsibleRowCollapsesLowestPriorityAndCompactsPlacement() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteCollapsibleRow(modifier = RemoteModifier.size(100.rdp, 100.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "A".rs }
                                            .priority(10f)
                                            .size(40.rdp, 100.rdp)
                                            .background(Color(0xFFAA0000))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "B".rs }
                                            .priority(1f)
                                            .size(40.rdp, 100.rdp)
                                            .background(Color(0xFF00AA00))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "C".rs }
                                            .priority(5f)
                                            .size(40.rdp, 100.rdp)
                                            .background(Color(0xFF0000AA))
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument().apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, autoUpdate = false)
                }
            }

            // Assertions run against the first rendered frame.
            rule.onNodeWithContentDescription("A").assertIsDisplayed()
            rule.onNodeWithContentDescription("C").assertIsDisplayed()
            rule.onNodeWithContentDescription("B").assertIsNotDisplayed()

            val aBounds = rule.onNodeWithContentDescription("A").getUnclippedBoundsInRoot()
            val cBounds = rule.onNodeWithContentDescription("C").getUnclippedBoundsInRoot()
            assertThat(aBounds.left.value).isEqualTo(0f)
            // C compacts into B's slot, directly after A — not at its document-order offset (80).
            assertThat(cBounds.left.value).isEqualTo(40f)
        }
    }
}
