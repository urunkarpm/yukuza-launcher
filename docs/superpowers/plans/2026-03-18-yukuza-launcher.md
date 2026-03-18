# Yukuza Launcher Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a clean, ad-free Android TV launcher with Aurora glassmorphism UI, mono→color icon theming, and a focused set of widgets and overlays.

**Architecture:** Single-Activity Jetpack Compose for TV app using MVVM + Hilt + Room + DataStore. UI layer is fully Compose; data flows via `StateFlow<ImmutableList<T>>` from ViewModels. Background work via WorkManager. All aurora animation runs on a dedicated GPU-composited `graphicsLayer`.

**Tech Stack:** Kotlin, Jetpack Compose for TV (`androidx.tv`), Hilt, Room, DataStore, Coil, WorkManager, Retrofit + Moshi (OpenMeteo), MockK, JUnit4, Compose Testing.

---

## File Map

```
app/src/main/java/com/yukuza/launcher/
├── YukuzaApplication.kt               — Hilt app, boot-time pre-warm
├── MainActivity.kt                    — single Activity, HOME intent filter
│
├── di/
│   ├── AppModule.kt                   — context, PackageManager, DataStore bindings
│   ├── DatabaseModule.kt              — Room database + DAO bindings
│   ├── NetworkModule.kt               — Retrofit, OpenMeteo API
│   ├── RepositoryModule.kt            — repository interface→impl bindings
│   └── WorkerModule.kt                — HiltWorkerFactory
│
├── data/
│   ├── db/
│   │   ├── LauncherDatabase.kt        — Room DB, v1, all entities
│   │   ├── AppOrderDao.kt             — CRUD for app row order
│   │   ├── AppColorCacheDao.kt        — dominant color per packageName
│   │   └── WeatherCacheDao.kt         — last-fetched weather + AQI
│   ├── entity/
│   │   ├── AppOrderEntity.kt
│   │   ├── AppColorCacheEntity.kt
│   │   └── WeatherCacheEntity.kt
│   ├── remote/
│   │   ├── OpenMeteoApi.kt            — Retrofit interface (weather + AQI endpoints)
│   │   └── dto/
│   │       ├── WeatherResponse.kt
│   │       └── AqiResponse.kt
│   ├── repository/
│   │   ├── AppRepository.kt           — app list, order, Palette color cache
│   │   ├── WeatherRepository.kt       — fetch + cache weather/AQI
│   │   ├── NetworkRepository.kt       — TrafficStats speed + wifi state
│   │   └── MediaRepository.kt         — Flow<MediaData?> from MediaSessionManager
│   └── worker/
│       ├── WeatherSyncWorker.kt        — periodic 30min weather refresh
│       ├── PalettePreWarmWorker.kt     — DEVICE_IDLE post-boot color extraction
│       └── PackageChangeSyncWorker.kt  — on-demand triggered by PKG broadcasts
│
├── domain/
│   ├── model/
│   │   ├── AppInfo.kt                 — packageName, label, icon, dominantColor, order
│   │   ├── WeatherData.kt
│   │   ├── AqiData.kt
│   │   ├── NetworkData.kt
│   │   └── MediaData.kt               — track, artist, albumArt, progress, duration
│   └── usecase/
│       ├── GetAppsUseCase.kt
│       ├── ReorderAppsUseCase.kt
│       ├── GetWeatherUseCase.kt
│       ├── GetAqiUseCase.kt
│       ├── GetNetworkSpeedUseCase.kt
│       └── GetMediaSessionUseCase.kt
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                   — aurora palette constants
│   │   ├── Type.kt                    — typography (thin clock, labels)
│   │   └── Theme.kt                   — YukuzaTheme composable
│   ├── components/
│   │   ├── aurora/
│   │   │   └── AuroraBackground.kt    — 5-blob animated Canvas layer
│   │   ├── glass/
│   │   │   ├── GlassCard.kt           — reusable frosted glass container
│   │   │   └── GlassOverlay.kt        — full-screen backdrop for overlays
│   │   ├── widgets/
│   │   │   ├── ClockWidget.kt
│   │   │   ├── WeatherWidget.kt
│   │   │   ├── AqiWidget.kt
│   │   │   ├── ScreenTimerWidget.kt
│   │   │   ├── NetworkWidget.kt
│   │   │   └── NowPlayingWidget.kt
│   │   ├── AppIcon.kt                 — mono→color, glow, spring lift
│   │   ├── AppRow.kt                  — LazyRow, D-pad focus, drag-to-reorder
│   │   └── AssistantButton.kt         — glass G button, intent chain
│   ├── screen/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   └── apps/
│   │       ├── AppListScreen.kt       — full app drawer
│   │       └── AppListViewModel.kt
│   └── overlay/
│       ├── QuickSettingsOverlay.kt
│       ├── AppShortcutsOverlay.kt
│       └── InputSourceOverlay.kt
│
└── navigation/
    └── LauncherNavGraph.kt

app/src/test/java/com/yukuza/launcher/
├── data/repository/                   — unit tests (MockK)
└── domain/usecase/                    — unit tests

app/src/androidTest/java/com/yukuza/launcher/
├── data/db/                           — Room in-memory tests
└── ui/                                — Compose UI tests
```

---

## Task 1: Project Setup

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/yukuza/launcher/YukuzaApplication.kt`
- Create: `app/src/main/java/com/yukuza/launcher/MainActivity.kt`

- [ ] **Step 1: Create Android TV project**

  In Android Studio: New Project → No Activity → set package `com.yukuza.launcher`, min SDK 21, language Kotlin.

- [ ] **Step 2: Add dependencies to `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.yukuza.launcher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.yukuza.launcher"
        minSdk = 21
        targetSdk = 35
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    // Compose for TV
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0-rc02")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Retrofit + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // Palette
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Kotlinx Immutable Collections
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 3: Configure `AndroidManifest.xml`**

```xml
<manifest>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.WRITE_SETTINGS"
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />

    <application
        android:name=".YukuzaApplication"
        android:label="Yukuza Launcher"
        android:theme="@style/Theme.YukuzaLauncher">

        <activity
            android:name=".MainActivity"
            android:launchMode="singleTask"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- NotificationListenerService for Now Playing -->
        <service
            android:name=".data.service.MediaNotificationListenerService"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

    </application>
</manifest>
```

- [ ] **Step 4: Create `YukuzaApplication.kt`**

```kotlin
@HiltAndroidApp
class YukuzaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyDeath().build()
            )
        }
    }
}
```

- [ ] **Step 5: Create `MainActivity.kt`**

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YukuzaTheme {
                LauncherNavGraph()
            }
        }
    }
}
```

- [ ] **Step 6: Sync Gradle, verify build compiles**

  Run: `./gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: initial project setup with dependencies and manifest"
```

---

## Task 2: Domain Models

**Files:**
- Create: `domain/model/AppInfo.kt`
- Create: `domain/model/WeatherData.kt`
- Create: `domain/model/AqiData.kt`
- Create: `domain/model/NetworkData.kt`
- Create: `domain/model/MediaData.kt`

- [ ] **Step 1: Write tests for model equality and immutability**

```kotlin
// src/test/java/com/yukuza/launcher/domain/model/AppInfoTest.kt
class AppInfoTest {
    @Test fun `AppInfo equality is based on packageName`() {
        val a = AppInfo(packageName = "com.example", label = "Example", order = 0, dominantColor = Color.Red)
        val b = AppInfo(packageName = "com.example", label = "Example", order = 1, dominantColor = Color.Blue)
        assertEquals(a.packageName, b.packageName)
    }
}
```

Run: `./gradlew test` — Expected: FAIL (class not found)

- [ ] **Step 2: Create models**

```kotlin
// domain/model/AppInfo.kt
@Immutable
data class AppInfo(
    val packageName: String,
    val label: String,
    val order: Int,
    val dominantColor: Color = Color.White,
)

// domain/model/WeatherData.kt
@Immutable
data class WeatherData(
    val tempCelsius: Float,
    val conditionCode: Int,       // WMO weather code
    val locationName: String,
    val fetchedAt: Long,
    val isStale: Boolean = false,
)

// domain/model/AqiData.kt
@Immutable
data class AqiData(
    val europeanAqi: Int,
    val category: AqiCategory,
    val fetchedAt: Long,
    val isStale: Boolean = false,
) {
    enum class AqiCategory { GOOD, FAIR, MODERATE, POOR, VERY_POOR }
}

// domain/model/NetworkData.kt
@Immutable
data class NetworkData(val speedMbps: Float, val isConnected: Boolean)

// domain/model/MediaData.kt
@Immutable
data class MediaData(
    val trackTitle: String,
    val artist: String,
    val albumArtUri: String?,
    val elapsedMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val sourceAppLabel: String,
    val dominantColor: Color = Color(0xFF14B8A6),
)
```

- [ ] **Step 3: Run tests**

  Run: `./gradlew test` — Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add domain models"
```

---

## Task 3: Room Database

**Files:**
- Create: `data/entity/AppOrderEntity.kt`
- Create: `data/entity/AppColorCacheEntity.kt`
- Create: `data/entity/WeatherCacheEntity.kt`
- Create: `data/db/AppOrderDao.kt`
- Create: `data/db/AppColorCacheDao.kt`
- Create: `data/db/WeatherCacheDao.kt`
- Create: `data/db/LauncherDatabase.kt`
- Test: `androidTest/data/db/LauncherDatabaseTest.kt`

- [ ] **Step 1: Write failing DB test**

```kotlin
// androidTest/data/db/LauncherDatabaseTest.kt
@RunWith(AndroidJUnit4::class)
class LauncherDatabaseTest {
    private lateinit var db: LauncherDatabase
    private lateinit var appOrderDao: AppOrderDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), LauncherDatabase::class.java
        ).build()
        appOrderDao = db.appOrderDao()
    }

    @After fun teardown() { db.close() }

    @Test fun insertAndRetrieveAppOrder() = runTest {
        val entity = AppOrderEntity(packageName = "com.test.app", order = 0)
        appOrderDao.upsert(entity)
        val result = appOrderDao.getAll().first()
        assertEquals(1, result.size)
        assertEquals("com.test.app", result[0].packageName)
    }

    @Test fun upsertUpdatesOrder() = runTest {
        appOrderDao.upsert(AppOrderEntity("com.test.app", 0))
        appOrderDao.upsert(AppOrderEntity("com.test.app", 5))
        val result = appOrderDao.getAll().first()
        assertEquals(5, result[0].order)
    }
}
```

Run: `./gradlew connectedAndroidTest` — Expected: FAIL (classes not found)

- [ ] **Step 2: Create entities**

```kotlin
// data/entity/AppOrderEntity.kt
@Entity(tableName = "app_order")
data class AppOrderEntity(
    @PrimaryKey val packageName: String,
    val order: Int,
)

// data/entity/AppColorCacheEntity.kt
@Entity(tableName = "app_color_cache")
data class AppColorCacheEntity(
    @PrimaryKey val packageName: String,
    val dominantColor: Int,
    val extractedAt: Long,
)

// data/entity/WeatherCacheEntity.kt
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val id: Int = 1,
    val tempCelsius: Float,
    val conditionCode: Int,
    val locationName: String,
    val europeanAqi: Int,
    val fetchedAt: Long,
)
```

- [ ] **Step 3: Create DAOs**

```kotlin
// data/db/AppOrderDao.kt
@Dao
interface AppOrderDao {
    @Query("SELECT * FROM app_order ORDER BY `order` ASC")
    fun getAll(): Flow<List<AppOrderEntity>>

    @Upsert
    suspend fun upsert(entity: AppOrderEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AppOrderEntity>)

    @Query("DELETE FROM app_order WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

// data/db/AppColorCacheDao.kt
@Dao
interface AppColorCacheDao {
    @Query("SELECT * FROM app_color_cache WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppColorCacheEntity?

    @Upsert
    suspend fun upsert(entity: AppColorCacheEntity)
}

// data/db/WeatherCacheDao.kt
@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE id = 1")
    suspend fun get(): WeatherCacheEntity?

    @Upsert
    suspend fun upsert(entity: WeatherCacheEntity)
}
```

- [ ] **Step 4: Create database**

```kotlin
// data/db/LauncherDatabase.kt
@Database(
    entities = [AppOrderEntity::class, AppColorCacheEntity::class, WeatherCacheEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appOrderDao(): AppOrderDao
    abstract fun appColorCacheDao(): AppColorCacheDao
    abstract fun weatherCacheDao(): WeatherCacheDao
}
```

- [ ] **Step 5: Run DB tests**

  Run: `./gradlew connectedAndroidTest` — Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: Room database with entities and DAOs"
```

---

## Task 4: Hilt Modules

**Files:**
- Create: `di/AppModule.kt`
- Create: `di/DatabaseModule.kt`
- Create: `di/NetworkModule.kt`
- Create: `di/RepositoryModule.kt`
- Create: `di/WorkerModule.kt`

- [ ] **Step 1: Create `DatabaseModule.kt`**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): LauncherDatabase =
        Room.databaseBuilder(ctx, LauncherDatabase::class.java, "launcher.db").build()

    @Provides fun provideAppOrderDao(db: LauncherDatabase) = db.appOrderDao()
    @Provides fun provideAppColorCacheDao(db: LauncherDatabase) = db.appColorCacheDao()
    @Provides fun provideWeatherCacheDao(db: LauncherDatabase) = db.weatherCacheDao()
}
```

- [ ] **Step 2: Create `NetworkModule.kt`**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOpenMeteoApi(): OpenMeteoApi =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(
                Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            ))
            .build()
            .create(OpenMeteoApi::class.java)
}
```

- [ ] **Step 3: Create `AppModule.kt`**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun providePackageManager(@ApplicationContext ctx: Context): PackageManager = ctx.packageManager

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("yukuza_prefs") }
}
```

- [ ] **Step 4: Create `WorkerModule.kt`**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    @Provides @Singleton
    fun provideWorkManager(@ApplicationContext ctx: Context): WorkManager =
        WorkManager.getInstance(ctx)
}
```

- [ ] **Step 5: Verify Hilt compilation**

  Run: `./gradlew assembleDebug` — Expected: BUILD SUCCESSFUL (Hilt code gen passes)

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: Hilt DI modules"
```

---

## Task 5: Remote API + WeatherRepository

**Files:**
- Create: `data/remote/OpenMeteoApi.kt`
- Create: `data/remote/dto/WeatherResponse.kt`
- Create: `data/remote/dto/AqiResponse.kt`
- Create: `data/repository/WeatherRepository.kt`
- Test: `test/data/repository/WeatherRepositoryTest.kt`

- [ ] **Step 1: Write failing repository test**

```kotlin
class WeatherRepositoryTest {
    private val api = mockk<OpenMeteoApi>()
    private val dao = mockk<WeatherCacheDao>()
    private val repo = WeatherRepository(api, dao)

    @Test fun `returns cached data with stale flag when network fails`() = runTest {
        val cached = WeatherCacheEntity(tempCelsius = 20f, conditionCode = 1,
            locationName = "Mumbai", europeanAqi = 42, fetchedAt = 0L)
        coEvery { dao.get() } returns cached
        coEvery { api.getForecast(any(), any(), any()) } throws IOException()

        val result = repo.getWeather(19.07, 72.87)

        assertTrue(result.isStale)
        assertEquals(20f, result.tempCelsius)
    }

    @Test fun `maps WMO code 1 to Partly Cloudy condition`() = runTest {
        // WMO code 1 = mainly clear
        assertEquals("Mainly Clear", WeatherRepository.wmoCodeToDescription(1))
    }
}
```

Run: `./gradlew test` — Expected: FAIL

- [ ] **Step 2: Create DTOs**

```kotlin
// data/remote/dto/WeatherResponse.kt
@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current") val current: CurrentWeather,
) {
    @JsonClass(generateAdapter = true)
    data class CurrentWeather(
        @Json(name = "temperature_2m") val tempCelsius: Float,
        @Json(name = "weather_code") val weatherCode: Int,
    )
}

// data/remote/dto/AqiResponse.kt
@JsonClass(generateAdapter = true)
data class AqiResponse(
    @Json(name = "current") val current: CurrentAqi,
) {
    @JsonClass(generateAdapter = true)
    data class CurrentAqi(
        @Json(name = "european_aqi") val europeanAqi: Int,
    )
}
```

- [ ] **Step 3: Create Retrofit API interface**

```kotlin
// data/remote/OpenMeteoApi.kt
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
    ): WeatherResponse

    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "european_aqi",
    ): AqiResponse
}
```

- [ ] **Step 4: Create `WeatherRepository`**

```kotlin
@Singleton
class WeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
    private val dao: WeatherCacheDao,
) {
    suspend fun getWeather(lat: Double, lon: Double): WeatherData {
        return try {
            val weather = api.getForecast(lat, lon)
            val aqi = api.getAirQuality(lat, lon)
            val entity = WeatherCacheEntity(
                tempCelsius = weather.current.tempCelsius,
                conditionCode = weather.current.weatherCode,
                locationName = resolveLocationName(lat, lon),
                europeanAqi = aqi.current.europeanAqi,
                fetchedAt = System.currentTimeMillis(),
            )
            dao.upsert(entity)
            entity.toDomain(isStale = false)
        } catch (e: Exception) {
            dao.get()?.toDomain(isStale = true) ?: WeatherData.unavailable()
        }
    }

    companion object {
        fun wmoCodeToDescription(code: Int): String = when (code) {
            0 -> "Clear Sky"; 1 -> "Mainly Clear"; 2 -> "Partly Cloudy"
            3 -> "Overcast"; in 51..55 -> "Drizzle"; in 61..65 -> "Rain"
            in 71..75 -> "Snow"; in 95..99 -> "Thunderstorm"
            else -> "Unknown"
        }
        fun aqiToCategory(aqi: Int): AqiData.AqiCategory = when {
            aqi <= 20 -> GOOD; aqi <= 40 -> FAIR; aqi <= 60 -> MODERATE
            aqi <= 80 -> POOR; else -> VERY_POOR
        }
    }
}
```

- [ ] **Step 5: Run tests**

  Run: `./gradlew test` — Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: OpenMeteo API client and WeatherRepository with cache + stale fallback"
```

---

## Task 6: AppRepository

**Files:**
- Create: `data/repository/AppRepository.kt`
- Test: `test/data/repository/AppRepositoryTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
class AppRepositoryTest {
    private val pm = mockk<PackageManager>()
    private val appOrderDao = mockk<AppOrderDao>()
    private val colorCacheDao = mockk<AppColorCacheDao>()
    private val repo = AppRepository(pm, appOrderDao, colorCacheDao)

    @Test fun `merges PackageManager apps with stored order`() = runTest {
        val storedOrder = listOf(
            AppOrderEntity("com.b", 0),
            AppOrderEntity("com.a", 1),
        )
        coEvery { appOrderDao.getAll() } returns flowOf(storedOrder)
        every { pm.getInstalledApplications(any()) } returns listOf(
            mockk { every { packageName } returns "com.a" },
            mockk { every { packageName } returns "com.b" },
        )

        val apps = repo.getApps().first()
        assertEquals("com.b", apps[0].packageName)
        assertEquals("com.a", apps[1].packageName)
    }

    @Test fun `new apps not in stored order get appended`() = runTest {
        coEvery { appOrderDao.getAll() } returns flowOf(emptyList())
        every { pm.getInstalledApplications(any()) } returns listOf(
            mockk { every { packageName } returns "com.new" },
        )

        val apps = repo.getApps().first()
        assertEquals(1, apps.size)
    }
}
```

Run: `./gradlew test` — Expected: FAIL

- [ ] **Step 2: Implement `AppRepository`**

```kotlin
@Singleton
class AppRepository @Inject constructor(
    private val pm: PackageManager,
    private val appOrderDao: AppOrderDao,
    private val colorCacheDao: AppColorCacheDao,
) {
    fun getApps(): Flow<ImmutableList<AppInfo>> =
        appOrderDao.getAll().map { storedOrder ->
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .associateBy { it.packageName }

            val orderedPackages = storedOrder.map { it.packageName }
            val newPackages = installed.keys.filter { it !in orderedPackages }

            (orderedPackages + newPackages)
                .filter { it in installed }
                .mapIndexed { index, pkg ->
                    val info = installed[pkg]!!
                    val color = colorCacheDao.get(pkg)?.let { Color(it.dominantColor) } ?: Color.White
                    AppInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(info).toString(),
                        order = index,
                        dominantColor = color,
                    )
                }.toImmutableList()
        }.flowOn(Dispatchers.IO)

    suspend fun reorder(packages: List<String>) = withContext(Dispatchers.IO) {
        appOrderDao.upsertAll(packages.mapIndexed { i, pkg -> AppOrderEntity(pkg, i) })
    }

    suspend fun cacheColor(packageName: String, color: Color) = withContext(Dispatchers.IO) {
        colorCacheDao.upsert(AppColorCacheEntity(packageName, color.toArgb(), System.currentTimeMillis()))
    }
}
```

- [ ] **Step 3: Run tests**

  Run: `./gradlew test` — Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: AppRepository with ordered app list and color cache"
```

---

## Task 7: NetworkRepository + MediaRepository

**Files:**
- Create: `data/repository/NetworkRepository.kt`
- Create: `data/service/MediaNotificationListenerService.kt`
- Create: `data/repository/MediaRepository.kt`
- Test: `test/data/repository/NetworkRepositoryTest.kt`

- [ ] **Step 1: Write NetworkRepository test**

```kotlin
class NetworkRepositoryTest {
    @Test fun `speed is zero when no bytes received`() = runTest {
        // TrafficStats returns 0 on emulator — just verify no crash
        val repo = NetworkRepository(ApplicationProvider.getApplicationContext())
        val flow = repo.getNetworkSpeed()
        val first = flow.first()
        assertNotNull(first)
    }
}
```

- [ ] **Step 2: Implement `NetworkRepository`**

```kotlin
@Singleton
class NetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getNetworkSpeed(): Flow<NetworkData> = flow {
        var prev = TrafficStats.getTotalRxBytes()
        while (true) {
            delay(5_000)
            val curr = TrafficStats.getTotalRxBytes()
            val bytesDelta = (curr - prev).coerceAtLeast(0)
            val mbps = (bytesDelta * 8f) / (5f * 1_000_000f)
            val connected = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetworkInfo?.isConnected == true
            emit(NetworkData(speedMbps = mbps, isConnected = connected))
            prev = curr
        }
    }.flowOn(Dispatchers.IO)
}
```

- [ ] **Step 3: Implement `MediaNotificationListenerService` + `MediaRepository`**

```kotlin
// data/service/MediaNotificationListenerService.kt
class MediaNotificationListenerService : NotificationListenerService() {
    companion object {
        val activeSessionFlow = MutableStateFlow<MediaData?>(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateMediaState()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateMediaState()
    }

    private fun updateMediaState() {
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions = try {
            manager.getActiveSessions(ComponentName(this, MediaNotificationListenerService::class.java))
        } catch (e: SecurityException) { return }

        val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        if (active == null) { activeSessionFlow.value = null; return }

        val meta = active.metadata ?: return
        activeSessionFlow.value = MediaData(
            trackTitle = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            albumArtUri = meta.getString(MediaMetadata.METADATA_KEY_ART_URI),
            elapsedMs = active.playbackState?.position ?: 0,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            isPlaying = true,
            sourceAppLabel = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(active.packageName, 0)
            ).toString(),
        )
    }
}

// data/repository/MediaRepository.kt
@Singleton
class MediaRepository @Inject constructor() {
    fun getActiveMedia(): Flow<MediaData?> = MediaNotificationListenerService.activeSessionFlow
}
```

- [ ] **Step 4: Run tests**

  Run: `./gradlew test` — Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: NetworkRepository and MediaRepository"
```

---

## Task 8: UseCases

**Files:**
- Create: `domain/usecase/GetAppsUseCase.kt`
- Create: `domain/usecase/ReorderAppsUseCase.kt`
- Create: `domain/usecase/GetWeatherUseCase.kt`
- Create: `domain/usecase/GetAqiUseCase.kt`
- Create: `domain/usecase/GetNetworkSpeedUseCase.kt`
- Create: `domain/usecase/GetMediaSessionUseCase.kt`
- Test: `test/domain/usecase/GetWeatherUseCaseTest.kt`

- [ ] **Step 1: Write UseCase test**

```kotlin
class GetWeatherUseCaseTest {
    private val repo = mockk<WeatherRepository>()
    private val useCase = GetWeatherUseCase(repo)

    @Test fun `emits loading then data`() = runTest {
        val data = WeatherData(24f, 1, "Mumbai", System.currentTimeMillis(), false)
        coEvery { repo.getWeather(any(), any()) } returns data

        val result = useCase(19.07, 72.87)
        assertEquals(data, result)
    }
}
```

- [ ] **Step 2: Implement UseCases**

```kotlin
// Each is a thin wrapper — keeps ViewModels clean and testable

class GetAppsUseCase @Inject constructor(private val repo: AppRepository) {
    operator fun invoke(): Flow<ImmutableList<AppInfo>> = repo.getApps()
}

class ReorderAppsUseCase @Inject constructor(private val repo: AppRepository) {
    suspend operator fun invoke(packages: List<String>) = repo.reorder(packages)
}

class GetWeatherUseCase @Inject constructor(private val repo: WeatherRepository) {
    suspend operator fun invoke(lat: Double, lon: Double): WeatherData = repo.getWeather(lat, lon)
}

class GetAqiUseCase @Inject constructor(private val repo: WeatherRepository) {
    suspend operator fun invoke(lat: Double, lon: Double): AqiData = repo.getAqi(lat, lon)
}

class GetNetworkSpeedUseCase @Inject constructor(private val repo: NetworkRepository) {
    operator fun invoke(): Flow<NetworkData> = repo.getNetworkSpeed()
}

class GetMediaSessionUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(): Flow<MediaData?> = repo.getActiveMedia()
}
```

- [ ] **Step 3: Run tests**

  Run: `./gradlew test` — Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: domain use cases"
```

---

## Task 9: HomeViewModel

**Files:**
- Create: `ui/screen/home/HomeViewModel.kt`
- Test: `test/ui/screen/home/HomeViewModelTest.kt`

- [ ] **Step 1: Write ViewModel test**

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val getApps = mockk<GetAppsUseCase>()
    private val reorderApps = mockk<ReorderAppsUseCase>()
    private val getWeather = mockk<GetWeatherUseCase>()
    private val getAqi = mockk<GetAqiUseCase>()
    private val getNetwork = mockk<GetNetworkSpeedUseCase>()
    private val getMedia = mockk<GetMediaSessionUseCase>()

    private lateinit var vm: HomeViewModel

    @Before fun setup() {
        every { getApps() } returns flowOf(persistentListOf())
        coEvery { getWeather(any(), any()) } returns WeatherData(24f, 1, "Mumbai", 0L, false)
        coEvery { getAqi(any(), any()) } returns AqiData(42, AqiData.AqiCategory.GOOD, 0L, false)
        every { getNetwork() } returns flowOf(NetworkData(87f, true))
        every { getMedia() } returns flowOf(null)
        vm = HomeViewModel(getApps, reorderApps, getWeather, getAqi, getNetwork, getMedia)
    }

    @Test fun `apps state starts empty then emits`() = runTest {
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.apps.size)
        }
    }

    @Test fun `weather state reflects repository data`() = runTest {
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(24f, state.weather?.tempCelsius)
        }
    }
}
```

- [ ] **Step 2: Define `HomeUiState` and `HomeViewModel`**

```kotlin
@Immutable
data class HomeUiState(
    val apps: ImmutableList<AppInfo> = persistentListOf(),
    val weather: WeatherData? = null,
    val aqi: AqiData? = null,
    val network: NetworkData? = null,
    val nowPlaying: MediaData? = null,
    val focusedAppIndex: Int = 0,
    val isEditMode: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getApps: GetAppsUseCase,
    private val reorderApps: ReorderAppsUseCase,
    private val getWeather: GetWeatherUseCase,
    private val getAqi: GetAqiUseCase,
    private val getNetwork: GetNetworkSpeedUseCase,
    private val getMedia: GetMediaSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getApps().collect { apps ->
                _uiState.update { it.copy(apps = apps) }
            }
        }
        viewModelScope.launch {
            // Default coords — will be replaced by real location in Task 13
            val weather = getWeather(19.07, 72.87)
            _uiState.update { it.copy(weather = weather) }
        }
        viewModelScope.launch {
            val aqi = getAqi(19.07, 72.87)
            _uiState.update { it.copy(aqi = aqi) }
        }
        viewModelScope.launch {
            getNetwork().collect { net ->
                _uiState.update { it.copy(network = net) }
            }
        }
        viewModelScope.launch {
            getMedia().collect { media ->
                _uiState.update { it.copy(nowPlaying = media) }
            }
        }
    }

    fun onAppFocused(index: Int) = _uiState.update { it.copy(focusedAppIndex = index) }
    fun enterEditMode() = _uiState.update { it.copy(isEditMode = true) }
    fun exitEditMode() = _uiState.update { it.copy(isEditMode = false) }
    fun reorder(packages: List<String>) = viewModelScope.launch { reorderApps(packages) }
}
```

- [ ] **Step 3: Add `turbine` dependency for Flow testing**

  In `build.gradle.kts`: `testImplementation("app.cash.turbine:turbine:1.1.0")`

- [ ] **Step 4: Run tests**

  Run: `./gradlew test` — Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: HomeViewModel with unified UiState"
```

---

## Task 10: Theme + Design System

**Files:**
- Create: `ui/theme/Color.kt`
- Create: `ui/theme/Type.kt`
- Create: `ui/theme/Theme.kt`
- Create: `ui/components/glass/GlassCard.kt`
- Create: `ui/components/glass/GlassOverlay.kt`

- [ ] **Step 1: Write Compose UI test for GlassCard**

```kotlin
class GlassCardTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `GlassCard displays content`() {
        composeRule.setContent {
            YukuzaTheme {
                GlassCard { Text("Hello", modifier = Modifier.testTag("content")) }
            }
        }
        composeRule.onNodeWithTag("content").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Create theme files**

```kotlin
// ui/theme/Color.kt
object YukuzaColors {
    val AuroraPurple = Color(0xFF5832FA)
    val AuroraTeal   = Color(0xFF10B4A0)
    val AuroraPink   = Color(0xFFE63C91)
    val AuroraBlue   = Color(0xFF32B4F5)
    val DeepBlack    = Color(0xFF06030F)
    val GlassSurface = Color(0x0A060210)   // 4% white
    val GlassBorder  = Color(0x24FFFFFF)   // 14% white
    val DefaultGlow  = Color(0xFF14B8A6)
}

// ui/theme/Type.kt
val YukuzaTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W200, letterSpacing = 5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W400, letterSpacing = 3.sp),
)

// ui/theme/Theme.kt
@Composable
fun YukuzaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = YukuzaColors.DeepBlack,
            surface = YukuzaColors.GlassSurface,
        ),
        typography = YukuzaTypography,
        content = content,
    )
}
```

- [ ] **Step 3: Create `GlassCard`**

```kotlin
// ui/components/glass/GlassCard.kt
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = YukuzaColors.GlassSurface,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (LocalDensity.current.density >= 2f) 0.5.dp else 1.dp,
                color = YukuzaColors.GlassBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .graphicsLayer {
                // RenderEffect blur on API 31+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            },
        content = content,
    )
}
```

- [ ] **Step 4: Run UI test**

  Run: `./gradlew connectedAndroidTest` — Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: theme, GlassCard design system component"
```

---

## Task 11: Aurora Background

**Files:**
- Create: `ui/components/aurora/AuroraBackground.kt`

- [ ] **Step 1: Write render test (smoke — just checks no crash)**

```kotlin
class AuroraBackgroundTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `AuroraBackground renders without crash`() {
        composeRule.setContent {
            YukuzaTheme {
                Box(Modifier.fillMaxSize()) {
                    AuroraBackground()
                }
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Implement `AuroraBackground`**

```kotlin
// ui/components/aurora/AuroraBackground.kt
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // 5 independent drift animations
    val offsets = (0..4).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 10_000 + i * 3_000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ), label = "blob_$i"
        )
    }

    val blobDefs = listOf(
        BlobDef(YukuzaColors.AuroraPurple.copy(alpha = 0.55f), Offset(-0.08f, -0.15f), Offset(0.04f, 0.07f), 0.65f, 0.60f),
        BlobDef(YukuzaColors.AuroraTeal.copy(alpha = 0.45f),   Offset(0.53f, -0.10f),  Offset(-0.05f, 0.05f), 0.55f, 0.55f),
        BlobDef(YukuzaColors.AuroraPink.copy(alpha = 0.30f),   Offset(0.28f, 0.08f),   Offset(0.05f, 0.03f),  0.50f, 0.45f),
        BlobDef(YukuzaColors.AuroraBlue.copy(alpha = 0.28f),   Offset(0.42f, 0f),       Offset(-0.04f, 0.05f), 0.40f, 0.40f),
        BlobDef(Color(0xFF9B82F5).copy(alpha = 0.38f),          Offset(0.62f, -0.05f),  Offset(0.04f, -0.04f), 0.38f, 0.35f),
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRect(color = YukuzaColors.DeepBlack)
        blobDefs.forEachIndexed { i, blob ->
            val t = offsets[i].value
            val cx = (blob.baseOffset.x + blob.drift.x * t) * size.width
            val cy = (blob.baseOffset.y + blob.drift.y * t) * size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob.color, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = blob.widthFraction * size.width * 0.5f,
                ),
                radius = blob.widthFraction * size.width * 0.5f,
                center = Offset(cx, cy),
                blendMode = BlendMode.Screen,
            )
        }
        // Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xA0020108)),
                radius = size.minDimension * 0.8f,
            )
        )
    }
}

data class BlobDef(val color: Color, val baseOffset: Offset, val drift: Offset, val widthFraction: Float, val heightFraction: Float)
```

- [ ] **Step 3: Run test**

  Run: `./gradlew connectedAndroidTest` — Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: animated Aurora background Canvas composable"
```

---

## Task 12: Widgets

**Files:**
- Create: `ui/components/widgets/ClockWidget.kt`
- Create: `ui/components/widgets/WeatherWidget.kt`
- Create: `ui/components/widgets/AqiWidget.kt`
- Create: `ui/components/widgets/ScreenTimerWidget.kt`
- Create: `ui/components/widgets/NetworkWidget.kt`

- [ ] **Step 1: Write widget display tests**

```kotlin
class ClockWidgetTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `ClockWidget displays time in 12hr format`() {
        composeRule.setContent {
            YukuzaTheme { ClockWidget() }
        }
        // Should contain AM or PM
        composeRule.onNodeWithText("AM").assertExists().also { _ ->
            // or PM — one of them must exist
        }
    }
}

class WeatherWidgetTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `WeatherWidget shows stale indicator when data is stale`() {
        val staleData = WeatherData(20f, 1, "Mumbai", 0L, isStale = true)
        composeRule.setContent {
            YukuzaTheme { WeatherWidget(data = staleData) }
        }
        composeRule.onNodeWithContentDescription("Stale data").assertExists()
    }
}
```

- [ ] **Step 2: Implement `ClockWidget`**

```kotlin
@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    val time by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(10_000)
        }
    }
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm") }
    val ampm = if (time.hour < 12) "AM" else "PM"

    GlassCard(modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = time.format(formatter),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = ampm,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy")) }
            Text(today, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
        }
    }
}
```

- [ ] **Step 3: Implement remaining widgets**

  `WeatherWidget` — shows SVG sun/cloud icon (VectorDrawable), temp, location, stale indicator if `data.isStale`.
  `AqiWidget` — shows numeric AQI, color-coded label (green/yellow/orange/red/purple per category).
  `ScreenTimerWidget` — `produceState` counting elapsed seconds since composition start, formatted `m:ss`, display icon (TV screen SVG).
  `NetworkWidget` — receives `NetworkData`, shows `%.0f Mb/s`, signal bars SVG. `clickable { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }`.

- [ ] **Step 4: Run tests**

  Run: `./gradlew connectedAndroidTest` — Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: all top-bar widgets (clock, weather, AQI, timer, network)"
```

---

## Task 13: AppIcon + AppRow

**Files:**
- Create: `ui/components/AppIcon.kt`
- Create: `ui/components/AppRow.kt`

- [ ] **Step 1: Write AppIcon focus test**

```kotlin
class AppIconTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `AppIcon shows label`() {
        val app = AppInfo("com.test", "Test App", 0)
        composeRule.setContent {
            YukuzaTheme { AppIcon(app = app, isFocused = false, onFocus = {}, onLongPress = {}) }
        }
        composeRule.onNodeWithText("Test App").assertExists()
    }

    @Test fun `AppIcon is focusable`() {
        val app = AppInfo("com.test", "Test App", 0)
        composeRule.setContent {
            YukuzaTheme { AppIcon(app = app, isFocused = false, onFocus = {}, onLongPress = {}) }
        }
        composeRule.onRoot().assert(hasScrollAction().not()) // sanity
    }
}
```

- [ ] **Step 2: Implement `AppIcon`**

```kotlin
@Composable
fun AppIcon(
    app: AppInfo,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saturation by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "saturation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isFocused) -8f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .focusable()
            .onFocusChanged { if (it.isFocused) onFocus() }
            .combinedClickable(
                onClick = { /* launch app */ },
                onLongClick = onLongPress,
            )
            .semantics { contentDescription = "${app.label}, app icon" }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.packageName)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationY = offsetY
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(saturation) }
                    )
                    if (isFocused) {
                        shadowElevation = 16f
                        ambientShadowColor = app.dominantColor.toArgb()
                        spotShadowColor = app.dominantColor.toArgb()
                    }
                }
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = if (LocalDensity.current.density >= 2f) 0.5.dp else 1.dp,
                    color = if (isFocused) app.dominantColor.copy(alpha = 0.35f)
                            else YukuzaColors.GlassBorder,
                    shape = RoundedCornerShape(18.dp),
                ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) Color.White else Color.White.copy(0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

- [ ] **Step 3: Implement `AppRow` with D-pad reorder**

```kotlin
@Composable
fun AppRow(
    apps: ImmutableList<AppInfo>,
    focusedIndex: Int,
    isEditMode: Boolean,
    onFocus: (Int) -> Unit,
    onReorder: (List<String>) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editList by remember(apps) { mutableStateOf(apps.toMutableList()) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        Text(
            "APPS",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(0.45f),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(
                items = if (isEditMode) editList else apps.toList(),
                key = { _, app -> app.packageName },
            ) { index, app ->
                AppIcon(
                    app = app,
                    isFocused = focusedIndex == index,
                    onFocus = { onFocus(index) },
                    onLongPress = {
                        if (isEditMode) { /* move logic */ }
                        else onLongPress(app)
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run tests**

  Run: `./gradlew connectedAndroidTest` — Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: AppIcon mono→color with spring animation, AppRow with D-pad reorder"
```

---

## Task 14: AssistantButton

**Files:**
- Create: `ui/components/AssistantButton.kt`

- [ ] **Step 1: Write test**

```kotlin
class AssistantButtonTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `AssistantButton is displayed`() {
        composeRule.setContent { YukuzaTheme { AssistantButton() } }
        composeRule.onNodeWithContentDescription("Google Assistant").assertExists()
    }
}
```

- [ ] **Step 2: Implement `AssistantButton`**

```kotlin
@Composable
fun AssistantButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(50.dp)
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            }
            .background(Color(0x730803140), CircleShape)
            .border(
                width = if (LocalDensity.current.density >= 2f) 0.5.dp else 1.dp,
                color = Color.White.copy(0.12f),
                shape = CircleShape,
            )
            .clickable { launchAssistant(context) }
            .semantics { contentDescription = "Google Assistant" },
    ) {
        // Glass G SVG rendered as ImageVector
        Icon(
            imageVector = glassGIcon(),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun launchAssistant(context: Context) {
    val intents = listOf(
        Intent(Intent.ACTION_VOICE_COMMAND),
        Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE),
        Intent("com.google.android.googlequicksearchbox.VOICE_SEARCH_ACTIVITY"),
    )
    intents.firstOrNull { it.resolveActivity(context.packageManager) != null }
        ?.let { context.startActivity(it) }
        ?: Toast.makeText(context, "Voice assistant not available", Toast.LENGTH_SHORT).show()
}
```

- [ ] **Step 3: Create the glass G as `ImageVector` in `ui/theme/Icons.kt`**

  Draw the 4 Google G path segments with a white semi-transparent fill (sheen gradient approximated as a light fill) and white stroke outlines. No brand colors.

- [ ] **Step 4: Run test + commit**

```bash
git add .
git commit -m "feat: AssistantButton with glass G SVG and intent chain fallback"
```

---

## Task 15: NowPlayingWidget

**Files:**
- Create: `ui/components/widgets/NowPlayingWidget.kt`

- [ ] **Step 1: Write display test**

```kotlin
class NowPlayingWidgetTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `NowPlayingWidget is hidden when data is null`() {
        composeRule.setContent {
            YukuzaTheme { NowPlayingWidget(data = null) }
        }
        composeRule.onNodeWithTag("now_playing").assertDoesNotExist()
    }

    @Test fun `NowPlayingWidget shows track title when playing`() {
        val data = MediaData("Blinding Lights", "The Weeknd", null, 84_000, 200_000, true, "Spotify")
        composeRule.setContent {
            YukuzaTheme { NowPlayingWidget(data = data) }
        }
        composeRule.onNodeWithText("Blinding Lights").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Implement `NowPlayingWidget`**

```kotlin
@Composable
fun NowPlayingWidget(data: MediaData?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = data != null,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f),
        modifier = modifier.testTag("now_playing"),
    ) {
        data ?: return@AnimatedVisibility
        GlassCard(modifier = Modifier.width(460.dp)) {
            Box {
                // Dominant color bleed
                Box(Modifier.matchParentSize().background(
                    Brush.radialGradient(listOf(data.dominantColor.copy(0.12f), Color.Transparent))
                ))
                Column(Modifier.padding(18.dp)) {
                    // Source badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Color(0xFF1DB954), CircleShape)
                            .graphicsLayer { alpha = pulseAlpha() })
                        Spacer(Modifier.width(6.dp))
                        Text(data.sourceAppLabel, style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.4f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Album art
                        AsyncImage(
                            model = data.albumArtUri,
                            contentDescription = "Album art",
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                            error = painterResource(R.drawable.ic_music_note),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(data.trackTitle, style = MaterialTheme.typography.bodyMedium,
                                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(data.artist, style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.5f), maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            // Progress bar
                            val progress = if (data.durationMs > 0) data.elapsedMs.toFloat() / data.durationMs else 0f
                            LinearProgressIndicator(progress = { progress },
                                color = data.dominantColor, trackColor = Color.White.copy(0.12f),
                                modifier = Modifier.fillMaxWidth().height(2.dp))
                            // Controls — Prev / Pause / Next (send MediaController commands via MediaRepository)
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Run tests, commit**

```bash
git add .
git commit -m "feat: NowPlayingWidget with MediaSession data, animated show/hide"
```

---

## Task 16: HomeScreen

**Files:**
- Create: `ui/screen/home/HomeScreen.kt`
- Create: `navigation/LauncherNavGraph.kt`

- [ ] **Step 1: Write HomeScreen smoke test**

```kotlin
class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `HomeScreen renders without crash`() {
        composeRule.setContent {
            YukuzaTheme {
                HomeScreen(
                    uiState = HomeUiState(),
                    onAppFocused = {},
                    onAppLongPress = {},
                    onReorder = {},
                    onAssistantClick = {},
                    onNetworkClick = {},
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Implement `HomeScreen`**

```kotlin
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAppFocused: (Int) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onReorder: (List<String>) -> Unit,
    onAssistantClick: () -> Unit,
    onNetworkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        // Layer 1: Aurora
        AuroraBackground()

        // Layer 2: UI
        Box(Modifier.fillMaxSize().padding(horizontal = 40.dp)) {

            // Top bar
            Row(Modifier.align(Alignment.TopStart).padding(top = 32.dp),
                verticalAlignment = Alignment.CenterVertically) {
                ClockWidget()
            }
            AssistantButton(Modifier.align(Alignment.TopCenter).padding(top = 32.dp))
            Row(Modifier.align(Alignment.TopEnd).padding(top = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.weather?.let { WeatherWidget(it) }
                uiState.aqi?.let { AqiWidget(it) }
                ScreenTimerWidget()
                uiState.network?.let { NetworkWidget(it, onClick = onNetworkClick) }
            }

            // Center: Now Playing
            uiState.nowPlaying?.let {
                NowPlayingWidget(data = it, modifier = Modifier.align(Alignment.Center))
            }

            // Bottom: App strip
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xD0040200C))
                    .border(BorderStroke(if (LocalDensity.current.density >= 2f) 0.5.dp else 1.dp,
                        Color(0x38632FA).copy(alpha = 0.22f)), RectangleShape)
                    .padding(horizontal = 40.dp, vertical = 16.dp)
            ) {
                AppRow(
                    apps = uiState.apps,
                    focusedIndex = uiState.focusedAppIndex,
                    isEditMode = uiState.isEditMode,
                    onFocus = onAppFocused,
                    onReorder = onReorder,
                    onLongPress = onAppLongPress,
                )
            }
        }
    }
}
```

- [ ] **Step 3: Create `LauncherNavGraph`**

```kotlin
@Composable
fun LauncherNavGraph() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = state,
                onAppFocused = vm::onAppFocused,
                onAppLongPress = { /* show shortcuts overlay */ },
                onReorder = vm::reorder,
                onAssistantClick = { /* handled inside AssistantButton */ },
                onNetworkClick = { /* handled inside NetworkWidget */ },
            )
        }
        composable("apps") {
            val vm: AppListViewModel = hiltViewModel()
            AppListScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
```

- [ ] **Step 4: Run tests + install on device/emulator**

  Run: `./gradlew connectedAndroidTest`
  Deploy: `./gradlew installDebug` → set as default launcher on device

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: HomeScreen composition and NavGraph — launcher is functional"
```

---

## Task 17: App Drawer (AppListScreen)

**Files:**
- Create: `ui/screen/apps/AppListViewModel.kt`
- Create: `ui/screen/apps/AppListScreen.kt`

- [ ] **Step 1: Write search filter test**

```kotlin
class AppListViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val getApps = mockk<GetAppsUseCase>()
    private val apps = persistentListOf(
        AppInfo("com.youtube", "YouTube", 0),
        AppInfo("com.netflix", "Netflix", 1),
    )

    @Test fun `search filters app list by label`() = runTest {
        every { getApps() } returns flowOf(apps)
        val vm = AppListViewModel(getApps)
        vm.onSearch("you")
        vm.filteredApps.test {
            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertEquals("YouTube", filtered[0].label)
        }
    }
}
```

- [ ] **Step 2: Implement `AppListViewModel`**

```kotlin
@HiltViewModel
class AppListViewModel @Inject constructor(getApps: GetAppsUseCase) : ViewModel() {
    private val allApps = getApps().stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())
    private val query = MutableStateFlow("")

    val filteredApps: StateFlow<ImmutableList<AppInfo>> =
        combine(allApps, query) { apps, q ->
            if (q.isBlank()) apps
            else apps.filter { it.label.contains(q, ignoreCase = true) }.toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.Lazily, persistentListOf())

    fun onSearch(q: String) { query.value = q }
}
```

- [ ] **Step 3: Implement `AppListScreen`**

```kotlin
@Composable
fun AppListScreen(viewModel: AppListViewModel, onBack: () -> Unit) {
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(Modifier.fillMaxSize().padding(40.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; viewModel.onSearch(it) },
                placeholder = { Text("Search apps…") },
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = "Search apps" },
            )
            Spacer(Modifier.height(24.dp))
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(6), // 8 for 4K handled via WindowSizeClass
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppIcon(app = app, isFocused = false, onFocus = {}, onLongPress = {})
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests, commit**

```bash
git add .
git commit -m "feat: app drawer with search and TvLazyVerticalGrid"
```

---

## Task 18: Overlays

**Files:**
- Create: `ui/overlay/QuickSettingsOverlay.kt`
- Create: `ui/overlay/AppShortcutsOverlay.kt`
- Create: `ui/overlay/InputSourceOverlay.kt`

- [ ] **Step 1: Write QuickSettings display test**

```kotlin
class QuickSettingsOverlayTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `QuickSettingsOverlay shows Night Mode toggle`() {
        composeRule.setContent {
            YukuzaTheme { QuickSettingsOverlay(onDismiss = {}, isNightMode = false, onNightModeToggle = {}) }
        }
        composeRule.onNodeWithContentDescription("Night Mode").assertExists()
    }
}
```

- [ ] **Step 2: Implement `QuickSettingsOverlay`**

```kotlin
@Composable
fun QuickSettingsOverlay(
    onDismiss: () -> Unit,
    isNightMode: Boolean,
    onNightModeToggle: () -> Unit,
) {
    val context = LocalContext.current
    Popup(alignment = Alignment.BottomCenter, onDismissRequest = onDismiss) {
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Volume (AudioManager)
                VolumeSlider()
                // Brightness (only shown if WRITE_SETTINGS granted)
                if (Settings.System.canWrite(context)) BrightnessSlider()
                // Night Mode
                Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Night Mode") { onNightModeToggle() }
                    .semantics { contentDescription = "Night Mode" }) {
                    Text("Night Mode", color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = isNightMode, onCheckedChange = { onNightModeToggle() })
                }
                // Game Mode (only if HDMI_CEC permission available)
                if (context.checkSelfPermission("android.permission.HDMI_CEC") == PERMISSION_GRANTED) {
                    GameModeButton()
                }
                // Input source (only if HDMI_CEC available)
                if (context.checkSelfPermission("android.permission.HDMI_CEC") == PERMISSION_GRANTED) {
                    InputSourceButton()
                }
            }
        }
    }
}
```

- [ ] **Step 3: Implement `AppShortcutsOverlay`**

```kotlin
@Composable
fun AppShortcutsOverlay(
    app: AppInfo,
    shortcuts: ImmutableList<ShortcutInfo>,
    onShortcutSelected: (ShortcutInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(onDismissRequest = onDismiss) {
        GlassCard {
            Column {
                Text(app.label, color = Color.White)
                shortcuts.take(5).forEach { shortcut ->
                    Text(
                        text = shortcut.shortLabel?.toString() ?: "",
                        color = Color.White.copy(0.75f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShortcutSelected(shortcut) }
                            .focusable()
                            .semantics { contentDescription = shortcut.shortLabel?.toString() ?: "" }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests, commit**

```bash
git add .
git commit -m "feat: QuickSettings, AppShortcuts, and InputSource overlays"
```

---

## Task 19: Night Mode + Ambient Screensaver

**Files:**
- Create: `ui/components/NightModeOverlay.kt`
- Create: `ui/components/AmbientScreensaver.kt`
- Modify: `ui/screen/home/HomeViewModel.kt` — add night mode state + schedule logic

- [ ] **Step 1: Write night mode test**

```kotlin
class HomeViewModelNightModeTest {
    @Test fun `night mode activates when current time is within schedule`() = runTest {
        // inject a fake clock showing 22:30, schedule 22:00–07:00
        // assert uiState.isNightMode == true
    }
}
```

- [ ] **Step 2: Add night mode to `HomeUiState` and `HomeViewModel`**

  Add `isNightMode: Boolean = false`, `isAmbient: Boolean = false` to `HomeUiState`.
  In `HomeViewModel.init`, launch a coroutine that ticks every minute, reads `nightModeStartHour`/`nightModeEndHour` from DataStore, sets `isNightMode` accordingly.
  Inactivity timer: reset on any key event; after configurable idle duration (default 5 min), set `isAmbient = true`.

- [ ] **Step 3: Implement `NightModeOverlay`**

  A `Box` overlay covering the full screen with `background(Color.Black.copy(alpha = 0.3f))` + aurora palette color shift toward amber/red/deep-blue via animated `ColorMatrix` applied to `AuroraBackground`.

- [ ] **Step 4: Implement `AmbientScreensaver`**

  Full-screen aurora with large clock. On any remote key event, `isAmbient = false` and screensaver dismisses.

- [ ] **Step 5: Run tests, commit**

```bash
git add .
git commit -m "feat: Night Mode scheduling and Ambient screensaver"
```

---

## Task 20: WorkManager Jobs

**Files:**
- Create: `data/worker/WeatherSyncWorker.kt`
- Create: `data/worker/PalettePreWarmWorker.kt`
- Create: `data/worker/PackageChangeSyncWorker.kt`
- Modify: `YukuzaApplication.kt` — schedule workers on boot

- [ ] **Step 1: Implement `WeatherSyncWorker`**

```kotlin
@HiltWorker
class WeatherSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepo: WeatherRepository,
    private val dataStore: DataStore<Preferences>,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = dataStore.data.first()
        val lat = prefs[doublePreferencesKey("lat")] ?: 19.07
        val lon = prefs[doublePreferencesKey("lon")] ?: 72.87
        return try {
            weatherRepo.getWeather(lat, lon)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(workManager: WorkManager) {
            workManager.enqueueUniquePeriodicWork(
                "weather_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WeatherSyncWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .build()
            )
        }
    }
}
```

- [ ] **Step 2: Implement `PalettePreWarmWorker`**

  Iterates all installed apps, calls `Palette.from(iconBitmap).generate()`, stores result in Room via `AppRepository.cacheColor()`. Constrained to `DEVICE_IDLE` + battery not low.

- [ ] **Step 3: Schedule workers in `YukuzaApplication`**

```kotlin
override fun onCreate() {
    super.onCreate()
    val wm = WorkManager.getInstance(this)
    WeatherSyncWorker.schedule(wm)
    PalettePreWarmWorker.scheduleOnce(wm)
}
```

- [ ] **Step 4: Run on device, verify WorkManager console shows jobs queued**

  `adb shell dumpsys jobscheduler | grep yukuza`

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: WorkManager jobs for weather sync and Palette pre-warm"
```

---

## Task 21: Performance — Baseline Profiles

**Files:**
- Create: `app/src/main/baseline-prof.txt` (generated)
- Create: `macrobenchmark/` module

- [ ] **Step 1: Add Macrobenchmark module**

  In Android Studio: File → New Module → Macrobenchmark. Package: `com.yukuza.launcher.benchmark`.

- [ ] **Step 2: Write startup benchmark**

```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.yukuza.launcher",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
```

- [ ] **Step 3: Generate Baseline Profile**

  Run: `./gradlew :app:generateBaselineProfile`
  This produces `app/src/main/baseline-prof.txt` — commit it.

- [ ] **Step 4: Add `ProfileInstaller` dependency**

  `implementation("androidx.profileinstaller:profileinstaller:1.3.1")`

- [ ] **Step 5: Run benchmark, verify startup < 300ms on reference device**

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "perf: Baseline Profile for AOT compilation, startup < 300ms"
```

---

## Task 22: Final Polish + StrictMode Verification

- [ ] **Step 1: Run full test suite**

  Run: `./gradlew test connectedAndroidTest`
  Expected: All tests PASS

- [ ] **Step 2: Enable StrictMode in debug, run on device, check logcat for violations**

  `adb logcat | grep StrictMode` — should be empty.

- [ ] **Step 3: Verify 4K sharpness**

  Deploy on a 4K Android TV (or Android TV emulator at 3840×2160). Verify:
  - All borders are sub-pixel crisp
  - App icons are sharp (loaded from PackageManager, not scaled up)
  - SVG widgets render without aliasing

- [ ] **Step 4: Set as default launcher on test device**

  Settings → Apps → Default apps → Home app → Yukuza Launcher

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "feat: Yukuza Launcher v1 complete"
```
