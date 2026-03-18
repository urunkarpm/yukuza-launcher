package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.WeatherRepository
import com.yukuza.launcher.domain.model.WeatherData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWeatherUseCaseTest {
    private val repo = mockk<WeatherRepository>()
    private val useCase = GetWeatherUseCase(repo)

    @Test
    fun `delegates to repository and returns data`() = runTest {
        val data = WeatherData(
            tempCelsius = 24f,
            conditionCode = 1,
            locationName = "Mumbai",
            fetchedAt = System.currentTimeMillis(),
            isStale = false
        )
        coEvery { repo.getWeather(any(), any()) } returns data

        val result = useCase(19.07, 72.87)
        assertEquals(data, result)
    }
}
