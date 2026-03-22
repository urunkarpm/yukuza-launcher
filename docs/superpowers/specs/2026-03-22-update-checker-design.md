# Update Checker — Design Spec
**Date:** 2026-03-22
**Status:** Approved

---

## Overview

Add an in-app update checker that queries GitHub Releases for `prasenjeet-urunkar/yukuza-launcher`, compares the latest release tag against the running `BuildConfig.VERSION_NAME`, and — when an update is available — lets the user download and install the APK directly from the app.

Two entry points:
1. **Silent check on launch** — runs automatically in `HomeViewModel.init`, shows a dialog if an update is found.
2. **Manual check in Quick Settings** — a "Check for Update" row in `QuickSettingsOverlay`.

---

## Data Layer

### `GithubReleasesApi` (new)
Retrofit interface, base URL `https://api.github.com/`.

```
GET /repos/prasenjeet-urunkar/yukuza-launcher/releases/latest
→ GithubReleaseDto
```

### `GithubReleaseDto` (new)
Moshi data class:
- `tag_name: String` — e.g. `"v1.3"`
- `body: String` — release notes markdown
- `assets: List<GithubAssetDto>`

### `GithubAssetDto` (new)
- `browser_download_url: String` — direct APK download URL

### `UpdateRepository` (new)
- Injected with `GithubReleasesApi`
- `suspend fun checkForUpdate(): UpdateInfo?`
  - Fetches latest release
  - Strips `v` prefix from `tag_name`, compares to `BuildConfig.VERSION_NAME`
  - Returns `UpdateInfo` if newer, `null` if already up to date or on error

### `NetworkModule` (modified)
- New `@Provides @Singleton fun provideGithubReleasesApi(): GithubReleasesApi` pointing at `https://api.github.com/`

---

## Domain Layer

### `UpdateInfo` (new)
```kotlin
data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)
```

### `CheckUpdateUseCase` (new)
Thin wrapper around `UpdateRepository.checkForUpdate()`.

---

## ViewModel Layer

### `HomeUiState` (modified)
Two new fields:
```kotlin
val updateInfo: UpdateInfo? = null,
val isCheckingUpdate: Boolean = false,
```

### `HomeViewModel` (modified)
- `init {}` — calls `checkForUpdate()` silently on startup
- `fun checkForUpdate()` — sets `isCheckingUpdate = true`, invokes use case, updates `updateInfo`
- `fun dismissUpdate()` — clears `updateInfo`
- `fun downloadAndInstall(context: Context, url: String)` — enqueues `DownloadManager` request, registers a `BroadcastReceiver` for `ACTION_DOWNLOAD_COMPLETE`, then fires `ACTION_INSTALL_PACKAGE` via `FileProvider`

---

## UI Layer

### `UpdateDialog` (new composable)
Shown on `HomeScreen` when `uiState.updateInfo != null`.
- Rendered inside a `GlassCard` (matches existing visual style)
- Shows: version label, release notes, "Download & Install" button, "Dismiss" button
- Button switches to loading state while downloading

### `QuickSettingsOverlay` (modified)
New "Check for Update" row:
- Idle: shows "Check for Update" button
- `isCheckingUpdate = true`: shows a spinner
- Update found: shows "Update available — tap to install"
- Up to date: briefly shows "You're up to date"

---

## Manifest Changes

```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

`res/xml/file_provider_paths.xml` exposes the `DownloadManager` downloads directory.

---

## Error Handling

- Network failure during version check: silently ignored on auto-check; shows "Could not check for updates" toast on manual check
- Download failure: `DownloadManager` handles retries; on `STATUS_FAILED` show a snackbar/toast
- Version comparison: if `tag_name` can't be parsed, treat as no update available

---

## No New Dependencies

All building blocks already present:
- Retrofit + Moshi — network + parsing
- Hilt — DI
- `DownloadManager` — system service, no extra library
- `FileProvider` — from `androidx.core` already on classpath
