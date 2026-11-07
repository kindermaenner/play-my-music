# 04 Mapping Handling

Mappings define how scanned QR codes relate to editions, metadata, and audio files.

## Mapping File Structure

Each mapping JSON file represents an edition and contains metadata plus a list of tracks:

```json
{
  "meta": {
    "edition": "Edition Name",
    "kuerzel": "EN",
    "urlpart": "prefix_part",
    "image": "cover.jpg"
  },
  "titles": [
    {
      "id": 1,
      "artist": "Artist Name",
      "title": "Song Title",
      "year": 1985,
      "nasLocation": ""
    }
  ]
}
```

- `meta.edition`: Display name of the edition/collection.
- `meta.kuerzel`: Short abbreviation for UI badges.
- `meta.urlpart`: URL fragment parsed from the QR code (used together with `id` to uniquely identify the track).
- `meta.image`: Optional file name of the cover artwork image located alongside the mapping JSON.
- `titles[].id`: Numeric ID of the card/track.
- `titles[].nasLocation`: Optional explicit relative path override on the NAS. If empty or absent, standard `<edition>/<trackFolderName>/<paddedId>.mp3` path resolution is used.

## Loading Strategy

`AssetMappingLoader` aggregates mapping JSON files from three sources:
1. **`assets/mappings/`**: Default mappings packaged inside the APK.
2. **`assets/local/mappings/`**: Environment-specific or local test mappings for development.
3. **`context.filesDir/offline-library/editions/*/`**: Mappings synchronized from the NAS via Offline Sync.

## Filtering

An optional `mappingFilePostfix` setting filters files based on suffix or prefix, allowing users to select subsets or profiles.

## Dynamic Reloading

Mappings are loaded at application startup and reloaded dynamically after:
- Any metadata synchronization (`syncMetadata`).
- Any track synchronization (`syncTracks`).
- Changing the `mappingFilePostfix` in settings.
