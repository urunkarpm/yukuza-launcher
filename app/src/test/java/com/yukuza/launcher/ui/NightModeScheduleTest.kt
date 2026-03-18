package com.yukuza.launcher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightModeScheduleTest {

    private fun isNightModeActive(currentHour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            // Wraps around midnight (e.g., 22–7)
            currentHour >= startHour || currentHour < endHour
        }
    }

    @Test
    fun `night mode active at 23h with schedule 22 to 7`() {
        assertTrue(isNightModeActive(23, 22, 7))
    }

    @Test
    fun `night mode active at 02h with schedule 22 to 7`() {
        assertTrue(isNightModeActive(2, 22, 7))
    }

    @Test
    fun `night mode inactive at 08h with schedule 22 to 7`() {
        assertFalse(isNightModeActive(8, 22, 7))
    }

    @Test
    fun `night mode inactive at 12h with schedule 22 to 7`() {
        assertFalse(isNightModeActive(12, 22, 7))
    }
}
