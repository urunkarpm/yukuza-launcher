package com.yukuza.launcher

import android.app.Application
import android.os.StrictMode
import androidx.work.WorkManager
import com.yukuza.launcher.data.worker.PalettePreWarmWorker
import com.yukuza.launcher.data.worker.WeatherSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YukuzaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        val wm = WorkManager.getInstance(this)
        WeatherSyncWorker.schedule(wm)
        PalettePreWarmWorker.scheduleOnce(wm)
    }
}
