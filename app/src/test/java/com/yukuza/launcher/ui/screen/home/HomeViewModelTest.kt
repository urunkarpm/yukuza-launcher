package com.yukuza.launcher.ui.screen.home

import app.cash.turbine.test
import com.yukuza.launcher.domain.model.AqiData
import com.yukuza.launcher.domain.model.NetworkData
import com.yukuza.launcher.domain.model.WeatherData
import com.yukuza.launcher.domain.usecase.GetAqiUseCase
import com.yukuza.launcher.domain.usecase.GetAppsUseCase
import com.yukuza.launcher.domain.usecase.GetMediaSessionUseCase
import com.yukuza.launcher.domain.usecase.GetNetworkSpeedUseCase
import com.yukuza.launcher.domain.usecase.GetWeatherUseCase
import com.yukuza.launcher.domain.usecase.ReorderAppsUseCase
import com.yukuza.launcher.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getApps = mockk<GetAppsUseCase>()
    private val reorderApps = mockk<ReorderAppsUseCase>(relaxed = true)
    private val getWeather = mockk<GetWeatherUseCase>()
    private val getAqi = mockk<GetAqiUseCase>()
    private val getNetwork = mockk<GetNetworkSpeedUseCase>()
    private val getMedia = mockk<GetMediaSessionUseCase>()

    private lateinit var vm: HomeViewModel

    @Before
    fun setup() {
        every { getApps() } returns flowOf(persistentListOf())
        coEvery { getWeather(any(), any()) } returns WeatherData(24f, 1, "Mumbai", 0L, false)
        coEvery { getAqi(any(), any()) } returns AqiData(42, AqiData.AqiCategory.GOOD, 0L, false)
        every { getNetwork() } returns flowOf(NetworkData(87f, true))
        every { getMedia() } returns flowOf(null)
        vm = HomeViewModel(getApps, reorderApps, getWeather, getAqi, getNetwork, getMedia)
    }

    @Test
    fun `apps state starts empty`() = runTest {
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.apps.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weather state reflects repository data after init`() = runTest {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(24f, state.weather?.tempCelsius)
    }
}
