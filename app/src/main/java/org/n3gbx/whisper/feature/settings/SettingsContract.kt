package org.n3gbx.whisper.feature.settings

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.feature.settings.Setting.Section.DATA
import org.n3gbx.whisper.feature.settings.Setting.Section.OTHER
import org.n3gbx.whisper.feature.settings.Setting.Section.APPEARANCE
import org.n3gbx.whisper.feature.settings.Setting.Section.CONTENT
import org.n3gbx.whisper.feature.settings.Setting.Value
import org.n3gbx.whisper.feature.settings.Setting.Toggle
import org.n3gbx.whisper.feature.settings.Setting.Button
import org.n3gbx.whisper.feature.settings.Setting.Type.THEME
import org.n3gbx.whisper.feature.settings.Setting.Type.LANGUAGE
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_PLAY
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOAD_WIFI_ONLY
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_DOWNLOAD
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOADS
import org.n3gbx.whisper.feature.settings.Setting.Type.BACKUP
import org.n3gbx.whisper.feature.settings.Setting.Type.VERSION
import org.n3gbx.whisper.feature.settings.Setting.Type.CLEAR_DATA

@Immutable
data class SettingsUiState(
    val installationId: String? = null,
    val autoPlay: Boolean = false,
    val autoDownload: Boolean = false,
    val downloadWifiOnly: Boolean = false,
    val theme: String = "",
    val language: String = "",
    val version: String = ""
) {

    val settings = listOf(
        Toggle(autoPlay, AUTO_PLAY, CONTENT),
        Toggle(autoDownload, AUTO_DOWNLOAD, CONTENT),
        Toggle(downloadWifiOnly, DOWNLOAD_WIFI_ONLY, CONTENT),
        Value(theme, THEME, APPEARANCE),
        Value(language, LANGUAGE, APPEARANCE),
        Button(DOWNLOADS, DATA),
        Button(BACKUP, DATA),
        Button(CLEAR_DATA, DATA),
        Value(version, VERSION, OTHER),
    ).groupBy { it.section }
}

@Immutable
sealed interface Setting {
    val type: Type
    val section: Section

    @Immutable
    data class Toggle(
        val value: Boolean,
        override val type: Type,
        override val section: Section
    ) : Setting

    @Immutable
    data class Value(
        val value: String,
        override val type: Type,
        override val section: Section
    ): Setting

    @Immutable
    data class Button(
        override val type: Type,
        override val section: Section,
    ): Setting

    @Immutable
    enum class Type(
        val title: String,
        val description: String? = null,
    ) {
        AUTO_PLAY("Auto play", "Auto play latest episode when book opens"),
        AUTO_DOWNLOAD("Auto download", "Auto download episodes when playing"),
        DOWNLOAD_WIFI_ONLY("Download only via Wi-Fi", "Do not download content over cellular data to reduce data usage"),
        THEME("App Theme"),
        LANGUAGE("App Language"),
        DOWNLOADS("Downloads"),
        BACKUP("Backup"),
        VERSION("App Version"),
        CLEAR_DATA("Clear Data"),
    }

    @Immutable
    enum class Section(
        val title: String,
    ) {
        CONTENT("Content"),
        APPEARANCE("Appearance"),
        DATA("Data"),
        OTHER("Other")
    }
}