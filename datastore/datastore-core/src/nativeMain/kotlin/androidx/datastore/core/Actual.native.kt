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

import okio.FileSystem


actual fun Path(path:String): Path = OkioPath(path, FileSystem.SYSTEM)

// todo: FIXME DON'T MERGE.  Need real atomic int.
internal actual class AtomicInt actual constructor(private var value: Int) {

    actual fun get(): Int {
        return value
    }
    actual fun set(value: Int) {
        this.value = value
    }
    actual fun add(amount: Int): Int {
        value += amount
        return value
    }
    actual fun getAndIncrement(): Int {
        return value++
    }
    actual fun incrementAndGet(): Int {
        return ++value
    }
    actual fun getAndDecrement(): Int {
        return value--
    }
    actual fun decrementAndGet(): Int {
        return --value
    }
}

public actual typealias IOException = okio.IOException
public actual typealias FileNotFoundException = okio.FileNotFoundException
public actual typealias EOFException = okio.EOFException