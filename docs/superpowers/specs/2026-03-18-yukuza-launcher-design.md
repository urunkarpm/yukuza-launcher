# Yukuza Launcher — Design Spec
**Date:** 2026-03-18
**Status:** Under Review

---

## 1. Overview

A clean, ad-free Android TV launcher targeting all Android-based TV platforms (Android TV, Google TV, Fire TV, generic Android TV boxes). The guiding philosophy: focused, snappy, and premium — no bloat, no ads, no compromise on feel.

**Visual identity:** Aurora glassmorphism. Slowly drifting Northern Lights color blobs (purple, teal, pink) behind a deep black canvas. All UI elements rendered as frosted glass panels. App icons are greyscale at rest, bloom to full color with a brand-matched glow on D-pad focus. All launcher chrome assets are SVG — no raster resources in the launcher itself — to stay razor-sharp on 4K panels.

---

## 2. Target Platforms

| Platform | HOME intent support | Notes |
|---|---|---|
| Android TV (API 21+) | Full | Primary target |
| Google TV (API 29+) | Full | Primary target |
| Generic Android TV boxes | Full | Varies by OEM |
| Fire TV | Sideload only, no HOME replacement | Fire TV's launcher lock-down prevents `CATEGORY_HOME` replacement without ADB or device owner privileges on most generations. Yukuza will function as an app on Fire TV but cannot replace the system launcher in v1. |

**Minimum SDK:** 21. `RenderEffect.createBlurEffect` (Android 12+ / API 31) used where available; static semi-transparent overlay fallback on API < 31.

**GMS vs non-GMS:** Many generic TV boxes and Fire TV lack Google Play Services. All GMS-dependent APIs must have fallbacks (see §13 Permissions & Fallbacks).

---

## 3. Architecture

### Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose for TV (`androidx.tv:tv-foundation`, `androidx.tv:tv-material`)
- **DI:** Hilt
- **Database:** Room (app order, pinned state, Palette-extracted dominant colors)
- **Preferences:** DataStore (widget toggles, units, night mode schedule, user settings)
- **Async:** Kotlin Coroutines + Flow
- **Image loading:** Coil (memory + disk cache for app icons)
- **Background work:** WorkManager (weather/AQI polling, Palette pre-warming)
- **Location:** `FusedLocationProvider` on GMS devices; `LocationManager.getLastKnownLocation` on non-GMS; manual city entry as final fallback

### Structure
```
MainActivity  (single entry, declared CATEGORY_HOME launcher in AndroidManifest)
└── LauncherNavGraph (Compose Navigation)
    ├── HomeScreen          ← default destination
    └── AppListScreen       ← full app drawer (grid + search)
```

### Layers
```
UI Layer        HomeScreen, AppListScreen, overlay composables
                LauncherViewModel, SettingsViewModel
Domain Layer    GetAppsUseCase, ReorderAppsUseCase, GetWeatherUseCase,
                GetAqiUseCase, GetMediaSessionUseCase, GetNetworkSpeedUseCase
Data Layer      AppRepository      — PackageManager + Room (app list, order, Palette cache)
                WeatherRepository  — OpenMeteo HTTP + Room cache
                NetworkRepository  — TrafficStats / NetworkStatsManager (no permission)
                MediaRepository    — wraps MediaSessionManager for testability
```

**NetworkRepository** owns: live download speed polling via `TrafficStats.getTotalRxBytes()`, wifi connectivity state, and the `Settings.ACTION_WIFI_SETTINGS` intent trigger. Consumed by `GetNetworkSpeedUseCase` and the Network widget.

**MediaRepository** wraps `MediaSessionManager.getActiveSessions()` in a `Flow<MediaMetadata?>` — the ViewModel never calls `MediaSessionManager` directly, keeping it testable.

---

## 4. Home Screen Layout

```
┌─────────────────────────────────────────────────────────────┐
│  [Clock+AM/PM  Date]     [G]     [Wx][AQI][Timer][Net]      │  ← top bar
│                                                             │
│                   [Now Playing widget]                      │  ← center (conditional)
│                                                             │
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│  Apps  [App1][App2][App3]…[AppN]  ···                       │  ← frosted app strip
└─────────────────────────────────────────────────────────────┘
```

**Aurora background:** Five radial gradient blobs, `mix-blend-mode: Screen`, animated via `Animatable` on a dedicated `graphicsLayer`. Fully GPU-composited, never triggers Compose recomposition.

**Vignette:** Radial dark overlay at screen edges for text legibility across all aurora states.

**Widget layout:** All four widgets (Weather, AQI, Screen Timer, Network) are shown as a fixed horizontal row in the top-right, always visible. Each is a separate glass card. On panels narrower than 1280px, the Network widget collapses to icon-only. No user-configurable widget hiding in v1 — all four are always on.

**Google G button** (center top): frosted glass circle, triggers Google Assistant (see §7).

---

## 5. Widgets

### Top-bar widgets (always visible)

| Widget | Data Source | Refresh | Tap Action |
|---|---|---|---|
| Clock (12hr + AM/PM) | `System.currentTimeMillis()` | Every 10s | None |
| Weather (temp + condition icon) | OpenMeteo `/v1/forecast?hourly=temperature_2m,weathercode` | Every 30 min (WorkManager) | None |
| AQI (numeric + Good/Moderate/etc.) | OpenMeteo `/v1/air-quality?hourly=european_aqi` — European AQI standard, 0–500 scale, color-banded Good/Fair/Moderate/Poor/Very Poor | Every 30 min (same WorkManager job as weather) | None |
| Screen-on timer | Stopwatch from `SystemClock.elapsedRealtime()` since launcher process start. Resets on device sleep/wake (new session). Not persisted in Room — purely in-memory. | Every second | None |
| Network speed | `TrafficStats.getTotalRxBytes()` delta per 5s interval | Every 5s | Opens `Settings.ACTION_WIFI_SETTINGS` |

**Location strategy:**
1. GMS devices: `FusedLocationProvider.getLastLocation()` — one-time, no continuous updates
2. Non-GMS: `LocationManager.getLastKnownLocation(NETWORK_PROVIDER)`
3. Fallback: manual city entry in Settings screen, stored in DataStore

### Now Playing widget (center, conditional)

Appears **only** when a music/audio app is active **in the background**. Driven by `MediaRepository` which wraps `MediaSessionManager.getActiveSessions(notificationListenerComponentName)`. Requires `BIND_NOTIFICATION_LISTENER_SERVICE` permission (user-granted via system settings prompt on first use).

Shows:
- Spinning album art (loaded via Coil from `MediaMetadata.METADATA_KEY_ART_URI`)
- Track title, artist, album
- Progress bar with elapsed / total timestamps (updated every second via `MediaController.PlaybackState`)
- Prev / Pause / Next controls (sends `MediaController.transportControls` commands)
- Source app name + pulsing live dot
- Album dominant color (Palette API) bleeds subtly into glass background as a radial tint

**Palette fallback:** If `Palette.generate()` returns no dominant swatch, or album art is null, fall back to the launcher's default aurora teal (`#14B8A6` at 12% opacity).

**Visibility rules:** Widget is hidden when: no active `MediaSession`, the media app is in the foreground, or the user has dismissed it (dismiss persists until next session).

---

## 6. App Icons

Icons are loaded via `LauncherApps.getActivityList()` and `PackageManager.getApplicationIcon()` — real app icons, as-is from the APK. The SVG-only rule applies to launcher chrome assets only; third-party app icons are raster bitmaps loaded through Coil.

### Theming: Mono → Color on Focus

- **Unfocused:** `graphicsLayer { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }` + `alpha = 0.7f`
- **Focused:** `animateFloatAsState(targetValue = if (focused) 1f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))` drives saturation from 0 → 1 and alpha 0.7 → 1.0
- **Focus lift:** `graphicsLayer { translationY = animatedOffset; scaleX = animatedScale; scaleY = animatedScale }` — translateY −8dp, scale 1.1, spring with medium bounce
- **Glow ring:** `Modifier.shadow(elevation, shape, ambientColor = dominantColor, spotColor = dominantColor)` using the cached dominant color

### Dominant Color Cache

- Extracted via `Palette.from(iconBitmap).generate()` — dominant swatch preferred, vibrant swatch as fallback
- Cached in Room (`AppColorCache` table: `packageName`, `dominantColor: Int`, `extractedAt: Long`)
- **Population strategy:** Lazy — extracted on first focus, stored immediately. A background WorkManager `OneTimeWorkRequest` pre-warms all installed apps' colors after boot (constrained to `DEVICE_IDLE`). On first launch before pre-warm completes, unextracted icons use a neutral white glow.

### App Ordering & Edit Mode

- Order persisted in Room (`AppOrderEntity` table)
- Long-press any icon → **Edit Mode**: dimmed overlay, drag handles appear on all icons, "Remove from row" and "Uninstall" contextual options
- **D-pad drag-to-reorder:** Long-press (select held) enters edit mode → D-pad left/right moves the selected app one slot at a time → press Select/Center to confirm placement → press Back to exit edit mode without saving

---

## 7. Google Assistant

A frosted glass circle centered at the top of the screen. The 'G' logo rendered as a pure glass SVG (white sheen gradient, subtle edge strokes, no Google brand colors — avoids trademark complications).

**Intent chain (in order):**
```kotlin
listOf(
    Intent(Intent.ACTION_VOICE_COMMAND),
    Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE),
    Intent("com.google.android.googlequicksearchbox.VOICE_SEARCH_ACTIVITY")
).firstOrNull { it.resolveActivity(packageManager) != null }
    ?.let { startActivity(it) }
    ?: showToast("Voice assistant not available on this device")
```

On non-GMS devices where no voice activity resolves, the G button is hidden entirely (DataStore flag set on boot after resolution check).

---

## 8. App Shortcuts (Long Press)

Standard Android `LauncherApps.getShortcuts(ShortcutQuery().apply { setQueryFlags(FLAG_MATCH_DYNAMIC or FLAG_MATCH_MANIFEST) }, userHandle)`.

**Overlay component:** Compose `Popup` anchored to the focused icon, rendered as a glass card. D-pad navigation: Up/Down moves between shortcuts, Select launches, Back dismisses. Max 5 shortcuts shown; excess truncated.

---

## 9. Quick Settings Overlay

Glass panel slides up from the bottom on Menu button press (or dedicated remote button if available via `KeyEvent.KEYCODE_SETTINGS`).

**Contents:**
- Volume slider (AudioManager)
- Brightness slider (if `WRITE_SETTINGS` granted, else hidden)
- Wi-Fi status indicator
- Bluetooth toggle
- Night Mode toggle (with schedule info)
- Game Mode toggle (visible only if `HdmiControlManager` is available, see §10)
- Input Source Switcher button (see §10)

D-pad navigates between items. Back dismisses.

---

## 10. TV Hardware Features

### Input Source Switcher & Game Mode

`HdmiControlManager` requires `android.permission.HDMI_CEC` — a **privileged** system permission not grantable to a standard sideloaded or Play Store APK without OEM signing or device owner privileges.

**Strategy:**
- At boot, check `context.checkSelfPermission("android.permission.HDMI_CEC")`
- If granted (system-signed build or OEM-granted): full Input Source Switcher and Game Mode CEC commands available
- If denied (standard APK): Input Source Switcher and Game Mode buttons are **hidden** from Quick Settings with no degraded UI shown
- This is documented as a known limitation for non-system builds

**Game Mode behavior (when available):**
1. Send CEC `<Set Stream Path>` or `<Active Source>` for HDMI port of last-used game console (stored in DataStore)
2. Call `DisplayManager` to request low-latency display mode if available (API 31+)
3. Optionally launch last-played game app (user-configurable in Settings)

---

## 11. Night Mode

- **Schedule:** stored in DataStore as `nightModeStartHour: Int`, `nightModeEndHour: Int`
- **Auto-activation:** `LauncherViewModel` collects a `Flow<LocalTime>` from a ticker and triggers night mode when current time enters the window
- **Manual toggle:** Quick Settings overlay toggle
- **Visual effect:** Aurora blob colors shift toward warmer palette (amber/red/deep blue tones) via animated color interpolation in `DrawScope`
- **Brightness:** If `Settings.System.canWrite(context)` → `Settings.System.putInt(resolver, SCREEN_BRIGHTNESS, dimmedValue)`. If permission denied → UI-level dimming via `Box` with semi-transparent black overlay as fallback. Permission prompt shown on first Night Mode activation via `Settings.ACTION_MANAGE_WRITE_SETTINGS` intent.

---

## 12. App Drawer (AppListScreen)

Accessed via a "See All Apps" button at the end of the home screen app row (D-pad right past the last icon, or a dedicated button).

**Layout:** `TvLazyVerticalGrid` with 6 columns on 1080p, 8 columns on 4K. App icons same mono→color theming as home row.

**Search:** Search bar at top, focused by default on screen entry. D-pad Down moves into the grid. Search filters `AppRepository` list by app label.

**Sort order:** Alphabetical by default. User can toggle "Most Used" sort (usage data from `UsageStatsManager` if `PACKAGE_USAGE_STATS` permission granted; alphabetical fallback if denied).

**D-pad navigation:** Up from top row → focus returns to search bar. Back → returns to HomeScreen.

---

## 13. Permissions & Fallbacks

| Permission | Required For | Grant Type | Fallback if Denied |
|---|---|---|---|
| `RECEIVE_BOOT_COMPLETED` | Pre-warm icon/color cache at boot | Install-time | Cache pre-warms on first launch instead |
| `ACCESS_COARSE_LOCATION` | Weather/AQI location | Runtime | Manual city entry in Settings |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Now Playing widget MediaSession access | Special (user opens system settings) | Now Playing widget hidden |
| `WRITE_SETTINGS` | Screen brightness in Night Mode | Special (ACTION_MANAGE_WRITE_SETTINGS) | UI-level dim overlay fallback |
| `PACKAGE_USAGE_STATS` | "Most Used" app sort | Special (ACTION_USAGE_ACCESS_SETTINGS) | Alphabetical sort only |
| `android.permission.HDMI_CEC` | Input Source / Game Mode | Privileged system permission | Feature hidden entirely |
| No permission needed | Network speed (TrafficStats), app list, app icons, clock, screen timer | — | — |

---

## 14. Error & Empty States

| Scenario | UI Response |
|---|---|
| No internet / Weather API unavailable | Weather widget shows last cached value with a "stale" indicator (clock icon + last-update time). If no cache, shows `--°C` |
| No internet / AQI unavailable | Same as weather — cached value or `--` |
| Location permission denied | Weather widget prompts "Set location" → opens Settings screen for manual city entry |
| PackageManager returns 0 apps | App row shows a single "No apps found" glass card |
| No active MediaSession | Now Playing widget is invisible (no empty state shown) |
| Palette extraction fails / null album art | Default aurora teal glow (`#14B8A6`) |
| Voice assistant not available | G button hidden on non-GMS devices; toast on GMS if intent fails |
| Network speed unreachable | Network widget shows "—" |

---

## 15. Accessibility

- All interactive elements have `semantics { contentDescription = "..." }` annotations
- Focus order follows natural reading order (left→right, top→bottom)
- TalkBack: app icons announce name + focused/unfocused state; widget values announced on focus
- Minimum touch/focus target size: 48×48dp (TV remote D-pad selection areas)
- `isTraversalGroup = true` on widget area and app strip for logical TalkBack grouping
- Color contrast: all text meets WCAG AA (4.5:1) against glass panel backgrounds

---

## 16. Performance Architecture

**Reference hardware baseline:** Mid-range Android TV box, 2 GB RAM, Amlogic S905X3 (or equivalent Cortex-A55 quad-core), running Android 9–12.

**Target metrics:**

| Metric | Target |
|---|---|
| Cold launch to first frame | < 300ms on reference hardware |
| App row focus transition | 60fps / 16ms frame budget |
| Icon load (Coil memory cache hit) | < 8ms |
| Aurora animation GPU frame time | < 4ms |
| Weather/AQI refresh | Background only, zero UI impact |

**Key strategies:**
- **Baseline Profiles** — AOT-compile critical Compose paths at install time via `ProfileInstaller`
- App icon bitmaps pre-loaded into Coil memory cache in a background coroutine at boot (`RECEIVE_BOOT_COMPLETED`); UI never waits for icon decoding
- Dominant color extraction is **lazy** (on first focus) + **pre-warmed** via a `DEVICE_IDLE`-constrained WorkManager `OneTimeWorkRequest` after boot. Pre-warm does not block cold start.
- `LauncherViewModel` initialized in `Application.onCreate()` — data ready before first composition
- Aurora animation on dedicated `Canvas` `graphicsLayer` — GPU-composited, zero Compose recomposition
- All transitions via `Modifier.graphicsLayer { ... }` — GPU compositor layer, no per-frame pixel work
- `@Immutable` / `@Stable` on all ViewModel-exposed data classes
- `derivedStateOf` for all computed UI state (e.g. focused index)
- `LazyRow(key = { it.packageName })` — stable identity, position preserved across list mutations
- `StateFlow<ImmutableList<T>>` throughout — Compose skips recomposition when reference unchanged
- All `PackageManager`, Room, and file I/O on `Dispatchers.IO`
- StrictMode enabled in debug builds — accidental main-thread I/O crashes immediately

### WorkManager Jobs

| Job | Type | Constraints | Period | Retry |
|---|---|---|---|---|
| `WeatherSyncWorker` | Periodic | `NetworkType.CONNECTED` | 30 min | Exponential backoff, max 3 attempts |
| `PalettePreWarmWorker` | One-time (post-boot) | `DEVICE_IDLE`, battery not low | — | No retry (lazy fallback handles misses) |
| `PackageChangeSyncWorker` | One-time (triggered by `PackageManager` broadcast) | None | On demand | No retry |

---

## 17. Out of Scope (v1)

- Custom wallpapers
- Multi-user profiles
- App folders / grouping
- Notification badges on icons
- Kids mode / app lock
- Icon pack support
- Cloud sync of preferences
- Fire TV HOME replacement
- HDMI CEC features on non-system builds
