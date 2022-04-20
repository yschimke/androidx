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

expect abstract class InputStream {
    @Throws(IOException::class)
    abstract fun read(): Int
}
expect abstract class OutputStream {
    @Throws(IOException::class)
    abstract fun write(var1: Int)
    open fun close()
}

expect class File constructor(pathName:String)

internal expect object FileUtils {
    //Tried to make this top level function, but got missing method runtime error on jvm
    internal fun createPath(file:File):Path
}

// todo(b/228878451) consolidate with compose AtomicInt
internal expect class AtomicInt(value: Int) {
    fun get(): Int
    fun set(value: Int)
    fun add(amount: Int): Int
    fun getAndIncrement(): Int
    fun incrementAndGet(): Int
    fun getAndDecrement(): Int
    fun decrementAndGet(): Int
}

expect open class IOException constructor(
    message: String?,
    cause: Throwable?
) : Exception {
    constructor(message: String?)
}

expect open class EOFException constructor(message: String?) : IOException

expect open class FileNotFoundException constructor(message: String?) : IOException

//This class is the deciding factor for what io library to use.
internal abstract class Path {

    internal abstract suspend fun <T> read(readerAction: suspend InputStream.() -> T): T
    internal abstract fun createDirectories()
    internal abstract fun delete()

    internal abstract fun openReadWrite():FileHandle

    internal abstract fun move(toPath:Path)

    internal abstract fun append(morePath:String):Path

    internal abstract val isAbsolute:Boolean
    internal abstract val exists:Boolean
    internal abstract val parent:Path?
}



internal abstract class FileHandle {

    abstract fun appendingOutputStream():OutputStream

    abstract fun flush()
    abstract fun close()
}

