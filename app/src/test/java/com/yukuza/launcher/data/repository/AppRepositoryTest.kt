package com.yukuza.launcher.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.yukuza.launcher.data.db.AppColorCacheDao
import com.yukuza.launcher.data.db.AppOrderDao
import com.yukuza.launcher.data.entity.AppOrderEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRepositoryTest {
    private val pm = mockk<PackageManager>()
    private val appOrderDao = mockk<AppOrderDao>()
    private val colorCacheDao = mockk<AppColorCacheDao>(relaxed = true)
    private val repo = AppRepository(pm, appOrderDao, colorCacheDao)

    @Test
    fun `merges PackageManager apps with stored order`() = runTest {
        val storedOrder = listOf(
            AppOrderEntity("com.b", 0),
            AppOrderEntity("com.a", 1),
        )
        coEvery { appOrderDao.getAll() } returns flowOf(storedOrder)

        val appInfoA = ApplicationInfo().apply { packageName = "com.a"; flags = 0 }
        val appInfoB = ApplicationInfo().apply { packageName = "com.b"; flags = 0 }
        every { pm.getInstalledApplications(any<Int>()) } returns listOf(appInfoA, appInfoB)
        every { pm.getApplicationLabel(appInfoA) } returns "App A"
        every { pm.getApplicationLabel(appInfoB) } returns "App B"
        coEvery { colorCacheDao.get(any()) } returns null

        val apps = repo.getApps().first()
        assertEquals("com.b", apps[0].packageName)
        assertEquals("com.a", apps[1].packageName)
    }

    @Test
    fun `new apps not in stored order get appended`() = runTest {
        coEvery { appOrderDao.getAll() } returns flowOf(emptyList())

        val appInfo = ApplicationInfo().apply { packageName = "com.new"; flags = 0 }
        every { pm.getInstalledApplications(any<Int>()) } returns listOf(appInfo)
        every { pm.getApplicationLabel(appInfo) } returns "New App"
        coEvery { colorCacheDao.get(any()) } returns null

        val apps = repo.getApps().first()
        assertEquals(1, apps.size)
        assertEquals("com.new", apps[0].packageName)
    }
}
