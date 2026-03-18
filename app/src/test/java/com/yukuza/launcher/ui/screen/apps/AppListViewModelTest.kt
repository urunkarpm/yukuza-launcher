package com.yukuza.launcher.ui.screen.apps

import app.cash.turbine.test
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.domain.usecase.GetAppsUseCase
import com.yukuza.launcher.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getApps = mockk<GetAppsUseCase>()
    private val apps = persistentListOf(
        AppInfo("com.youtube", "YouTube", 0),
        AppInfo("com.netflix", "Netflix", 1),
    )

    @Test
    fun `search filters app list by label`() = runTest {
        every { getApps() } returns flowOf(apps)
        val vm = AppListViewModel(getApps)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        vm.onSearch("you")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        vm.filteredApps.test {
            val item = awaitItem()
            val filtered = if (item.isEmpty()) awaitItem() else item
            assertEquals(1, filtered.size)
            assertEquals("YouTube", filtered[0].label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty query returns all apps`() = runTest {
        every { getApps() } returns flowOf(apps)
        val vm = AppListViewModel(getApps)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        vm.filteredApps.test {
            val item = awaitItem()
            val all = if (item.isEmpty()) awaitItem() else item
            assertEquals(2, all.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
