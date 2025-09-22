package org.n3gbx.whisper.ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class Timer @Inject constructor() {
    private var timerJob: Job? = null

    fun start(duration: Long, onComplete: () -> Unit) {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            delay(duration)
            onComplete()
        }
    }

    fun cancel() {
        timerJob?.cancel()
    }
}