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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.nio.ByteBuffer

internal actual fun decodePlayerBitmap(
    encoding: Short,
    type: Short,
    width: Int,
    height: Int,
    data: ByteArray,
): ImageBitmap? =
    when (encoding.toInt()) {
        // PNG (and other BitmapFactory-decodable formats).
        0 -> BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
        // Raw RGBA_8888 pixel buffer.
        1 ->
            if (width > 0 && height > 0) {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    .apply { copyPixelsFromBuffer(ByteBuffer.wrap(data)) }
                    .asImageBitmap()
            } else null
        else -> null
    }
