# Hide & Uninstall Apps via Long-Press — Design Spec

**Date:** 2026-03-23
**Status:** Approved

---

## Overview

Add a long-press context menu to app icons in both the full app list screen and the home screen app row. The menu allows users to hide apps from the launcher or uninstall/disable them. Hidden apps are shown in a dedicated "Hidden Apps" section at the bottom of the app list and can be unhidden from there.

---

## Requirements

- Long-pressing any app icon in the app list or home screen row opens a context menu overlay.
- The overlay shows:
  - **Hide** — hides the app from the visible app list and home row
  - **Unhide** — shown instead of Hide when the app is already hidden
  - **Uninstall** — for user-installed apps (uses Android's ACTION_DELETE intent)
  - **Disable** — for system/updated-system apps that cannot be uninstalled (opens ACTION_APPLICATION_DETAILS_SETTINGS)
- Hidden apps are removed from the home screen app row automatically.
- A "Hidden Apps" section appears at the bottom of the full app list **only when at least one app is hidden**. It shows hidden apps dimmed with an eye-off badge. Users can long-press them to unhide.
- Search in the app list filters both visible and hidden sections independently. The "Hidden Apps" header is hidden when no hidden apps match the search. Hidden apps in search results remain dimmed. Selecting a hidden app from search results launches it directly (normal click path, no unhide prompt).
- On the home screen app row, long-press now **always opens `AppContextMenuOverlay`**. The previous `AppShortcutsOverlay` is removed from the long-press path. Edit mode (reordering) entry is moved to a dedicated "Edit" button visible while the context menu is open, or a separate UI affordance — the existing long-press-to-reorder gesture is superseded.

---

## Data Layer

### Database Version

The current `LauncherDatabase` is **version 2**. This feature bumps it to **version 3**. The existing `MIGRATION_1_2` remains unchanged. A new `MIGRATION_2_3` is added. `fallbackToDestructiveMigration` is **not** used — migrations must be explicit for all version paths. If a device somehow skips versions, Room will throw a `MigrationException` rather than silently destroy data.

### `AppOrderEntity` (extended)

```kotlin
@ColumnInfo(name = "isHidden", defaultValue = "0")
val isHidden: Boolean = false
```

### Database Migration (v2 → v3)

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE app_order ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

Register in **both** `LauncherDatabase` (`.addMigrations(MIGRATION_1_2, MIGRATION_2_3)`) **and** `DatabaseModule.provideDatabase()` (add `MIGRATION_2_3` to the existing `.addMigrations(...)` call there).

### `AppOrderDao` (new methods)

The existing `AppOrderDao.upsertAll()` uses `@Upsert` (replace-on-conflict) which would overwrite the `order` column. Do **not** use it for hide/unhide. Instead add:

```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertIfAbsent(entity: AppOrderEntity)

@Query("UPDATE app_order SET isHidden = :isHidden WHERE packageName = :packageName")
suspend fun setHidden(packageName: String, isHidden: Boolean)
```

`hideApp()` and `unhideApp()` in the repository call `insertIfAbsent()` (with `isHidden = false, order = 0`) first, then `setHidden()`, preserving the existing `order` value for apps that already have a row.

### `AppRepository` (changes)

**`getApps()` is renamed to `getAllApps()`** and updated to map `isHidden` from `AppOrderEntity` into `AppInfo`. All callers are updated.

**`GetAppsUseCase.invoke()`** is updated internally to call `getAllApps()` instead of the old `getApps()` — the use case's public API is unchanged. A new **`GetVisibleAppsUseCase`** is added that wraps `getVisibleApps()`. `HomeViewModel` is updated to inject and use `GetVisibleAppsUseCase` so the home row never shows hidden apps. `AppListViewModel` injects `AppRepository` directly (which is `@Singleton` and `@Inject`-able) to call `getVisibleApps()` and `getHiddenApps()` separately.

**New `AppRepository` methods (explicit list):**

```kotlin
fun getAllApps(): Flow<ImmutableList<AppInfo>>          // renamed from getApps(); maps isHidden
fun getVisibleApps(): Flow<ImmutableList<AppInfo>>     // getAllApps() filtered to !isHidden
fun getHiddenApps(): Flow<ImmutableList<AppInfo>>      // getAllApps() filtered to isHidden
suspend fun hideApp(packageName: String)               // insertIfAbsent + setHidden(true)
suspend fun unhideApp(packageName: String)             // insertIfAbsent + setHidden(false)
fun refresh()                                          // manual one-shot re-query trigger
```

`getVisibleApps()` and `getHiddenApps()` are Flow filter transformations:

```kotlin
fun getVisibleApps(): Flow<ImmutableList<AppInfo>> =
    getAllApps().map { apps -> apps.filter { !it.isHidden }.toImmutableList() }

fun getHiddenApps(): Flow<ImmutableList<AppInfo>> =
    getAllApps().map { apps -> apps.filter { it.isHidden }.toImmutableList() }
```

**Missing rows:** Apps with no `app_order` entry are mapped with `isHidden = false` as the default in the repository mapping function.

**`hideApp(packageName: String)`** and **`unhideApp(packageName: String)`**: both call `insertIfAbsent()` then `setHidden()`. Exposed as `suspend` functions; called from ViewModels in `viewModelScope.launch`.

**`reorder()` fix — prevents clobbering `isHidden`:** The existing `reorder()` method calls `upsertAll()` with `@Upsert` (replace-on-conflict), which would reset `isHidden` to `false` on every reorder. Fix: replace the `upsertAll()` call in `reorder()` with a targeted `@Query`:

```kotlin
@Query("UPDATE app_order SET `order` = :order WHERE packageName = :packageName")
suspend fun updateOrder(packageName: String, order: Int)
```

`reorder()` in the repository calls `updateOrder()` for each app and `insertIfAbsent()` for apps with no existing row. The existing `upsertAll()` / `@Upsert` method is no longer used by `reorder()` and can be deprecated or removed.

**Package change refresh — `PackageChangeSyncWorker` fix:** The existing worker's `doWork()` returns `Result.success()` without doing anything, so uninstalled apps are never removed from the list. Fix: `PackageChangeSyncWorker` is updated to call a new `AppRepository.syncWithPackageManager()` method that (a) queries the current installed package list from `PackageManager`, (b) deletes rows in `app_order` for packages no longer installed. This causes `getAllApps()` (which joins Room + PackageManager) to re-emit without the removed app. **No new BroadcastReceiver is added** — the existing manifest-registered receiver triggers the worker, which now does the sync.

**Disable flow refresh:** Disabling an app does not fire `ACTION_PACKAGE_REMOVED` so the worker does not catch it. `AppListViewModel` and `HomeViewModel` expose a `refresh()` method that triggers a one-shot re-query of `PackageManager`. In the composable, attach a `DisposableEffect` to `LocalLifecycleOwner` that calls `viewModel.refresh()` on `Lifecycle.Event.ON_RESUME`:

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

This `DisposableEffect` is placed in both `AppListScreen` and `HomeScreen`.

---

## Domain Model

### `AppInfo` (extended)

```kotlin
data class AppInfo(
    val packageName: String,
    val label: String,
    val order: Int,
    val dominantColor: Int?,
    val isPinned: Boolean,
    val isHidden: Boolean = false   // new field
)
```

---

## UI Layer

### `AppContextMenuOverlay` (new composable)

Full-screen overlay triggered by long-press on any app icon.

**Trigger mechanism:**
- In `AppListScreen`: a `selectedApp: AppInfo?` local state variable (`remember { mutableStateOf<AppInfo?>(null) }`) in the screen composable. When non-null, `AppContextMenuOverlay` is rendered on top. Set to `null` on dismissal.
- In `HomeScreen`: same pattern using a `selectedApp: AppInfo?` local state in the home screen composable.
- Each screen owns its own `selectedApp` state independently — no ViewModel needed for this transient UI state.

**Focus management:**
- Before triggering the overlay, the triggering icon saves a `FocusRequester` reference in a `remember {}` tied to its `packageName`.
- The overlay's first button requests focus via `LaunchedEffect(Unit) { firstButtonFocusRequester.requestFocus() }`.
- On dismissal (any method), the saved icon `FocusRequester` is called: `LaunchedEffect(dismissed) { if (dismissed) iconFocusRequester.requestFocus() }`.

**Visual design (Dim + Side Panel):**
- Full-screen black scrim at `alpha = 0.7f`
- The long-pressed app icon is highlighted with a border ring in its `dominantColor` (fallback: `Color.White`)
- A side panel slides in to the right of the icon with action buttons
- Panel buttons use `Modifier.focusable()` with TV-appropriate focus indication

**Back-press handling:** `BackHandler(enabled = true) { selectedApp = null }`. Back dismisses the overlay and triggers focus return to the icon.

**Activity result for Uninstall (Compose-compatible):**
```kotlin
val uninstallLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        // list auto-updates via BroadcastReceiver; no manual refresh needed
    }
    // RESULT_CANCELED: no action
    selectedApp = null
}
```
The launcher is declared at the composable level (in `AppListScreen` or `HomeScreen`) and passed down to `AppContextMenuOverlay`.

**System app detection:**
```kotlin
val flags = packageManager.getApplicationInfo(packageName, 0).flags
val isSystemApp = (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
```

**Intents:**
- Uninstall: `Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))` — launched via `uninstallLauncher.launch(intent)`
- Disable: `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))` — launched via `context.startActivity(intent)`. List refreshes on `ON_RESUME`.

**Dismissal:** Back press, scrim tap, or any action selection sets `selectedApp = null`.

**Buttons:**

| Condition | Buttons shown (app list) | Buttons shown (home row) |
|-----------|--------------------------|--------------------------|
| User app, not hidden | Hide, Uninstall | Hide, Uninstall, Edit Order |
| User app, hidden | Unhide, Uninstall | Unhide, Uninstall, Edit Order |
| System app, not hidden | Hide, Disable | Hide, Disable, Edit Order |
| System app, hidden | Unhide, Disable | Unhide, Disable, Edit Order |

---

### `AppListScreen` (modified)

Two sections in the `TvLazyVerticalGrid`:

1. **Visible section** (top) — `visibleApps` StateFlow, filtered by search query
2. **Hidden Apps section** (bottom) — rendered only when `hiddenApps.value.isNotEmpty()` (or when filtered-by-search result is non-empty). Sticky header "Hidden Apps" + `hiddenApps` StateFlow filtered by search query.

Hidden app icons rendered via the same `AppIcon` composable with `isHidden = true` (50% alpha + eye-off badge).

**Search across sections:** Both flows are filtered by the same `searchQuery` StateFlow in `AppListViewModel`. When search is active and no hidden apps match, the "Hidden Apps" header is not shown.

### `AppListViewModel` (modified)

```kotlin
val visibleApps: StateFlow<ImmutableList<AppInfo>>  // getVisibleApps() filtered by searchQuery
val hiddenApps: StateFlow<ImmutableList<AppInfo>>   // getHiddenApps() filtered by searchQuery

fun hideApp(packageName: String) { viewModelScope.launch { repository.hideApp(packageName) } }
fun unhideApp(packageName: String) { viewModelScope.launch { repository.unhideApp(packageName) } }
```

### `AppIcon` (modified)

New parameter: `isHidden: Boolean = false`

When `isHidden = true`:
- `Modifier.alpha(0.5f)` on the root container
- Small `Icon(Icons.Rounded.VisibilityOff)` badge in the top-right corner via `Box` overlay

### `AppRow` (modified, home screen)

- Consumes `getVisibleApps()` instead of `getAllApps()`.
- Long-press now always opens `AppContextMenuOverlay` (the `AppShortcutsOverlay` call is removed from this path).
- Edit mode (reordering) entry is moved into `AppContextMenuOverlay` as an "Edit Order" third button, visible only when the overlay is triggered from the home row (controlled by an `showEditOrder: Boolean` parameter on the overlay).
- `AppContextMenuOverlay` receives an `onEnterEditMode: (() -> Unit)? = null` callback. `HomeScreen` passes `{ homeViewModel.enterEditMode() }`. Pressing "Edit Order" calls this callback and dismisses the overlay. `AppListScreen` passes `null` (button not shown).
- `AppRow` continues to read `isEditMode` from `HomeViewModel` as before; the only change is that entry into edit mode now comes via the overlay callback rather than direct long-press.

### `AppShortcutsOverlay` (caller update only)

The `AppShortcutsOverlay` composable itself is unchanged. Any call to the old `getApps()` inside it is updated to `getAllApps()`. No feature behavior changes.

---

## Error Handling

- **`NameNotFoundException`** from `PackageManager.getApplicationInfo()` in the overlay: catch and dismiss the overlay silently. The app will be removed from the list on the next `getAllApps()` emission.
- **Hide/unhide DB operations:** Fire-and-forget in `viewModelScope.launch`. Local Room writes that should not fail under normal conditions.
- **Uninstall `RESULT_CANCELED`:** No action taken; overlay dismissed.
- **Uninstall `RESULT_OK`:** App list auto-updates via `BroadcastReceiver`; no manual refresh needed.
- **Disable (no result):** App list refreshes on `ON_RESUME` via lifecycle observer.

---

## Out of Scope

- Hiding apps from the home row pinned strip
- Bulk hide/unhide
- Password-protecting hidden apps
- New feature additions to `AppShortcutsOverlay`
