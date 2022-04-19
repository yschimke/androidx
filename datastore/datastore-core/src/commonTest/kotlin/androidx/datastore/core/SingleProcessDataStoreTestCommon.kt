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

import androidx.datastore.core.handlers.NoOpCorruptionHandler
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
//import okio.Path
//import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

@ExperimentalCoroutinesApi
// todo: revisit this class name.  How do we want to handle common vs platform specific
class SingleProcessDataStoreTestCommon {

    private lateinit var store: DataStore<Byte>
    private lateinit var testingCodec: TestingCodec
    private lateinit var testCoroutineDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var fileSystem: FakeFileSystem
    private lateinit var path: Path

    @BeforeTest
    fun setUp() {
        testingCodec = TestingCodec()
        fileSystem = FakeFileSystem()
        path = OkioPath("/fakeFile", fileSystem)
        testCoroutineDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testCoroutineDispatcher)
        store = SingleProcessDataStore(
            codec = testingCodec,
            scope = testScope,
            producePath = {path}
        )
    }

    @AfterTest
    fun cleanUp() {
        // todo: do we need to clean up the Test coroutines?
    }

    @Test
    fun testReadNewMessage() = testScope.runTest {
        assertEquals(0, store.data.first())
    }

    @Test
    fun testReadWithNewInstance() = testScope.runTest {
        coroutineScope {
            val newStore = newDataStore(scope = this)
            newStore.updateData { 1 }
        }
        coroutineScope() {
            val newStore = newDataStore(scope = this)
            assertEquals(1, newStore.data.first())
        }
    }


    private fun newDataStore(
        file: Path = path,
        codec: Codec<Byte> = testingCodec,
        scope: CoroutineScope = testScope,
        initTasksList: List<suspend (api: InitializerApi<Byte>) -> Unit> = listOf(),
        corruptionHandler: CorruptionHandler<Byte> = NoOpCorruptionHandler<Byte>()
    ): DataStore<Byte> {
        return SingleProcessDataStore(
            codec = codec,
            scope = scope,
            initTasksList = initTasksList,
            corruptionHandler = corruptionHandler,
            producePath = {file}
        )
    }
}