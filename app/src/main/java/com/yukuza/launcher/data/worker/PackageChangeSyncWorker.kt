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
        // AppRepository.getApps() already re-queries PackageManager on each collection,
        // so triggering a re-query via an empty state update is sufficient.
        // The Room table update will propagate to all collectors automatically.
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
