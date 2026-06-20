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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.utilities.DataMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The read-only "value view" of a {@link RemoteContext}: the resolved value of a variable id, plus
 * the animation time. {@link RemoteContext} implements this, so any code that only *reads* document
 * state can be typed against this narrower contract rather than the full (read + write + host-effect
 * + lifecycle) context.
 *
 * <p>This is the first step of the RemoteContext read/write split (see the embedded player's
 * {@code GraphContext}, which is fundamentally a read view that evaluates computed ops reactively):
 * a value evaluator should not be able to mutate the store, and the type now says so. The
 * complementary write/host contracts and the op-signature migration that lets an evaluator avoid
 * subclassing the platform context are tracked separately.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteReadContext {
    /** @return the float value of variable [id]. */
    float getFloat(int id);

    /** @return the integer value of variable [id]. */
    int getInteger(int id);

    /** @return the long value of variable [id]. */
    long getLong(int id);

    /** @return the (ARGB) color value of variable [id]. */
    int getColor(int id);

    /** @return the text for [id], or null. */
    @Nullable String getText(int id);

    /** @return the object cached under [id] (bitmap, font, …), or null. */
    @Nullable Object getObject(int id);

    /** @return the shader data for [id], or null. */
    @Nullable ShaderData getShader(int id);

    /** @return the raw float path for [instanceId], or null. */
    float @Nullable [] getPathData(int instanceId);

    /** @return the data map for [id], or null. */
    @Nullable DataMap getDataMap(int id);

    /** @return the variable id registered under [name]. */
    int getVariableId(@NonNull String name);

    /** @return the current animation time (seconds). */
    float getAnimationTime();
}
