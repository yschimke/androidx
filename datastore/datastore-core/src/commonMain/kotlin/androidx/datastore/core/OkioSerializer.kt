/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.core

import okio.BufferedSink
import okio.BufferedSource

public interface OkioSerializer<T> {
    /**
     * Value to return if there is no data on disk.
     */
    public val defaultValue: T

    /**
     * Unmarshal object from stream.
     *
     * @param input the Source with the data to deserialize
     */
    public suspend fun readFrom(input: BufferedSource): T

    /**
     *  Marshal object to a stream. Closing the provided OutputStream is a no-op.
     *
     *  @param t the data to write to output
     *  @output the Sink to serialize data to
     */
    public suspend fun writeTo(t: T, output: BufferedSink)
}