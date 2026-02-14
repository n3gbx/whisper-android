package org.n3gbx.whisper.core.common

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.n3gbx.whisper.model.StringResource
import org.n3gbx.whisper.utils.asRawString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetString @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(stringResource: StringResource) = stringResource.asRawString(context.resources)
}