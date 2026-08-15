# 07 Configuration System

The app uses a layered configuration approach to allow both flexibility for the user and sane defaults.

## Storage
Persistent settings are stored using **Jetpack DataStore (Preferences)**.

## Layering & Merging
1. **Hardcoded Defaults**: `assets/settings/settings.json`.
2. **Build-time Overrides**: `assets/local/settings/settings.json` (if present).
3. **User Overrides**: Values edited in the `SettingsScreen` (highest priority).

## Key Parameters
- **NAS Address & Base Folder**: Locations for music storage.
- **QR Validation Prefix**: Ensures only valid QR codes are processed.
- **SMB Credentials**: Username, password, and domain for network access.
