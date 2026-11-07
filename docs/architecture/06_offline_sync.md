# 06 Offline Sync

To support playback without an active network connection, the application provides an offline synchronization mechanism.

## Directory Structure

### 1. Remote Structure on NAS
The synchronization operates on a dedicated base folder on the NAS configured via `nasBaseFolder` (e.g. `Multimedia\playmymusic`).
Underneath this base folder, each edition/collection is stored in its own dedicated directory:

```text
<nasAddress>\<nasBaseFolder>\
    ├── <EditionName>\
    │   ├── <EditionName>.json       Mapping JSON containing edition metadata and track definitions
    │   ├── <CoverImage>.jpg         Optional artwork specified in meta.image
    │   └── <trackFolderName>\       Audio files folder (configurable, defaults to "tracks")
    │       ├── 001.mp3              Track files padded by trackIdPadding or named explicitly
    │       ├── 002.mp3
    │       └── ...
    └── <AnotherEdition>\
        ...
```

### 2. Local Storage Structure
Synchronized files are stored within the application's internal storage (`context.filesDir`):

```text
<context.filesDir>/offline-library/editions/
    ├── <SanitizedEditionName>/
    │   ├── <EditionName>.json
    │   ├── <CoverImage>.jpg
    │   └── <trackFolderName>/
    │       ├── 001.mp3
    │       └── ...
    └── <AnotherSanitizedEditionName>/
        ...
```

> [!NOTE]
> Directory names are sanitized (removing special characters and stripping trailing slashes from SMB directory names) so that local storage folder names strictly match the edition names resolved from the mapping metadata.

---

## Synchronization Architecture & Operations

### 1. Sync Metadata
- Connects to the NAS base directory via SMB.
- Iterates over all edition subdirectories.
- Copies the mapping JSON file (`*.json`) to the local edition directory.
- Reads `meta.image` from the mapping and, if present on the NAS, copies the image file locally.
- Sets the local file timestamp to match the remote timestamp (`setLastModified`).
- **Reload:** Immediately reloads the in-memory `MappingRepository` so that newly downloaded editions are available for QR scanning without restarting the app.

### 2. Sync Tracks
- Performs all steps of **Sync Metadata**.
- Iterates over each edition's `<trackFolderName>` (e.g. `tracks/`) on the NAS.
- **Incremental Copy via Fingerprint:** Compares file size (`length()`) and modification timestamp (`lastModified()`). If the local file exists with matching size and timestamp, the download is skipped.
- **Atomic File Writing:** Tracks are downloaded to temporary files (e.g. `001.mp3.tmp`) and atomically renamed upon completion (`001.mp3`) to prevent playback corruption during parallel operations.
- **Timestamp Preservation:** Sets `localFile.setLastModified(remote.lastModified())`.
- **Reload:** Reloads the in-memory `MappingRepository` upon completion.
- **Additive Policy:** Synchronization is additive and overwriting (files are not deleted locally if removed from the NAS).

---

## Background Execution & Foreground Service

- **Foreground Service with Status Bar Notification:** Track synchronization runs via an Android Foreground Service displaying an ongoing notification with progress (`x / y Tracks, z%`).
- **Screen Off / App Minimized:** The foreground service prevents the OS from killing the process when the user locks the screen or navigates to other apps.
- **Permission (`POST_NOTIFICATIONS`):** On Android 13+ (API 33), the app requests notification permission. If denied, a notification warns the user that background sync cannot proceed with the screen locked.

---

## Parallel Playback & Priority (Offline-First)

When a QR code is scanned:

1. **Local Check:**
   `ResolveTrackUriUseCase` checks `LocalTrackResolver` for the local file in `context.filesDir/offline-library/editions/<SanitizedEdition>/`.
   If the file exists (and is not an incomplete `.tmp` file), a local `file:/...` URI is returned. Media3/ExoPlayer plays the track directly from disk without touching the network.
2. **Network Fallback:**
   If the local file does not exist (or is currently being downloaded as `.tmp`), the use case falls back to generating an SMB URI (`smb://...`) to stream directly from the NAS. This allows seamless gameplay while synchronization progresses.
3. **Completely Offline:**
   If network access is unavailable and the file is not cached locally, resolution fails immediately with a clear error message instead of hanging.

---

## Onboarding & Delta-Check at Startup

- **First-Run Onboarding:** Non-blocking banner/card on the main screen notifying the user about the offline download option in settings.
- **Startup Delta-Check:** When connected to the home network, a quick, non-blocking check compares the metadata fingerprint of the NAS against local storage. If new or updated tracks are found, an indicator prompts the user that updates are available for download.
