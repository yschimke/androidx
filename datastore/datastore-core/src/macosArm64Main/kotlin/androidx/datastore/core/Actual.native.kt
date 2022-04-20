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
import okio.FileSystem
import okio.Path.Companion.toPath


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

internal actual object FileUtils {
    internal actual fun createPath(file:File): Path =
        OkioPath(file.pathName.toPath(), file.fileSystem)
}


actual class File actual constructor(val pathName:String) {
    internal var fileSystem:FileSystem = FileSystem.SYSTEM
}


public actual abstract class InputStream(
    private val delegate: okio.BufferedSource
) {
    @Throws(IOException::class)
    actual abstract fun read(): Int

}

actual abstract class OutputStream(
    private val delegate: okio.BufferedSink
) {
    @Throws(IOException::class)
    actual abstract fun write(var1: Int)
    actual open fun close() {}

}

internal open class OkioInputStream(private val delegate: okio.BufferedSource)
    : InputStream(delegate) {
    @Throws(IOException::class)
    override fun read(): Int {
        return delegate.readInt()
    }
}

internal open class OkioOutputStream(private val delegate: BufferedSink) : OutputStream(delegate) {
    override fun write(var1: Int) {
        delegate.writeInt(var1)
    }

    override fun close() {
        delegate.close()
    }

}