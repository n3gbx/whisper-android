# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
./gradlew lint                   # Run Android Lint
./gradlew clean                  # Clean build artifacts
```

- Debug builds use the `.debug` application ID suffix
- Release builds use ProGuard minification
- Java/Kotlin target: version 17; compileSdk: 35; minSdk: 26

## Architecture

Single-module Android app (`org.n3gbx.whisper`) using Jetpack Compose + Hilt DI.

**Layered structure:**

```
UI (Compose Screen) → ViewModel → Repository → Room DB / Firestore
```

**MVI pattern per feature** — each feature in `feature/` has three files:
- `*Contract.kt` — immutable UI state + sealed event types
- `*Screen.kt` — Compose UI, receives state + event callbacks
- `*ViewModel.kt` — handles events, emits state via `StateFlow`

**Key packages:**
- `core/` — `MainActivity`, `MainApplication` (Hilt + WorkManager init), app-level ViewModel, DI modules, `PlayerPlaybackService` (Media3 session), WorkManager workers
- `data/` — repositories (`BookRepository`, `EpisodeRepository`, `SettingsRepository`), `NetworkBoundResource` pattern for local+remote sync, DTOs, `Mapper.kt`
- `database/` — Room database, DAOs (`BookDao`, `EpisodeDao`, `EpisodeProgressDao`, `EpisodeDownloadDao`), entities, type converters
- `datastore/` — `MainDatastore` (user preferences via DataStore)
- `domain/` — use cases for cache/file operations
- `feature/` — `catalog`, `library`, `player`, `miniplayer`, `settings`, `downloads`
- `model/` — domain models (`Book`, `Episode`, `EpisodeProgress`, `EpisodeDownload`, `Result`, `Identifier`, etc.)
- `ui/` — navigation graph (`NavHost`, `NavRoute`, `NavBar`, `NavTypes`), Material3 theme, shared Compose components

**Key architectural points:**
- `PlayerViewModel` is a Hilt singleton shared across features (player + miniplayer)
- `NetworkBoundResource` coordinates local cache and Firestore fetches via Flow
- Background work (episode duration probing, downloads) runs via WorkManager with custom worker factories in `core/worker/`
- Navigation uses type-safe Compose Navigation with serializable route objects in `NavRoute.kt`
- Dependencies centrally managed in `gradle/libs.versions.toml`

**Backend:** Firebase (Firestore for data, Authentication via Google Identity, Analytics)

**Media:** Media3/ExoPlayer with `PlayerPlaybackService` as a foreground media session service

## Development Best Practises

- Use comments sparingly. Only comment complex code.