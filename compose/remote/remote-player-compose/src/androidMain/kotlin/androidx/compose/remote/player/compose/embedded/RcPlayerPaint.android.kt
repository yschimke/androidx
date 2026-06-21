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

import androidx.compose.remote.core.MatrixAccess
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.RemoteReadContext
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap

/** Maps a packed tile-mode index to a framework [android.graphics.Shader.TileMode]. */
private fun nativeTileMode(index: Int): android.graphics.Shader.TileMode =
    when (index) {
        1 -> android.graphics.Shader.TileMode.REPEAT
        2 -> android.graphics.Shader.TileMode.MIRROR
        else -> android.graphics.Shader.TileMode.CLAMP
    }

/** Wraps a framework [android.graphics.Shader] as a Compose [Brush] for the DrawScope paint path. */
private fun nativeShaderBrush(shader: android.graphics.Shader): Brush =
    object : ShaderBrush() {
        override fun createShader(size: Size): android.graphics.Shader = shader
    }

/**
 * Builds the AGSL [android.graphics.RuntimeShader] for a `SHADER` op (from a [ShaderData], with its
 * float/int/bitmap uniforms applied), mirroring the View player's `AndroidPaintContext.setShader`.
 * Returns null for id 0, missing data/text, or below API 33 (RuntimeShader is API 33+), so the
 * caller falls back to the solid color.
 */
private fun buildRuntimeShader(
    shaderId: Int,
    remoteContext: RemoteContext,
): android.graphics.Shader? {
    if (shaderId == 0) return null
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return null
    val data = remoteContext.mRemoteComposeState.getFromId(shaderId) as? ShaderData ?: return null
    val text = remoteContext.getText(data.shaderTextId) ?: return null
    return try {
        val shader = android.graphics.RuntimeShader(text)
        for (name in data.uniformFloatNames) {
            shader.setFloatUniform(name, data.getUniformFloats(name))
        }
        for (name in data.uniformIntegerNames) {
            shader.setIntUniform(name, data.getUniformInts(name))
        }
        for (name in data.uniformBitmapNames) {
            val bitmap = resolveBitmap(remoteContext, data.getUniformBitmapId(name))?.asAndroidBitmap()
            if (bitmap != null) {
                shader.setInputShader(
                    name,
                    android.graphics.BitmapShader(
                        bitmap,
                        android.graphics.Shader.TileMode.CLAMP,
                        android.graphics.Shader.TileMode.CLAMP,
                    ),
                )
            }
        }
        shader
    } catch (e: RuntimeException) {
        null
    }
}

internal actual fun ComposeLocalPaint.applyPlatformShader(
    shaderId: Int,
    remoteContext: RemoteContext,
) {
    val shader = buildRuntimeShader(shaderId, remoteContext)
    platformShader = shader
    brush = shader?.let { nativeShaderBrush(it) }
}

internal actual fun ComposeLocalPaint.applyPlatformTexture(
    bitmapId: Int,
    tileModes: Int,
    remoteContext: RemoteContext,
) {
    val bitmap = resolveBitmap(remoteContext, bitmapId)?.asAndroidBitmap()
    val shader =
        bitmap?.let {
            android.graphics.BitmapShader(
                it,
                nativeTileMode(tileModes and 0xF),
                nativeTileMode((tileModes shr 16) and 0xF),
            )
        }
    platformShader = shader
    brush = shader?.let { nativeShaderBrush(it) }
}

internal actual fun ComposeLocalPaint.applyPlatformShaderMatrix(
    matrixWord: Int,
    read: RemoteReadContext,
) {
    val shader = platformShader as? android.graphics.Shader ?: return
    val id = Utils.idFromNan(Float.fromBits(matrixWord))
    if (id == 0) {
        shader.setLocalMatrix(null)
        return
    }
    val matrix = read.getObject(id) as? MatrixAccess ?: return
    val values = matrix.get()
    val m3x3 =
        when (values.size) {
            9 -> values
            16 ->
                floatArrayOf(
                    values[0],
                    values[1],
                    values[3],
                    values[4],
                    values[5],
                    values[7],
                    values[8],
                    values[9],
                    values[15],
                )
            else -> return
        }
    shader.setLocalMatrix(android.graphics.Matrix().apply { setValues(m3x3) })
}
