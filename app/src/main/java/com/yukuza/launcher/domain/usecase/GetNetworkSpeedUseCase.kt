package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.NetworkRepository
import com.yukuza.launcher.domain.model.NetworkData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetworkSpeedUseCase @Inject constructor(private val repo: NetworkRepository) {
    operator fun invoke(): Flow<NetworkData> = repo.getNetworkSpeed()
}
