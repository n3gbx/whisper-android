package org.n3gbx.whisper.core.common

import javax.inject.Inject

class GetEpisodesCacheDirSizeLimit @Inject constructor() {

    operator fun invoke() = 256 * 1024 * 1024
}