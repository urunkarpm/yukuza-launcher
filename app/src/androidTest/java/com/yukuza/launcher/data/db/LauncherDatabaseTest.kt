package com.yukuza.launcher.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherDatabaseTest {
    private lateinit var db: LauncherDatabase
    private lateinit var appOrderDao: AppOrderDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LauncherDatabase::class.java
        ).build()
        appOrderDao = db.appOrderDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveAppOrder() = runTest {
        val entity = AppOrderEntity(packageName = "com.test.app", order = 0)
        appOrderDao.upsert(entity)
        val result = appOrderDao.getAll().first()
        assertEquals(1, result.size)
        assertEquals("com.test.app", result[0].packageName)
    }

    @Test
    fun upsertUpdatesOrder() = runTest {
        appOrderDao.upsert(AppOrderEntity("com.test.app", 0))
        appOrderDao.upsert(AppOrderEntity("com.test.app", 5))
        val result = appOrderDao.getAll().first()
        assertEquals(5, result[0].order)
    }
}
