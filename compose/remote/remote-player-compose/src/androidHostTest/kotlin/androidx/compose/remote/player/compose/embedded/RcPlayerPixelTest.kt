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

package androidx.compose.remote.player.compose.embedded

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.WriteToDocument
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel verification harness experiment. `captureToImage()` times out under Robolectric here
 * (forceRedraw never settles), so this tries rasterizing the laid-out view tree directly with
 * `View.draw(Canvas(bitmap))` under NATIVE graphics — no frame wait, no external screenshot library.
 * If [viewDrawCaptureWorks] passes, this unlocks pixel assertions for the embedded player's draw path.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerPixelTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun viewDrawCaptureWorks() {
        rule.setContent { androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Color.Red)) }
        rule.waitForIdle()

        val view = rule.activity.window.decorView
        val w = view.width.coerceAtLeast(1)
        val h = view.height.coerceAtLeast(1)
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        val center = bmp.getPixel(w / 2, h / 2)
        assert(android.graphics.Color.red(center) > 200) {
            "Expected red center, got #${Integer.toHexString(center)}"
        }
    }

    /** Renders [content] in a 100dp player box at top-start and rasterizes the content view. */
    private fun renderPlayerToBitmap(
        content: @Composable @RemoteComposable () -> Unit
    ): android.graphics.Bitmap {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bytes =
            kotlinx.coroutines.runBlocking {
                captureSingleRemoteDocument(context = ctx, content = content).bytes
            }
        val document =
            androidx.compose.remote.core
                .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                .apply {
                    ByteArrayInputStream(bytes).use {
                        initFromBuffer(
                            androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                        )
                    }
                }
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document, autoUpdate = false) }
        }
        rule.waitForIdle()
        val view = rule.activity.findViewById<android.view.View>(android.R.id.content)
        val bmp =
            android.graphics.Bitmap.createBitmap(
                view.width.coerceAtLeast(1),
                view.height.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888,
            )
        view.draw(android.graphics.Canvas(bmp))
        return bmp
    }

    private fun redPaint() =
        RemotePaint().apply { color = RemoteColor(Color.Red) }

    /** A solid red rect drawn by the embedded canvas path must read back red. */
    @Test
    fun solidRectRendersRed() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawRect(
                    paint = redPaint(),
                    topLeft = RemoteOffset(0f.rf, 0f.rf),
                    size = RemoteSize(100f.rf, 100f.rf),
                )
            }
        }
        val px = bmp.getPixel((50 * d).toInt(), (50 * d).toInt())
        assert(android.graphics.Color.red(px) > 200 && android.graphics.Color.green(px) < 60) {
            "Expected pure red, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Pixel-level proof that a bitmap decoded lazily (on first draw, not eagerly at composition)
     * still renders: draw a solid-red inline bitmap and read back red. Exercises the
     * `resolveBitmap` decode-on-draw path.
     */
    @Test
    fun lazilyDecodedBitmapRendersWhenDrawn() {
        val d = rule.density.density
        val red =
            android.graphics.Bitmap.createBitmap(
                    20,
                    20,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
                .apply { eraseColor(android.graphics.Color.RED) }
        val remoteBitmap = red.asImageBitmap().rb
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawImage(remoteBitmap, RemoteOffset(0f.rf, 0f.rf), null)
            }
        }
        val px = bmp.getPixel((10 * d).toInt(), (10 * d).toInt())
        assert(android.graphics.Color.red(px) > 200 && android.graphics.Color.green(px) < 60) {
            "Lazily-decoded bitmap should render red, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Pixel-level proof that DRAW_TO_BITMAP round-trips: draw a red rect into an offscreen bitmap,
     * restore the on-screen canvas, then blit that bitmap to screen. The center pixel must be red —
     * which only holds if the offscreen redirect actually captured the rect into the bitmap and the
     * blit drew it back.
     */
    @Test
    fun drawToOffscreenBitmapRoundTripsToScreen() {
        val d = rule.density.density
        val offscreen =
            android.graphics.Bitmap.createBitmap(
                    100,
                    100,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
                .asImageBitmap()
                .rb
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawToOffscreenBitmap(offscreen) {
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
                drawImage(offscreen, RemoteOffset(0f.rf, 0f.rf), null)
            }
        }
        val px = bmp.getPixel((50 * d).toInt(), (50 * d).toInt())
        assert(android.graphics.Color.red(px) > 200 && android.graphics.Color.green(px) < 60) {
            "Offscreen-rendered bitmap should blit red to screen, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Pixel-level proof that CLIP_RECT actually clips: clip to the top-left 50x50 quadrant then fill
     * the whole 100x100 canvas red. Inside the clip must be pure red; outside must NOT be red
     * (background shows through).
     */
    @Test
    fun clipRectActuallyClips() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                clipRect(0f.rf, 0f.rf, 50f.rf, 50f.rf) {
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
            }
        }
        val inside = bmp.getPixel((25 * d).toInt(), (25 * d).toInt())
        val outside = bmp.getPixel((75 * d).toInt(), (75 * d).toInt())
        val insideIsRed =
            android.graphics.Color.red(inside) > 200 && android.graphics.Color.green(inside) < 60
        val outsideIsRed =
            android.graphics.Color.red(outside) > 200 && android.graphics.Color.green(outside) < 60
        assert(insideIsRed) { "Inside clip should be red, got #${Integer.toHexString(inside)}" }
        assert(!outsideIsRed) {
            "Outside clip should NOT be red (clip leaked), got #${Integer.toHexString(outside)}"
        }
    }

    /**
     * Pixel check for a horizontal red->blue linear gradient drawn via a paint shader. Documents
     * whether the embedded player rasterizes the gradient (left red-dominant, right blue-dominant) —
     * i.e. whether `RemoteLinearShader` reaches the handled GRADIENT path or the discarded SHADER one.
     */
    @Test
    fun linearGradientPixels() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        shader =
                            androidx.compose.remote.creation.compose.shaders.RemoteLinearShader(
                                0f.rf,
                                0f.rf,
                                100f.rf,
                                0f.rf,
                                listOf(Color.Red.rc, Color.Blue.rc),
                                null,
                                androidx.compose.ui.graphics.TileMode.Clamp,
                            )
                    }
                drawRect(
                    paint = paint,
                    topLeft = RemoteOffset(0f.rf, 0f.rf),
                    size = RemoteSize(100f.rf, 100f.rf),
                )
            }
        }
        val y = (50 * d).toInt()
        val left = bmp.getPixel((10 * d).toInt(), y)
        val right = bmp.getPixel((90 * d).toInt(), y)
        assert(android.graphics.Color.red(left) > android.graphics.Color.blue(left)) {
            "gradient left should be red-dominant, got #${Integer.toHexString(left)}"
        }
        assert(android.graphics.Color.blue(right) > android.graphics.Color.red(right)) {
            "gradient right should be blue-dominant, got #${Integer.toHexString(right)}"
        }
    }

    /**
     * Pixel-level proof that CLIP_PATH clips: clip to a top-left 50x50 square path, then fill the
     * whole canvas red. Inside the path must be red; outside must not be.
     */
    @Test
    fun clipPathActuallyClips() {
        val d = rule.density.density
        val clipSquare = androidx.compose.remote.creation.RemotePath("M 0 0 L 50 0 L 50 50 L 0 50 Z")
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                clipPath(clipSquare) {
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
            }
        }
        val inside = bmp.getPixel((25 * d).toInt(), (25 * d).toInt())
        val outside = bmp.getPixel((75 * d).toInt(), (75 * d).toInt())
        val insideIsRed =
            android.graphics.Color.red(inside) > 200 && android.graphics.Color.green(inside) < 60
        val outsideIsRed =
            android.graphics.Color.red(outside) > 200 && android.graphics.Color.green(outside) < 60
        assert(insideIsRed) { "Inside clip path should be red, got #${Integer.toHexString(inside)}" }
        assert(!outsideIsRed) {
            "Outside clip path should NOT be red, got #${Integer.toHexString(outside)}"
        }
    }

    /**
     * Pixel-level proof that [androidx.compose.remote.core.operations.layout.LoopOperation] runs in
     * the embedded draw stream AND loads its index variable each iteration: a 3-iteration loop draws
     * a 20px-wide red stripe at `x = index * 30` (so stripes at 0, 30, 60). All three stripe centers
     * must be red and the gap between them must not — which only holds if the loop ran three times
     * with the index advancing.
     */
    @Test
    fun loopDrawsAStripePerIterationAtTheIndexPosition() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                loop(from = 0f.rf, until = 3f.rf, step = 1f.rf) { index ->
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(index * 30f, 0f.rf),
                        size = RemoteSize(20f.rf, 100f.rf),
                    )
                }
            }
        }
        fun isRed(x: Int): Boolean {
            val px = bmp.getPixel((x * d).toInt(), (50 * d).toInt())
            return android.graphics.Color.red(px) > 200 && android.graphics.Color.green(px) < 60
        }
        assert(isRed(10)) { "stripe 0 (index 0) should be red" }
        assert(isRed(40)) { "stripe 1 (index 1) should be red" }
        assert(isRed(70)) { "stripe 2 (index 2) should be red" }
        assert(!isRed(25)) { "gap between stripe 0 and 1 should NOT be red" }
    }

    /**
     * Proves the `WriteToDocument` escape hatch round-trips: author a red paint + full-canvas
     * `drawRect` directly against the raw [androidx.compose.remote.creation.RemoteComposeWriter] (no
     * typed DSL), capture, and render through the player. The center must read back red — which only
     * holds if the applied node emitted the raw ops at the right point in the document tree.
     */
    @Test
    fun writeToDocumentEmitsRawWriterOps() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            WriteToDocument(modifier = RemoteModifier.size(100.rdp)) { writer ->
                val bundle = androidx.compose.remote.core.operations.paint.PaintBundle()
                bundle.setColor(android.graphics.Color.RED)
                writer.buffer.addPaint(bundle)
                writer.drawRect(0f, 0f, 100f, 100f)
            }
        }
        val px = bmp.getPixel((50 * d).toInt(), (50 * d).toInt())
        assert(android.graphics.Color.red(px) > 200 && android.graphics.Color.green(px) < 60) {
            "WriteToDocument raw drawRect should render red, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Proves the border modifier honors a rounded-corner shape (`BorderModifierOperation.mShapeType`
     * / `mRoundedCorner`). A 100dp box with a 20dp-wide red border rounded at radius 40dp: the middle
     * of the top edge sits on the stroke (red), but the extreme corner is rounded away (empty) — which
     * a rectangular border would instead paint red.
     */
    @Test
    fun roundedBorderLeavesCornersEmpty() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .border(20.rdp, Color.Red.rc, RemoteRoundedCornerShape(40.rdp))
            )
        }
        fun isRed(x: Int, y: Int): Boolean {
            val px = bmp.getPixel((x * d).toInt(), (y * d).toInt())
            return android.graphics.Color.red(px) > 180 && android.graphics.Color.green(px) < 80
        }
        assert(isRed(50, 3)) { "top edge of the rounded border should be red" }
        assert(!isRed(3, 3)) { "rounded border corner should be empty, not red" }
    }
}

