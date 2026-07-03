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
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.basicMarquee
import androidx.compose.remote.creation.compose.modifier.clearAndSetSemantics
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.text
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests for the semantics modifier (content description, text, role, CLEAR_AND_SET mode) and the
 * marquee modifier, asserted through the Compose semantics tree — no screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerSemanticsModifierTest {

    @get:Rule val rule = createComposeRule()

    private fun renderDocument(content: @androidx.compose.runtime.Composable () -> Unit) {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes = captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                CoreDocument().apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) {
                    RcPlayer(document = document, autoUpdate = false)
                }
            }
            rule.mainClock.advanceTimeBy(100)
        }
    }

    @Test
    fun semanticsRoleSurfacesToTheAccessibilityTree() {
        renderDocument {
            RemoteBox(
                modifier =
                    RemoteModifier.semantics {
                            contentDescription = "switchy".rs
                            role = Role.Switch
                        }
                        .size(40.rdp)
                        .background(Color(0xFF3F51B5))
            )
        }

        rule
            .onNodeWithContentDescription("switchy")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
    }

    @Test
    fun semanticsTextSurfacesToTheAccessibilityTree() {
        renderDocument {
            RemoteBox(
                modifier =
                    RemoteModifier.semantics { text = "labelled box".rs }
                        .size(40.rdp)
                        .background(Color(0xFF3F51B5))
            )
        }

        rule.onNodeWithText("labelled box").assertExists()
    }

    @Test
    fun clearAndSetSemanticsReplacesDescendantSemantics() {
        renderDocument {
            RemoteColumn(
                modifier =
                    RemoteModifier.clearAndSetSemantics { contentDescription = "outer".rs }
                        .size(100.rdp)
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "inner".rs }
                            .size(40.rdp)
                            .background(Color(0xFFAA0000))
                )
            }
        }

        rule.onNodeWithContentDescription("outer").assertExists()
        // CLEAR_AND_SET wipes descendant semantics, so the inner description must not surface.
        rule.onNodeWithContentDescription("inner").assertDoesNotExist()
    }

    @Test
    fun mergedSemanticsCombineDescendantsIntoOneNode() {
        renderDocument {
            RemoteColumn(
                modifier =
                    RemoteModifier.semantics(mergeDescendants = true) {
                            contentDescription = "outer".rs
                        }
                        .size(100.rdp)
            ) {
                RemoteText(text = "inner label")
            }
        }

        // MERGE folds the descendants' semantics into the parent node: the merged node carries
        // both the container description and the child text.
        rule
            .onNodeWithContentDescription("outer")
            .assert(hasText("inner label"))
    }

    /**
     * Marquee actually *moves*: two-colored content (red then blue) wider than the container
     * scrolls, so the sampled pixel band across the container changes between two frames 1s
     * apart. (Solid boxes, not text: glyphs don't rasterize under this Robolectric setup, and a
     * uniform color would show no change while scrolling.) Bitmaps are extracted only to compare
     * bands — no goldens.
     */
    /**
     * The marquee modifier measures its content unbounded and clips it to the container: the red
     * half of the 120dp content is visible in the 60dp container, the blue half is clipped out,
     * and nothing bleeds past the container edge — all on the first rendered frames. (Actual
     * scrolling is asserted on-device in RcPlayerMarqueeMotionTest: Compose's basicMarquee doesn't
     * animate under Robolectric's window.) Bitmaps are extracted only to sample pixels.
     */
    @Test
    fun marqueeContentRendersClippedToContainer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = runBlocking {
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        RemoteBox(modifier = RemoteModifier.size(60.rdp, 20.rdp).basicMarquee()) {
                            RemoteRow {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(60.rdp, 20.rdp)
                                            .background(Color(0xFFCC0000))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(60.rdp, 20.rdp)
                                            .background(Color(0xFF0000CC))
                                )
                            }
                        }
                    },
                )
                .bytes
        }
        val document =
            CoreDocument().apply {
                ByteArrayInputStream(bytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = document, autoUpdate = false)
            }
        }
        rule.mainClock.advanceTimeBy(64)

        val d = rule.density.density
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val y = (10 * d).toInt()
        val inside = bmp.getPixel((30 * d).toInt(), y)
        val pastEdge = bmp.getPixel((70 * d).toInt(), y)
        // Unscrolled: the red half fills the container.
        assertThat(android.graphics.Color.red(inside)).isGreaterThan(150)
        assertThat(android.graphics.Color.blue(inside)).isLessThan(90)
        // The overflowing blue half is clipped at the container edge: past it there is only the
        // (near-white) window background — neither saturated blue nor saturated red.
        val saturatedBlue =
            android.graphics.Color.blue(pastEdge) > 150 && android.graphics.Color.red(pastEdge) < 90
        val saturatedRed =
            android.graphics.Color.red(pastEdge) > 150 && android.graphics.Color.blue(pastEdge) < 90
        assertThat(saturatedBlue).isFalse()
        assertThat(saturatedRed).isFalse()
    }
}
