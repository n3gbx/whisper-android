package org.n3gbx.whisper.core.common

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class GetEpisodesCacheDir @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    operator fun invoke() = File(context.externalCacheDir, "episodes")
}