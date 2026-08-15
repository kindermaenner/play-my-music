Path resolution logic is centralized in the `ResolveTrackUriUseCase` within the `:domain` module.

## Resolution Logic
1.  **Local Check**: Use `LocalTrackResolver` (implemented as `AppLocalTrackResolver` in `:app`) to check for existence in the offline library.
2.  **NAS Path Building**: If not local, construct the UNC path using `PlaybackConfig`.
3.  **SMB URI Normalization**: Convert the path to an `smb://` URI.
