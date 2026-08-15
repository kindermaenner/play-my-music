# 09 QR Scanning and Parsing

QR code recognition is the primary interaction method for users.

## Scanning Pipeline
- **Hardware Interaction**: CameraX `PreviewView` provides a live feed.
- **Image Analysis**: Google ML Kit Barcode Scanning identifies QR codes.

## Validation Logic
The normalization and validation logic is encapsulated in the `IdentifyTrackUseCase`. It handles:
- Removing protocols (`http://`, `https://`).
- Stripping URL parameters and fragments.
- Validating the resulting suffix against the configured prefix.
- Identifying the `urlPart` and `trackId`.
