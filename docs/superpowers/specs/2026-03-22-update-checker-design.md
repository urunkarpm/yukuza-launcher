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

No `@Named` or custom Hilt qualifier needed — Hilt distinguishes providers by return type (`GithubReleasesApi` is a unique type).

### `GithubReleaseDto` (new)
Moshi data class:
- `tag_name: String` — e.g. `"v1.3"`
- `body: String` — release notes markdown
- `assets: List<GithubAssetDto>`

### `GithubAssetDto` (new)
- `name: String` — filename
- `browser_download_url: String` — direct download URL

### `UpdateRepository` (new)
Concrete class with `@Singleton @Inject constructor(private val api: GithubReleasesApi)` — matching the existing repository pattern (no interface, no `RepositoryModule` change needed).

```kotlin
suspend fun checkForUpdate(): UpdateInfo?
```

Implementation:
1. Fetch latest release via `api` wrapped in `withTimeout(10_000)`.
2. Strip `v` prefix from `tag_name`.
3. Compare to `BuildConfig.VERSION_NAME` using **split-by-dot numeric comparison**:
   - Split both strings on `.`
   - Parse each segment as `Int`; if any segment cannot be parsed (e.g. `"3-beta"`), return `null`
   - Pad the shorter list with `0` so `1.3` and `1.3.0` compare equal
   - Return `UpdateInfo` only if remote version is strictly greater
4. Find the APK asset: filter `assets` where `name.endsWith(".apk")`, then prefer the first asset whose name contains `"release"`. If no `"release"` asset, take the first `.apk`. If no `.apk` at all, return `null`.
5. Return `null` on any exception (network, timeout, parse error).

### `NetworkModule` (modified)
New provider built identically to the existing providers — using the existing private `converterFactory()` helper already in `NetworkModule`, no `OkHttpClient` parameter (none exists in the current module):

```kotlin
@Provides @Singleton
fun provideGithubReleasesApi(): GithubReleasesApi =
    Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(converterFactory())
        .build()
        .create(GithubReleasesApi::class.java)
```

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
Three new fields:
```kotlin
val updateInfo: UpdateInfo? = null,
val isCheckingUpdate: Boolean = false,
val lastCheckWasUpToDate: Boolean = false,  // true briefly after a manual check finds no update
```

### `HomeViewModel` (modified)
- `init {}` — launches `checkForUpdate()` silently. Timeout enforced in repository.
- `fun checkForUpdate()` — sets `isCheckingUpdate = true`, invokes use case:
  - If update found: `updateInfo = result`, `lastCheckWasUpToDate = false`
  - If no update: `updateInfo = null`, `lastCheckWasUpToDate = true`
  - In both cases: `isCheckingUpdate = false`
- `fun clearUpToDateFlag()` — sets `lastCheckWasUpToDate = false` (called by the UI after the 2-second "You're up to date" display).
- `fun dismissUpdate()` — clears `updateInfo`.
- **No download/install logic on the ViewModel** — handled in the UI layer.

---

## UI Layer

### Download & Install (UI-layer side effect)
When the user taps "Download & Install" in `UpdateDialog`, the composable:

1. Captures `val context = LocalContext.current` at the top of the composable (Activity context, safe to use here as it is captured inside the composable scope and not escaped to a longer-lived lambda).
2. Sets local `isDownloading = true`.
3. Checks `DownloadManager` availability: `context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager` — if null, show toast "Downloader unavailable on this device", set `isDownloading = false`, return.
4. Checks install permission on Android 8+: `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls())` — show toast "Enable 'Install unknown apps' for Yukuza Launcher in Settings", set `isDownloading = false`, return.
5. Defines the destination file: `val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "yukuza-update.apk")`. The same `apkFile` object is used in step 5 and step 7.
6. Enqueues a `DownloadManager.Request(Uri.parse(downloadUrl))` with destination `Uri.fromFile(apkFile)`.
7. Registers a `BroadcastReceiver` for `ACTION_DOWNLOAD_COMPLETE` inside a `DisposableEffect` keyed on the download ID using `ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)`. The `onDispose` block unregisters the receiver.
8. In the `ACTION_DOWNLOAD_COMPLETE` receiver:
   - Verify the received download ID matches the enqueued ID.
   - Query `DownloadManager` with `DownloadManager.Query().setFilterById(downloadId)`.
   - Check `cursor.getInt(COLUMN_STATUS)`:
     - `STATUS_SUCCESSFUL` → fire `ACTION_INSTALL_PACKAGE` via `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)`.
     - `STATUS_FAILED` → show toast "Download failed", set `isDownloading = false`.

### `UpdateDialog` (new composable)
Shown on `HomeScreen` when `uiState.updateInfo != null`.
- Rendered inside a `GlassCard` (matches existing visual style).
- Shows: version label, release notes, "Download & Install" button, "Dismiss" button.
- Button shows an indeterminate `CircularProgressIndicator` while `isDownloading = true`.
- "Dismiss" calls `HomeViewModel.dismissUpdate()`.

### `QuickSettingsOverlay` (modified)
New parameters added to the composable signature (wired from `HomeScreen` via `uiState`):
```kotlin
isCheckingUpdate: Boolean,
updateInfo: UpdateInfo?,
lastCheckWasUpToDate: Boolean,
onCheckForUpdate: () -> Unit,
onClearUpToDateFlag: () -> Unit,
```

New "Check for Update" row behavior:
- Idle (`!isCheckingUpdate && updateInfo == null && !lastCheckWasUpToDate`): shows "Check for Update" button → calls `onCheckForUpdate`.
- `isCheckingUpdate = true`: shows a circular spinner.
- Update found (`updateInfo != null`): shows "Update available — tap to install".
- Up to date (`lastCheckWasUpToDate = true`): shows "You're up to date". A `LaunchedEffect(lastCheckWasUpToDate)` delays 2 seconds then calls `onClearUpToDateFlag()` to reset the flag.

`HomeScreen` wires all new parameters from `uiState` and `viewModel`.

---

## Manifest Changes

```xml
<!-- Required for Android 8+ to trigger package installer -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<!-- FileProvider to share downloaded APK with system installer -->
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

### `res/xml/file_provider_paths.xml` (new)
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="downloads" path="." />
</paths>
```

`path="."` covers the entire `getExternalFilesDir` tree, avoiding reliance on undocumented OEM-specific path strings.

---

## Error Handling

| Scenario | Handling |
|---|---|
| Network failure / timeout during version check | Silently ignored on auto-check; toast "Could not check for updates" on manual check |
| No `.apk` asset found in release | Treated as no update available |
| Version tag parse failure (e.g. `-beta` suffix) | Treated as no update available |
| Version segment count mismatch (e.g. `1.3` vs `1.3.0`) | Pad shorter list with `0` |
| `DownloadManager` unavailable (OEM disabled) | Toast "Downloader unavailable on this device", abort |
| `canRequestPackageInstalls()` returns false | Toast instructing user to enable permission in Settings, abort |
| Download `STATUS_FAILED` | Toast "Download failed" |

---

## No New Dependencies

All building blocks already present:
- Retrofit + Moshi — network + parsing
- Hilt — DI
- `DownloadManager` — system service
- `FileProvider` — from `androidx.core`, already on classpath
- `INTERNET` permission — already declared
- `ContextCompat.registerReceiver` — from `androidx.core`, already on classpath
