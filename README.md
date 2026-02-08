# FitSync (Android MVP)

Android phone app (Kotlin + Compose) that reads workouts from Samsung Health through Health Connect, exports TCX files with HR when available, and uploads to Strava.

## What this MVP does

- Reads Health Connect sessions in a configurable window (`daysBack`, default 7).
- Pulls associated streams in each session window:
  - `ExerciseSessionRecord`
  - `HeartRateRecord`
  - `DistanceRecord`
  - `SpeedRecord`
  - `TotalCaloriesBurnedRecord`
  - `ActiveCaloriesBurnedRecord`
  - `StepsRecord`
- Maps exercise type to Strava activity type:
  - indoor rowing/erg -> `rowing`
  - treadmill -> `run`
  - elliptical -> `elliptical`
  - stationary/spin bike -> `ride`
  - fallback -> `workout`
- Supports per-type user override mapping in Settings.
- Exports TCX to app cache and uploads to Strava Upload API.
- Uses Room for idempotent sync state (`lastSeenHash` dedupe).
- Uses WorkManager for periodic background sync (every 6h).

## Important assumptions

- Samsung Health is already writing workouts to Health Connect.
- Sync happens post-workout from the phone; no real-time watch streaming.
- Strava credentials are provided via Gradle properties.
- TCX is implemented first; architecture leaves room for adding FIT exporter later.

## Setup

1. Open this project in Android Studio (JDK 17).
2. In Strava, create an API app at [https://www.strava.com/settings/api](https://www.strava.com/settings/api).
3. Set your app callback URL to match this format:
   - `sh2s://oauth/callback` (or your custom scheme/host values)
4. Add the following to `~/.gradle/gradle.properties` or project `local.properties` via Gradle properties:

```properties
STRAVA_CLIENT_ID=YOUR_CLIENT_ID
STRAVA_CLIENT_SECRET=YOUR_CLIENT_SECRET
STRAVA_REDIRECT_SCHEME=sh2s
STRAVA_REDIRECT_HOST=oauth
```

5. Install/run on a phone with Health Connect available.
6. In app:
   - Login to Strava.
   - Grant Health Connect read permissions.
   - Tap `Scan Sessions`.
   - Tap `Sync Now`.

## Architecture

- `app/`: Application + DI-lite container
- `data/healthconnect`: Health Connect read/query/stream merge
- `data/strava`: OAuth token exchange, refresh, upload + polling
- `data/db`: Room entities/DAO for sync/dedupe status
- `data/preferences`: DataStore settings + mapping overrides
- `domain/export`: `TcxExporter`
- `domain/mapping`: exercise type -> Strava type mapping
- `domain/sync`: sync orchestration + hash idempotency
- `presentation`: Compose UI, navigation, view model
- `worker`: background sync worker

## Dedupe model

Per session row stores:
- `healthConnectSessionId`
- `startTime`
- `endTime`
- `lastSeenHash`
- `stravaUploadId`
- `stravaActivityId`
- `uploadStatus`
- `lastAttempt`
- `error`

If hash is unchanged and already synced, upload is skipped. If hash changes, behavior follows `reuploadOnHashChange` setting.

## Error states surfaced in UI

- Health Connect unavailable/not installed
- Health Connect permissions missing
- Strava not authenticated / token refresh failure
- Upload failure and rate-limit errors

## Tests

Unit tests for TCX exporter:
- Trackpoint timestamps remain monotonic
- HR values included when present
- Generated XML is parseable

## Next (optional) improvements

- Add FIT exporter implementation and feature flag TCX/FIT selection.
- Add richer upload retry/backoff strategy.
- Add a dedicated auth callback activity and stricter OAuth state validation.
- Add instrumentation tests for Health Connect permission flows.
