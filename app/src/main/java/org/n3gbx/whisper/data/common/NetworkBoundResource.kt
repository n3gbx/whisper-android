package org.n3gbx.whisper.data.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import org.n3gbx.whisper.model.Result

class NetworkBoundResource @Inject constructor() {

    inline operator fun <QueryResult, FetchResult> invoke(
        crossinline query: suspend () -> Flow<QueryResult>,
        crossinline fetch: suspend () -> FetchResult,
        crossinline saveFetched: suspend (FetchResult, QueryResult?) -> Unit,
        crossinline shouldFetch: (currentData: QueryResult?) -> Boolean,
    ): Flow<Result<QueryResult>> = flow {
        val flow: Flow<Result<QueryResult>> = runCatching {
            val currentData = query().firstOrNull()

            if (shouldFetch(currentData)) {
                emit(Result.Loading())
                saveFetched(fetch(), currentData)
            }
            query().map { Result.Success(it) }
        }.getOrElse { e ->
            e.printStackTrace()
            flowOf(Result.Error(e))
        }

        emitAll(flow)
    }
}