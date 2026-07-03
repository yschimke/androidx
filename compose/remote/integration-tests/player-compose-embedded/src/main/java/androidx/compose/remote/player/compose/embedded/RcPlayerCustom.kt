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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * A `Custom` (host-extension) component from a document: a config name plus its resolved
 * properties, keyed by property type. `Custom` components have no built-in rendering — the host
 * app supplies it, dispatched by [config]. This is the embedded-player equivalent of the View
 * player's `setCustomSupport`/`CustomContext`, surfaced as a Compose callback ([RcPlayer]'s
 * `customContent`).
 *
 * Properties are exposed by their property `type` (the author-assigned key): [floats] resolved
 * against the document's variable store (so a variable-backed float is its live value), [ints] and
 * [texts] as authored. String/text properties resolve via the text id carried in the property.
 *
 * Return-channel properties (the core's `FLOAT_RETURN`/`TEXT_RETURN`) flow the other way: the host
 * computes a value and writes it back into the document so other ops react. Their property types
 * are listed in [floatReturns]/[textReturns]; the host writes via [returnFloat]/[returnText]. (For
 * convenience the current stored value of each return channel is also present in
 * [floats]/[texts].)
 */
public class RcCustomComponent
internal constructor(
    public val config: String,
    public val componentId: Int,
    public val floats: Map<Int, Float>,
    public val ints: Map<Int, Int>,
    public val texts: Map<Int, String>,
    public val floatReturns: Set<Int> = emptySet(),
    public val textReturns: Set<Int> = emptySet(),
    private val onReturnFloat: (type: Int, value: Float) -> Unit = { _, _ -> },
    private val onReturnText: (type: Int, value: String) -> Unit = { _, _ -> },
) {
    /** Write [value] back to the document variable bound to the [type] `FLOAT_RETURN` property. */
    public fun returnFloat(type: Int, value: Float) {
        onReturnFloat(type, value)
    }

    /** Write [value] back to the document text bound to the [type] `TEXT_RETURN` property. */
    public fun returnText(type: Int, value: String) {
        onReturnText(type, value)
    }
}

/**
 * App-provided renderer for [Custom] components, dispatched by [RcCustomComponent.config].
 * Defaults to nothing (a Custom component renders empty unless the host supplies content).
 * Provided by [RcPlayer]'s `customContent` parameter.
 */
internal val LocalRcCustomContent:
    ProvidableCompositionLocal<@Composable (RcCustomComponent) -> Unit> =
    compositionLocalOf {
        {}
    }

/** Renders a [Custom] component by delegating to the host's [LocalRcCustomContent] composable. */
@Composable
internal fun RcPlayerCustom(layout: Custom, modifier: Modifier) {
    val remoteContext = LocalRemoteContext.current
    val content = LocalRcCustomContent.current

    val data = layout.readData()
    val config =
        if (data.configId != -1) remoteContext.getText(data.configId) ?: "" else data.config ?: ""

    val floats = HashMap<Int, Float>()
    val ints = HashMap<Int, Int>()
    val texts = HashMap<Int, String>()
    // Return channels: property type -> the document variable/text id the host writes back to.
    val floatReturnTargets = HashMap<Int, Int>()
    val textReturnTargets = HashMap<Int, Int>()
    @Suppress("UNCHECKED_CAST")
    val properties = data.properties as? List<Custom.CustomProperty> ?: emptyList()
    for (prop in properties) {
        val type = prop.mType.toInt()
        when (prop.mDataType) {
            // FLOAT_RETURN: mFloatValue is a NaN-encoded id; host writes back there. Surface the
            // current stored value too.
            Custom.CustomProperty.FLOAT_RETURN -> {
                floats[type] = resolveFloat(prop.mFloatValue, prop.mFloatValue, remoteContext)
                floatReturnTargets[type] = Utils.idFromNan(prop.mFloatValue)
            }
            // TEXT_RETURN: mIntValue is the target text id; host writes back there.
            Custom.CustomProperty.TEXT_RETURN -> {
                texts[type] = remoteContext.getText(prop.mIntValue) ?: ""
                textReturnTargets[type] = prop.mIntValue
            }
            else ->
                when {
                    // Resolve a variable-backed float to its live value; a literal stays as
                    // authored.
                    prop.isFloat() ->
                        floats[type] =
                            resolveFloat(prop.mFloatValue, prop.mFloatValue, remoteContext)
                    // String/text properties carry a text id in mIntValue.
                    prop.isString() -> texts[type] = remoteContext.getText(prop.mIntValue) ?: ""
                    else -> ints[type] = prop.mIntValue
                }
        }
    }

    Box(modifier = modifier) {
        content(
            RcCustomComponent(
                config = config,
                componentId = layout.componentId,
                floats = floats,
                ints = ints,
                texts = texts,
                floatReturns = floatReturnTargets.keys,
                textReturns = textReturnTargets.keys,
                onReturnFloat = { type, value ->
                    floatReturnTargets[type]?.let { remoteContext.overrideFloat(it, value) }
                },
                onReturnText = { type, value ->
                    textReturnTargets[type]?.let { remoteContext.loadText(it, value) }
                },
            )
        )
    }
}
