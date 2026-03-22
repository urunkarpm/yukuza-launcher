package com.yukuza.launcher.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.yukuza.launcher.data.db.AppColorCacheDao
import com.yukuza.launcher.data.db.AppLaunchCountDao
import com.yukuza.launcher.data.db.AppOrderDao
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppRepositoryTest {
    private val context = mockk<Context>(relaxed = true)
    private val pm = mockk<PackageManager>(relaxed = true)
    private val appOrderDao = mockk<AppOrderDao>(relaxed = true)
    private val colorCacheDao = mockk<AppColorCacheDao>(relaxed = true)
    private val launchCountDao = mockk<AppLaunchCountDao>(relaxed = true)
    private val repo = AppRepository(context, pm, appOrderDao, colorCacheDao, launchCountDao)

    @Test
    fun `repository can be instantiated`() {
        assertNotNull(repo)
    }
}
