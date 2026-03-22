# Update Checker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add GitHub Releases update checking — silent check on launch and manual button in Quick Settings — with in-app APK download and install via `DownloadManager` + `FileProvider`.

**Architecture:** New Retrofit API interface (`GithubReleasesApi`) → `UpdateRepository` → `CheckUpdateUseCase` → `HomeViewModel` (state). UI: `UpdateDialog` composable on `HomeScreen` + new row in `QuickSettingsOverlay`. Download/install is a UI-layer side effect using `DownloadManager` and `FileProvider`; no Context is passed to the ViewModel.

**Tech Stack:** Retrofit + Moshi (already in project), Hilt DI, Jetpack Compose, `DownloadManager` (system service), `FileProvider` (androidx.core), MockK + coroutines-test for unit tests.

**Spec:** `docs/superpowers/specs/2026-03-22-update-checker-design.md`

---

## File Map

**Create:**
- `app/src/main/java/com/yukuza/launcher/data/remote/dto/GithubReleaseDto.kt` — Moshi DTOs for the GitHub releases API response
- `app/src/main/java/com/yukuza/launcher/data/remote/GithubReleasesApi.kt` — Retrofit interface
- `app/src/main/java/com/yukuza/launcher/domain/model/UpdateInfo.kt` — domain model
- `app/src/main/java/com/yukuza/launcher/data/repository/UpdateRepository.kt` — version comparison + APK selection logic
- `app/src/main/java/com/yukuza/launcher/domain/usecase/CheckUpdateUseCase.kt` — thin use case wrapper
- `app/src/main/java/com/yukuza/launcher/ui/overlay/UpdateDialog.kt` — download/install dialog composable
- `app/src/main/res/xml/file_provider_paths.xml` — FileProvider path declaration
- `app/src/test/java/com/yukuza/launcher/data/repository/UpdateRepositoryTest.kt` — unit tests

**Modify:**
- `app/src/main/AndroidManifest.xml` — add `REQUEST_INSTALL_PACKAGES` permission and `FileProvider` provider entry
- `app/src/main/java/com/yukuza/launcher/di/NetworkModule.kt` — add `provideGithubReleasesApi()`
- `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeViewModel.kt` — add 3 state fields and 3 new functions
- `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeScreen.kt` — render `UpdateDialog`, add update params to signature
- `app/src/main/java/com/yukuza/launcher/ui/overlay/QuickSettingsOverlay.kt` — add "Check for Update" row + 5 new params
- `app/src/main/java/com/yukuza/launcher/navigation/LauncherNavGraph.kt` — wire new ViewModel callbacks to `HomeScreen`
- `app/src/main/res/values/strings.xml` — add update-related strings

---

## Task 1: DTOs, API interface, and domain model

**Files:**
- Create: `app/src/main/java/com/yukuza/launcher/data/remote/dto/GithubReleaseDto.kt`
- Create: `app/src/main/java/com/yukuza/launcher/data/remote/GithubReleasesApi.kt`
- Create: `app/src/main/java/com/yukuza/launcher/domain/model/UpdateInfo.kt`

- [ ] **Step 1: Create GithubReleaseDto**

```kotlin
// app/src/main/java/com/yukuza/launcher/data/remote/dto/GithubReleaseDto.kt
package com.yukuza.launcher.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubReleaseDto(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "body") val body: String,
    @Json(name = "assets") val assets: List<GithubAssetDto>,
)

@JsonClass(generateAdapter = true)
data class GithubAssetDto(
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
)
```

- [ ] **Step 2: Create GithubReleasesApi**

```kotlin
// app/src/main/java/com/yukuza/launcher/data/remote/GithubReleasesApi.kt
package com.yukuza.launcher.data.remote

import com.yukuza.launcher.data.remote.dto.GithubReleaseDto
import retrofit2.http.GET

// Base URL: https://api.github.com/
interface GithubReleasesApi {
    @GET("repos/prasenjeet-urunkar/yukuza-launcher/releases/latest")
    suspend fun getLatestRelease(): GithubReleaseDto
}
```

- [ ] **Step 3: Create UpdateInfo domain model**

```kotlin
// app/src/main/java/com/yukuza/launcher/domain/model/UpdateInfo.kt
package com.yukuza.launcher.domain.model

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)
```

- [ ] **Step 4: Build the project to verify no compile errors**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/data/remote/dto/GithubReleaseDto.kt \
        app/src/main/java/com/yukuza/launcher/data/remote/GithubReleasesApi.kt \
        app/src/main/java/com/yukuza/launcher/domain/model/UpdateInfo.kt
git commit -m "feat: add GitHub releases DTOs, API interface, and UpdateInfo model"
```

---

## Task 2: UpdateRepository (TDD)

**Files:**
- Create: `app/src/main/java/com/yukuza/launcher/data/repository/UpdateRepository.kt`
- Create: `app/src/test/java/com/yukuza/launcher/data/repository/UpdateRepositoryTest.kt`

The repository contains all version-comparison logic — this is where the value is and where tests matter most.

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/yukuza/launcher/data/repository/UpdateRepositoryTest.kt
package com.yukuza.launcher.data.repository

import com.yukuza.launcher.data.remote.GithubReleasesApi
import com.yukuza.launcher.data.remote.dto.GithubAssetDto
import com.yukuza.launcher.data.remote.dto.GithubReleaseDto
import com.yukuza.launcher.domain.model.UpdateInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateRepositoryTest {

    private val api = mockk<GithubReleasesApi>()
    private val repo = UpdateRepository(api, currentVersion = "1.2")

    private fun release(
        tag: String,
        assetName: String = "yukuza-release.apk",
        url: String = "https://example.com/yukuza-release.apk",
    ) = GithubReleaseDto(
        tagName = tag,
        body = "Release notes",
        assets = listOf(GithubAssetDto(name = assetName, browserDownloadUrl = url)),
    )

    @Test
    fun `returns UpdateInfo when remote version is newer`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.3")
        val result = repo.checkForUpdate()
        assertEquals(UpdateInfo("1.3", "https://example.com/yukuza-release.apk", "Release notes"), result)
    }

    @Test
    fun `returns null when remote version equals current`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.2")
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `returns null when remote version is older`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.1")
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `correctly compares minor versions above 9`() = runTest {
        // String "1.10" > "1.9" lexicographically is false — numeric comparison must be used
        val repoV9 = UpdateRepository(api, currentVersion = "1.9")
        coEvery { api.getLatestRelease() } returns release("v1.10")
        val result = repoV9.checkForUpdate()
        assertEquals("1.10", result?.latestVersion)
    }

    @Test
    fun `pads missing segments — 1_3 equals 1_3_0`() = runTest {
        val repoV130 = UpdateRepository(api, currentVersion = "1.3.0")
        coEvery { api.getLatestRelease() } returns release("v1.3")
        assertNull(repoV130.checkForUpdate())
    }

    @Test
    fun `returns null when tag cannot be parsed`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.3-beta")
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `prefers asset with release in name`() = runTest {
        val releaseWithMultiple = GithubReleaseDto(
            tagName = "v1.3",
            body = "notes",
            assets = listOf(
                GithubAssetDto("yukuza-debug.apk", "https://example.com/debug.apk"),
                GithubAssetDto("yukuza-release.apk", "https://example.com/release.apk"),
            ),
        )
        coEvery { api.getLatestRelease() } returns releaseWithMultiple
        assertEquals("https://example.com/release.apk", repo.checkForUpdate()?.downloadUrl)
    }

    @Test
    fun `returns null when no apk asset found`() = runTest {
        val noApk = GithubReleaseDto("v1.3", "notes", listOf(GithubAssetDto("source.zip", "https://example.com/source.zip")))
        coEvery { api.getLatestRelease() } returns noApk
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `returns null on network exception`() = runTest {
        coEvery { api.getLatestRelease() } throws RuntimeException("network error")
        assertNull(repo.checkForUpdate())
    }
}
```

- [ ] **Step 2: Run tests to verify they all fail**

Run: `./gradlew :app:test --tests "com.yukuza.launcher.data.repository.UpdateRepositoryTest" --info 2>&1 | tail -20`
Expected: FAILED — `UpdateRepository` class does not exist yet

- [ ] **Step 3: Create UpdateRepository**

```kotlin
// app/src/main/java/com/yukuza/launcher/data/repository/UpdateRepository.kt
package com.yukuza.launcher.data.repository

import com.yukuza.launcher.BuildConfig
import com.yukuza.launcher.data.remote.GithubReleasesApi
import com.yukuza.launcher.domain.model.UpdateInfo
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val api: GithubReleasesApi,
    // Injected for testability; production always uses BuildConfig.VERSION_NAME
    private val currentVersion: String = BuildConfig.VERSION_NAME,
) {
    suspend fun checkForUpdate(): UpdateInfo? = try {
        withTimeout(10_000) {
            val release = api.getLatestRelease()
            val remoteVersion = release.tagName.removePrefix("v")

            if (!isNewerVersion(remoteVersion, currentVersion)) return@withTimeout null

            val apkAsset = release.assets
                .filter { it.name.endsWith(".apk") }
                .let { apks -> apks.firstOrNull { it.name.contains("release") } ?: apks.firstOrNull() }
                ?: return@withTimeout null

            UpdateInfo(
                latestVersion = remoteVersion,
                downloadUrl = apkAsset.browserDownloadUrl,
                releaseNotes = release.body,
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: return false }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: return false }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false // equal
    }
}
```

- [ ] **Step 4: Run tests to verify they all pass**

Run: `./gradlew :app:test --tests "com.yukuza.launcher.data.repository.UpdateRepositoryTest"`
Expected: BUILD SUCCESSFUL, all tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/data/repository/UpdateRepository.kt \
        app/src/test/java/com/yukuza/launcher/data/repository/UpdateRepositoryTest.kt
git commit -m "feat: add UpdateRepository with version comparison logic (TDD)"
```

---

## Task 3: CheckUpdateUseCase + Hilt wiring

**Files:**
- Create: `app/src/main/java/com/yukuza/launcher/domain/usecase/CheckUpdateUseCase.kt`
- Modify: `app/src/main/java/com/yukuza/launcher/di/NetworkModule.kt`

- [ ] **Step 1: Create CheckUpdateUseCase**

```kotlin
// app/src/main/java/com/yukuza/launcher/domain/usecase/CheckUpdateUseCase.kt
package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.UpdateRepository
import com.yukuza.launcher.domain.model.UpdateInfo
import javax.inject.Inject

class CheckUpdateUseCase @Inject constructor(private val repo: UpdateRepository) {
    suspend operator fun invoke(): UpdateInfo? = repo.checkForUpdate()
}
```

- [ ] **Step 2: Add GithubReleasesApi provider to NetworkModule**

Open `app/src/main/java/com/yukuza/launcher/di/NetworkModule.kt`. Add this function after the existing three providers (the `converterFactory()` private helper is already there — reuse it):

```kotlin
@Provides
@Singleton
fun provideGithubReleasesApi(): GithubReleasesApi =
    Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(converterFactory())
        .build()
        .create(GithubReleasesApi::class.java)
```

Also add the import at the top of the file:
```kotlin
import com.yukuza.launcher.data.remote.GithubReleasesApi
```

- [ ] **Step 3: Build to verify Hilt wiring compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/domain/usecase/CheckUpdateUseCase.kt \
        app/src/main/java/com/yukuza/launcher/di/NetworkModule.kt
git commit -m "feat: add CheckUpdateUseCase and wire GithubReleasesApi in NetworkModule"
```

---

## Task 4: Manifest permissions + FileProvider paths

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/file_provider_paths.xml`

- [ ] **Step 1: Create file_provider_paths.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="downloads" path="." />
</paths>
```

Save to: `app/src/main/res/xml/file_provider_paths.xml`

- [ ] **Step 2: Add permission and FileProvider to AndroidManifest.xml**

After the existing `<uses-permission>` lines, add:
```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

Inside `<application>`, after the existing `<receiver>` block, add:
```xml
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

- [ ] **Step 3: Build to confirm manifest parses without error**

Run: `./gradlew :app:processDebugManifest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml \
        app/src/main/res/xml/file_provider_paths.xml
git commit -m "feat: add REQUEST_INSTALL_PACKAGES permission and FileProvider"
```

---

## Task 5: HomeUiState + HomeViewModel update logic

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeViewModel.kt`

- [ ] **Step 1: Add new fields to HomeUiState**

In `HomeViewModel.kt`, find the `HomeUiState` data class (lines 37–52) and add three fields at the end:

```kotlin
val updateInfo: UpdateInfo? = null,
val isCheckingUpdate: Boolean = false,
val lastCheckWasUpToDate: Boolean = false,
```

Also add the import at the top of the file:
```kotlin
import com.yukuza.launcher.domain.model.UpdateInfo
import com.yukuza.launcher.domain.usecase.CheckUpdateUseCase
```

- [ ] **Step 2: Inject CheckUpdateUseCase into HomeViewModel**

In the `HomeViewModel` constructor (`@HiltViewModel class HomeViewModel @Inject constructor(...)`), add:
```kotlin
private val checkUpdate: CheckUpdateUseCase,
```

- [ ] **Step 3: Add checkForUpdate, dismissUpdate, and clearUpToDateFlag functions**

Add these three functions after `toggleCityPicker()` at the end of `HomeViewModel`:

```kotlin
fun checkForUpdate() {
    viewModelScope.launch {
        _uiState.update { it.copy(isCheckingUpdate = true) }
        val info = checkUpdate()
        _uiState.update {
            it.copy(
                isCheckingUpdate = false,
                updateInfo = info,
                lastCheckWasUpToDate = info == null,
            )
        }
    }
}

fun dismissUpdate() = _uiState.update { it.copy(updateInfo = null) }

fun clearUpToDateFlag() = _uiState.update { it.copy(lastCheckWasUpToDate = false) }
```

- [ ] **Step 4: Call checkForUpdate silently in init**

In the `init` block of `HomeViewModel`, after the existing four `viewModelScope.launch` calls, add:
```kotlin
viewModelScope.launch { checkUpdate() /* silent — ignore result */ }
```

Wait — the silent check on init should update state too. Replace the above with:
```kotlin
checkForUpdate()
```

(This calls the existing `checkForUpdate()` function which already updates `_uiState`.)

- [ ] **Step 5: Build to verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeViewModel.kt
git commit -m "feat: add update state and checkForUpdate/dismiss/clear to HomeViewModel"
```

---

## Task 6: Add update strings to resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add update-related strings**

Open `app/src/main/res/values/strings.xml`. After the last `</string>` line and before `</resources>`, add:

```xml
<!-- Update Checker -->
<string name="update_check_for_update">Check for Update</string>
<string name="update_checking">Checking…</string>
<string name="update_available">Update available — tap to install</string>
<string name="update_up_to_date">You\'re up to date</string>
<string name="update_dialog_title">Update Available</string>
<string name="update_dialog_version">Version %1$s is available</string>
<string name="update_dialog_install">Download &amp; Install</string>
<string name="update_dialog_dismiss">Dismiss</string>
<string name="update_toast_unavailable">Downloader unavailable on this device</string>
<string name="update_toast_no_permission">Enable \'Install unknown apps\' for Yukuza Launcher in Settings</string>
<string name="update_toast_failed">Download failed</string>
<string name="update_toast_no_update">Could not check for updates</string>
```

- [ ] **Step 2: Build to verify no resource errors**

Run: `./gradlew :app:mergeDebugResources`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add update checker strings"
```

---

## Task 7: UpdateDialog composable

**Files:**
- Create: `app/src/main/java/com/yukuza/launcher/ui/overlay/UpdateDialog.kt`

This composable handles the download-and-install side effect entirely in the UI layer — no Context leaks into the ViewModel.

- [ ] **Step 1: Create UpdateDialog**

```kotlin
// app/src/main/java/com/yukuza/launcher/ui/overlay/UpdateDialog.kt
package com.yukuza.launcher.ui.overlay

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yukuza.launcher.R
import com.yukuza.launcher.domain.model.UpdateInfo
import com.yukuza.launcher.ui.components.glass.GlassCard
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }

    // Register BroadcastReceiver for download completion — auto-unregistered on dispose.
    // Keyed on downloadId so the receiver is re-registered with the new ID each time a
    // download is enqueued (per spec).
    DisposableEffect(downloadId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != downloadId) return

                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
                val query = DownloadManager.Query().setFilterById(completedId)
                val cursor = dm.query(query)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = it.getInt(statusCol)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val apkFile = File(
                                ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                "yukuza-update.apk",
                            )
                            val apkUri = FileProvider.getUriForFile(
                                ctx,
                                "${ctx.packageName}.fileprovider",
                                apkFile,
                            )
                            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                                data = apkUri
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(installIntent)
                        } else {
                            Toast.makeText(ctx, ctx.getString(R.string.update_toast_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                isDownloading = false
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        GlassCard(
            Modifier
                .fillMaxWidth(0.5f)
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.update_dialog_version, updateInfo.latestVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 6,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            startDownload(
                                context = context,
                                url = updateInfo.downloadUrl,
                                onDownloadStarted = { id ->
                                    downloadId = id
                                    isDownloading = true
                                },
                            )
                        },
                        enabled = !isDownloading,
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.update_dialog_install))
                    }
                    OutlinedButton(onClick = onDismiss, enabled = !isDownloading) {
                        Text(stringResource(R.string.update_dialog_dismiss), color = Color.White)
                    }
                }
            }
        }
    }
}

private fun startDownload(
    context: Context,
    url: String,
    onDownloadStarted: (Long) -> Unit,
) {
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (dm == null) {
        Toast.makeText(context, context.getString(R.string.update_toast_unavailable), Toast.LENGTH_SHORT).show()
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        Toast.makeText(context, context.getString(R.string.update_toast_no_permission), Toast.LENGTH_LONG).show()
        return
    }
    val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "yukuza-update.apk")
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Yukuza Launcher update")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setDestinationUri(Uri.fromFile(apkFile))
    val id = dm.enqueue(request)
    onDownloadStarted(id)
}
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/overlay/UpdateDialog.kt
git commit -m "feat: add UpdateDialog with DownloadManager and FileProvider install flow"
```

---

## Task 8: QuickSettingsOverlay update row

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/overlay/QuickSettingsOverlay.kt`

- [ ] **Step 1: Add new parameters to QuickSettingsOverlay signature**

In `QuickSettingsOverlay.kt`, update the function signature of `QuickSettingsOverlay` to add five new parameters after `onCitySelected`:

```kotlin
isCheckingUpdate: Boolean,
updateInfo: com.yukuza.launcher.domain.model.UpdateInfo?,
lastCheckWasUpToDate: Boolean,
onCheckForUpdate: () -> Unit,
onClearUpToDateFlag: () -> Unit,
```

- [ ] **Step 2: Add update row inside the Column**

Inside `QuickSettingsOverlay`, in the `Column` block, add this after the `CityDropdown(...)` call:

```kotlin
// Update checker row
UpdateCheckerRow(
    isCheckingUpdate = isCheckingUpdate,
    updateInfo = updateInfo,
    lastCheckWasUpToDate = lastCheckWasUpToDate,
    onCheckForUpdate = onCheckForUpdate,
    onClearUpToDateFlag = onClearUpToDateFlag,
)
```

- [ ] **Step 3: Add UpdateCheckerRow private composable**

Add this private function at the bottom of the file:

```kotlin
@Composable
private fun UpdateCheckerRow(
    isCheckingUpdate: Boolean,
    updateInfo: com.yukuza.launcher.domain.model.UpdateInfo?,
    lastCheckWasUpToDate: Boolean,
    onCheckForUpdate: () -> Unit,
    onClearUpToDateFlag: () -> Unit,
) {
    // Auto-clear "up to date" flag after 2 seconds
    if (lastCheckWasUpToDate) {
        androidx.compose.runtime.LaunchedEffect(lastCheckWasUpToDate) {
            kotlinx.coroutines.delay(2_000)
            onClearUpToDateFlag()
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            isCheckingUpdate -> {
                Text(
                    androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.update_checking),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            }
            updateInfo != null -> {
                Text(
                    androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.update_available),
                    color = Color(0xFFB39DFF),
                    modifier = Modifier.weight(1f),
                )
            }
            lastCheckWasUpToDate -> {
                Text(
                    androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.update_up_to_date),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                )
            }
            else -> {
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onCheckForUpdate) {
                    Text(
                        androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.update_check_for_update),
                        color = Color(0xFFB39DFF),
                    )
                }
            }
        }
    }
}
```

Also add missing imports at the top:
```kotlin
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
```
(Check if already present — only add what's missing.)

- [ ] **Step 4: Build to verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/overlay/QuickSettingsOverlay.kt
git commit -m "feat: add Check for Update row to QuickSettingsOverlay"
```

---

## Task 9: Wire everything in HomeScreen and LauncherNavGraph

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/yukuza/launcher/navigation/LauncherNavGraph.kt`

This task connects all the new wiring so the app runs end-to-end.

- [ ] **Step 1: Add update parameters to HomeScreen signature**

In `HomeScreen.kt`, add these new parameters to the `HomeScreen` composable (after `onWeatherClick`):

```kotlin
onNightModeToggle: () -> Unit = {},
onCheckForUpdate: () -> Unit = {},
onDismissUpdate: () -> Unit = {},
onClearUpToDateFlag: () -> Unit = {},
```

- [ ] **Step 2: Render UpdateDialog in HomeScreen**

In the `Box` in `HomeScreen`, after the `CityPickerPopup` block, add:

```kotlin
// Update dialog
if (uiState.updateInfo != null) {
    com.yukuza.launcher.ui.overlay.UpdateDialog(
        updateInfo = uiState.updateInfo,
        onDismiss = onDismissUpdate,
    )
}
```

- [ ] **Step 3: Wire QuickSettingsOverlay call in HomeScreen**

Search `HomeScreen.kt` for any existing `QuickSettingsOverlay` call. If it exists, update its call site to pass the five new parameters:
```kotlin
isCheckingUpdate = uiState.isCheckingUpdate,
updateInfo = uiState.updateInfo,
lastCheckWasUpToDate = uiState.lastCheckWasUpToDate,
onCheckForUpdate = onCheckForUpdate,
onClearUpToDateFlag = onClearUpToDateFlag,
```

`QuickSettingsOverlay` is NOT currently called from `HomeScreen` (confirmed by reading the source). Add the conditional block after the `CityPickerPopup` block:

```kotlin
if (uiState.showSettings) {
    com.yukuza.launcher.ui.overlay.QuickSettingsOverlay(
        onDismiss = onSettingsToggle,
        isNightMode = uiState.isNightMode,
        onNightModeToggle = onNightModeToggle,
        cityQuery = uiState.cityQuery,
        citySuggestions = uiState.citySuggestions,
        cityName = uiState.cityName,
        onCityQueryChange = onCityQueryChange,
        onCitySelected = onCitySelected,
        isCheckingUpdate = uiState.isCheckingUpdate,
        updateInfo = uiState.updateInfo,
        lastCheckWasUpToDate = uiState.lastCheckWasUpToDate,
        onCheckForUpdate = onCheckForUpdate,
        onClearUpToDateFlag = onClearUpToDateFlag,
    )
}
```

- [ ] **Step 4: Wire new callbacks in LauncherNavGraph**

In `LauncherNavGraph.kt`, update the `HomeScreen(...)` call to pass:
```kotlin
onNightModeToggle = { vm.setNightMode(!state.isNightMode) },
onCheckForUpdate = vm::checkForUpdate,
onDismissUpdate = vm::dismissUpdate,
onClearUpToDateFlag = vm::clearUpToDateFlag,
```

- [ ] **Step 5: Build the full app**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeScreen.kt \
        app/src/main/java/com/yukuza/launcher/navigation/LauncherNavGraph.kt
git commit -m "feat: wire update dialog and check into HomeScreen and NavGraph"
```

---

## Task 10: Final build and smoke test

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew :app:test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Build release APK**

Run: `./gradlew :app:assembleRelease`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual smoke test checklist (install on device/emulator)**

- [ ] App launches without crash
- [ ] If GitHub has a release newer than 1.2: `UpdateDialog` appears automatically
- [ ] Dismiss button closes the dialog
- [ ] Open Quick Settings → "Check for Update" button is visible
- [ ] Tap "Check for Update" → spinner appears briefly → result shown
- [ ] If update available: tap "Download & Install" → spinner → system installer opens
- [ ] If up to date: "You're up to date" appears, fades after 2 seconds

- [ ] **Step 4: Final commit tag**

```bash
git add -p  # stage any remaining changes
git commit -m "feat: complete update checker — GitHub releases, download, and install"
```
