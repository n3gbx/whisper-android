package org.n3gbx.whisper.model

enum class ApplicationLanguage(val tag: String) {
    SYSTEM(tag = ""),
    ENGLISH(tag = "en"),
    BELARUSIAN(tag = "be");

    companion object {
        fun tagOf(tag: String) = entries.firstOrNull { it.tag == tag }
    }
}