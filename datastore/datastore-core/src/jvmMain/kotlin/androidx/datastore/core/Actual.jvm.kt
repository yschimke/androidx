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

@file:RestrictTo(Scope.LIBRARY)

package androidx.datastore.core

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

// todo(b/228878451) consolidate with compose AtomicInt
internal actual class AtomicInt actual constructor(value: Int) {
    val delegate = java.util.concurrent.atomic.AtomicInteger(value)
    actual fun get(): Int = delegate.get()
    actual fun set(value: Int) = delegate.set(value)
    actual fun add(amount: Int): Int = delegate.addAndGet(amount)
    actual fun getAndIncrement(): Int = delegate.getAndIncrement()
    actual fun incrementAndGet(): Int = delegate.incrementAndGet()
    actual fun getAndDecrement(): Int = delegate.getAndDecrement()
    actual fun decrementAndGet(): Int = delegate.decrementAndGet()
}

public actual typealias IOException = java.io.IOException