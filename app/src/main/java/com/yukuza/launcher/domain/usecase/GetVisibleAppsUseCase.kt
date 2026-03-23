package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.AppRepository
import com.yukuza.launcher.domain.model.AppInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVisibleAppsUseCase @Inject constructor(private val repo: AppRepository) {
    operator fun invoke(): Flow<ImmutableList<AppInfo>> = repo.getVisibleApps()
}
