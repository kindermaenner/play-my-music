# play-my-music

play-my-music is an Android QR-based audio player for local music libraries stored on a NAS.

The application scans a QR code, resolves it through one or more mapping files, builds the target audio path, and plays the matching track from an SMB-accessible network share.

## Features

- QR code scanning with CameraX and ML Kit
- Mapping-based track lookup
- SMB playback from a NAS via Media3 / ExoPlayer
- Optional metadata and track sync for offline usage
- Support for pause, resume, and next-card workflow
- File-based application configuration
- Optional local override files for private test data

## How It Works

The runtime flow is:

1. Scan a QR code.
2. Validate and parse the scanned value.
3. Resolve the edition/path segment and track ID from mapping JSON.
4. Build the NAS file path.
5. Convert the NAS path to an SMB URI.
6. Play the audio file.

## Configuration

Application settings are file-based.

- Default settings: `app/src/main/assets/settings/settings.json`
- Optional local override: `app/src/main/assets/local/settings/settings.json`

Example configuration:

```json
{
  "nasAddress": "192.168.0.45",
  "nasBaseFolder": "Multimedia\\playmymusic",
  "qrValidation": "example.org/game/de/",
  "mappingFilePostfix": "",
  "trackIdPadding": 3,
  "trackFolderName": "tracks",
  "smbUsername": "",
  "smbPassword": "",
  "smbDomain": ""
}
```

Field overview:

- `nasAddress`: NAS host or address. The app builds a UNC-style path automatically when needed.
- `nasBaseFolder`: Base directory below the NAS root.
- `qrValidation`: Prefix fragment used to validate and parse scanned QR values.
- `mappingFilePostfix`: Optional mapping file filter. Empty means all mapping JSON files are loaded.
- `trackIdPadding`: Number of digits used when generating file names such as `001.mp3` or `0001.mp3`.
- `trackFolderName`: Name of the track subfolder inside each edition directory.
- `smbUsername`, `smbPassword`, `smbDomain`: Optional SMB credentials. Leave empty for anonymous access.

## Mapping Files

Mapping files are loaded from:

- `app/src/main/assets/mappings/`
- `app/src/main/assets/local/mappings/` for local, non-committed test data
- local offline library under the app's internal storage after sync

Each mapping file contains:

- metadata for an edition or collection
- a list of tracks
- optional explicit NAS locations per track

If a track does not define `nasLocation`, the app builds the playback path as:

`<nasAddress>\<nasBaseFolder>\<edition>\<paddedTrackId>.mp3`

If `nasLocation` is present, the app uses:

`<nasAddress>\<nasLocation>`

## Offline Sync

The settings screen provides two sync actions:

- `Sync metadata`: copies mapping JSON files and optional artwork from the NAS to local app storage
- `Sync tracks`: also copies the track files from each edition's track folder for offline playback

After sync, the app prefers local files for:

- mapping JSON
- artwork images
- audio tracks

If no local copy exists, the app falls back to the remote NAS path.

## Local Test Data

Private local data should be stored in:

- `app/src/main/assets/local/settings/`
- `app/src/main/assets/local/mappings/`

These folders are intended for machine-local configuration and test data that should not be committed.

## Build

From the project root:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

To build the debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Notes

- The player is designed as a general QR-to-NAS audio player.
- QR parsing and mapping are configuration-driven.
- SMB playback behavior can depend on NAS configuration, share permissions, and supported authentication modes.
