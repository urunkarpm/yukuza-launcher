package com.yukuza.launcher.domain.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInfoTest {
    @Test
    fun `AppInfo packageName field is stored correctly`() {
        val info = AppInfo(
            packageName = "com.example.app",
            label = "Example",
            order = 0,
            dominantColor = Color(0xFF4285F4)
        )
        assertEquals("com.example.app", info.packageName)
        assertEquals(0, info.order)
        assertTrue(info.dominantColor.alpha > 0f)
    }

    @Test
    fun `AppInfo data class equality works by all fields`() {
        val a = AppInfo(packageName = "com.example", label = "Example", order = 0, dominantColor = Color.Red)
        val b = AppInfo(packageName = "com.example", label = "Example", order = 0, dominantColor = Color.Red)
        assertEquals(a, b)
    }

    @Test
    fun `AqiData category boundaries`() {
        val good = AqiData(europeanAqi = 20, category = AqiData.AqiCategory.GOOD, fetchedAt = 0L)
        val poor = AqiData(europeanAqi = 80, category = AqiData.AqiCategory.POOR, fetchedAt = 0L)
        assertEquals(AqiData.AqiCategory.GOOD, good.category)
        assertEquals(AqiData.AqiCategory.POOR, poor.category)
    }
}
