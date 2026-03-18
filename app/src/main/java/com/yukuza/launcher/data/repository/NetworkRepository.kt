package com.yukuza.launcher.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import com.yukuza.launcher.domain.model.NetworkData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getNetworkSpeed(): Flow<NetworkData> = flow {
        var prev = TrafficStats.getTotalRxBytes()
        while (true) {
            delay(5_000)
            val curr = TrafficStats.getTotalRxBytes()
            val bytesDelta = (curr - prev).coerceAtLeast(0)
            val mbps = (bytesDelta * 8f) / (5f * 1_000_000f)
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            @Suppress("DEPRECATION")
            val connected = cm.activeNetworkInfo?.isConnected == true
            emit(NetworkData(speedMbps = mbps, isConnected = connected))
            prev = curr
        }
    }.flowOn(Dispatchers.IO)
}
