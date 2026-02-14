package org.n3gbx.whisper.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.n3gbx.whisper.core.Constants.INVALID_SIZE
import java.io.File
import java.net.URI
import javax.inject.Inject

class GetEpisodeFileSizeUseCase @Inject constructor() {

    private val httpClient by lazy { OkHttpClient() }

    suspend operator fun invoke(url: String): Long {
        return withContext(Dispatchers.IO) {
            when {
                url.isHttpUrl() -> {
                    val request = Request.Builder().url(url).build()
                    val response = httpClient.newCall(request).execute()

                    if (!response.isSuccessful) INVALID_SIZE

                    response.body?.contentLength() ?: INVALID_SIZE
                }
                url.isFilePath() -> {
                    val file = File(url)
                    if (file.exists() && file.isFile) file.length() else INVALID_SIZE
                }
                else -> INVALID_SIZE
            }
        }
    }

    private fun String.isHttpUrl(): Boolean =
        runCatching {
            val uri = URI(this)
            uri.scheme == "http" || uri.scheme == "https"
        }.getOrDefault(false)

    private fun String.isFilePath(): Boolean =
        runCatching {
            URI(this).scheme == "file"
        }.getOrDefault(false)
}