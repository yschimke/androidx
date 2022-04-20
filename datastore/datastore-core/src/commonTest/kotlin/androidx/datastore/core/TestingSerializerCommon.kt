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

expect class TestFileSystem

expect fun getTestFile(pathName:String, testFileSystem: TestFileSystem):File
expect fun createTestFileSystem():TestFileSystem

class TestingSerializerCommon(
    var failReadWithCorruptionException: Boolean = false,
    var failingRead: Boolean = false,
    var failingWrite: Boolean = false,
    override val defaultValue: Byte = 0
) : Serializer<Byte> {

    override suspend fun readFrom(input: InputStream): Byte {
        if (failReadWithCorruptionException) {
            throw CorruptionException(
                "CorruptionException",
                IOException("I was asked to fail on read")
            )
        }

        if (failingRead) {
            throw IOException("I was asked to fail on reads")
        }

        val read = try {
            input.read().toByte()
        } catch (e: EOFException) {
            return 0
        }
        return read
    }

    override suspend fun writeTo(t: Byte, output: OutputStream) {
        if (failingWrite) {
            throw IOException("I was asked to fail on writes")
        }
        output.write(t.toInt())
    }
}