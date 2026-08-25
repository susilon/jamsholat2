# Project Guidelines for AI Agents

## Project Overview
**JamSholat 2** is an Android application designed as a digital prayer clock (Jam Sholat). It features prayer time calculations, a dynamic user interface with video backgrounds, and customizable settings for mosques or homes.

## Technology Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Build System:** Gradle (Kotlin DSL - `.gradle.kts`)
- **JDK:** Java 17
- **Target SDK:** 35 / **Min SDK:** 23

### Key Libraries
- **Prayer Calculations:** `com.batoulapps.adhan:adhan`
- **Persistence:** `androidx.datastore:datastore-preferences`
- **Serialization:** `kotlinx-serialization-json`
- **Media Handling:** `androidx.media3:media3-exoplayer` (Video backgrounds)
- **Image Loading:** `io.coil-kt:coil-compose`
- **Location:** `com.google.android.gms:play-services-location`
- **Lifecycle/Navigation:** `androidx.lifecycle`, `androidx.navigation-compose`

## Project Architecture
The project follows a clean separation of concerns:

- `com.jamsholat2.android.data`: Data models, configuration entities (`AppConfig`, `PrayerConfig`), and repositories (`ConfigRepository`) for persistent storage using DataStore.
- `com.jamsholat2.android.domain`: Business logic, including prayer time calculations (`PrayerCalculator`) and audio playback logic (`BeepPlayer`).
- `com.jamsholat2.android.ui`:
    - `components`: Reusable UI widgets (`TimeBox`, `InfoCard`, `VideoBackground`).
    - `settings`: Screens and dialogs for application configuration.
    - `theme`: Design system definitions.
    - `JamSholatViewModel`: Central state management for the main screen.
- `com.jamsholat2.android.util`: Helper classes for date/time manipulation (`DateTimeUtil`) and file management (`BackgroundFileManager`).

## Coding Standards & Conventions
- **Compose Best Practices:** Use stateless composables where possible. Hoist state to ViewModels or parent composables.
- **Concurrency:** Use Kotlin Coroutines for asynchronous tasks (DataStore I/O, Location updates).
- **Naming:** Follow standard Kotlin and Android naming conventions (PascalCase for classes, camelCase for functions/variables).
- **Serialization:** Use `@Serializable` for data classes that need to be stored in DataStore.
- **Resource Management:** Ensure Media3 players and location listeners are properly released in `onDispose` or ViewModel `onCleared()`.
- **UI Design Convention (TV-Style Settings):**
    - Use `aquaFocusBorder` for all interactive elements to provide visual feedback on focus.
    - For numeric range settings (like volume or speed), use a `Text` label for the current value followed by a `Slider`.
    - `Slider` should have `.focusable()` and `.onPreviewKeyEvent` handling for `Key.DirectionLeft` and `Key.DirectionRight` to allow D-pad navigation.
    - Avoid using separate +/- buttons for range settings; rely on the `Slider` with appropriate `steps` for discrete values.
    - Maintain consistent spacing and rounding (e.g., `RoundedCornerShape(8.dp)`).

## Development Workflow
1. **Adding Features:** Implement data models in `data`, business logic in `domain`, and UI in `ui/components`.
2. **Configuration Changes:** Update `AppConfig` or specific config classes, then reflect changes in `ConfigRepository`.
3. **UI Updates:** Modify `Theme.kt` for global styles or specific components in `ui/components`.
