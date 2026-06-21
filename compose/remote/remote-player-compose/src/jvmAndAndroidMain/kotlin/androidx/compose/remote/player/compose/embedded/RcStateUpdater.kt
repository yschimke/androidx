/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.RemoteContext
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Host-facing API to update a document's named user state, handed to the `onNamedAction` callback of
 * [RcPlayer]. A platform-agnostic port of the View player's
 * `androidx.compose.remote.player.core.state.StateUpdater`: the "user-local" setters prefix the name
 * with the `USER` domain and override the matching [RemoteContext] variable.
 */
public interface RcStateUpdater {
    /** Override the named long (no domain prefix); null is a no-op. */
    public fun setNamedLong(name: String, value: Long?)

    /** Override/clear the USER-domain float [floatName]. */
    public fun setUserLocalFloat(floatName: String, value: Float?)

    /** Override/clear the USER-domain integer [integerName]. */
    public fun setUserLocalInt(integerName: String, value: Int?)

    /** Override the USER-domain color [name] (ARGB int). */
    public fun setUserLocalColor(name: String, value: Int?)

    /** Override/clear the USER-domain image [name]. */
    public fun setUserLocalBitmap(name: String, content: ImageBitmap?)

    /** Override/clear the USER-domain string [stringName]. */
    public fun setUserLocalString(stringName: String, value: String?)

    public companion object {
        /** The user-domain-qualified variable name (mirrors `RemoteDomains.USER + ":" + name`). */
        public fun getUserDomainString(name: String): String = "USER:$name"
    }
}

/** Default [RcStateUpdater], applying overrides to [remoteContext]. */
internal class RcStateUpdaterImpl(private val remoteContext: RemoteContext) : RcStateUpdater {
    override fun setNamedLong(name: String, value: Long?) {
        if (value != null) remoteContext.setNamedLong(name, value)
    }

    override fun setUserLocalFloat(floatName: String, value: Float?) {
        val name = RcStateUpdater.getUserDomainString(floatName)
        if (value != null) remoteContext.setNamedFloatOverride(name, value)
        else remoteContext.clearNamedFloatOverride(name)
    }

    override fun setUserLocalInt(integerName: String, value: Int?) {
        val name = RcStateUpdater.getUserDomainString(integerName)
        if (value != null) remoteContext.setNamedIntegerOverride(name, value)
        else remoteContext.clearNamedIntegerOverride(name)
    }

    override fun setUserLocalColor(name: String, value: Int?) {
        if (value != null) {
            remoteContext.setNamedColorOverride(RcStateUpdater.getUserDomainString(name), value)
        }
    }

    override fun setUserLocalBitmap(name: String, content: ImageBitmap?) {
        val domainName = RcStateUpdater.getUserDomainString(name)
        if (content != null) remoteContext.setNamedDataOverride(domainName, content)
        else remoteContext.clearNamedDataOverride(domainName)
    }

    override fun setUserLocalString(stringName: String, value: String?) {
        val name = RcStateUpdater.getUserDomainString(stringName)
        if (value != null) remoteContext.setNamedStringOverride(name, value)
        else remoteContext.clearNamedStringOverride(name)
    }
}
