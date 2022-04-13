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

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

@ExperimentalCoroutinesApi
class OkioStorage<T> (
    private val fileSystem: FileSystem,
    private val producePath: () -> Path,
    private val serializer: OkioSerializer<T>,
) : Storage<T> {

    private val SCRATCH_SUFFIX = ".tmp"

    private val path: Path by lazy {
        val newPath = producePath()
        // todo: figure out why we can't canonicalize a non existent file. Until then
        // require absolute
        // val newPath = fileSystem.canonicalize(initialPath)
        check(newPath.isAbsolute) {
            "Path supplied must be an absolute path: $newPath"
        }
        check(newPath.parent != null) {
            "Path supplied must have a parent directory: $newPath"
        }
        synchronized(activeFileLock) {
            check(!activeFiles.contains(newPath)) {
                "There are multiple DataStores active for the same file: $newPath. You " +
                 "should either maintain your DataStore as a singleton or confirm that there " +
                 "is no two DataStore's active on the same file (by confirming that the scope " +
                 "is cancelled)"
            }
            activeFiles.add(newPath)
            newPath
        }
    }

    override suspend fun readData(): T {
        try {
            fileSystem.read(path) {
                return serializer.readFrom(this)
            }
        } catch (ex: FileNotFoundException) {
            if (fileSystem.exists(path)) {
                throw ex
            }
            return serializer.defaultValue
        }
    }

    override suspend fun writeData(newData: T) {
        fileSystem.createDirectories(path.parent!!)

        val scratchPath = "$path$SCRATCH_SUFFIX".toPath()
        try {
            fileSystem.openReadWrite(scratchPath).use { file ->
                file.appendingSink().buffer().use { bufferedSink ->
                    //todo: deal with unclosableness (tried wrapper but odd expect/actual for
                    //BufferedSink caused error.
                    serializer.writeTo(newData, bufferedSink)
                }
                file.flush()
                // TODO(b/151635324): fsync the directory, otherwise a badly timed crash could
                //  result in reverting to a previous state.
            }

            fileSystem.atomicMove(scratchPath, path)
            // todo: maybe we need to catch an exception if this fails and give better description
        } catch (ex: okio.IOException) {
            if (fileSystem.exists(scratchPath)) {
                fileSystem.delete(scratchPath)
            }
            throw ex
        }
    }

    //todo: figure out why this didn't work
//    private class UnclosableBufferedSync(val bufferedSync: BufferedSink) : BufferedSink {
//        override val buffer: Buffer
//            get() = bufferedSync.buffer
//
//        override fun emit(): BufferedSink = bufferedSync.emit()
//
//        override fun emitCompleteSegments() = bufferedSync.emitCompleteSegments()
//
//        override fun flush() = bufferedSync.flush()
//
//        override fun write(source: ByteArray): BufferedSink = bufferedSync.write(source)
//
//        override fun write(source: ByteArray, offset: Int, byteCount: Int): BufferedSink
//            = bufferedSync.write(source, offset, byteCount)
//
//        override fun write(byteString: ByteString): BufferedSink = bufferedSync.write(byteString)
//
//        override fun write(byteString: ByteString, offset: Int, byteCount: Int): BufferedSink
//            = bufferedSync.write(byteString, offset, byteCount)
//
//        override fun write(source: Source, byteCount: Long): BufferedSink
//            = bufferedSync.write(source, byteCount)
//
//        override fun writeAll(source: Source): Long = bufferedSync.writeAll(source)
//
//
//        override fun writeByte(b: Int): BufferedSink = bufferedSync.writeByte(b)
//
//        override fun writeDecimalLong(v: Long): BufferedSink = bufferedSync.writeDecimalLong(v)
//
//        override fun writeHexadecimalUnsignedLong(v: Long): BufferedSink
//            = bufferedSync.writeHexadecimalUnsignedLong(v)
//
//        override fun writeInt(i: Int): BufferedSink = bufferedSync.writeInt(i)
//
//        override fun writeIntLe(i: Int): BufferedSink = bufferedSync.writeIntLe(i)
//
//        override fun writeLong(v: Long): BufferedSink = bufferedSync.writeLong(v)
//
//        override fun writeLongLe(v: Long): BufferedSink = bufferedSync.writeLongLe(v)
//
//        override fun writeShort(s: Int): BufferedSink = bufferedSync.writeShort(s)
//
//        override fun writeShortLe(s: Int): BufferedSink = bufferedSync.writeShortLe(s)
//
//        override fun writeUtf8(string: String): BufferedSink = bufferedSync.writeUtf8(string)
//
//        override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): BufferedSink
//            = bufferedSync.writeUtf8(string, beginIndex, endIndex)
//
//        override fun writeUtf8CodePoint(codePoint: Int): BufferedSink
//            = bufferedSync.writeUtf8CodePoint(codePoint)
//
//        override fun close() {
//            // We will not close the underlying FileOutputStream until after we're done syncing
//            // the fd. This is useful for things like b/173037611.
//        }
//
//        override fun timeout(): Timeout = bufferedSync.timeout()
//
//        override fun write(source: Buffer, byteCount: Long) = bufferedSync.write(source, byteCount)
//    }

    override fun onComplete() {
        synchronized(activeFileLock) {
            activeFiles.remove(path)
        }
    }

   internal companion object {
       internal val activeFiles = mutableSetOf<Path>()
       internal val activeFileLock = Lock()
   }

   internal class Lock : SynchronizedObject()
}