package com.yukuza.launcher.data.repository

import com.yukuza.launcher.data.db.WeatherCacheDao
import com.yukuza.launcher.data.entity.WeatherCacheEntity
import com.yukuza.launcher.data.remote.OpenMeteoApi
import com.yukuza.launcher.data.remote.dto.AqiResponse
import com.yukuza.launcher.data.remote.dto.WeatherResponse
import com.yukuza.launcher.domain.model.AqiData.AqiCategory
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class WeatherRepositoryTest {
    private val api = mockk<OpenMeteoApi>()
    private val dao = mockk<WeatherCacheDao>(relaxed = true)
    private val repo = WeatherRepository(api, dao)

    @Test
    fun `returns cached data with stale flag when network fails`() = runTest {
        val cached = WeatherCacheEntity(
            tempCelsius = 20f,
            conditionCode = 1,
            locationName = "Mumbai",
            europeanAqi = 42,
            fetchedAt = 0L
        )
        coEvery { dao.get() } returns cached
        coEvery { api.getForecast(any(), any()) } throws IOException()
        coEvery { api.getAirQuality(any(), any()) } throws IOException()

        val result = repo.getWeather(19.07, 72.87)

        assertTrue(result.isStale)
        assertEquals(20f, result.tempCelsius)
    }

    @Test
    fun `maps WMO code 1 to Mainly Clear`() {
        assertEquals("Mainly Clear", WeatherRepository.wmoCodeToDescription(1))
    }

    @Test
    fun `maps WMO code 0 to Clear Sky`() {
        assertEquals("Clear Sky", WeatherRepository.wmoCodeToDescription(0))
    }

    @Test
    fun `aqiToCategory returns GOOD for aqi 20 or below`() {
        assertEquals(AqiCategory.GOOD, WeatherRepository.aqiToCategory(20))
        assertEquals(AqiCategory.GOOD, WeatherRepository.aqiToCategory(0))
    }

    @Test
    fun `aqiToCategory returns VERY_POOR for aqi above 80`() {
        assertEquals(AqiCategory.VERY_POOR, WeatherRepository.aqiToCategory(81))
    }
}
