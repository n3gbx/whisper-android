package org.n3gbx.whisper.feature.settings

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.R
import org.n3gbx.whisper.feature.settings.Setting.Button
import org.n3gbx.whisper.feature.settings.Setting.Link
import org.n3gbx.whisper.feature.settings.Setting.Section.CONTENT
import org.n3gbx.whisper.feature.settings.Setting.Section.DATA
import org.n3gbx.whisper.feature.settings.Setting.Section.OTHER
import org.n3gbx.whisper.feature.settings.Setting.Toggle
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_DOWNLOAD
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_PLAY
import org.n3gbx.whisper.feature.settings.Setting.Type.BACKUP
import org.n3gbx.whisper.feature.settings.Setting.Type.CLEAR_DATA
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOADS
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOAD_WIFI_ONLY
import org.n3gbx.whisper.feature.settings.Setting.Type.VERSION
import org.n3gbx.whisper.feature.settings.Setting.Type.INSTALLATION_ID
import org.n3gbx.whisper.feature.settings.Setting.Value
import org.n3gbx.whisper.model.StringResource
import org.n3gbx.whisper.model.StringResource.Companion.fromRes

@Immutable
data class SettingsUiState(
    val installationId: String? = null,
    val autoPlay: Boolean = false,
    val autoDownload: Boolean = false,
    val downloadWifiOnly: Boolean = false,
    val version: String = "",
    val showClearApplicationDataDialog: Boolean = false,
) {

    val settings = listOf(
        Toggle(autoPlay, AUTO_PLAY, CONTENT),
        Toggle(autoDownload, AUTO_DOWNLOAD, CONTENT),
        Toggle(downloadWifiOnly, DOWNLOAD_WIFI_ONLY, CONTENT),
        Link(BACKUP, DATA),
        Button(DOWNLOADS, DATA),
        Button(CLEAR_DATA, DATA),
        Value(version, VERSION, OTHER),
        Value(installationId, INSTALLATION_ID, OTHER),
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
    data class Value<T>(
        val value: T,
        override val type: Type,
        override val section: Section
    ): Setting

    @Immutable
    data class Button(
        override val type: Type,
        override val section: Section,
    ): Setting

    @Immutable
    data class Link(
        override val type: Type,
        override val section: Section,
    ): Setting

    @Immutable
    enum class Type(
        val title: StringResource,
        val description: StringResource? = null,
    ) {
        AUTO_PLAY(fromRes(R.string.settings_content_auto_play_title), fromRes(R.string.settings_content_auto_play_description)),
        AUTO_DOWNLOAD(fromRes(R.string.settings_content_auto_download_title), fromRes(R.string.settings_content_auto_download_description)),
        DOWNLOAD_WIFI_ONLY(fromRes(R.string.settings_content_download_only_wifi_title), fromRes(R.string.settings_content_download_only_wifi_description)),
        DOWNLOADS(fromRes(R.string.settings_data_downloads_title)),
        BACKUP(fromRes(R.string.settings_data_backup_title), fromRes(R.string.settings_data_backup_description)),
        VERSION(fromRes(R.string.settings_other_application_version_title)),
        INSTALLATION_ID(fromRes(R.string.settings_other_installation_id_title)),
        CLEAR_DATA(fromRes(R.string.settings_data_clear_data_title)),
    }

    @Immutable
    enum class Section(
        val title: StringResource,
    ) {
        CONTENT(fromRes(R.string.settings_content_title)),
        DATA(fromRes(R.string.settings_data_title)),
        OTHER(fromRes(R.string.settings_other_title))
    }
}

sealed interface SettingsUiEvent {
    data object Restart : SettingsUiEvent
    data object NavigateToDownloads : SettingsUiEvent
    data class NavigateToBrowser(val link: String) : SettingsUiEvent
}