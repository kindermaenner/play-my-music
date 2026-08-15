# 08 Player Architecture

Audio playback is powered by the **Google Media3 ExoPlayer** engine.

## Custom SMB Integration
Standard Media3 does not support SMB out of the box. The app provides a `SmbDataSource` which:
- Implements the `BaseDataSource` interface.
- Uses **JCIFS-NG** (`SmbRandomAccessFile`) to handle byte-range requests.
- Supports seeking within network files.

## State Management
The `MainScreen` manages the player instance and observes its state (playing, buffering, errors). It handles play/pause toggling and media item preparation.
