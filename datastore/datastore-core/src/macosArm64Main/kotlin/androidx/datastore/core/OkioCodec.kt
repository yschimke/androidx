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
    override fun appendingOutputStream(): OutputStream {
        return OkioOutputStream(okioFileHandle.appendingSink().buffer())
    }

    override fun flush() {
        okioFileHandle.flush()
    }

    override fun close() {
        okioFileHandle.close()
    }
}


internal class OkioPath(private val path:okio.Path, private val fileSystem: FileSystem) : Path() {

    override suspend fun <T> read(readerAction: suspend InputStream.() -> T): T {
        return fileSystem.read(path) {
            readerAction(OkioInputStream(this))
        }
    }


    override fun createDirectories() {
        fileSystem.createDirectories(path)
    }

    override fun delete() {
        fileSystem.delete(path)
    }

    override fun openReadWrite(): FileHandle {
        return OkioFileHandle(fileSystem.openReadWrite(path))
    }

    override fun append(morePath:String):Path {
        return OkioPath("${path}$morePath".toPath(), fileSystem)
    }

    override fun move(toPath: Path) {
        check(toPath is OkioPath) {"toPath must be an OkioPath.  Was ${toPath::class}"}
        fileSystem.atomicMove(this.path, toPath.path)
    }

    override fun toString(): String {
        return path.toString()
    }

    override val isAbsolute: Boolean
        get() = path.isAbsolute
    override val exists: Boolean
        get() = fileSystem.exists(path)
    override val parent: Path?
        get() = path.parent?.let {
                    OkioPath(it, fileSystem)
                }


}