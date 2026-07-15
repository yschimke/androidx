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

@file:Suppress(
    "RestrictedApiAndroidX"
) // Referring to background, remote-player-core, remote-testing

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDensityBehavior
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.embedded.integration.previews.ExperimentalRemoteDocumentPreview
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImpl
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot coverage for the two-way experimental document previews: the same document rendered
 * through the production View player and the experimental embedded Compose player — each against
 * its own golden, plus a direct parity comparison between the two.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class ExperimentalPreviewScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    /** A deterministic document: static nested layout, solid colors, no text, no animation. */
    private fun sampleDocument(): RemoteDocument = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coreDoc =
            captureRule.captureDocument(
                context = context,
                content = {
                    RemoteColumn(modifier = RemoteModifier.size(120.rdp).background(Color.White)) {
                        RemoteRow(modifier = RemoteModifier.padding(8.rdp)) {
                            RemoteBox(
                                modifier = RemoteModifier.size(40.rdp).background(Color(0xFFAA2222))
                            )
                            RemoteBox(
                                modifier = RemoteModifier.size(40.rdp).background(Color(0xFF22AA22))
                            )
                        }
                        RemoteBox(
                            modifier =
                                RemoteModifier.padding(8.rdp)
                                    .size(88.rdp, 40.rdp)
                                    .background(Color(0xFF2222AA))
                        )
                    }
                },
            )
        RemoteDocument(coreDoc)
    }

    @Composable
    private fun PreviewUnderTest(document: RemoteDocument, impl: PlayerImpl, tag: String) {
        Box(modifier = Modifier.size(120.dp).testTag(tag)) {
            ExperimentalRemoteDocumentPreview(remoteDocument = document, playerImpl = impl)
        }
    }

    @Ignore("Disable until ScreenshotTestRule is fixed for applications")
    @Test
    fun documentPreviewThroughViewPlayer() {
        val document = sampleDocument()
        rule.setContent { PreviewUnderTest(document, PlayerImpl.JAVA, "view") }
        rule.waitForIdle()
        rule
            .onNodeWithTag("view")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "experimentalDocPreview_java")
    }

    @Ignore("Disable until ScreenshotTestRule is fixed for applications")
    @Test
    fun documentPreviewThroughEmbeddedPlayer() {
        val document = sampleDocument()
        rule.setContent { PreviewUnderTest(document, PlayerImpl.COMPOSE, "embedded") }
        rule.waitForIdle()
        rule
            .onNodeWithTag("embedded")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "experimentalDocPreview_compose")
    }

    /**
     * Parity: both players render the same document to (nearly) the same pixels. Golden-free — the
     * two captures are compared directly with MSSIM, so this guards embedded-vs-View drift even
     * before goldens exist.
     */
    @Test
    fun viewAndEmbeddedPlayersRenderTheDocumentTheSame() {
        val document = sampleDocument()
        rule.setContent { RemotePlayersSideBySide(document) }
        rule.waitForIdle()
        val viewBitmap = rule.onNodeWithTag("view").captureToImage().asAndroidBitmap()
        val embeddedBitmap = rule.onNodeWithTag("embedded").captureToImage().asAndroidBitmap()

        val viewPixels = IntArray(viewBitmap.width * viewBitmap.height)
        viewBitmap.getPixels(
            viewPixels,
            0,
            viewBitmap.width,
            0,
            0,
            viewBitmap.width,
            viewBitmap.height,
        )
        val embeddedPixels = IntArray(embeddedBitmap.width * embeddedBitmap.height)
        embeddedBitmap.getPixels(
            embeddedPixels,
            0,
            embeddedBitmap.width,
            0,
            0,
            embeddedBitmap.width,
            embeddedBitmap.height,
        )

        val result =
            MSSIMMatcher(threshold = 0.98)
                .compareBitmaps(viewPixels, embeddedPixels, viewBitmap.width, viewBitmap.height)
        assertTrue(
            "View and embedded player renders diverge: ${result.comparisonStatistics}",
            result.matches,
        )
    }

    @Composable
    private fun RemotePlayersSideBySide(document: RemoteDocument) {
        androidx.compose.foundation.layout.Column {
            PreviewUnderTest(document, PlayerImpl.JAVA, "view")
            PreviewUnderTest(document, PlayerImpl.COMPOSE, "embedded")
        }
    }

    /**
     * A document with multi-line text — exercises the text line-height path. The View player uses
     * the font's natural line spacing; the embedded Compose player must match it. Regression guard
     * for the RcPlayerText line-height fix (raw pixel font size was previously passed straight to
     * `.sp`, double-applying the display density and inflating line spacing ~2-3x).
     */
    private fun multilineTextDocument(): RemoteDocument = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coreDoc =
            captureRule.captureDocument(
                context = context,
                content = {
                    RemoteColumn(modifier = RemoteModifier.size(120.rdp).background(Color.White)) {
                        RemoteText(text = "Line one\nLine two\nLine three\nLine four")
                    }
                },
            )
        RemoteDocument(coreDoc)
    }

    /**
     * A card-like document using the same size modifier as Material RemoteCard —
     * `heightIn(min = 64.rdp).fillMaxWidth()`. The min height is a density-dependent value; the
     * embedded player must not double-apply the display density to it (which made cards ~2.6x too
     * tall). Captured under an explicit [behavior] so we can verify the constraint sizing honors
     * the document's `DENSITY_BEHAVIOR` header: remote-core folds density into the widthIn/heightIn
     * min/max per that header (via DimensionInModifierOperation.updateVariables), so the embedded
     * player divides getMin/getMax by density regardless of behavior and must match the View player.
     */
    private fun cardSizeDocument(behavior: RemoteDensityBehavior): RemoteDocument = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val metrics = context.resources.displayMetrics
        val displayInfo =
            RemoteCreationDisplayInfo(
                width = metrics.widthPixels,
                height = metrics.heightPixels,
                densityDpi = metrics.densityDpi,
                densityBehavior = behavior,
            )
        val coreDoc =
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = displayInfo,
                content = {
                    RemoteColumn(modifier = RemoteModifier.size(200.rdp).background(Color.White)) {
                        RemoteColumn(
                            modifier =
                                RemoteModifier.heightIn(min = 64.rdp)
                                    .fillMaxWidth()
                                    .background(Color(0xFFCCCCCC))
                                    .padding(8.rdp)
                        ) {
                            RemoteText(text = "This is a basic card")
                        }
                    }
                },
            )
        RemoteDocument(coreDoc)
    }

    /** A button-like document using RemoteButton's size modifier: heightIn(min).widthIn(min). */
    private fun buttonSizeDocument(): RemoteDocument = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coreDoc =
            captureRule.captureDocument(
                context = context,
                content = {
                    RemoteColumn(modifier = RemoteModifier.size(220.rdp).background(Color.White)) {
                        RemoteColumn(
                            modifier =
                                RemoteModifier.heightIn(min = 52.rdp)
                                    .widthIn(min = 12.rdp)
                                    .background(Color(0xFFCCCCCC))
                                    .padding(16.rdp)
                        ) {
                            RemoteText(text = "Button")
                        }
                    }
                },
            )
        RemoteDocument(coreDoc)
    }

    @Test
    fun viewAndEmbeddedPlayersRenderButtonSizeTheSame() {
        val document = buttonSizeDocument()
        rule.setContent { RemotePlayersSideBySide(document) }
        rule.waitForIdle()
        val viewBitmap = rule.onNodeWithTag("view").captureToImage().asAndroidBitmap()
        val embeddedBitmap = rule.onNodeWithTag("embedded").captureToImage().asAndroidBitmap()

        val viewPixels = IntArray(viewBitmap.width * viewBitmap.height)
        viewBitmap.getPixels(
            viewPixels, 0, viewBitmap.width, 0, 0, viewBitmap.width, viewBitmap.height)
        val embeddedPixels = IntArray(embeddedBitmap.width * embeddedBitmap.height)
        embeddedBitmap.getPixels(
            embeddedPixels, 0, embeddedBitmap.width, 0, 0, embeddedBitmap.width, embeddedBitmap.height)

        val result =
            MSSIMMatcher(threshold = 0.98)
                .compareBitmaps(viewPixels, embeddedPixels, viewBitmap.width, viewBitmap.height)
        assertTrue(
            "View and embedded player button-size renders diverge: ${result.comparisonStatistics}",
            result.matches,
        )
    }

    private fun assertPlayersRenderSame(document: RemoteDocument, message: String) {
        rule.setContent { RemotePlayersSideBySide(document) }
        rule.waitForIdle()
        val viewBitmap = rule.onNodeWithTag("view").captureToImage().asAndroidBitmap()
        val embeddedBitmap = rule.onNodeWithTag("embedded").captureToImage().asAndroidBitmap()

        val viewPixels = IntArray(viewBitmap.width * viewBitmap.height)
        viewBitmap.getPixels(
            viewPixels, 0, viewBitmap.width, 0, 0, viewBitmap.width, viewBitmap.height)
        val embeddedPixels = IntArray(embeddedBitmap.width * embeddedBitmap.height)
        embeddedBitmap.getPixels(
            embeddedPixels, 0, embeddedBitmap.width, 0, 0, embeddedBitmap.width, embeddedBitmap.height)

        val result =
            MSSIMMatcher(threshold = 0.98)
                .compareBitmaps(viewPixels, embeddedPixels, viewBitmap.width, viewBitmap.height)
        assertTrue("$message: ${result.comparisonStatistics}", result.matches)
    }

    @Test
    fun viewAndEmbeddedPlayersRenderCardSizeTheSame_legacyBehavior() {
        assertPlayersRenderSame(
            cardSizeDocument(RemoteDensityBehavior.Legacy),
            "card-size renders diverge (LEGACY density behavior)",
        )
    }

    @Test
    fun viewAndEmbeddedPlayersRenderCardSizeTheSame_dpBehavior() {
        assertPlayersRenderSame(
            cardSizeDocument(RemoteDensityBehavior.Dp),
            "card-size renders diverge (DP density behavior)",
        )
    }

    @Test
    fun viewAndEmbeddedPlayersRenderCardSizeTheSame_pixelsBehavior() {
        assertPlayersRenderSame(
            cardSizeDocument(RemoteDensityBehavior.Pixels),
            "card-size renders diverge (PIXELS density behavior)",
        )
    }

    @Test
    fun viewAndEmbeddedPlayersRenderMultilineTextTheSame() {
        val document = multilineTextDocument()
        rule.setContent { RemotePlayersSideBySide(document) }
        rule.waitForIdle()
        val viewBitmap = rule.onNodeWithTag("view").captureToImage().asAndroidBitmap()
        val embeddedBitmap = rule.onNodeWithTag("embedded").captureToImage().asAndroidBitmap()

        val viewPixels = IntArray(viewBitmap.width * viewBitmap.height)
        viewBitmap.getPixels(
            viewPixels,
            0,
            viewBitmap.width,
            0,
            0,
            viewBitmap.width,
            viewBitmap.height,
        )
        val embeddedPixels = IntArray(embeddedBitmap.width * embeddedBitmap.height)
        embeddedBitmap.getPixels(
            embeddedPixels,
            0,
            embeddedBitmap.width,
            0,
            0,
            embeddedBitmap.width,
            embeddedBitmap.height,
        )

        val result =
            MSSIMMatcher(threshold = 0.98)
                .compareBitmaps(viewPixels, embeddedPixels, viewBitmap.width, viewBitmap.height)
        assertTrue(
            "View and embedded player text renders diverge: ${result.comparisonStatistics}",
            result.matches,
        )
    }
}
