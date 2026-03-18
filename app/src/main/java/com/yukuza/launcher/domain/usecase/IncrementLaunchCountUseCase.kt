package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.AppRepository
import javax.inject.Inject

class IncrementLaunchCountUseCase @Inject constructor(private val repo: AppRepository) {
    suspend operator fun invoke(packageName: String) = repo.incrementLaunchCount(packageName)
}
