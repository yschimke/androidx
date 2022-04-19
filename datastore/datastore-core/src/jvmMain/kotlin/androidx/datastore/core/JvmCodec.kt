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

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal class JavaIOPath(val file:File) : Path() {
    constructor(path:String) : this(File(path))

    override suspend fun <T> read(readerAction: suspend BufferedSource.() -> T): T {
        return BufferedInputStream(FileInputStream(file)).use { fin ->
            readerAction(JavaIOBufferedSource(fin))
        }
    }

    override fun createDirectories() {
        file.createParentDirectories()
    }

    override fun delete() {
        file.delete()
    }

    override fun openReadWrite(): FileHandle {
        return JavaIOFileHandle(FileOutputStream(file, true))
    }

    override fun move(toPath: Path) {
        file.renameTo(File(toPath.toString()))
    }

    override fun append(morePath: String): Path {
        return JavaIOPath("${file.absolutePath}$morePath")
    }

    override val isAbsolute: Boolean
        get() = file.isAbsolute
    override val exists: Boolean
        get() = file.exists()
    override val parent: Path?
        get() = if (file.parent != null) JavaIOPath(file.parent) else null

}

internal fun File.createParentDirectories() {
    val parent: File? = canonicalFile.parentFile

    parent?.let {
        it.mkdirs()
        if (!it.isDirectory) {
            throw IOException("Unable to create parent directories of $this")
        }
    }
}

internal class JavaIOFileHandle(private val out:FileOutputStream) : FileHandle() {
    override fun appendingBufferedSync(): BufferedSink {
        return JavaIOBufferedSink(BufferedOutputStream(out))
    }

    override fun flush() {
        out.fd.sync()
    }

    override fun close() {
        out.close()
    }

}

internal class JavaIOBufferedSink(internal val out:BufferedOutputStream) : BufferedSink {
    override fun close() {
        //nothing to close. Will be closed by file handle
    }

    override fun write(byteArray: ByteArray) {
        out.write(byteArray)
    }
}

internal class JavaIOBufferedSource(internal val fin:BufferedInputStream) : BufferedSource {
    override fun readByte(): Byte {
        return fin.read().toByte()
    }
}

internal class JavaIOCodec<T>(private val serializer: Serializer<T>) : Codec<T> {
    override val defaultValue: T
        get() = serializer.defaultValue

    override suspend fun readFrom(input: BufferedSource): T {
        check(input is JavaIOBufferedSource)
            {"BufferedSource must be a JavaBufferedSource.  Was: ${input::class}"}
        return serializer.readFrom(input.fin)
    }

    override suspend fun writeTo(t: T, output: BufferedSink) {
        check(output is JavaIOBufferedSink)
            {"BufferedSink must be a JavaBufferedSink.  Was: ${output::class}"}
        serializer.writeTo(t, output.out)
    }

}