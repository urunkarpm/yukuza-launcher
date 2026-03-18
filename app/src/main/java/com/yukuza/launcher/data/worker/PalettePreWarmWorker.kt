package com.yukuza.launcher.data.worker

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.hilt.work.HiltWorker
import androidx.palette.graphics.Palette
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yukuza.launcher.data.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class PalettePreWarmWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val appRepository: AppRepository,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pm = applicationContext.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }

            for (appInfo in apps) {
                try {
                    val icon = pm.getApplicationIcon(appInfo.packageName)
                    val bitmap = (icon as? BitmapDrawable)?.bitmap ?: continue
                    val palette = Palette.from(bitmap).generate()
                    val dominant = palette.getDominantColor(0xFFFFFFFF.toInt())
                    appRepository.cacheColor(
                        appInfo.packageName,
                        androidx.compose.ui.graphics.Color(dominant)
                    )
                } catch (e: Exception) {
                    // Skip this app if icon extraction fails
                    continue
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun scheduleOnce(workManager: WorkManager) {
            workManager.enqueueUniqueWork(
                "palette_prewarm",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PalettePreWarmWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .setRequiresDeviceIdle(true)
                            .build()
                    )
                    .build()
            )
        }
    }
}
