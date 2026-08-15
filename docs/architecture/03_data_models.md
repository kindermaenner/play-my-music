# 03 Data Models

The system relies on a set of core models to represent music tracks and application state.

## Core Models
- **`Song`**: The primary entity representing a music track. Contains metadata like `id`, `title`, `artist`, and `nasLocation`.
- **`MappingFile`**: Represents a JSON file containing metadata about an "edition" and its associated tracks.
- **`MetaData`**: Contains edition-level info: `edition` name, `kuerzel` (abbreviation), `urlpart`, and cover image path.
- **`AppSettings`**: The aggregate configuration including `PlaybackSettings` (NAS address, base folder) and `SmbAuthSettings` (credentials).

## Key-Value Structure
The `MappingLoader` creates a map where the key is a `SongKey` (combination of `urlPart` and `trackId`) to allow fast lookups during the scanning process.

---

## Class Diagram
See [diagrams/src/03_class.puml](diagrams/src/03_class.puml)

![Class Diagram](diagrams/out/03_class.svg)
