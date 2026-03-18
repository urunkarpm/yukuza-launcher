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
import kotlinx.coroutines.flow.onStart
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

    fun getApps(): Flow<ImmutableList<AppInfo>> =
        appOrderDao.getAll().combine(countRefresh) { order, _ -> order }.map { _ ->
            val installed = queryLaunchableApps()
            val counts = launchCountDao.getAll().associateBy({ it.packageName }, { it.count })

            val tvSettings = AppInfo(
                packageName = PACKAGE_TV_SETTINGS,
                label = "Settings",
                order = Int.MAX_VALUE,
                dominantColor = Color(0xFF607D8B),
                isPinned = true,
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
                    )
                }
                .sortedByDescending { it.order }

            (listOf(tvSettings) + regularApps).toImmutableList()
        }.flowOn(Dispatchers.IO)

    suspend fun reorder(packages: List<String>) = withContext(Dispatchers.IO) {
        appOrderDao.upsertAll(packages.mapIndexed { i, pkg -> AppOrderEntity(pkg, i) })
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
