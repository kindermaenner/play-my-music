# 04 Mapping Handling

Mappings define how QR codes relate to music files on the NAS or in the offline storage.

## Loading Strategy
The `AssetMappingLoader` scans multiple locations for JSON mapping files:
1.  **`assets/mappings/`**: Default mappings bundled with the app.
2.  **`assets/local/mappings/`**: Environment-specific or local-only mappings.
3.  **`filesDir/offline-library/editions/*/`**: Mappings synchronized via the Offline Sync Service.

## Filtering
A `mappingFilePostfix` filter is applied to exclude irrelevant mapping files. This allows managing different "profiles" or collections.

## nasLocation Override
If a `Song` object contains a `nasLocation` value, the standard path resolution (Edition/tracks/ID.mp3) is bypassed, and the app uses this explicit path instead.
