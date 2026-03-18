package com.yukuza.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.yukuza.launcher.data.db.AppColorCacheDao
import com.yukuza.launcher.data.db.AppOrderDao
import com.yukuza.launcher.data.entity.AppColorCacheEntity
import com.yukuza.launcher.data.entity.AppOrderEntity
import com.yukuza.launcher.domain.model.AppInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pm: PackageManager,
    private val appOrderDao: AppOrderDao,
    private val colorCacheDao: AppColorCacheDao,
) {
    private fun queryLaunchableApps(): Map<String, ResolveInfo> {
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val standard = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return (pm.queryIntentActivities(leanback, 0) + pm.queryIntentActivities(standard, 0))
            .filter { it.activityInfo.packageName != context.packageName }
            .associateBy { it.activityInfo.packageName }
    }

    fun getApps(): Flow<ImmutableList<AppInfo>> =
        appOrderDao.getAll().map { storedOrder ->
            val installed = queryLaunchableApps()

            val orderedPackages = storedOrder.map { it.packageName }
            val newPackages = installed.keys.filter { it !in orderedPackages }

            (orderedPackages + newPackages)
                .filter { it in installed }
                .mapIndexed { index, pkg ->
                    val resolveInfo = installed[pkg]!!
                    val color = colorCacheDao.get(pkg)?.let { Color(it.dominantColor) } ?: Color.White
                    AppInfo(
                        packageName = pkg,
                        label = resolveInfo.loadLabel(pm).toString(),
                        order = index,
                        dominantColor = color,
                    )
                }.toImmutableList()
        }.flowOn(Dispatchers.IO)

    suspend fun reorder(packages: List<String>) = withContext(Dispatchers.IO) {
        appOrderDao.upsertAll(packages.mapIndexed { i, pkg -> AppOrderEntity(pkg, i) })
    }

    suspend fun cacheColor(packageName: String, color: Color) = withContext(Dispatchers.IO) {
        colorCacheDao.upsert(
            AppColorCacheEntity(packageName, color.toArgb(), System.currentTimeMillis())
        )
    }
}
