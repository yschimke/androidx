/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.tooling.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking

/** Selects which player implementation renders the captured [RemoteDocument] in the preview. */
public enum class PlayerImpl {
    /** The legacy player, [androidx.compose.remote.player.compose.RemoteDocumentPlayer]. */
    JAVA,
    /** The new embedded Compose player, [RcPlayer]. */
    COMPOSE,
}

/**
 * Displays a Remote Compose Composable in the Android Studio Preview.
 *
 * This function captures the provided [content] using the specified [profile] and renders it as a
 * [RemoteDocument] to simulate how it would appear when played back in a remote context.
 *
 * @param profile The [Profile] defining the target environment for the remote content. Defaults to
 *   [RcPlatformProfiles.ANDROIDX].
 * @param playerImpl Selects which player renders the captured document: the legacy
 *   [PlayerImpl.JAVA] player or the new embedded [PlayerImpl.COMPOSE] player. Defaults to
 *   [PlayerImpl.JAVA] to match the legacy preview behavior.
 * @param modifier The modifier to be applied to the box containing the preview.
 * @param content The Composable content to be captured and previewed. It does not have be annotated
 *   with [@RemoteComposable].
 */
@Composable
public fun RemoteContentPreview(
    modifier: Modifier = Modifier,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    playerImpl: PlayerImpl = PlayerImpl.JAVA,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val context = LocalContext.current

    val document = remember {
        runBlocking {
            RemoteDocument(
                captureSingleRemoteDocument(context = context, profile = profile, content = content)
                    .bytes
            )
        }
    }

    LaunchedEffect(Unit) {}

    Box(modifier = modifier) {
        when (playerImpl) {
            PlayerImpl.COMPOSE ->
                RcPlayer(document = document.document, modifier = Modifier.fillMaxSize())
            // Force the View player regardless of the global RemoteComposePlayerFlags.useEmbeddedPlayer
            // flag (which otherwise short-circuits RemoteDocumentPlayer to the embedded player, making
            // the JAVA preview silently render COMPOSE).
            PlayerImpl.JAVA ->
                RemoteDocumentPreview(
                    document,
                    modifier = Modifier.fillMaxSize(),
                    useEmbeddedPlayer = false,
                )
        }
    }
}
