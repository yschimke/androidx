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
import okio.Path.Companion.toPath
import okio.buffer

//This can be moved to its own module, so it can be used by common tests, but doesn't need to get
//picked up for all android/jvm implementations.

internal class OkioFileHandle(private val okioFileHandle:okio.FileHandle) : FileHandle() {
    override fun appendingBufferedSync(): BufferedSink {
        return OkioBufferedSync(okioFileHandle.appendingSink().buffer())
    }

    override fun flush() {
        okioFileHandle.flush()
    }

    override fun close() {
        okioFileHandle.close()
    }
}

internal class OkioBufferedSync(private val okioBufferedSink: okio.BufferedSink) : BufferedSink {
    override fun close() {
        okioBufferedSink.close()
    }

    override fun write(byteArray: ByteArray) {
        okioBufferedSink.write(byteArray)
    }
}

internal class OkioBufferedSource(private val okioBufferedSource: okio.BufferedSource) : BufferedSource {
    override fun readByte(): Byte {
        return okioBufferedSource.readByte()
    }

}


internal class OkioPath(path:String, private val fileSystem: FileSystem) : Path() {
    private val okioPath = path.toPath()
    override suspend fun <T> read(readerAction: suspend BufferedSource.() -> T): T {
        return fileSystem.read(okioPath) {
            readerAction(OkioBufferedSource(this))
        }
    }


    override fun createDirectories() {
        fileSystem.createDirectories(okioPath)
    }

    override fun delete() {
        fileSystem.delete(okioPath)
    }

    override fun openReadWrite(): FileHandle {
        return OkioFileHandle(fileSystem.openReadWrite(okioPath))
    }

    override fun append(morePath:String):Path {
        return OkioPath("$this$morePath", fileSystem)
    }

    override fun move(toPath: Path) {
        check(toPath is OkioPath) {"toPath must be an OkioPath.  Was ${toPath::class}"}
        fileSystem.atomicMove(this.okioPath, toPath.okioPath)
    }

    override val isAbsolute: Boolean
        get() = okioPath.isAbsolute
    override val exists: Boolean
        get() = fileSystem.exists(okioPath)
    override val parent: Path?
        get() = OkioPath(okioPath.parent.toString(), fileSystem)

}