package org.n3gbx.whisper.core.worker.custom

import androidx.work.DelegatingWorkerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainDelegationWorkerFactory @Inject constructor(
    mainWorkerFactory: MainWorkerFactory
) : DelegatingWorkerFactory() {

    init {
        addFactory(mainWorkerFactory)
    }
}