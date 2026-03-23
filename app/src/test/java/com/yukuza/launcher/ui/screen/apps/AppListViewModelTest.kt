package com.yukuza.launcher.ui.screen.apps

import app.cash.turbine.test
import com.yukuza.launcher.data.repository.AppRepository
import com.yukuza.launcher.domain.model.AppInfo
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

    private val repository = mockk<AppRepository>(relaxed = true)
    private val visibleApps = persistentListOf(
        AppInfo("com.youtube", "YouTube", 0),
        AppInfo("com.netflix", "Netflix", 1),
    )

    @Test
    fun `search filters visible apps by label`() = runTest {
        every { repository.getVisibleApps() } returns flowOf(visibleApps)
        every { repository.getHiddenApps() } returns flowOf(persistentListOf())
        val vm = AppListViewModel(repository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        vm.onSearch("you")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        vm.visibleApps.test {
            val item = awaitItem()
            val filtered = if (item.isEmpty()) awaitItem() else item
            assertEquals(1, filtered.size)
            assertEquals("YouTube", filtered[0].label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty query returns all visible apps`() = runTest {
        every { repository.getVisibleApps() } returns flowOf(visibleApps)
        every { repository.getHiddenApps() } returns flowOf(persistentListOf())
        val vm = AppListViewModel(repository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        vm.visibleApps.test {
            val item = awaitItem()
            val all = if (item.isEmpty()) awaitItem() else item
            assertEquals(2, all.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
