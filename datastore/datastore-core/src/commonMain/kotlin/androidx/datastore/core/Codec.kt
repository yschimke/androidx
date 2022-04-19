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

//This function helps to decide the correct file system to use
expect fun Path(path:String):Path

//This is the primary interface people will be interfacing with that replaces Serializer<T>
interface Codec<T> {
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


//This class marshals users' codecs to the OutputStream or Sink.
//This is abstract and not expected/actual to allow OKIO to function in common.  This is useful
//for our common tests and leave the possibility open for OKIO on any platform.
interface BufferedSink {
    fun close()
    fun write(byteArray: ByteArray)
    //todo: all the other write(*) methods
}

//This class marshals users' codecs to the InputStream or Source.
interface BufferedSource {
    fun readByte(): Byte
    //todo: all the other read* methods
}

//This class is the deciding factor for what io library to use.
abstract class Path {

    internal abstract suspend fun <T> read(readerAction: suspend BufferedSource.() -> T): T
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

    abstract fun appendingBufferedSync():BufferedSink

    abstract fun flush()
    abstract fun close()
}

