# 06 Offline Sync

To support playback without a constant network connection, the app provides a synchronization mechanism.

## Synchronized Content
- **Metadata**: JSON mapping files and associated artwork (cover images).
- **Audio Tracks**: MP3 files from the NAS are copied to the local device.

## Storage Structure
Cached files are stored in the app's internal storage:
`context.filesDir/offline-library/editions/{EditionName}/`

## Priority
The `MainScreen` implements an **Offline-First** strategy:
1. Check `OfflineLibraryStorage` for the local file.
2. If the local file is present, play it directly from disk.
3. If missing, fall back to streaming via SMB from the NAS.
