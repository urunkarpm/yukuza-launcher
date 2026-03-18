package com.yukuza.launcher.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test

class NetworkRepositoryTest {
    @Test
    fun `NetworkData has expected structure`() {
        // Just verify the data class is accessible and constructable
        val data = com.yukuza.launcher.domain.model.NetworkData(speedMbps = 0f, isConnected = false)
        assertNotNull(data)
    }
}
