package org.n3gbx.whisper.feature.settings

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.n3gbx.whisper.MainDispatcherRule
import org.n3gbx.whisper.core.common.GetString
import org.n3gbx.whisper.data.SettingsRepository
import org.n3gbx.whisper.domain.DeleteLocalDataUseCase
import org.n3gbx.whisper.feature.settings.Setting.Section.CONTENT
import org.n3gbx.whisper.feature.settings.Setting.Section.DATA
import org.n3gbx.whisper.feature.settings.Setting.Section.OTHER
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_DOWNLOAD
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_PLAY
import org.n3gbx.whisper.feature.settings.Setting.Type.BACKUP
import org.n3gbx.whisper.feature.settings.Setting.Type.CACHE_OPTIMIZATION
import org.n3gbx.whisper.feature.settings.Setting.Type.CLEAR_DATA
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOAD_WIFI_ONLY
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOADS
import org.n3gbx.whisper.feature.settings.Setting.Type.INSTALLATION_ID
import org.n3gbx.whisper.feature.settings.Setting.Type.VERSION

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockSettingsRepository: SettingsRepository = mockk()
    private val mockDeleteLocalData: DeleteLocalDataUseCase = mockk()
    private val mockGetString: GetString = mockk()

    private lateinit var sut: SettingsViewModel

    @Before
    fun setUp() {
        every { mockSettingsRepository.getAutoPlaySetting() } returns flowOf(false)
        every { mockSettingsRepository.getAutoDownloadSetting() } returns flowOf(false)
        every { mockSettingsRepository.getDownloadWifiOnlySetting() } returns flowOf(false)
        every { mockSettingsRepository.getCacheOptimizationSetting() } returns flowOf(false)
        every { mockSettingsRepository.getInstallationId() } returns flowOf(null)
        every { mockGetString(any()) } returns "An error occurred"
    }

    private fun createSut() {
        sut = SettingsViewModel(mockSettingsRepository, mockDeleteLocalData, mockGetString)
    }

    @Test
    fun givenRepositoryEmitsSettings_whenViewModelCreated_thenStateReflectsThoseSettings() = runTest {
        every { mockSettingsRepository.getAutoPlaySetting() } returns flowOf(true)
        every { mockSettingsRepository.getAutoDownloadSetting() } returns flowOf(true)
        every { mockSettingsRepository.getDownloadWifiOnlySetting() } returns flowOf(true)
        every { mockSettingsRepository.getCacheOptimizationSetting() } returns flowOf(true)
        every { mockSettingsRepository.getInstallationId() } returns flowOf("install-id-123")

        createSut()
        advanceUntilIdle()

        with(sut.uiState.value) {
            autoPlay shouldBe true
            autoDownload shouldBe true
            downloadWifiOnly shouldBe true
            optimizeCache shouldBe true
            installationId shouldBe "install-id-123"
        }
    }

    @Test
    fun givenAutoPlayDisabled_whenAutoPlayToggleClicked_thenSetAutoPlaySettingIsCalledWithTrue() = runTest {
        coEvery { mockSettingsRepository.setAutoPlaySetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(false, AUTO_PLAY, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setAutoPlaySetting(true) }
    }

    @Test
    fun givenAutoPlayEnabled_whenAutoPlayToggleClicked_thenSetAutoPlaySettingIsCalledWithFalse() = runTest {
        every { mockSettingsRepository.getAutoPlaySetting() } returns flowOf(true)
        coEvery { mockSettingsRepository.setAutoPlaySetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(true, AUTO_PLAY, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setAutoPlaySetting(false) }
    }

    @Test
    fun givenAutoDownloadDisabled_whenAutoDownloadToggleClicked_thenSetAutoDownloadSettingIsCalledWithTrue() = runTest {
        coEvery { mockSettingsRepository.setAutoDownloadSetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(false, AUTO_DOWNLOAD, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setAutoDownloadSetting(true) }
    }

    @Test
    fun givenAutoDownloadEnabled_whenAutoDownloadToggleClicked_thenSetAutoDownloadSettingIsCalledWithFalse() = runTest {
        every { mockSettingsRepository.getAutoDownloadSetting() } returns flowOf(true)
        coEvery { mockSettingsRepository.setAutoDownloadSetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(true, AUTO_DOWNLOAD, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setAutoDownloadSetting(false) }
    }

    @Test
    fun givenCacheOptimizationDisabled_whenCacheOptimizationToggleClicked_thenSetCacheOptimizationSettingIsCalledWithTrue() = runTest {
        coEvery { mockSettingsRepository.setCacheOptimizationSetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(false, CACHE_OPTIMIZATION, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setCacheOptimizationSetting(true) }
    }

    @Test
    fun givenCacheOptimizationEnabled_whenCacheOptimizationToggleClicked_thenSetCacheOptimizationSettingIsCalledWithFalse() = runTest {
        every { mockSettingsRepository.getCacheOptimizationSetting() } returns flowOf(true)
        coEvery { mockSettingsRepository.setCacheOptimizationSetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(true, CACHE_OPTIMIZATION, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setCacheOptimizationSetting(false) }
    }

    @Test
    fun givenDownloadWifiOnlyDisabled_whenDownloadWifiOnlyToggleClicked_thenSetDownloadWifiOnlySettingIsCalledWithTrue() = runTest {
        coEvery { mockSettingsRepository.setDownloadWifiOnlySetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(false, DOWNLOAD_WIFI_ONLY, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setDownloadWifiOnlySetting(true) }
    }

    @Test
    fun givenDownloadWifiOnlyEnabled_whenDownloadWifiOnlyToggleClicked_thenSetDownloadWifiOnlySettingIsCalledWithFalse() = runTest {
        every { mockSettingsRepository.getDownloadWifiOnlySetting() } returns flowOf(true)
        coEvery { mockSettingsRepository.setDownloadWifiOnlySetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Toggle(true, DOWNLOAD_WIFI_ONLY, CONTENT))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setDownloadWifiOnlySetting(false) }
    }

    @Test
    fun givenBackupLinkSetting_whenClicked_thenNavigateToBrowserEventIsEmittedWithBackupUrl() = runTest {
        val stubUrl = "https://developer.android.com/identity/data/autobackup#BackupLocation"
        createSut()

        sut.uiEvents.test {
            sut.onSettingClick(Setting.Link(stubUrl, BACKUP, DATA))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() shouldBe SettingsUiEvent.NavigateToBrowser(stubUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenDownloadsButtonSetting_whenClicked_thenNavigateToDownloadsEventIsEmitted() = runTest {
        createSut()

        sut.uiEvents.test {
            sut.onSettingClick(Setting.Button(DOWNLOADS, DATA))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() shouldBe SettingsUiEvent.NavigateToDownloads
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenClearDataButtonSetting_whenClicked_thenShowClearApplicationDataDialogIsTrue() = runTest {
        createSut()
        advanceUntilIdle()

        sut.onSettingClick(Setting.Button(CLEAR_DATA, DATA))
        advanceUntilIdle()

        sut.uiState.value.showClearApplicationDataDialog shouldBe true
    }

    @Test
    fun givenVersionValueSetting_whenClicked_thenNoEventIsEmitted() = runTest {
        createSut()

        sut.uiEvents.test {
            sut.onSettingClick(Setting.Value("1.0.0", VERSION, OTHER))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenInstallationIdValueSetting_whenClicked_thenNoEventIsEmitted() = runTest {
        createSut()

        sut.uiEvents.test {
            sut.onSettingClick(Setting.Value("device-id-abc", INSTALLATION_ID, OTHER))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenClearDataDialogShown_whenDismissed_thenShowClearApplicationDataDialogIsFalse() = runTest {
        createSut()
        advanceUntilIdle()
        sut.onSettingClick(Setting.Button(CLEAR_DATA, DATA))
        advanceUntilIdle()

        sut.onClearApplicationDataDialogDismiss()

        sut.uiState.value.showClearApplicationDataDialog shouldBe false
    }

    @Test
    fun givenClearDataDialogShown_whenConfirmedAndDeleteSucceeds_thenRestartEventIsEmitted() = runTest {
        coEvery { mockDeleteLocalData() } returns true
        createSut()
        advanceUntilIdle()
        sut.onSettingClick(Setting.Button(CLEAR_DATA, DATA))
        advanceUntilIdle()

        sut.uiEvents.test {
            sut.onClearApplicationDataDialogConfirm()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() shouldBe SettingsUiEvent.Restart
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenClearDataDialogShown_whenConfirmedAndDeleteFails_thenShowMessageEventIsEmitted() = runTest {
        coEvery { mockDeleteLocalData() } returns false
        createSut()
        advanceUntilIdle()
        sut.onSettingClick(Setting.Button(CLEAR_DATA, DATA))
        advanceUntilIdle()

        sut.uiEvents.test {
            sut.onClearApplicationDataDialogConfirm()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() shouldBe SettingsUiEvent.ShowMessage("An error occurred")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenClearDataDialogShown_whenConfirmed_thenDeleteLocalDataIsCalledOnce() = runTest {
        coEvery { mockDeleteLocalData() } returns true
        createSut()
        advanceUntilIdle()
        sut.onSettingClick(Setting.Button(CLEAR_DATA, DATA))
        advanceUntilIdle()

        sut.uiEvents.test {
            sut.onClearApplicationDataDialogConfirm()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockDeleteLocalData() }
    }
}
