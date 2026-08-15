# 09 QR Scanning and Parsing

QR code recognition is the primary interaction method for users.

## Scanning Pipeline
- **Hardware Interaction**: CameraX `PreviewView` provides a live feed.
- **Image Analysis**: Google ML Kit Barcode Scanning identifies QR codes.

## Validation Logic
`QrUtils` handles the normalization of raw barcode content:
- Removes protocols (`http://`, `https://`).
- Strips URL parameters and fragments.
- Compares the result against the configured `qrValidation` prefix to prevent unauthorized or incompatible codes from being processed.
