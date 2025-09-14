package org.n3gbx.whisper.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Tab
sealed interface Root

@Serializable
data object CatalogRoot : Root, Tab

@Serializable
data object LibraryRoot : Root, Tab

@Serializable
data object SettingsRoot : Root, Tab

@Serializable
data object PlayerRoot : Root

@Serializable
data object Catalog

@Serializable
data object Library

@Serializable
data object Settings

@Serializable
data class Player(val bookId: String? = null)