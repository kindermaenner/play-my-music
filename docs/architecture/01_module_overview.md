# 01 Module Overview

The project is structured into three main modules to ensure separation of concerns and testability.

## Module `:domain` (Pure Kotlin Library)
Contains the core business logic and rules of the game.
- **`model`**: Defines core entities like `Track` and `PlaybackConfig`.
- **`repository`**: Interfaces for data access (`MappingRepository`, `LocalTrackResolver`).
- **`usecase`**: Encapsulates game logic like `IdentifyTrackUseCase` and `ResolveTrackUriUseCase`.

## Module `:data` (Android Library)
Responsible for concrete data implementations and external integrations.
- **`mapping`**: `JsonMappingRepository` for parsing local and asset-based JSON files.
- **`model`**: DTOs for JSON deserialization (`MappingFile`, `Song`).

## Module `:app` (Android Application)
Contains UI components, hardware interactions, and glue code.
- **`camera`**: `CameraPreview` using CameraX and ML Kit.
- **`player`**: `SmbDataSource` for Media3/ExoPlayer.
- **`settings`**: `SettingsRepository` using DataStore.
- **`ui`**: Jetpack Compose screens and ViewModels.

---

## Component Diagram

![Component Diagram](diagrams/out/01_component.svg)
