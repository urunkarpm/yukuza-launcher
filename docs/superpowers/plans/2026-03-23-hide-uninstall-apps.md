# Hide & Uninstall Apps via Long-Press — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a long-press context menu (dim scrim + side panel) to app icons in both the full app list and home row, letting users hide apps, restore them from a "Hidden Apps" section, and uninstall or disable apps.

**Architecture:** Extend `AppOrderEntity` with an `isHidden` column (DB v2→v3 migration), expose filtered flows (`getVisibleApps`, `getHiddenApps`) from `AppRepository`, wire a new `AppContextMenuOverlay` composable triggered by long-press in `AppListScreen` and `HomeScreen`. Fix `reorder()` so it never clobbers `isHidden`. Fix `PackageChangeSyncWorker` so uninstalled apps are removed from Room and the list refreshes automatically.

**Tech Stack:** Kotlin, Jetpack Compose (TV Material 3 & Foundation), Hilt, Room DB, Kotlin Coroutines/Flow, AndroidX WorkManager

---

## File Map

| Action | File |
|--------|------|
| Modify | `app/src/main/java/com/yukuza/launcher/data/entity/AppOrderEntity.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/data/db/LauncherDatabase.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/data/db/AppOrderDao.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/di/DatabaseModule.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/domain/model/AppInfo.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/data/repository/AppRepository.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/domain/usecase/GetAppsUseCase.kt` |
| Create | `app/src/main/java/com/yukuza/launcher/domain/usecase/GetVisibleAppsUseCase.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/data/worker/PackageChangeSyncWorker.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/ui/screen/apps/AppListViewModel.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeViewModel.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/ui/components/AppIcon.kt` |
| Create | `app/src/main/java/com/yukuza/launcher/ui/overlay/AppContextMenuOverlay.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/ui/screen/apps/AppListScreen.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/ui/components/AppRow.kt` |
| Modify | `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeScreen.kt` |

---

## Task 1: Extend AppOrderEntity and bump DB to v3

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/data/entity/AppOrderEntity.kt`
- Modify: `app/src/main/java/com/yukuza/launcher/data/db/LauncherDatabase.kt`
- Modify: `app/src/main/java/com/yukuza/launcher/di/DatabaseModule.kt`

- [ ] **Step 1: Add `isHidden` to `AppOrderEntity`**

Replace the entire file content:

```kotlin
package com.yukuza.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_order")
data class AppOrderEntity(
    @PrimaryKey val packageName: String,
    val order: Int,
    @ColumnInfo(name = "isHidden", defaultValue = "0")
    val isHidden: Boolean = false,
)
```

- [ ] **Step 2: Add `MIGRATION_2_3` to `LauncherDatabase` and bump version to 3**

Replace the `@Database` annotation version and add the migration to the companion object:

```kotlin
@Database(
    entities = [
        AppOrderEntity::class,
        AppColorCacheEntity::class,
        WeatherCacheEntity::class,
        AppLaunchCountEntity::class,
    ],
    version = 3,          // was 2
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appOrderDao(): AppOrderDao
    abstract fun appColorCacheDao(): AppColorCacheDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun appLaunchCountDao(): AppLaunchCountDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_launch_count` " +
                        "(`packageName` TEXT NOT NULL, `count` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`packageName`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_order ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
```

- [ ] **Step 3: Register `MIGRATION_2_3` in `DatabaseModule`**

In `DatabaseModule.provideDatabase()`, change:
```kotlin
.addMigrations(LauncherDatabase.MIGRATION_1_2)
```
to:
```kotlin
.addMigrations(LauncherDatabase.MIGRATION_1_2, LauncherDatabase.MIGRATION_2_3)
```

- [ ] **Step 4: Build to verify no compile errors**

```bash
cd "/home/prasenjeet-urunkar/Documents/Yukuza Launcher"
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/data/entity/AppOrderEntity.kt \
        app/src/main/java/com/yukuza/launcher/data/db/LauncherDatabase.kt \
        app/src/main/java/com/yukuza/launcher/di/DatabaseModule.kt
git commit -m "feat: extend AppOrderEntity with isHidden field, bump DB to v3"
```

---

## Task 2: Add new AppOrderDao methods

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/data/db/AppOrderDao.kt`

- [ ] **Step 1: Add `insertIfAbsent`, `setHidden`, and `updateOrder` methods**

Replace the entire file:

```kotlin
package com.yukuza.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.yukuza.launcher.data.entity.AppOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOrderDao {
    @Query("SELECT * FROM app_order ORDER BY `order` ASC")
    fun getAll(): Flow<List<AppOrderEntity>>

    @Upsert
    suspend fun upsert(entity: AppOrderEntity)

    // NOT used by reorder() or hide/unhide — kept for any legacy callers only
    @Upsert
    suspend fun upsertAll(entities: List<AppOrderEntity>)

    // Inserts a row only if it doesn't already exist — preserves existing order and isHidden
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: AppOrderEntity)

    // Updates isHidden without touching order
    @Query("UPDATE app_order SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, isHidden: Boolean)

    // Updates order without touching isHidden
    @Query("UPDATE app_order SET `order` = :order WHERE packageName = :packageName")
    suspend fun updateOrder(packageName: String, order: Int)

    @Query("DELETE FROM app_order WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT packageName FROM app_order")
    suspend fun getAllPackageNames(): List<String>
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/data/db/AppOrderDao.kt
git commit -m "feat: add insertIfAbsent, setHidden, updateOrder DAO methods"
```

---

## Task 3: Extend AppInfo model

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/domain/model/AppInfo.kt`

- [ ] **Step 1: Add `isHidden` field to `AppInfo`**

Replace the entire file:

```kotlin
package com.yukuza.launcher.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppInfo(
    val packageName: String,
    val label: String,
    val order: Int,
    val dominantColor: Color = Color.White,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
) {
    companion object {
        const val PACKAGE_TV_SETTINGS = "com.android.tv.settings"
    }
}
```

- [ ] **Step 2: Build to verify — `AppInfo` is used widely, check for compile errors**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL` (the new field has a default so existing construction sites don't break)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/domain/model/AppInfo.kt
git commit -m "feat: add isHidden field to AppInfo"
```

---

## Task 4: Update AppRepository

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/data/repository/AppRepository.kt`

- [ ] **Step 1: Replace `AppRepository` with updated version**

Replace the entire file:

```kotlin
package com.yukuza.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.yukuza.launcher.data.db.AppColorCacheDao
import com.yukuza.launcher.data.db.AppLaunchCountDao
import com.yukuza.launcher.data.db.AppOrderDao
import com.yukuza.launcher.data.entity.AppColorCacheEntity
import com.yukuza.launcher.data.entity.AppLaunchCountEntity
import com.yukuza.launcher.data.entity.AppOrderEntity
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.domain.model.AppInfo.Companion.PACKAGE_TV_SETTINGS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pm: PackageManager,
    private val appOrderDao: AppOrderDao,
    private val colorCacheDao: AppColorCacheDao,
    private val launchCountDao: AppLaunchCountDao,
) {
    private val countRefresh = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    private fun queryLaunchableApps(): Map<String, ResolveInfo> {
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val standard = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return (pm.queryIntentActivities(leanback, 0) + pm.queryIntentActivities(standard, 0))
            .filter { it.activityInfo.packageName != context.packageName }
            .associateBy { it.activityInfo.packageName }
    }

    /** All apps (including hidden). Renamed from getApps(). */
    fun getAllApps(): Flow<ImmutableList<AppInfo>> =
        appOrderDao.getAll().combine(countRefresh) { order, _ -> order }.map { orderEntities ->
            val installed = queryLaunchableApps()
            val counts = launchCountDao.getAll().associateBy({ it.packageName }, { it.count })
            val hiddenSet = orderEntities.filter { it.isHidden }.map { it.packageName }.toSet()

            val tvSettings = AppInfo(
                packageName = PACKAGE_TV_SETTINGS,
                label = "Settings",
                order = Int.MAX_VALUE,
                dominantColor = Color(0xFF607D8B),
                isPinned = true,
                isHidden = PACKAGE_TV_SETTINGS in hiddenSet,
            )

            val regularApps = installed.keys
                .filter { it != PACKAGE_TV_SETTINGS }
                .map { pkg ->
                    val resolveInfo = installed[pkg]!!
                    val color = colorCacheDao.get(pkg)?.let { Color(it.dominantColor) } ?: Color.White
                    AppInfo(
                        packageName = pkg,
                        label = resolveInfo.loadLabel(pm).toString(),
                        order = counts[pkg] ?: 0,
                        dominantColor = color,
                        isHidden = pkg in hiddenSet,
                    )
                }
                .sortedByDescending { it.order }

            (listOf(tvSettings) + regularApps).toImmutableList()
        }.flowOn(Dispatchers.IO)

    /** Only apps not hidden. Use this for the home row and visible section of app list. */
    fun getVisibleApps(): Flow<ImmutableList<AppInfo>> =
        getAllApps().map { apps -> apps.filter { !it.isHidden }.toImmutableList() }

    /** Only hidden apps. Use this for the "Hidden Apps" section of app list. */
    fun getHiddenApps(): Flow<ImmutableList<AppInfo>> =
        getAllApps().map { apps -> apps.filter { it.isHidden }.toImmutableList() }

    /** Hides an app. Inserts a row if none exists, then sets isHidden = true. */
    suspend fun hideApp(packageName: String) = withContext(Dispatchers.IO) {
        appOrderDao.insertIfAbsent(AppOrderEntity(packageName, 0, isHidden = false))
        appOrderDao.setHidden(packageName, true)
    }

    /** Unhides an app. Inserts a row if none exists, then sets isHidden = false. */
    suspend fun unhideApp(packageName: String) = withContext(Dispatchers.IO) {
        appOrderDao.insertIfAbsent(AppOrderEntity(packageName, 0, isHidden = false))
        appOrderDao.setHidden(packageName, false)
    }

    /**
     * Reorders apps. Uses targeted UPDATE (not upsert) to preserve isHidden values.
     * Inserts rows for any package not yet in app_order before updating.
     */
    suspend fun reorder(packages: List<String>) = withContext(Dispatchers.IO) {
        packages.forEachIndexed { i, pkg ->
            appOrderDao.insertIfAbsent(AppOrderEntity(pkg, i, isHidden = false))
            appOrderDao.updateOrder(pkg, i)
        }
    }

    /** Forces a re-query of PackageManager (e.g. after a disable action). */
    fun refresh() {
        countRefresh.tryEmit(Unit)
    }

    /**
     * Deletes app_order rows for packages that are no longer installed.
     * Called by PackageChangeSyncWorker after an uninstall broadcast.
     */
    suspend fun syncWithPackageManager() = withContext(Dispatchers.IO) {
        val installed = queryLaunchableApps().keys + setOf(PACKAGE_TV_SETTINGS)
        val inDb = appOrderDao.getAllPackageNames()
        inDb.filter { it !in installed }.forEach { appOrderDao.delete(it) }
    }

    suspend fun incrementLaunchCount(packageName: String) = withContext(Dispatchers.IO) {
        val current = launchCountDao.getCount(packageName) ?: 0
        launchCountDao.upsert(AppLaunchCountEntity(packageName, current + 1))
        countRefresh.emit(Unit)
    }

    suspend fun cacheColor(packageName: String, color: Color) = withContext(Dispatchers.IO) {
        colorCacheDao.upsert(
            AppColorCacheEntity(packageName, color.toArgb(), System.currentTimeMillis())
        )
    }
}
```

⚠️ **Do not build yet** — `GetAppsUseCase` still calls `repo.getApps()` which no longer exists. Build verification happens in Task 5 Step 4 after the use case is updated.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/data/repository/AppRepository.kt
git commit -m "feat: rename getApps to getAllApps, add getVisibleApps/getHiddenApps/hideApp/unhideApp/refresh/syncWithPackageManager"
```

---

## Task 5: Update use cases and fix PackageChangeSyncWorker

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/domain/usecase/GetAppsUseCase.kt`
- Create: `app/src/main/java/com/yukuza/launcher/domain/usecase/GetVisibleAppsUseCase.kt`
- Modify: `app/src/main/java/com/yukuza/launcher/data/worker/PackageChangeSyncWorker.kt`

- [ ] **Step 1: Update `GetAppsUseCase` to call `getAllApps()`**

```kotlin
package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.AppRepository
import com.yukuza.launcher.domain.model.AppInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppsUseCase @Inject constructor(private val repo: AppRepository) {
    operator fun invoke(): Flow<ImmutableList<AppInfo>> = repo.getAllApps()
}
```

- [ ] **Step 2: Create `GetVisibleAppsUseCase`**

```kotlin
package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.AppRepository
import com.yukuza.launcher.domain.model.AppInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVisibleAppsUseCase @Inject constructor(private val repo: AppRepository) {
    operator fun invoke(): Flow<ImmutableList<AppInfo>> = repo.getVisibleApps()
}
```

- [ ] **Step 3: Update `PackageChangeSyncWorker` to actually sync**

```kotlin
package com.yukuza.launcher.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yukuza.launcher.data.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PackageChangeSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val appRepository: AppRepository,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        appRepository.syncWithPackageManager()
        return Result.success()
    }
}

class PackageChangeBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
            intent.action == Intent.ACTION_PACKAGE_REMOVED
        ) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "packageSync",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PackageChangeSyncWorker>().build()
            )
        }
    }
}
```

- [ ] **Step 4: Build to verify everything compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/domain/usecase/GetAppsUseCase.kt \
        app/src/main/java/com/yukuza/launcher/domain/usecase/GetVisibleAppsUseCase.kt \
        app/src/main/java/com/yukuza/launcher/data/worker/PackageChangeSyncWorker.kt
git commit -m "feat: add GetVisibleAppsUseCase, fix PackageChangeSyncWorker to sync on uninstall"
```

---

## Task 6: Rewrite AppListViewModel

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/screen/apps/AppListViewModel.kt`

- [ ] **Step 1: Replace `AppListViewModel` with new implementation**

```kotlin
package com.yukuza.launcher.ui.screen.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yukuza.launcher.data.repository.AppRepository
import com.yukuza.launcher.domain.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Visible (non-hidden) apps filtered by search query. */
    val visibleApps: StateFlow<ImmutableList<AppInfo>> =
        combine(repository.getVisibleApps(), query) { apps, q ->
            if (q.isBlank()) apps
            else apps.filter { it.label.contains(q, ignoreCase = true) }.toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    /** Hidden apps filtered by search query. */
    val hiddenApps: StateFlow<ImmutableList<AppInfo>> =
        combine(repository.getHiddenApps(), query) { apps, q ->
            if (q.isBlank()) apps
            else apps.filter { it.label.contains(q, ignoreCase = true) }.toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    fun onSearch(q: String) {
        query.value = q
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { repository.hideApp(packageName) }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { repository.unhideApp(packageName) }
    }

    /** Call on ON_RESUME to refresh the list after a disable action. */
    fun refresh() {
        repository.refresh()
    }
}
```

⚠️ **Do not build yet** — `AppListScreen` still references the old `filteredApps` StateFlow, causing a compile error. Build verification happens in Task 10 Step 3 after the screen is updated.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/screen/apps/AppListViewModel.kt
git commit -m "feat: rewrite AppListViewModel with visibleApps/hiddenApps/hideApp/unhideApp"
```

---

## Task 7: Update HomeViewModel

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeViewModel.kt`

- [ ] **Step 1: Inject `GetVisibleAppsUseCase` instead of `GetAppsUseCase`, add `refresh()` and `hideApp()`/`unhideApp()`**

In `HomeViewModel.kt`, make these changes:

**a) Replace the `GetAppsUseCase` import and injection with `GetVisibleAppsUseCase`:**

Change the import:
```kotlin
// Remove:
import com.yukuza.launcher.domain.usecase.GetAppsUseCase
// Add:
import com.yukuza.launcher.domain.usecase.GetVisibleAppsUseCase
```

Change the constructor parameter:
```kotlin
// Remove:
private val getApps: GetAppsUseCase,
// Add:
private val getApps: GetVisibleAppsUseCase,
```

**b) Inject `AppRepository` for hide/unhide and refresh:**

Add import:
```kotlin
import com.yukuza.launcher.data.repository.AppRepository
```

Add constructor parameter after the existing injected dependencies:
```kotlin
private val appRepository: AppRepository,
```

**c) Add these three methods anywhere in the ViewModel body:**

```kotlin
fun refresh() {
    appRepository.refresh()
}

fun hideApp(packageName: String) {
    viewModelScope.launch { appRepository.hideApp(packageName) }
}

fun unhideApp(packageName: String) {
    viewModelScope.launch { appRepository.unhideApp(packageName) }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL` (HomeScreen may have references to change — but HomeViewModel itself should compile. AppListScreen errors persist from Task 6 until Task 10.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeViewModel.kt
git commit -m "feat: use GetVisibleAppsUseCase in HomeViewModel, add hideApp/unhideApp/refresh"
```

---

## Task 8: Update AppIcon with `isHidden` visual state

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/components/AppIcon.kt`

- [ ] **Step 1: Add `isHidden` parameter and visual treatment**

In `AppIcon.kt`, make the following changes:

**a) Add the import for the icon:**
```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.draw.alpha
```

**b) Add `isHidden: Boolean = false` parameter to the composable:**

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,      // NEW
) {
```

**c) Wrap the root `Column` with an alpha modifier when hidden. Change:**
```kotlin
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .width(88.dp)
```
**to:**
```kotlin
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .width(88.dp)
        .then(if (isHidden) Modifier.alpha(0.5f) else Modifier)
```

**d) Add the eye-off badge inside the `Box` that wraps the icon image. After the `AsyncImage`, add:**

```kotlin
if (isHidden) {
    Icon(
        imageVector = Icons.Rounded.VisibilityOff,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .size(18.dp)
            .align(Alignment.TopEnd)
            .padding(2.dp),
    )
}
```

The `Box` with `contentAlignment = Alignment.Center` already supports absolute positioning via `align()` on children.

- [ ] **Step 2: Build to verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/components/AppIcon.kt
git commit -m "feat: add isHidden visual state to AppIcon (50% alpha + eye-off badge)"
```

---

## Task 9: Create AppContextMenuOverlay

**Files:**
- Create: `app/src/main/java/com/yukuza/launcher/ui/overlay/AppContextMenuOverlay.kt`

- [ ] **Step 1: Create the overlay composable**

```kotlin
package com.yukuza.launcher.ui.overlay

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yukuza.launcher.domain.model.AppInfo
import androidx.compose.ui.draw.alpha
import androidx.activity.compose.BackHandler

@Composable
fun AppContextMenuOverlay(
    app: AppInfo,
    onDismiss: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onEnterEditMode: (() -> Unit)? = null,   // non-null only on home row
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Determine if the app is a system app
    val isSystemApp = remember(app.packageName) {
        try {
            val flags = context.packageManager.getApplicationInfo(app.packageName, 0).flags
            (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        } catch (e: Exception) {
            false
        }
    }

    // Uninstall launcher (Compose-compatible)
    val uninstallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // RESULT_OK or RESULT_CANCELED — either way, dismiss overlay
        // List auto-updates via PackageChangeSyncWorker on uninstall
        onDismiss()
    }

    BackHandler { onDismiss() }

    val firstButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        firstButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Dim scrim — tap to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
        )

        // Content: icon + side panel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center),
        ) {
            // Highlighted app icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 3.dp,
                        color = app.dominantColor,
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(
                            try { context.packageManager.getApplicationIcon(app.packageName) }
                            catch (e: Exception) { null }
                        )
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }

            Spacer(Modifier.width(24.dp))

            // Side panel with action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E2E))
                    .padding(8.dp),
            ) {
                // App name header
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )

                // Hide / Unhide
                ContextMenuButton(
                    icon = if (app.isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    label = if (app.isHidden) "Unhide" else "Hide",
                    focusRequester = firstButtonFocusRequester,
                    onClick = {
                        if (app.isHidden) onUnhide() else onHide()
                        onDismiss()
                    },
                )

                // Uninstall or Disable
                ContextMenuButton(
                    icon = Icons.Rounded.Delete,
                    label = if (isSystemApp) "Disable" else "Uninstall",
                    tint = Color(0xFFFF6B6B),
                    onClick = {
                        if (isSystemApp) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${app.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            onDismiss()
                        } else {
                            uninstallLauncher.launch(
                                Intent(
                                    Intent.ACTION_DELETE,
                                    Uri.parse("package:${app.packageName}")
                                )
                            )
                        }
                    },
                )

                // Edit Order — only shown on home row
                if (onEnterEditMode != null) {
                    ContextMenuButton(
                        icon = Icons.Rounded.Edit,
                        label = "Edit Order",
                        onClick = {
                            onEnterEditMode()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    tint: Color = Color.White,
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.15f else 0.06f,
        animationSpec = tween(100),
        label = "bg",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .then(
                if (isFocused) Modifier.border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/overlay/AppContextMenuOverlay.kt
git commit -m "feat: add AppContextMenuOverlay (dim scrim + side panel with hide/uninstall/edit order)"
```

---

## Task 10: Update AppListScreen

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/screen/apps/AppListScreen.kt`

- [ ] **Step 1: Read the current AppListScreen.kt to understand its structure**

```bash
cat "/home/prasenjeet-urunkar/Documents/Yukuza Launcher/app/src/main/java/com/yukuza/launcher/ui/screen/apps/AppListScreen.kt"
```

- [ ] **Step 2: Update AppListScreen to use `visibleApps`/`hiddenApps`, add overlay trigger**

The key changes (adapt line numbers after reading the file):

**a) Update ViewModel state collection** — replace `filteredApps` with `visibleApps` and `hiddenApps`:

```kotlin
val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()
val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
```

**b) Add `selectedApp` local state and uninstall launcher at the top of the composable:**

```kotlin
var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

val uninstallLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { selectedApp = null }
```

**c) Add ON_RESUME lifecycle observer for disable refresh:**

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

**d) In the `TvLazyVerticalGrid`, use `visibleApps` for the top section. Add a second section for hidden apps after the visible items:**

```kotlin
// After the visible apps items block:
if (hiddenApps.isNotEmpty()) {
    item(span = { TvGridItemSpan(maxLineSpan) }) {
        Text(
            text = "Hidden Apps",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
    }
    items(hiddenApps, key = { it.packageName }) { app ->
        AppIcon(
            app = app,
            isFocused = false,
            onFocus = {},
            onLaunch = {
                // Hidden apps launch directly (no unhide prompt)
                context.packageManager
                    .getLaunchIntentForPackage(app.packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?.let { context.startActivity(it) }
            },
            onLongPress = { selectedApp = app },
            isHidden = true,
        )
    }
}
```

**e) Update the visible apps `items` block to set `onLongPress = { selectedApp = app }`.**

**f) Render the overlay when `selectedApp` is non-null. Place this as a sibling to the grid inside a `Box`:**

```kotlin
selectedApp?.let { app ->
    AppContextMenuOverlay(
        app = app,
        onDismiss = { selectedApp = null },
        onHide = { viewModel.hideApp(app.packageName) },
        onUnhide = { viewModel.unhideApp(app.packageName) },
        onEnterEditMode = null,  // app list — no edit order
    )
}
```

- [ ] **Step 3: Build to verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/screen/apps/AppListScreen.kt
git commit -m "feat: add Hidden Apps section and long-press context menu to AppListScreen"
```

---

## Task 11: Update AppRow and HomeScreen

**Files:**
- Modify: `app/src/main/java/com/yukuza/launcher/ui/components/AppRow.kt`
- Modify: `app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeScreen.kt`

- [ ] **Step 1: Read current HomeScreen.kt**

```bash
cat "/home/prasenjeet-urunkar/Documents/Yukuza Launcher/app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeScreen.kt"
```

- [ ] **Step 2: Verify `AppRow.kt` requires no changes**

`AppRow` already calls `onLongPress(app)` when not in edit mode. No changes are needed to `AppRow.kt` itself — the wiring change happens entirely in `HomeScreen` (Step 3 below). Confirm by reading the current `AppRow.kt` long-press handler and verifying it delegates to the `onLongPress` callback.

- [ ] **Step 3: Update `HomeScreen` to wire long-press to `AppContextMenuOverlay`**

In `HomeScreen.kt`, make these changes:

**a) Add `selectedApp` local state:**
```kotlin
var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
```

**b) Add uninstall launcher:**
```kotlin
val uninstallLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { selectedApp = null }
```

**c) Add ON_RESUME lifecycle observer:**
```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

**d) Find where `AppRow` is called. Change the `onLongPress` lambda to set `selectedApp`:**
```kotlin
onLongPress = { app -> selectedApp = app },
```

**e) Remove any `AppShortcutsOverlay` call wired to the home row long-press.**

**f) Add the overlay rendering (inside the root `Box` alongside the existing content):**
```kotlin
selectedApp?.let { app ->
    AppContextMenuOverlay(
        app = app,
        onDismiss = { selectedApp = null },
        onHide = { viewModel.hideApp(app.packageName) },
        onUnhide = { viewModel.unhideApp(app.packageName) },
        onEnterEditMode = { viewModel.enterEditMode() },
    )
}
```

- [ ] **Step 4: Build to verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Final end-to-end smoke test on device/emulator**

Manual test checklist:
1. Long-press any app in app list → context menu appears (dim + side panel)
2. Press "Hide" → app disappears from visible list, appears in "Hidden Apps" section (dimmed, eye-off badge)
3. Long-press a hidden app → "Unhide" appears instead of "Hide" → press it → app returns to visible list
4. Search for a hidden app → appears dimmed in search results → clicking launches it
5. Long-press any app in home row → context menu appears with "Edit Order" button
6. Press "Edit Order" → enters reorder mode (same as before)
7. Press "Uninstall" on a user app → Android uninstall dialog appears → confirm → app removed from list
8. Hidden apps do not appear in home row
9. After install of new app → it appears in the list (BroadcastReceiver → Worker works)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yukuza/launcher/ui/screen/home/HomeScreen.kt
git commit -m "feat: wire AppContextMenuOverlay to home row long-press, remove AppShortcutsOverlay from that path"
```
