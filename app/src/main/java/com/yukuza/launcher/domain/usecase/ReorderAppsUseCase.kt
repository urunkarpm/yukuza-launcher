package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.AppRepository
import javax.inject.Inject

class ReorderAppsUseCase @Inject constructor(private val repo: AppRepository) {
    suspend operator fun invoke(packages: List<String>) = repo.reorder(packages)
}
