# 05 Path Resolution

Generating valid SMB URIs from song metadata is handled by the `TrackPathResolver`.

## Standard Resolution
By default, the path follows this pattern:
`{NAS_ADDRESS}/{BASE_FOLDER}/{EDITION}/tracks/{ID_PADDED}.mp3`

- **`trackIdPadding`**: Track IDs are zero-padded (default 4 digits, e.g., `0001.mp3`).
- **`trackFolderName`**: The subfolder within an edition (default `tracks`).

## SMB URI Normalization
The `toPlayableUriOrNull` extension function ensures that local file paths or UNC network paths (e.g., `\\host\share`) are correctly formatted as `smb://` URIs for the `jcifs-ng` library.
