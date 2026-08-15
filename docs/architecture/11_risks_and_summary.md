# 11 Risks and Summary

## Critical Paths & Risks
1.  **Network Latency**: High-latency Wi-Fi can lead to stuttering in SMB playback. The `SmbDataSource` must buffer efficiently.
2.  **I/O Blocking**: JCIFS-NG uses blocking I/O; care must be taken to ensure network operations don't freeze the main thread.
3.  **Versioning**: The combination of assets and offline mappings can lead to versioning conflicts if the user updates mapping files on the NAS but doesn't re-sync.
4.  **Storage**: Syncing a large music collection can quickly consume available device storage.

## Architecture Summary
The "Play My Music" architecture is a **Clean Architecture System** with three layers:
1.  **Domain Layer (`:domain`)**: Pure Kotlin logic, interfaces, and models.
2.  **Data Layer (`:data`)**: Implementation of repositories and data models.
3.  **App Layer (`:app`)**: UI, manual DI, and Android framework integration.

By centralizing game logic in Use Cases, the system is highly testable and decoupled from specific storage or playback technologies.
