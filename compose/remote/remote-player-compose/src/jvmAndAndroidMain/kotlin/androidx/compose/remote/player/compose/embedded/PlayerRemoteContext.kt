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

import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.ScrollingEdgeEffect
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.types.LongConstant
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-agnostic [RemoteContext] for the embedded player. Ported from the View player's
 * `AndroidRemoteContext`: nearly every method just stores into / reads from the shared
 * `remote-core` state ([RemoteContext.mRemoteComposeState]) or the document ([mDocument]), which are
 * pure Kotlin/Java and run on any KMP target.
 *
 * The only platform-specific operation is decoding an embedded bitmap, which goes through the
 * [decodePlayerBitmap] expect/actual (Android `BitmapFactory` vs Skia on desktop). Edge-effect
 * overscroll glow is Android-only chrome and is not modelled here (returns null).
 *
 * The embedded player installs its reactive state by extending this with [GraphContext].
 */
internal open class PlayerRemoteContext(clock: RemoteClock) : RemoteContext(clock) {

    private class VarName(val name: String, val id: Int, val type: Int)

    private val varNames = HashMap<String, ArrayList<VarName>>()

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
        mRemoteComposeState.putPathData(instanceId, floatPath)
        mRemoteComposeState.putPathWinding(instanceId, winding)
    }

    override fun getPathData(instanceId: Int): FloatArray? =
        mRemoteComposeState.getPathData(instanceId)

    override fun getVariableId(name: String): Int {
        val list = varNames[name]
        if (list.isNullOrEmpty()) {
            throw NoSuchElementException("Variable $name not found")
        }
        return list[0].id
    }

    override fun loadVariableName(varName: String, varId: Int, varType: Int) {
        val list = varNames.getOrPut(varName) { ArrayList() }
        if (list.any { it.id == varId }) return
        list.add(VarName(varName, varId, varType))
    }

    override fun setNamedStringOverride(stringName: String, value: String) {
        varNames[stringName]?.forEach { overrideText(it.id, value) }
    }

    override fun clearNamedStringOverride(stringName: String) {
        varNames[stringName]?.forEach { mRemoteComposeState.clearDataOverride(it.id) }
    }

    override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
        setNamedIntegerOverride(booleanName, if (value) 1 else 0)
    }

    override fun clearNamedBooleanOverride(booleanName: String) {
        clearNamedIntegerOverride(booleanName)
    }

    override fun setNamedIntegerOverride(integerName: String, value: Int) {
        varNames[integerName]?.forEach { mRemoteComposeState.overrideInteger(it.id, value) }
    }

    override fun clearNamedIntegerOverride(integerName: String) {
        varNames[integerName]?.forEach { mRemoteComposeState.clearIntegerOverride(it.id) }
    }

    override fun setNamedFloatOverride(floatName: String, value: Float) {
        varNames[floatName]?.forEach { mRemoteComposeState.overrideFloat(it.id, value) }
    }

    override fun clearNamedFloatOverride(floatName: String) {
        varNames[floatName]?.forEach { mRemoteComposeState.clearFloatOverride(it.id) }
    }

    override fun setNamedLong(name: String, value: Long) {
        varNames[name]?.forEach {
            (mRemoteComposeState.getObject(it.id) as LongConstant).setValue(value)
        }
    }

    override fun setNamedDataOverride(dataName: String, value: Any) {
        varNames[dataName]?.forEach { mRemoteComposeState.overrideData(it.id, value) }
    }

    override fun clearNamedDataOverride(dataName: String) {
        varNames[dataName]?.forEach { mRemoteComposeState.clearDataOverride(it.id) }
    }

    override fun setNamedColorOverride(colorName: String, color: Int) {
        varNames[colorName]?.forEach { mRemoteComposeState.overrideColor(it.id, color) }
    }

    override fun addCollection(id: Int, collection: ArrayAccess) {
        mRemoteComposeState.addCollection(id, collection)
    }

    override fun putDataMap(id: Int, map: DataMap) {
        mRemoteComposeState.putDataMap(id, map)
    }

    override fun getDataMap(id: Int): DataMap? = mRemoteComposeState.getDataMap(id)

    override fun runAction(id: Int, metadata: String) {
        mDocument.performClick(this, id, metadata)
    }

    override fun runNamedAction(id: Int, value: Any?) {
        getText(id)?.let { mDocument.runNamedAction(it, value) }
    }

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        data: ByteArray,
    ) {
        if (!mRemoteComposeState.containsId(imageId)) {
            val image = decodePlayerBitmap(encoding, type, width, height, data)
            if (image != null) {
                mRemoteComposeState.cacheData(imageId, image)
            }
        }
    }

    override fun loadText(id: Int, text: String) {
        if (!mRemoteComposeState.containsId(id)) {
            mRemoteComposeState.cacheData(id, text)
        } else {
            mRemoteComposeState.updateData(id, text)
        }
    }

    fun overrideText(id: Int, text: String) {
        mRemoteComposeState.overrideData(id, text)
    }

    override fun getText(id: Int): String? = mRemoteComposeState.getFromId(id) as? String

    override fun loadFloat(id: Int, value: Float) {
        mRemoteComposeState.updateFloat(id, value)
    }

    override fun overrideFloat(id: Int, value: Float) {
        mRemoteComposeState.overrideFloat(id, value)
    }

    override fun loadInteger(id: Int, value: Int) {
        mRemoteComposeState.updateInteger(id, value)
    }

    override fun markVariableDirty(id: Int) {
        mRemoteComposeState.markVariableDirty(id)
    }

    override fun overrideInteger(id: Int, value: Int) {
        mRemoteComposeState.overrideInteger(id, value)
    }

    override fun overrideText(id: Int, valueId: Int) {
        overrideText(id, getText(valueId)!!)
    }

    override fun loadColor(id: Int, color: Int) {
        mRemoteComposeState.updateColor(id, color)
    }

    override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
        mRemoteComposeState.cacheData(id, animatedFloat)
    }

    override fun loadShader(id: Int, value: ShaderData) {
        mRemoteComposeState.cacheData(id, value)
    }

    override fun getFloat(id: Int): Float = mRemoteComposeState.getFloat(id).toFloat()

    override fun putObject(id: Int, value: Any) {
        mRemoteComposeState.updateObject(id, value)
    }

    override fun getObject(id: Int): Any? = mRemoteComposeState.getObject(id)

    override fun getInteger(id: Int): Int = mRemoteComposeState.getInteger(id)

    override fun getLong(id: Int): Long =
        (mRemoteComposeState.getObject(id) as LongConstant).getValue()

    override fun getColor(id: Int): Int = mRemoteComposeState.getColor(id)

    override fun listensTo(id: Int, variableSupport: VariableSupport) {
        mRemoteComposeState.listenToVar(id, variableSupport)
    }

    override fun getListeners(id: Int): ArrayList<VariableSupport>? =
        mRemoteComposeState.getListeners(id)

    override fun updateOps(): Int = mRemoteComposeState.getOpsToUpdate(this, currentTime)

    override fun getShader(id: Int): ShaderData? = mRemoteComposeState.getFromId(id) as? ShaderData

    override fun addTouchListener(touchExpression: androidx.compose.remote.core.TouchListener) {
        mDocument.addTouchListener(touchExpression)
    }

    override fun addClickArea(
        id: Int,
        contentDescriptionId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        metadataId: Int,
    ) {
        val contentDescription = mRemoteComposeState.getFromId(contentDescriptionId) as? String
        val metadata = mRemoteComposeState.getFromId(metadataId) as? String
        mDocument.addClickArea(id, contentDescription, left, top, right, bottom, metadata)
    }

    override fun hapticEffect(type: Int) {
        mDocument.haptic(type)
    }

    override fun createEdgeEffect(direction: Int): ScrollingEdgeEffect? = null
}

/**
 * Decode an embedded RemoteCompose bitmap into a Compose [ImageBitmap]. Android uses
 * `BitmapFactory` / raw-buffer copies; desktop (JVM) uses Skia. Returns null if the encoding/type
 * isn't supported on the platform.
 *
 * @param encoding how the data is encoded: 0 = png, 1 = raw, 2 = url
 * @param type the raw pixel layout: 0 = RGBA 8888, 1 = 888, 2 = 8-bit gray
 */
internal expect fun decodePlayerBitmap(
    encoding: Short,
    type: Short,
    width: Int,
    height: Int,
    data: ByteArray,
): ImageBitmap?
