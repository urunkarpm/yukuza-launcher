package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.UpdateRepository
import com.yukuza.launcher.domain.model.UpdateInfo
import javax.inject.Inject

class CheckUpdateUseCase @Inject constructor(private val repo: UpdateRepository) {
    suspend operator fun invoke(): UpdateInfo? = repo.checkForUpdate()
}
