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

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

internal actual fun decodePlayerBitmap(
    encoding: Short,
    type: Short,
    width: Int,
    height: Int,
    data: ByteArray,
): ImageBitmap? =
    try {
        when (encoding.toInt()) {
            // PNG (and other Skia-decodable encoded formats).
            0 -> Image.makeFromEncoded(data).toComposeImageBitmap()
            // Raw RGBA_8888 pixel buffer.
            1 ->
                if (width > 0 && height > 0) {
                    val info =
                        ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
                    Image.makeRaster(info, data, width * 4).toComposeImageBitmap()
                } else null
            else -> null
        }
    } catch (e: Throwable) {
        null
    }
