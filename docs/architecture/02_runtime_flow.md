# 02 Runtime Flow

The process from capturing a QR code to playing the music follows a structured pipeline:

1.  **QR Scanning**: The camera captures a QR code via `CameraPreview`.
2.  **ViewModel Event**: The UI notifies `MainViewModel.onQrScanned(qrText)`.
3.  **Identification**: `IdentifyTrackUseCase` parses the QR text and looks up the track in the `MappingRepository`.
4.  **Resolution**: `ResolveTrackUriUseCase` decides whether to use a local file (via `LocalTrackResolver`) or generate a NAS SMB URI.
5.  **Playback**: `MainViewModel` updates the `UiState` with the new track and URI. The `MainScreen` prepares and plays the track using `ExoPlayer`.

---

## Sequence Diagram
See [diagrams/src/02_sequence.puml](diagrams/src/02_sequence.puml)

![Sequence Diagram](diagrams/out/02_sequence.svg)
